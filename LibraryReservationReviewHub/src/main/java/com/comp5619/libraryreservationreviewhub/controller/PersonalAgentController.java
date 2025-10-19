package com.comp5619.libraryreservationreviewhub.controller;

import com.comp5619.libraryreservationreviewhub.app.PersonalAgent;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class PersonalAgentController {

    @Resource
    private PersonalAgent personalAgent;



    /**
     * 同步调用 Personal Agent
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping("/personal_agent/chat/sync")
    public String doChatSync(String message, String chatId) {
        return personalAgent.doChat(message, chatId);
    }


    /**
     * SSE 流式调用 Personal Agent
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/personal_agent/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithPersonalAppSSE(String message, String chatId) {
        return personalAgent.doChatByStream(message, chatId);
    }

    /**
     * SSE 流式调用 Personal Agent
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/personal_agent/chat/server_sent_event")
    public Flux<ServerSentEvent<String>> doChatWithPersonalAppServerSentEvent(String message, String chatId) {
        return personalAgent.doChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }
}
