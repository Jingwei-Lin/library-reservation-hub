package com.comp5619.libraryreservationreviewhub.app;


import com.comp5619.libraryreservationreviewhub.advisor.MyLoggerAdvisor;
import com.comp5619.libraryreservationreviewhub.chatmemory.FileBasedChatMemory;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
@Slf4j
public class PersonalAgent {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT =
            "Act as an experienced online librarian of a Book Reservation Hub. " +
                    "Introduce yourself at the beginning, explaining that you can help users discover, reserve, and review books online. " +
                    "You are responsible for recommending books to users based on their reading preferences, interests, and the current average ratings of available books. " +
                    "You should also be able to answer users’ questions about specific books, authors, genres, or the reservation system itself. " +
                    "During interactions, ask guiding questions to better understand the user's taste, reading history, or goals, " +
                    "then provide thoughtful and personalized recommendations. " +
                    "When responding, include short explanations about why a book might appeal to the user, and optionally mention related titles or authors. " +
                    "Your tone should be friendly, professional, and knowledgeable — just like a helpful librarian guiding readers to their next favorite book.";


    // AI 调用工具能力
    @Resource
    private ToolCallback[] allTools;



    /**
     * 初始化 ChatClient
     * @param dashscopeChatModel
     */
    public PersonalAgent(ChatModel dashscopeChatModel) {
//        // 初始化基于文件的对话记忆
        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                        // 自定义日志 Advisor，可按需开启
                        ,new MyLoggerAdvisor()
//                        // 自定义推理增强 Advisor，可按需开启
//                       ,new ReReadingAdvisor()
                )
                .build();
        }


        /**
         * AI 基础对话
         * 支持多轮对话记忆，工具调用
         * @param message
         * @param chatId
         * @return
         */
        public String doChat(String message, String chatId) {
            ChatResponse chatResponse = chatClient
                    .prompt()
                    .user(message)
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                    .toolCallbacks(allTools)
                    .call()
                    .chatResponse();
            String content = chatResponse.getResult().getOutput().getText();
            log.info("content: {}", content);
            return content;
        }
        /**
         * AI 基础对话
         * 支持多轮对话记忆，SSE 流式传输，工具调用
         *
         * @param message
         * @param chatId
         * @return
         */
        public Flux<String> doChatByStream(String message, String chatId) {
            return chatClient
                    .prompt()
                    .user(message)
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                    .toolCallbacks(allTools)
                    .stream()
                    .content();
        }

        record PersonalAgentReport(String title, List<String> suggestions) {

        }




//    /**
//     * AI 使用工具 聊天
//     *
//     * @param message
//     * @param chatId
//     * @return
//     */
//    public String doChatWithTools(String message, String chatId) {
//        ChatResponse chatResponse = chatClient
//                .prompt()
//                .user(message)
//                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
//                // 开启日志，便于观察效果
//                .advisors(new MyLoggerAdvisor())
//                .toolCallbacks(allTools)
//                .call()
//                .chatResponse();
//        String content = chatResponse.getResult().getOutput().getText();
//        log.info("content: {}", content);
//        return content;
//    }







}
