package com.elec5619.libraryreservationreviewhub.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The assistant's "recommend some books" tool gives real books to name (step 5 plan D4). Called through its
 * registered {@link ToolCallback}, the same path the model's tool calls take. (Tests under tools/** are left out
 * of the build, so this one lives here.)
 */
@SpringBootTest
class AiBookToolIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private ToolCallback[] allTools;

    @Test
    void getRandomBooksGivesFiveBooksWithTheirDetails() throws Exception {
        JsonNode books = call("get_random_books");

        assertThat(books.isArray()).isTrue();
        assertThat(books.size()).isEqualTo(5);
        Set<Long> ids = new HashSet<>();
        for (JsonNode book : books) {
            assertThat(book.path("title").asText()).isNotBlank();
            assertThat(book.path("author").asText()).isNotBlank();
            assertThat(book.path("genre").asText()).isNotBlank();
            ids.add(book.path("bookId").asLong());
        }
        assertThat(ids).hasSize(5);   // five different books
    }

    @Test
    void theToolSaysWhatItReturns() {
        String description = tool("get_random_books").getToolDefinition().description();
        assertThat(description).containsIgnoringCase("5 random");
    }

    private JsonNode call(String name) throws Exception {
        JsonNode result = JSON.readTree(tool(name).call("{}"));
        // a tool's result may come back as a JSON string that holds the JSON
        return result.isTextual() ? JSON.readTree(result.asText()) : result;
    }

    private ToolCallback tool(String name) {
        return Arrays.stream(allTools)
                .filter(t -> t.getToolDefinition().name().equals(name))
                .findFirst().orElseThrow();
    }
}
