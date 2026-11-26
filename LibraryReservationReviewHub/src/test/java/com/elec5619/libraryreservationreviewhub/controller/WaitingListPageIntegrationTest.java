package com.elec5619.libraryreservationreviewhub.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * The paged waiting list shows the same entries as /my (no cancelled or fulfilled ones), and its total counts
 * only those (step 5 plan D5). Runs in a transaction that is rolled back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WaitingListPageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void theTotalCountsOnlyTheEntriesShown() throws Exception {
        // user 2 (seed data has none for them): one entry still in the queue, three finished ones
        jdbc.update("INSERT INTO waiting_list (user_id, book_id, status) VALUES (2, 2, 'waiting'), "
                + "(2, 3, 'cancelled'), (2, 4, 'cancelled'), (2, 5, 'fulfilled')");

        MockHttpSession user = login("hanchen.wang@example.com");
        mockMvc.perform(get("/waiting-list/my/page").param("current", "1").param("pageSize", "10").session(user))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].status").value("waiting"));
        // the unpaged list agrees
        mockMvc.perform(get("/waiting-list/my").session(user))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void aFullPageIsFullWhenFinishedEntriesComeFirst() throws Exception {
        // newest first: two cancelled entries on top would have left the first page short
        jdbc.update("INSERT INTO waiting_list (user_id, book_id, join_date, status) VALUES "
                + "(2, 2, NOW() - INTERVAL 3 DAY, 'waiting'), (2, 3, NOW() - INTERVAL 2 DAY, 'waiting'), "
                + "(2, 4, NOW() - INTERVAL 1 DAY, 'cancelled'), (2, 5, NOW(), 'cancelled')");

        mockMvc.perform(get("/waiting-list/my/page").param("current", "1").param("pageSize", "2")
                        .session(login("hanchen.wang@example.com")))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records.length()").value(2));
    }

    private MockHttpSession login(String account) throws Exception {
        MvcResult result = mockMvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userAccount\":\"" + account + "\",\"userPassword\":\"123456789\"}"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
