package com.elec5619.libraryreservationreviewhub.tools;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the tools the AI assistant may call.
 * A file read/write tool used to be registered here; it was removed because the model could be talked into
 * reading or writing any file on the server (path traversal).
 */
@Configuration
public class ToolRegistration {

    @Bean
    public UserInfoTool userInfoTool() {
        return new UserInfoTool(); // a Spring bean, so its @Autowired fields are injected
    }

    @Bean
    public ToolCallback[] allTools(UserInfoTool userInfoTool, BookSearchTool bookSearchTool) {
        // use the Spring-managed BookSearchTool so its @Autowired fields are not null
        return ToolCallbacks.from(userInfoTool, bookSearchTool);
    }
}
