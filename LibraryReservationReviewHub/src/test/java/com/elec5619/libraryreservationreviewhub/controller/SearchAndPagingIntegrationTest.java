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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Book search matches its keyword literally, paging is stable, and page sizes are bounded (seed data: 101 books).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SearchAndPagingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** GET /book/search with "name=value&..." as request parameters (passed as they are, not URL-decoded) */
    private String search(String query) throws Exception {
        MockHttpServletRequestBuilder request = get("/book/search");
        for (String pair : query.split("&")) {
            String[] nv = pair.split("=", 2);
            request.param(nv[0], nv[1]);
        }
        return mockMvc.perform(request).andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
    }

    private MockHttpSession admin() throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userAccount\":\"admin@admin.com\",\"userPassword\":\"123456789\"}"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getRequest().getSession();
    }

    @Test
    void percentAndUnderscoreMatchOnlyThemselves() throws Exception {
        jdbcTemplate.update("INSERT INTO book (title, author, genre, description, quantity, `path`) "
                + "VALUES ('50% Off', 'A. Writer', 'Fantasy', 'plain', 1, NULL)");

        String percent = search("keyword=%&current=1&pageSize=12");
        assertThat((Integer) JsonPath.read(percent, "$.data.total")).isEqualTo(1);
        assertThat((String) JsonPath.read(percent, "$.data.records[0].title")).isEqualTo("50% Off");

        String underscore = search("keyword=_&current=1&pageSize=12");
        assertThat((Integer) JsonPath.read(underscore, "$.data.total")).isZero();
    }

    @Test
    void pagingByGenreShowsEveryBookOnceInAFixedOrder() throws Exception {
        List<Integer> seen = new ArrayList<>();
        for (int page = 1; page <= 11; page++) {
            String body = search("sortField=genre&sortOrder=ascend&current=" + page + "&pageSize=10");
            seen.addAll(JsonPath.read(body, "$.data.records[*].bookId"));
        }

        assertThat(seen).hasSize(101);
        assertThat(new HashSet<>(seen)).hasSize(101);
        // within a genre, the book id decides
        List<String> genres = new ArrayList<>();
        for (int page = 1; page <= 11; page++) {
            String body = search("sortField=genre&sortOrder=ascend&current=" + page + "&pageSize=10");
            genres.addAll(JsonPath.read(body, "$.data.records[*].genre"));
        }
        for (int i = 1; i < seen.size(); i++) {
            if (genres.get(i).equals(genres.get(i - 1))) {
                assertThat(seen.get(i)).isGreaterThan(seen.get(i - 1));
            }
        }
    }

    @Test
    void aBookPageHoldsAtMost100AndSaysSo() throws Exception {
        String body = search("current=1&pageSize=500");

        assertThat((List<?>) JsonPath.read(body, "$.data.records")).hasSize(100);
        assertThat((Integer) JsonPath.read(body, "$.data.pageSize")).isEqualTo(100);
    }

    @Test
    void reviewAndUserPagesHoldAtMost100() throws Exception {
        MockHttpSession session = admin();

        mockMvc.perform(get("/review/all?current=1&pageSize=500").session(session))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(100));
        mockMvc.perform(post("/user/list/page/vo").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"current\":1,\"pageSize\":500}"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.size").value(100));
    }
}
