package com.elec5619.libraryreservationreviewhub.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * A review's date keeps its time of day, and editing a review keeps the date it was first posted.
 * Seed data: review 1 is user 1's review of book 1.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReviewDatesIntegrationTest {

    private static final String POSTED = "2020-01-02 03:04:05";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockHttpSession reader() throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userAccount\":\"jingwei.lin2@example.com\",\"userPassword\":\"123456789\"}"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getRequest().getSession();
    }

    private LocalDateTime storedDate() {
        return jdbcTemplate.queryForObject("SELECT review_date FROM review WHERE review_id = 1", LocalDateTime.class);
    }

    @Test
    void theDateKeepsItsTimeOfDay() throws Exception {
        jdbcTemplate.update("UPDATE review SET review_date = ? WHERE review_id = 1", POSTED);

        String body = mockMvc.perform(get("/review/book/1")).andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        List<String> dates = JsonPath.read(body, "$.data[?(@.reviewId == 1)].reviewDate");

        // a timestamp with the time of day (its zone depends on the JVM's), not just "2020-01-02"
        assertThat(dates.get(0)).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*");
    }

    @Test
    void anEditKeepsTheDateTheReviewWasPosted() throws Exception {
        jdbcTemplate.update("UPDATE review SET review_date = ? WHERE review_id = 1", POSTED);
        LocalDateTime before = storedDate();

        mockMvc.perform(put("/review/update").session(reader()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewId\":1,\"rating\":3,\"comment\":\"Edited later\"}"))
                .andExpect(jsonPath("$.code").value(0));

        assertThat(jdbcTemplate.queryForObject("SELECT comment FROM review WHERE review_id = 1", String.class))
                .isEqualTo("Edited later");
        assertThat(storedDate()).isEqualTo(before);
    }
}
