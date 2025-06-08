package com.aaa.springai.llm.deepseek;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.autoconfigure.openai.OpenAiEmbeddingProperties;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://mp.weixin.qq.com/s/usiJ6B3fF3tsJExVm8vgJg
 * <p>
 *
 * @author liuzhen.tian
 * @version 1.0 DeepseekAiAutoConfiguration.java  2025/2/20 22:48
 * @see org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration
 */
@Configuration
@EnableConfigurationProperties({DeepseekAiAutoConfiguration.OpenAiConnectionProperties.class, DeepseekAiAutoConfiguration.OpenAiChatProperties.class})
public class DeepseekAiAutoConfiguration {


    public static final String DEEPSEEK_CHAT = "spring.ai.deepseek.chat";
    public static final String DEEPSEEK = "spring.ai.deepseek";

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = DEEPSEEK_CHAT, name = "enabled", havingValue = "true", matchIfMissing = true)
    public OpenAiChatModel deepSeekChatModel(OpenAiConnectionProperties commonProperties,
                                             OpenAiChatProperties chatProperties, ObjectProvider<RestClient.Builder> restClientBuilderProvider,
                                             ObjectProvider<WebClient.Builder> webClientBuilderProvider, List<FunctionCallback> toolFunctionCallbacks,
                                             ToolCallingManager toolCallingManager, RetryTemplate retryTemplate,
                                             ResponseErrorHandler responseErrorHandler, ObjectProvider<ObservationRegistry> observationRegistry,
                                             ObjectProvider<ChatModelObservationConvention> observationConvention) {

        var openAiApi = openAiApi(chatProperties, commonProperties,
                restClientBuilderProvider.getIfAvailable(RestClient::builder),
                webClientBuilderProvider.getIfAvailable(WebClient::builder), responseErrorHandler, "chat");


        var chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(chatProperties.getOptions())
                .toolCallingManager(toolCallingManager)
                .retryTemplate(retryTemplate)
                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .build();


        observationConvention.ifAvailable(chatModel::setObservationConvention);

        return chatModel;
    }

    private OpenAiApi openAiApi(OpenAiChatProperties chatProperties, OpenAiConnectionProperties commonProperties,
                                RestClient.Builder restClientBuilder, WebClient.Builder webClientBuilder,
                                ResponseErrorHandler responseErrorHandler, String modelType) {

        ResolvedConnectionProperties resolved = resolveConnectionProperties(commonProperties, chatProperties,
                modelType);

        return new OpenAiApi(resolved.baseUrl(), resolved.apiKey(), resolved.headers(),
                chatProperties.getCompletionsPath(), OpenAiEmbeddingProperties.DEFAULT_EMBEDDINGS_PATH,
                restClientBuilder, webClientBuilder, responseErrorHandler);
    }

    private static ResolvedConnectionProperties resolveConnectionProperties(
            OpenAiParentProperties commonProperties, OpenAiParentProperties modelProperties, String modelType) {

        String baseUrl = StringUtils.hasText(modelProperties.getBaseUrl()) ? modelProperties.getBaseUrl()
                : commonProperties.getBaseUrl();
        String apiKey = StringUtils.hasText(modelProperties.getApiKey()) ? modelProperties.getApiKey()
                : commonProperties.getApiKey();
        String projectId = StringUtils.hasText(modelProperties.getProjectId()) ? modelProperties.getProjectId()
                : commonProperties.getProjectId();
        String organizationId = StringUtils.hasText(modelProperties.getOrganizationId())
                ? modelProperties.getOrganizationId() : commonProperties.getOrganizationId();

        Map<String, List<String>> connectionHeaders = new HashMap<>();
        if (StringUtils.hasText(projectId)) {
            connectionHeaders.put("OpenAI-Project", List.of(projectId));
        }
        if (StringUtils.hasText(organizationId)) {
            connectionHeaders.put("OpenAI-Organization", List.of(organizationId));
        }

        Assert.hasText(baseUrl,
                "OpenAI base URL must be set.  Use the connection property: spring.ai.openai.base-url or spring.ai.openai."
                        + modelType + ".base-url property.");
        Assert.hasText(apiKey,
                "OpenAI API key must be set. Use the connection property: spring.ai.openai.api-key or spring.ai.openai."
                        + modelType + ".api-key property.");

        return new ResolvedConnectionProperties(baseUrl, apiKey, CollectionUtils.toMultiValueMap(connectionHeaders));
    }

    private record ResolvedConnectionProperties(String baseUrl, String apiKey, MultiValueMap<String, String> headers) {

    }


    @ConfigurationProperties(DEEPSEEK)
    class OpenAiConnectionProperties extends OpenAiParentProperties {

        public static final String DEFAULT_BASE_URL = "https://api.openai.com";

        public OpenAiConnectionProperties() {
            super.setBaseUrl(DEFAULT_BASE_URL);
        }

    }

    @ConfigurationProperties(DEEPSEEK_CHAT)
    public class OpenAiChatProperties extends OpenAiParentProperties {


        public static final String DEFAULT_CHAT_MODEL = "gpt-4o";

        public static final String DEFAULT_COMPLETIONS_PATH = "/v1/chat/completions";

        private static final Double DEFAULT_TEMPERATURE = 0.7;

        /**
         * Enable OpenAI chat model.
         */
        private boolean enabled = true;

        private String completionsPath = DEFAULT_COMPLETIONS_PATH;

        @NestedConfigurationProperty
        private OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(DEFAULT_CHAT_MODEL)
                .temperature(DEFAULT_TEMPERATURE)
                .build();

        public OpenAiChatOptions getOptions() {
            return this.options;
        }

        public void setOptions(OpenAiChatOptions options) {
            this.options = options;
        }

        public boolean isEnabled() {
            return this.enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCompletionsPath() {
            return this.completionsPath;
        }

        public void setCompletionsPath(String completionsPath) {
            this.completionsPath = completionsPath;
        }

    }


    class OpenAiParentProperties {

        private String apiKey;

        private String baseUrl;

        private String projectId;

        private String organizationId;

        public String getApiKey() {
            return this.apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return this.baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getProjectId() {
            return this.projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }

        public String getOrganizationId() {
            return this.organizationId;
        }

        public void setOrganizationId(String organizationId) {
            this.organizationId = organizationId;
        }

    }

}
