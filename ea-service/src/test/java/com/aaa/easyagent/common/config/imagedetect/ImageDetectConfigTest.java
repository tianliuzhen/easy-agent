package com.aaa.easyagent.common.config.imagedetect;

import com.aaa.easyagent.biz.imagedetect.service.ImageDetectService;
import com.aaa.easyagent.biz.imagedetect.service.impl.ImageDetectServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证独立 YML 绑定，以及关闭、模型缺失和配置错误时的启动隔离。
 */
class ImageDetectConfigTest {

    @Test
    void loadsIndependentYamlAndDisablesModelByDefault() throws IOException {
        contextRunner().run(context -> {
            assertThat(context).hasNotFailed();
            ImageDetectProperties properties = context.getBean(ImageDetectProperties.class);
            assertThat(properties.getModelLocation()).isEqualTo("classpath:model/seg_clothes.onnx");
            assertThat(properties.getMinAreaRatio()).isEqualTo(0.004f);
            assertThat(properties.getMaxImageBytes()).isEqualTo(5L * 1024 * 1024);
            ImageDetectService service = context.getBean(ImageDetectService.class);
            assertThat(service.isEnabled()).isFalse();
            assertThatThrownBy(() -> service.detect(new byte[]{1}, null, null))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("easy-agent.image-detect");
        });

        assertThat(new ClassPathResource("application.properties").getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .contains("classpath:/config/image-detect-config.yml");
    }

    @Test
    void missingModelDoesNotPreventContextStartup() throws IOException {
        contextRunner().withPropertyValues("easy-agent.image-detect.enabled=true",
                "easy-agent.image-detect.model-location=classpath:model/missing-test-model.onnx")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(ImageDetectProperties.class).isEnabled()).isTrue();
                    assertThat(context.getBean(ImageDetectService.class).isEnabled()).isFalse();
                });
    }

    @Test
    void invalidModelConfigurationDisablesDetectionWithoutPreventingStartup() throws IOException {
        contextRunner().withPropertyValues("easy-agent.image-detect.enabled=true",
                "easy-agent.image-detect.input-size=0")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(ImageDetectProperties.class).getInputSize()).isZero();
                    assertThat(context.getBean(ImageDetectService.class).isEnabled()).isFalse();
                });
    }

    private ApplicationContextRunner contextRunner() throws IOException {
        PropertySource<?> configuration = new YamlPropertySourceLoader()
                .load("image-detect-config", new ClassPathResource("config/image-detect-config.yml")).get(0);
        return new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().getPropertySources().addLast(configuration))
                .withUserConfiguration(PropertyBindingConfig.class, ImageDetectServiceImpl.class);
    }

    /** 复用 Spring Boot 的配置绑定机制，验证 YML 和覆盖参数确实生效。 */
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ImageDetectProperties.class)
    static class PropertyBindingConfig {
    }
}
