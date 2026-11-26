package com.elec5619.libraryreservationreviewhub.app;

import com.elec5619.libraryreservationreviewhub.model.dto.review.ReviewAddRequest;
import com.elec5619.libraryreservationreviewhub.service.ReviewService;
import com.elec5619.libraryreservationreviewhub.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * At the default INFO level, the logs carry no chat text, no user details and no whole records: those are
 * logged at DEBUG. Each test checks the console for a marker it put into the data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class QuietLoggingIntegrationTest {

    private static final File MEMORY_DIR = new File(System.getProperty("user.dir") + "/tmp/chat-memory");

    @MockitoBean
    private ChatModel chatModel;

    @Autowired
    private PersonalAgent personalAgent;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private UserService userService;

    @Autowired
    private MockMvc mockMvc;

    private final String chatId = UUID.randomUUID().toString();

    @AfterEach
    void deleteTheConversation() {
        new File(MEMORY_DIR, chatId + ".kryo").delete();
    }

    @Test
    void aChatTurnLogsNoPromptOrReply(CapturedOutput output) {
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(
                new ChatResponse(List.of(new Generation(new AssistantMessage("reply-marker-7f3a"))))));

        personalAgent.doChatByStream("question-marker-7f3a", chatId, "{\"phone\":\"0499111222\"}", 1L)
                .collectList().block();

        assertThat(output.getAll()).doesNotContain("question-marker-7f3a", "reply-marker-7f3a", "0499111222");
    }

    @Test
    @Transactional
    void aNewReviewIsNotLoggedWhole(CapturedOutput output) {
        ReviewAddRequest req = new ReviewAddRequest();
        req.setRating(4);
        req.setComment("comment-marker-7f3a");

        reviewService.addReview(2L, req, userService.getById(1L));

        assertThat(output.getAll()).doesNotContain("comment-marker-7f3a");
    }

    @Test
    void aSignInDoesNotLogTheEmail(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"userAccount\":\"email-marker-7f3a@example.com\",\"userPassword\":\"whatever-1\"}"));

        assertThat(output.getAll()).doesNotContain("email-marker-7f3a");
    }
}
