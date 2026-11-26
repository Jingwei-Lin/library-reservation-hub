package com.elec5619.libraryreservationreviewhub.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Review rules (step 5 plan D1): any listed book can be reviewed, a new review is checked like an edit, and
 * several reviews per user stay allowed. Runs in a transaction that is rolled back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReviewRulesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private MockHttpSession user;
    private long bookId;

    @BeforeEach
    void setUp() throws Exception {
        user = login("jingwei.lin2@example.com");
        bookId = insert("INSERT INTO book (title, author, genre, quantity, held_quantity, available) "
                + "VALUES ('Review rules test book', 'Test', 'Fantasy', 1, 0, TRUE)");
    }

    @Test
    void aBookWithEveryCopyOutCanStillBeReviewed() throws Exception {
        jdbc.update("UPDATE book SET quantity = 0 WHERE book_id = ?", bookId);
        add("{\"rating\":4,\"comment\":\"Read it before it went out\"}").andExpect(jsonPath("$.code").value(0));
        assertThat(reviews()).isEqualTo(1);
    }

    @Test
    void aDeletedBookCannotBeReviewed() throws Exception {
        jdbc.update("UPDATE book SET is_delete = TRUE WHERE book_id = ?", bookId);
        add("{\"rating\":4,\"comment\":\"Hidden book\"}").andExpect(jsonPath("$.code").value(40400));
        assertThat(reviews()).isZero();
    }

    @Test
    void aNewReviewNeedsARatingFromOneToFive() throws Exception {
        add("{\"rating\":0,\"comment\":\"Too low\"}").andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("Rating must be between 1 and 5"));
        add("{\"rating\":6,\"comment\":\"Too high\"}").andExpect(jsonPath("$.code").value(40000));
        add("{\"comment\":\"No rating\"}").andExpect(jsonPath("$.code").value(40000));
        assertThat(reviews()).isZero();
    }

    @Test
    void aNewReviewNeedsAComment() throws Exception {
        add("{\"rating\":3,\"comment\":\"   \"}").andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("Comment cannot be empty"));
        assertThat(reviews()).isZero();
    }

    @Test
    void aCommentIsAtMostAThousandCharacters() throws Exception {
        String max = "a".repeat(1000);
        String tooLong = "a".repeat(1001);
        add("{\"rating\":3,\"comment\":\"" + tooLong + "\"}").andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("Comment must be at most 1000 characters"));
        add("{\"rating\":3,\"comment\":\"" + max + "\"}").andExpect(jsonPath("$.code").value(0));

        long reviewId = jdbc.queryForObject("SELECT review_id FROM review WHERE book_id = ?", Long.class, bookId);
        mockMvc.perform(put("/review/update").session(user).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewId\":" + reviewId + ",\"rating\":3,\"comment\":\"" + tooLong + "\"}"))
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void severalReviewsOfOneBookAreStillAllowed() throws Exception {
        add("{\"rating\":4,\"comment\":\"First read\"}").andExpect(jsonPath("$.code").value(0));
        add("{\"rating\":5,\"comment\":\"Second read\"}").andExpect(jsonPath("$.code").value(0));
        assertThat(reviews()).isEqualTo(2);
    }

    private ResultActions add(String body) throws Exception {
        return mockMvc.perform(post("/review/add/" + bookId).session(user)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private int reviews() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM review WHERE book_id = ?", Integer.class, bookId);
    }

    private long insert(String sql) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    private MockHttpSession login(String account) throws Exception {
        MvcResult result = mockMvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userAccount\":\"" + account + "\",\"userPassword\":\"123456789\"}"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
