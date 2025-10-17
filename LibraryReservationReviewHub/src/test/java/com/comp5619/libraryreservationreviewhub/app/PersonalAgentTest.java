package com.comp5619.libraryreservationreviewhub.app;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PersonalAgentTest {


    @Resource
    private PersonalAgent personalAgent;


    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，我是hao chen";
        String answer = personalAgent.doChat(message, chatId);
        // 第二轮
        message = "我想读科幻类型的书";
        answer = personalAgent.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第三轮
        message = "我叫什么来着？刚跟你说过，帮我回忆一下";
        answer = personalAgent.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }

}