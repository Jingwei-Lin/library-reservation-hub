package com.elec5619.libraryreservationreviewhub.config;

import com.elec5619.libraryreservationreviewhub.chatmemory.FileBasedChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The AI chat's memory: one instance, shared by the agent that reads and writes it and the job that deletes idle
 * conversations. Same folder as before ({@code <working dir>/tmp/chat-memory}), so existing conversations are kept.
 */
@Configuration
public class ChatMemoryConfig {

    @Bean
    public FileBasedChatMemory chatMemory() {
        return new FileBasedChatMemory(System.getProperty("user.dir") + "/tmp/chat-memory");
    }
}
