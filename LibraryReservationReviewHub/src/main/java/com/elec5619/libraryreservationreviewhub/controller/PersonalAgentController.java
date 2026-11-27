package com.elec5619.libraryreservationreviewhub.controller;

import cn.hutool.json.JSONUtil;
import java.util.LinkedHashMap;
import java.util.Map;
import com.elec5619.libraryreservationreviewhub.app.PersonalAgent;
import com.elec5619.libraryreservationreviewhub.chatmemory.FileBasedChatMemory;
import com.elec5619.libraryreservationreviewhub.exception.BusinessException;
import com.elec5619.libraryreservationreviewhub.exception.ErrorCode;
import com.elec5619.libraryreservationreviewhub.exception.ThrowUtils;
import com.elec5619.libraryreservationreviewhub.model.entity.User;
import com.elec5619.libraryreservationreviewhub.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/ai")
public class PersonalAgentController {

    @Resource
    private PersonalAgent personalAgent;

    @Resource
    private UserService userService;




    /**
     * Chat with the assistant, the reply streamed as Server-Sent Events
     *
     * @param message what the user typed
     * @param chatId the conversation's id (a UUID)
     * @param request the request, for the signed-in user
     * @return the reply, as a stream of events
     */
    @GetMapping(value = "/personal_agent/chat/server_sent_event", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> doChatWithPersonalAppServerSentEvent(
            String message,
            String chatId,
            HttpServletRequest request
    ) {
        // the UI sends a uuidv4(); anything else (e.g. "../../x") would name a file outside the chat-memory folder
        ThrowUtils.throwIf(!FileBasedChatMemory.isValidConversationId(chatId), ErrorCode.PARAMS_ERROR,
                "chatId must be a UUID");

        // 1. The signed-in user, from the session
        // Through getLoginUser, like every other route: it reloads the user from the database, so a user who has
        // since been banned or deleted chats as a guest and the tools can't read their data. Guests keep the chat.
        User currentUser;
        try {
            currentUser = userService.getLoginUser(request);
        } catch (BusinessException e) {
            currentUser = null;
        }
        if (currentUser != null) {
            log.info("[SSE] user from the session: id={}", currentUser.getId());
        } else {
            log.warn("[SSE] no user in the session (a guest, or the session has ended)");
        }

        // 2. The user's details, as context the model can read
        // built by the JSON library: the name is data, so quotes in it can't add fields or text to the prompt
        Map<String, Object> context = new LinkedHashMap<>();
        if (currentUser == null) {
            context.put("status", "not_logged_in");
        } else {
            context.put("status", "logged_in");
            context.put("userId", currentUser.getId());
            context.put("userName", currentUser.getFirstName());
        }
        String userContext = JSONUtil.toJsonStr(context);

        // 3. The context goes to the agent with the message
        // The user id also goes to the tools through ToolContext, set by the server, so get_user_info can only
        // read the signed-in user (null for a guest), whatever the model or the message asks for.
        Long userId = currentUser == null ? null : currentUser.getId();
        return personalAgent.doChatByStream(message, chatId, userContext, userId)
                .map(chunk -> ServerSentEvent.<String>builder().data(chunk).build());
    }

}
