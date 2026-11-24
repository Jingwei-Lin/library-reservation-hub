package com.comp5619.libraryreservationreviewhub.controller;

import com.comp5619.libraryreservationreviewhub.app.PersonalAgent;
import com.comp5619.libraryreservationreviewhub.chatmemory.FileBasedChatMemory;
import com.comp5619.libraryreservationreviewhub.common.UserContextHolder;
import com.comp5619.libraryreservationreviewhub.exception.BusinessException;
import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
import com.comp5619.libraryreservationreviewhub.exception.ThrowUtils;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
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
     * 同步调用 Personal Agent
     *
     * @param message
     * @param chatId
     * @return
     */
//    @GetMapping("/personal_agent/chat/sync")
//    public String doChatSync(String message, String chatId) {
//        return personalAgent.doChat(message, chatId);
//    }


    /**
     * SSE 流式调用 Personal Agent
     *
     * @param message
     * @param chatId
     * @return
     */
//    @GetMapping(value = "/personal_agent/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public Flux<String> doChatWithPersonalAppSSE(String message, String chatId) {
//        return personalAgent.doChatByStream(message, chatId);
//    }

    /**
     * SSE 流式调用 Personal Agent
     * 该方法使用 Server-Sent Events (SSE) 实现与 AI 助手的流式对话

 *
     * @param message 用户输入的消息内容
     * @param chatId 对话的唯一标识符

 * @param request HTTP 请求对象，用于获取会话信息
     * @return 返回一个 Flux 流，包含 ServerSentEvent 类型的响应数据，用于流式传输 AI 的回复
     */
//    @GetMapping(value = "/personal_agent/chat/server_sent_event")
//    public Flux<ServerSentEvent<String>> doChatWithPersonalAppServerSentEvent(String message, String chatId) {
//        return personalAgent.doChatByStream(message, chatId)
//                .map(chunk -> ServerSentEvent.<String>builder()
//                        .data(chunk)
//                        .build());
//    }
    @GetMapping(value = "/personal_agent/chat/server_sent_event", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> doChatWithPersonalAppServerSentEvent(
            String message,
            String chatId,
            HttpServletRequest request
    ) {
        // the UI sends a uuidv4(); anything else (e.g. "../../x") would name a file outside the chat-memory folder
        ThrowUtils.throwIf(!FileBasedChatMemory.isValidConversationId(chatId), ErrorCode.PARAMS_ERROR,
                "chatId must be a UUID");

        // 1. 从 Session 中获取当前用户
        // Through getLoginUser, like every other route: it reloads the user from the database, so a user who has
        // since been banned or deleted chats as a guest and the tools can't read their data. Guests keep the chat.
        User currentUser;
        try {
            currentUser = userService.getLoginUser(request);
        } catch (BusinessException e) {
            currentUser = null;
        }
        if (currentUser != null) {
            log.info("[SSE] 已从 Session 取到用户: id={}, name={}", currentUser.getId(), currentUser.getFirstName());
        } else {
            log.warn("[SSE] Session 中没有用户（未登录或 Session 丢失）");
        }

        // 2. 把用户信息转换为 AI 可以理解的 context
        String userContext;
        if (currentUser == null) {
            userContext = "{\"status\":\"not_logged_in\"}";
        } else {
            userContext = String.format(
                    "{\"status\":\"logged_in\",\"userId\":%d,\"userName\":\"%s\"}",
                    currentUser.getId(),
                    currentUser.getFirstName()
            );
        }

        // 3. 把 userContext 一起传给 AI（比如拼接 message 或放到参数中）
        // The user id also goes to the tools through ToolContext, set by the server, so get_user_info can only
        // read the signed-in user (null for a guest), whatever the model or the message asks for.
        Long userId = currentUser == null ? null : currentUser.getId();
        return personalAgent.doChatByStream(message, chatId, userContext, userId)
                .map(chunk -> ServerSentEvent.<String>builder().data(chunk).build());
    }

}
