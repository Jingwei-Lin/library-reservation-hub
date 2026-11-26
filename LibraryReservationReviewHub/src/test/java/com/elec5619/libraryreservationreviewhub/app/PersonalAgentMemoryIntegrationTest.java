package com.elec5619.libraryreservationreviewhub.app;

import com.elec5619.libraryreservationreviewhub.chatmemory.FileBasedChatMemory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * What the chat stores (step 5 plan D2): the user's message as typed, not the user details sent with it.
 * The model is a stub, so no DashScope key is needed; the memory is the one the running app uses.
 */
@SpringBootTest
class PersonalAgentMemoryIntegrationTest {

    private static final String MEMORY_DIR = System.getProperty("user.dir") + "/tmp/chat-memory";

    @MockitoBean
    private ChatModel chatModel;

    @Autowired
    private PersonalAgent personalAgent;

    private final String chatId = UUID.randomUUID().toString();

    @AfterEach
    void deleteTheConversation() {
        new File(MEMORY_DIR, chatId + ".kryo").delete();
    }

    @Test
    void theUserDetailsAreSentButNotStored() {
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(
                new ChatResponse(List.of(new Generation(new AssistantMessage("Try The Hobbit."))))));
        String userContext = "{\"status\":\"logged_in\",\"userAccount\":\"jingwei.lin2@example.com\",\"phone\":\"0400000000\"}";

        String reply = String.join("", personalAgent.doChatByStream("Recommend a fantasy book", chatId, userContext, 1L)
                .collectList().block());
        assertThat(reply).isEqualTo("Try The Hobbit.");

        // the model still gets the details, in this turn's system prompt
        ArgumentCaptor<Prompt> sent = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).stream(sent.capture());
        assertThat(sent.getValue().getSystemMessage().getText()).contains("[UserContext]: " + userContext);
        assertThat(sent.getValue().getUserMessage().getText()).isEqualTo("Recommend a fantasy book");

        List<Message> stored = new FileBasedChatMemory(MEMORY_DIR).get(chatId);
        assertThat(stored).hasSize(2);
        assertThat(stored.get(0)).isInstanceOf(UserMessage.class);
        assertThat(stored.get(0).getText()).isEqualTo("Recommend a fantasy book");
        assertThat(stored).extracting(Message::getText).noneMatch(text -> text.contains("jingwei.lin2@example.com"));
    }
}
