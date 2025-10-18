package com.comp5619.libraryreservationreviewhub.tools;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WebSearchToolTest {

    @Value("${search-api.api-key}")
    private String searchApiKey;
    @Value("${search-api.search-engine-id}")
    private String searchEngineId;

    @Test
    void searchWeb() {
        WebSearchTool webSearchTool = new WebSearchTool(searchApiKey, searchEngineId);
        String query = "ELEC5619: Object Oriented Application Frameworks";
        String result = webSearchTool.searchWeb(query);
        System.out.println("Search Result: " + result);  // 打印返回值
        Assertions.assertNotNull(result);
    }
}