package com.comp5619.libraryreservationreviewhub.agent;


import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PersonalManusTest {
    @Resource
    private PersonalManus personalManus;

    @Test
    public void run() {
        String userPrompt = """
                我喜欢看星球大战这样的书，结合一些网络图片，给我推荐同类型的书籍，并以 PDF 格式输出""";
        String answer = personalManus.run(userPrompt);
        Assertions.assertNotNull(answer);
    }
}