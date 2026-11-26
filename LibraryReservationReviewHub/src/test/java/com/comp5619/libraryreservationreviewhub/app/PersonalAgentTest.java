package com.comp5619.libraryreservationreviewhub.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PersonalAgent's streaming chat against the real model (SSE, with the user's context).
 * Collects the Flux directly rather than using StepVerifier.
 */
@SpringBootTest
// Calls the real model, so it only runs when a real DashScope key is provided
@EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY", matches = "sk-.+")
class PersonalAgentTest {

    @Resource
    private PersonalAgent personalAgent;

    @Test
    void testChatByStream_WithUserContext() {
        String chatId = UUID.randomUUID().toString();
        String message1 = "Please help me find the book \"The Hobbit\".";
        String message2 = "Do you remember my name?.";
        String userContext = "{\"status\":\"logged_in\",\"userId\":1,\"userName\":\"Jingwei\"}";


        Flux<String> flux = personalAgent.doChatByStream(message1, chatId, userContext, 1L);

        // wait for the whole reply
        List<String> chunks = flux
                .timeout(Duration.ofSeconds(10))  // don't hang
                .collectList()
                .block();

        String fullResponse = String.join(" ", chunks);
        System.out.println(" Full Merged Response: " + fullResponse);

        // the model must answer
        assertThat(fullResponse).isNotBlank();
        assertThat(fullResponse).containsIgnoringCase("The Hobbit");

        Flux<String> flux2 = personalAgent.doChatByStream(message2, chatId, userContext, 1L);

        // wait for the whole reply
        List<String> chunks2 = flux2
                .timeout(Duration.ofSeconds(10))  // don't hang
                .collectList()
                .block();

        String fullResponse2 = String.join(" ", chunks2);
        System.out.println(" Full Merged Response: " + fullResponse2);


        assertThat(fullResponse2)
                .isNotBlank()
                .containsIgnoringCase("jing")
                .containsIgnoringCase("wei");
    }


}
