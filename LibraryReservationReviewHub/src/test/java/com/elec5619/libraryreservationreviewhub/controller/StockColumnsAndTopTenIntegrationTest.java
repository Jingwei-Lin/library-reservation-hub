package com.elec5619.libraryreservationreviewhub.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * The stock columns can't be NULL, and the top 10 orders equal averages by review count, then book id.
 * Seed averages: book 5 5.00, book 2 4.50, book 1 4.00 (3 reviews), book 3 4.00 (1 review), book 4 2.00.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StockColumnsAndTopTenIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void heldCopiesCanNotBeNull() {
        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE book SET held_quantity = NULL WHERE book_id = 1"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void tiedAveragesPutTheBookWithMoreReviewsFirst() throws Exception {
        // book 3: four reviews of 4, still averaging 4.00, now more reviews than book 1's three
        jdbcTemplate.update("INSERT INTO review (user_id, book_id, rating, comment) VALUES "
                + "(2, 3, 4, 'a'), (2, 3, 4, 'b'), (2, 3, 4, 'c')");

        String full = mockMvc.perform(get("/book-rating/top10-books")).andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        String ids = mockMvc.perform(get("/book-rating/top10")).andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();

        assertThat((List<Integer>) JsonPath.read(full, "$.data[*].bookId")).containsExactly(5, 2, 3, 1, 4);
        assertThat((List<Integer>) JsonPath.read(ids, "$.data[*].bookId")).containsExactly(5, 2, 3, 1, 4);
    }
}
