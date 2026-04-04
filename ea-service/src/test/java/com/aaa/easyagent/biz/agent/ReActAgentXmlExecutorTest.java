package com.aaa.easyagent.biz.agent;


class ReActAgentXmlExecutorTest {
    public static void main(String[] args) {
        String s = """
                <Question>查询白银价格</Question>
                <Thought>用户想要查询白银价格，我需要使用查询贵金属价格工具来获取相关信息。</Thought>
                <Action>查询贵金属价格</Action>
                <Action Input>{"type": "silver"}</Action Input>
                Observation: 白银目前20元每克
                <Thought>我已经获得了白银价格信息，现在可以给出最终答案。</Thought>
                <Final Answer>白银目前的价格是20元每克。</Final Answer>
                """;
        ReActAgentExecutor.parseXmlResponse(s);
    }

}
