package com.aaa.easyagent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
        // ElasticsearchVectorStoreAutoConfiguration.class
})
@tk.mybatis.spring.annotation.MapperScan("com.aaa.easyagent.core.mapper")
public class EasyAgentApplication {
    private static final Logger logger = LogManager.getLogger(EasyAgentApplication.class);

    public static void main(String[] args) {
        logger.info("Application starting...");
        SpringApplication.run(EasyAgentApplication.class, args);
        logger.info("Application started successfully");
    }

}
