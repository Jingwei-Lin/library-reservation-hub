package com.comp5619.libraryreservationreviewhub.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * The library statistics come from the server (step 5 plan D3): the list no longer loads every book to count
 * them, and counted at most 101. Runs in a transaction that is rolled back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookStatsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void theStatisticsCountEveryListedBook() throws Exception {
        // past the 101 the list used to load, a deleted book, one with no copy on the shelf, and a new genre
        for (int i = 0; i < 5; i++) {
            jdbc.update("INSERT INTO book (title, author, genre, quantity) VALUES (?, 'Test', 'Stats Genre', 2)", "Stats book " + i);
        }
        jdbc.update("INSERT INTO book (title, author, genre, quantity) VALUES ('Stats book out', 'Test', 'Stats Genre', 0)");
        jdbc.update("INSERT INTO book (title, author, genre, quantity, is_delete) VALUES ('Stats book gone', 'Test', 'Gone Genre', 3, TRUE)");

        int listed = jdbc.queryForObject("SELECT COUNT(*) FROM book WHERE is_delete = FALSE", Integer.class);
        int onTheShelf = jdbc.queryForObject("SELECT COUNT(*) FROM book WHERE is_delete = FALSE AND quantity > 0", Integer.class);
        int genres = jdbc.queryForObject("SELECT COUNT(DISTINCT genre) FROM book WHERE is_delete = FALSE", Integer.class);

        mockMvc.perform(get("/book/stats"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalBooks").value(listed))
                .andExpect(jsonPath("$.data.availableBooks").value(onTheShelf))
                .andExpect(jsonPath("$.data.categories").value(genres));
    }
}
