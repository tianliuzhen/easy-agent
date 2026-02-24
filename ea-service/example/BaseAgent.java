package com.aaa.easyagent.biz.agent.zexample;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 抽象基类，用于管理代理状态和执行。
 * 提供状态转换、内存管理和基于步骤的执行循环的基础功能。
 * 子类必须实现 `step` 方法。
 */
public abstract class BaseAgent {

    // 核心属性
    private final String name;
    private String description;

    // 提示
    private String systemPrompt;
    private String nextStepPrompt;

    // 依赖
    private LLM llm;
    private Memory memory;
    private AgentState state;

    // 执行控制
    private final int maxSteps;
    private final AtomicInteger currentStep;
    private final int duplicateThreshold;

    // 配置标志
    private final boolean allowArbitraryTypes;
    private final String extraConfig;

    /**
     * 构造函数
     * @param builder 构建器对象
     */
    protected BaseAgent(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.systemPrompt = builder.systemPrompt;
        this.nextStepPrompt = builder.nextStepPrompt;
        this.llm = builder.llm;
        this.memory = builder.memory;
        this.state = builder.state;
        this.maxSteps = builder.maxSteps;
        this.currentStep = new AtomicInteger(builder.currentStep);
        this.duplicateThreshold = builder.duplicateThreshold;
        this.allowArbitraryTypes = builder.allowArbitraryTypes;
        this.extraConfig = builder.extraConfig;

        // 初始化代理
        initializeAgent();
    }

    /**
     * 如果未提供，使用默认设置初始化代理
     */
    private void initializeAgent() {
        if (this.llm == null) {
            this.llm = new LLM(this.name.toLowerCase());
        }
        if (this.memory == null) {
            this.memory = new Memory();
        }
    }

    /**
     * 用于安全代理状态转换的上下文管理器内部类
     */
    public static class StateContext implements AutoCloseable {
        private final BaseAgent agent;
        private final AgentState previousState;

        public StateContext(BaseAgent agent, AgentState newState) {
            this.agent = agent;
            this.previousState = agent.state;

            if (newState == null) {
                throw new IllegalArgumentException("Invalid state: null");
            }

            agent.state = newState;
        }

        @Override
        public void close() {
            agent.state = previousState; // 恢复到之前的状态
        }
    }

    /**
     * 创建状态上下文
     * @param newState 要转换到的新状态
     * @return StateContext 对象
     */
    public StateContext stateContext(AgentState newState) {
        return new StateContext(this, newState);
    }

    /**
     * 向代理的内存添加消息
     * @param role 消息发送者的角色
     * @param content 消息内容
     * @param base64Image 可选的base64编码图像
     * @param kwargs 附加参数
     */
    public void updateMemory(String role, String content, String base64Image, Map<String, Object> kwargs) {
        if (kwargs == null) {
            kwargs = new HashMap<>();
        }

        Message message;
        switch (role) {
            case "user":
                message = Message.userMessage(content, base64Image);
                break;
            case "system":
                message = Message.systemMessage(content, base64Image);
                break;
            case "assistant":
                message = Message.assistantMessage(content, base64Image);
                break;
            case "tool":
                // 工具消息可能有额外的参数
                if (base64Image != null) {
                    kwargs.put("base64Image", base64Image);
                }
                message = Message.toolMessage(content, kwargs);
                break;
            default:
                throw new IllegalArgumentException("Unsupported message role: " + role);
        }

        this.memory.addMessage(message);
    }

    /**
     * 重载方法：不带kwargs的updateMemory
     */
    public void updateMemory(String role, String content, String base64Image) {
        updateMemory(role, content, base64Image, new HashMap<>());
    }

    /**
     * 重载方法：不带base64Image的updateMemory
     */
    public void updateMemory(String role, String content) {
        updateMemory(role, content, null, new HashMap<>());
    }

    /**
     * 执行代理的主循环
     * @param request 可选的初始用户请求
     * @return 执行结果的字符串摘要
     * @throws RuntimeException 如果代理在开始时不是IDLE状态
     */
    public String run(String request) {
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state: " + this.state);
        }

        if (request != null && !request.isEmpty()) {
            updateMemory("user", request);
        }

        List<String> results = new ArrayList<>();

        try (StateContext context = stateContext(AgentState.RUNNING)) {
            while (currentStep.get() < maxSteps && this.state != AgentState.FINISHED) {
                currentStep.incrementAndGet();
                System.out.println("Executing step " + currentStep.get() + "/" + maxSteps);

                try {
                    String stepResult = step();

                    // 检查是否卡住
                    if (isStuck()) {
                        handleStuckState();
                    }

                    results.add("Step " + currentStep.get() + ": " + stepResult);
                } catch (Exception e) {
                    this.state = AgentState.ERROR;
                    throw new RuntimeException("Error during step execution", e);
                }
            }

            if (currentStep.get() >= maxSteps) {
                currentStep.set(0);
                this.state = AgentState.IDLE;
                results.add("Terminated: Reached max steps (" + maxSteps + ")");
            }
        }

        // 清理沙箱客户端
        SandboxClient.getInstance().cleanup();

        return results.isEmpty() ? "No steps executed" : String.join("\n", results);
    }

    /**
     * 执行代理工作流中的单个步骤
     * 必须由子类实现以定义特定行为
     * @return 步骤执行结果
     */
    public abstract String step();

    /**
     * 处理卡住状态，通过添加提示来改变策略
     */
    public void handleStuckState() {
        String stuckPrompt = "Observed duplicate responses. Consider new strategies and avoid repeating ineffective paths already attempted.";
        this.nextStepPrompt = stuckPrompt + "\n" + this.nextStepPrompt;
        System.out.println("Agent detected stuck state. Added prompt: " + stuckPrompt);
    }

    /**
     * 通过检测重复内容检查代理是否卡在循环中
     * @return 如果卡住返回true，否则返回false
     */
    public boolean isStuck() {
        if (this.memory.getMessages().size() < 2) {
            return false;
        }

        Message lastMessage = this.memory.getMessages().get(this.memory.getMessages().size() - 1);
        if (lastMessage.getContent() == null || lastMessage.getContent().isEmpty()) {
            return false;
        }

        // 计算相同内容出现的次数
        int duplicateCount = 0;
        List<Message> messages = this.memory.getMessages();

        // 从倒数第二条消息开始向前检查
        for (int i = messages.size() - 2; i >= 0; i--) {
            Message msg = messages.get(i);
            if ("assistant".equals(msg.getRole()) &&
                    lastMessage.getContent().equals(msg.getContent())) {
                duplicateCount++;
            }
        }

        return duplicateCount >= duplicateThreshold;
    }

    // Getter 和 Setter 方法

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getNextStepPrompt() {
        return nextStepPrompt;
    }

    public void setNextStepPrompt(String nextStepPrompt) {
        this.nextStepPrompt = nextStepPrompt;
    }

    public LLM getLlm() {
        return llm;
    }

    public void setLlm(LLM llm) {
        this.llm = llm;
    }

    public Memory getMemory() {
        return memory;
    }

    public void setMemory(Memory memory) {
        this.memory = memory;
    }

    public AgentState getState() {
        return state;
    }

    public void setState(AgentState state) {
        this.state = state;
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    public int getCurrentStep() {
        return currentStep.get();
    }

    public void setCurrentStep(int step) {
        currentStep.set(step);
    }

    public int getDuplicateThreshold() {
        return duplicateThreshold;
    }

    /**
     * 从代理内存中检索消息列表
     * @return 消息列表
     */
    public List<Message> getMessages() {
        return this.memory.getMessages();
    }

    /**
     * 设置代理内存中的消息列表
     * @param messages 消息列表
     */
    public void setMessages(List<Message> messages) {
        this.memory.setMessages(messages);
    }

    /**
     * 构建器模式内部类
     */
    public static abstract class Builder<T extends Builder<T>> {
        private String name;
        private String description;
        private String systemPrompt;
        private String nextStepPrompt;
        private LLM llm;
        private Memory memory = new Memory();
        private AgentState state = AgentState.IDLE;
        private int maxSteps = 10;
        private int currentStep = 0;
        private int duplicateThreshold = 2;
        private boolean allowArbitraryTypes = true;
        private String extraConfig = "allow";

        public T name(String name) {
            this.name = name;
            return self();
        }

        public T description(String description) {
            this.description = description;
            return self();
        }

        public T systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return self();
        }

        public T nextStepPrompt(String nextStepPrompt) {
            this.nextStepPrompt = nextStepPrompt;
            return self();
        }

        public T llm(LLM llm) {
            this.llm = llm;
            return self();
        }

        public T memory(Memory memory) {
            this.memory = memory;
            return self();
        }

        public T state(AgentState state) {
            this.state = state;
            return self();
        }

        public T maxSteps(int maxSteps) {
            this.maxSteps = maxSteps;
            return self();
        }

        public T currentStep(int currentStep) {
            this.currentStep = currentStep;
            return self();
        }

        public T duplicateThreshold(int duplicateThreshold) {
            this.duplicateThreshold = duplicateThreshold;
            return self();
        }

        protected abstract T self();

        public abstract BaseAgent build();
    }
}

/**
 * 代理状态枚举
 */
enum AgentState {
    IDLE,       // 空闲状态
    RUNNING,    // 运行中
    FINISHED,   // 已完成
    ERROR       // 错误状态
}

/**
 * 消息类
 */
class Message {
    private String role;
    private String content;
    private String base64Image;
    private Map<String, Object> metadata;

    private Message(String role, String content, String base64Image, Map<String, Object> metadata) {
        this.role = role;
        this.content = content;
        this.base64Image = base64Image;
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    // 工厂方法
    public static Message userMessage(String content, String base64Image) {
        return new Message("user", content, base64Image, null);
    }

    public static Message systemMessage(String content, String base64Image) {
        return new Message("system", content, base64Image, null);
    }

    public static Message assistantMessage(String content, String base64Image) {
        return new Message("assistant", content, base64Image, null);
    }

    public static Message toolMessage(String content, Map<String, Object> kwargs) {
        return new Message("tool", content, null, kwargs);
    }

    // Getter方法
    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public String getBase64Image() {
        return base64Image;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}

/**
 * 内存类，用于存储和管理消息
 */
class Memory {
    private List<Message> messages;

    public Memory() {
        this.messages = new CopyOnWriteArrayList<>();
    }

    public void addMessage(Message message) {
        this.messages.add(message);
    }

    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }

    public void setMessages(List<Message> messages) {
        this.messages = new CopyOnWriteArrayList<>(messages);
    }
}

/**
 * LLM类（语言模型）
 */
class LLM {
    private String configName;

    public LLM(String configName) {
        this.configName = configName;
    }

    public LLM() {
        this.configName = "default";
    }

    public String getConfigName() {
        return configName;
    }
}

/**
 * 沙箱客户端单例
 */
class SandboxClient {
    private static SandboxClient instance;

    private SandboxClient() {
        // 私有构造函数
    }

    public static synchronized SandboxClient getInstance() {
        if (instance == null) {
            instance = new SandboxClient();
        }
        return instance;
    }

    public void cleanup() {
        // 清理沙箱环境的实现
        System.out.println("Sandbox cleanup completed");
    }
}

// 使用示例
class MyAgent extends BaseAgent {

    private MyAgent(Builder builder) {
        super(builder);
    }

    @Override
    public String step() {
        // 实现具体的步骤逻辑
        System.out.println("Executing custom step");
        return "Step completed successfully";
    }

    public static class Builder extends BaseAgent.Builder<Builder> {
        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public BaseAgent build() {
            return new MyAgent(this);
        }
    }

    public static void main(String[] args) {
        MyAgent agent = (MyAgent) new Builder()
                .name("TestAgent")
                .description("A test agent")
                .maxSteps(5)
                .build();

        String result = agent.run("Process this request");
        System.out.println(result);
    }
}
