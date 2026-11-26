package com.elec5619.libraryreservationreviewhub.controller;

import com.elec5619.libraryreservationreviewhub.support.StatementCounter;
import com.elec5619.libraryreservationreviewhub.tools.UserInfoTool;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * The lists look up their users and books in one query each, not one per row. Seed data: 8 reviews by users 1
 * and 2; books 1 to 5 have reviews.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QueryCountIntegrationTest {

    @TestConfiguration
    static class Counting {
        @Bean
        StatementCounter statementCounter() {
            return new StatementCounter();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StatementCounter counter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserInfoTool userInfoTool;

    private MockHttpSession login(String email) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userAccount\":\"" + email + "\",\"userPassword\":\"123456789\"}"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getRequest().getSession();
    }

    /** Two reservations for user 1, on books 2 and 3 */
    private void twoReservations() {
        jdbcTemplate.update("INSERT INTO reservation (user_id, book_id, status, pickup_date, due_date) VALUES "
                + "(1, 2, 'reserved', NOW(), NOW() + INTERVAL 7 DAY), (1, 3, 'returned', NOW(), NOW() + INTERVAL 7 DAY)");
    }

    @Test
    void aPageOfReviewsLooksUpItsUsersOnce() throws Exception {
        MockHttpSession admin = login("admin@admin.com");
        counter.reset();

        String body = mockMvc.perform(get("/review/all?current=1&pageSize=10").session(admin))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();

        assertThat((List<?>) JsonPath.read(body, "$.data.records")).hasSize(8);
        assertThat((List<String>) JsonPath.read(body, "$.data.records[*].userName"))
                .containsOnly("Jingwei Lin", "Hanchen Wang");
        // the admin check reads the signed-in user once; the names take one more query, not one per review
        assertThat(counter.count("UserMapper.selectById")).as(counter.toString()).isLessThanOrEqualTo(1);
        assertThat(counter.countFor("UserMapper")).as(counter.toString()).isLessThanOrEqualTo(2);
    }

    @Test
    void theTopTenLoadsItsBooksInOneQuery() throws Exception {
        counter.reset();

        String body = mockMvc.perform(get("/book-rating/top10-books"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();

        assertThat((List<?>) JsonPath.read(body, "$.data")).hasSize(5);
        assertThat(counter.count("BookMapper.selectById")).as(counter.toString()).isZero();
        assertThat(counter.countFor("BookMapper")).as(counter.toString()).isEqualTo(1);
    }

    @Test
    void myReservationsLoadTheirBooksInOneQuery() throws Exception {
        twoReservations();
        MockHttpSession reader = login("jingwei.lin2@example.com");
        counter.reset();

        String body = mockMvc.perform(get("/reservation/user").session(reader))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();

        assertThat((List<String>) JsonPath.read(body, "$.data[*].bookTitle"))
                .containsExactlyInAnyOrder("The Lord of the Rings", "Harry Potter and the Philosopher's Stone");
        assertThat(counter.count("BookMapper.selectById")).as(counter.toString()).isZero();
        assertThat(counter.countFor("BookMapper")).as(counter.toString()).isEqualTo(1);
    }

    @Test
    void theUserToolLoadsTheBooksOnce() {
        twoReservations();
        counter.reset();

        String json = userInfoTool.getUserInfo(new ToolContext(UserInfoTool.toolContextFor(1L)));

        assertThat((List<String>) JsonPath.read(json, "$.borrowedBooks[*].title"))
                .containsExactlyInAnyOrder("The Lord of the Rings", "Harry Potter and the Philosopher's Stone");
        assertThat(counter.count("BookMapper.selectById")).as(counter.toString()).isZero();
        // the reservations' titles and the tool's own book list
        assertThat(counter.countFor("BookMapper")).as(counter.toString()).isLessThanOrEqualTo(2);
    }
}
