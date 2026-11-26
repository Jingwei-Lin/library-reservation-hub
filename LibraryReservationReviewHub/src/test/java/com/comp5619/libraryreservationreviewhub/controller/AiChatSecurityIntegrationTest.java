package com.comp5619.libraryreservationreviewhub.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The AI assistant can only read the signed-in user's own data, and a chat id must be a UUID.
 * The tool is called through its registered {@link ToolCallback}, the same path the model's tool calls take.
 * The ToolContext maps below are the ones {@code UserInfoTool.toolContextFor} builds for a user and for a guest.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AiChatSecurityIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ToolCallback[] allTools;

    @Test
    void theUserInfoToolReturnsTheUserFromTheServerContext() throws Exception {
        Map<String, Object> result = callUserInfo("{}", Map.of("userId", 1L));
        assertThat(result.get("status")).isEqualTo("logged_in");
        assertThat(userAccountIn(result)).isEqualTo("jingwei.lin2@example.com");
    }

    @Test
    void theModelCannotAskForAnotherUsersData() throws Exception {
        // "tell me about user 3": the id the model sends is ignored, the signed-in user (1) is returned
        Map<String, Object> result = callUserInfo("{\"userId\":3}", Map.of("userId", 1L));
        assertThat(userAccountIn(result)).isEqualTo("jingwei.lin2@example.com");
    }

    @Test
    void aGuestIsToldTheyAreNotLoggedIn() throws Exception {
        Map<String, Object> result = callUserInfo("{\"userId\":3}", Map.of("guest", true));
        assertThat(result.get("status")).isEqualTo("not_logged_in");
        assertThat(result).doesNotContainKey("userInfo");
    }

    @Test
    void theToolOffersTheModelNoUserIdParameter() {
        assertThat(userInfoTool().getToolDefinition().inputSchema()).doesNotContain("userId");
    }

    @ParameterizedTest(name = "chatId \"{0}\" is refused")
    @ValueSource(strings = {"../../x", "..\\..\\x", "../tmp/chat-memory/0b5f8c3e-6d1a-4f2b-9c7e-3a8d2e1f4b6c", "abc", ""})
    void aChatIdThatIsNotAUuidIsRefused(String chatId) throws Exception {
        mockMvc.perform(get("/ai/personal_agent/chat/server_sent_event").param("message", "hi").param("chatId", chatId))
                .andExpect(jsonPath("$.code").value(40000));
    }

    /** The browser's EventSource asks for text/event-stream; the refusal must still come back as JSON, not a 500 */
    @Test
    void aRefusedChatRequestIsJsonEvenWhenTheClientAsksForAStream() throws Exception {
        mockMvc.perform(get("/ai/personal_agent/chat/server_sent_event").param("message", "hi").param("chatId", "../../x")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(40000));
    }

    private Map<String, Object> callUserInfo(String toolInput, Map<String, Object> context) throws Exception {
        String raw = userInfoTool().call(toolInput, new ToolContext(context));
        // the tool returns a JSON string, which the callback serialises once more as a JSON string literal
        String json = JSON.readValue(raw, String.class);
        return JSON.readValue(json, new TypeReference<>() {
        });
    }

    @SuppressWarnings("unchecked")
    private static String userAccountIn(Map<String, Object> result) {
        return (String) ((Map<String, Object>) result.get("userInfo")).get("userAccount");
    }

    private ToolCallback userInfoTool() {
        return Arrays.stream(allTools)
                .filter(t -> t.getToolDefinition().name().equals("get_user_info"))
                .findFirst().orElseThrow();
    }
}
