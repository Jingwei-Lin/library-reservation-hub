package com.elec5619.libraryreservationreviewhub.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Reviews with the same date come in a fixed order (newest id first), so pages neither repeat nor skip any; the
 * waiting-list page reports the page and size it used. The 8 seed reviews share one timestamp.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SteadyPagingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private MockHttpSession login(String email) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userAccount\":\"" + email + "\",\"userPassword\":\"123456789\"}"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getRequest().getSession();
    }

    private String body(String url, MockHttpSession session) throws Exception {
        return mockMvc.perform(get(url).session(session)).andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void reviewPagesWithEqualDatesNeitherRepeatNorSkip() throws Exception {
        MockHttpSession admin = login("admin@admin.com");
        List<Integer> ids = new ArrayList<>();
        for (int page = 1; page <= 3; page++) {
            ids.addAll(JsonPath.read(body("/review/all?current=" + page + "&pageSize=3", admin), "$.data.records[*].reviewId"));
        }

        assertThat(ids).containsExactly(8, 7, 6, 5, 4, 3, 2, 1);
    }

    @Test
    void aBooksAndAUsersReviewsComeInTheSameOrder() throws Exception {
        MockHttpSession reader = login("jingwei.lin2@example.com");

        assertThat((List<Integer>) JsonPath.read(body("/review/book/1", reader), "$.data[*].reviewId"))
                .containsExactly(3, 2, 1);
        assertThat((List<Integer>) JsonPath.read(body("/review/book/2", reader), "$.data[*].reviewId"))
                .containsExactly(5, 4);
    }

    @Test
    void theWaitingListPageReportsThePageAndSizeItUsed() throws Exception {
        String page = body("/waiting-list/my/page?current=0&pageSize=500", login("jingwei.lin2@example.com"));

        assertThat((Integer) JsonPath.read(page, "$.data.current")).isEqualTo(1);
        assertThat((Integer) JsonPath.read(page, "$.data.pageSize")).isEqualTo(100);
    }
}
