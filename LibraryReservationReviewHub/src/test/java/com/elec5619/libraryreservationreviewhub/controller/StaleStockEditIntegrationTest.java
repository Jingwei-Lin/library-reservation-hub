package com.elec5619.libraryreservationreviewhub.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * An admin's book edit can't turn a stale shelf count into stock: a quantity change says what it was loaded with,
 * and is refused if the shelf has changed since. Book 1 starts with 5 copies on the shelf.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StaleStockEditIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockHttpSession admin;

    private MockHttpSession login(String email) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userAccount\":\"" + email + "\",\"userPassword\":\"123456789\"}"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getRequest().getSession();
    }

    private ResultActions edit(String json) throws Exception {
        return mockMvc.perform(put("/book/1").session(admin).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private int shelf() {
        return jdbcTemplate.queryForObject("SELECT quantity FROM book WHERE book_id = 1", Integer.class);
    }

    private String title() {
        return jdbcTemplate.queryForObject("SELECT title FROM book WHERE book_id = 1", String.class);
    }

    /** The admin page loaded book 1 (5 on the shelf); then a reader reserves a copy, leaving 4 */
    @BeforeEach
    void aReservationAfterThePageLoaded() throws Exception {
        admin = login("admin@admin.com");
        mockMvc.perform(post("/reservation/create/1").session(login("hanchen.wang@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickupTime\":\"" + LocalDate.now().plusDays(1) + "\",\"dueTime\":\""
                                + LocalDate.now().plusDays(5) + "\"}"))
                .andExpect(jsonPath("$.code").value(0));
        assertThat(shelf()).isEqualTo(4);
    }

    @Test
    void aQuantityChangeFromAStaleCountIsRefused() throws Exception {
        // the admin adds one copy to what the page showed (5 → 6)
        edit("{\"title\":\"The Hobbit\",\"quantity\":6,\"expectedQuantity\":5}")
                .andExpect(jsonPath("$.code").value(50001))
                .andExpect(jsonPath("$.message").value("The stock changed since this page loaded. Reload and try again."));

        assertThat(shelf()).isEqualTo(4);
    }

    @Test
    void aQuantityWithoutTheLoadedCountIsRefused() throws Exception {
        // the old admin page: the whole row as loaded, quantity included
        edit("{\"title\":\"The Hobbit (fixed)\",\"quantity\":5}")
                .andExpect(jsonPath("$.code").value(40000));

        assertThat(shelf()).isEqualTo(4);
        assertThat(title()).isEqualTo("The Hobbit");
    }

    @Test
    void aTitleOnlyEditLeavesTheStockAlone() throws Exception {
        edit("{\"title\":\"The Hobbit (fixed)\"}").andExpect(jsonPath("$.code").value(0));

        assertThat(title()).isEqualTo("The Hobbit (fixed)");
        assertThat(shelf()).isEqualTo(4);
    }

    @Test
    void aQuantityChangeFromTheCurrentCountWorks() throws Exception {
        edit("{\"quantity\":6,\"expectedQuantity\":4}").andExpect(jsonPath("$.code").value(0));

        // two new copies: the first is held for user 1, who is waiting for book 1 in the seed data
        assertThat(shelf()).isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject("SELECT held_quantity FROM book WHERE book_id = 1", Integer.class))
                .isEqualTo(1);
    }
}
