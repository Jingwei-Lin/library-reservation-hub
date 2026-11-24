package com.comp5619.libraryreservationreviewhub.controller;

import com.comp5619.libraryreservationreviewhub.annotation.AuthCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Admin-only routes are refused unless the caller is a logged-in admin. Runs the real interceptor
 * (unlike the standalone controller tests) against the seeded accounts in Schema.sql.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthCheckEnforcementIntegrationTest {

    private static final String PASSWORD = "123456789";
    private static final String USER = "jingwei.lin2@example.com";   // user_id 1, not an admin
    private static final String ADMIN = "admin@admin.com";            // user_id 3

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * Every live route that is annotated admin-only and was reachable without login before this change.
     * (/user/add, /user/delete and /user/get/vo are commented out in UserController, so they don't exist.)
     */
    static Stream<Arguments> adminRoutes() {
        return Stream.of(
                Arguments.of("POST /user/update", post("/user/update").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"isAdmin\":true}")),
                Arguments.of("GET /user/get", get("/user/get").param("id", "1")),
                Arguments.of("POST /user/list/page/vo", post("/user/list/page/vo")
                        .contentType(MediaType.APPLICATION_JSON).content("{}")),
                Arguments.of("GET /review/all", get("/review/all")),
                Arguments.of("POST /book-rating/1/recalc", post("/book-rating/1/recalc")),
                Arguments.of("GET /waiting-list/user/1", get("/waiting-list/user/1"))
        );
    }

    @ParameterizedTest(name = "anonymous {0} is refused")
    @MethodSource("adminRoutes")
    void anonymousCallsToAdminRoutesAreRefused(String name, MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request).andExpect(jsonPath("$.code").value(40100));
        assertUserOneIsStillNotAdmin();
    }

    @ParameterizedTest(name = "a normal user calling {0} is refused")
    @MethodSource("adminRoutes")
    void normalUsersCallingAdminRoutesAreRefused(String name, MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.session(login(USER))).andExpect(jsonPath("$.code").value(40101));
        assertUserOneIsStillNotAdmin();
    }

    @Test
    void adminsCanStillCallAdminRoutes() throws Exception {
        MockHttpSession admin = login(ADMIN);
        mockMvc.perform(get("/user/get").param("id", "1").session(admin)).andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(post("/user/list/page/vo").contentType(MediaType.APPLICATION_JSON).content("{}")
                .session(admin)).andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/review/all").session(admin)).andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/waiting-list/user/1").session(admin)).andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void chatRoutesStayOpenToGuests() {
        // guests keep the AI assistant (Jingwei, 24/11), so none of its routes may carry @AuthCheck
        Method[] methods = PersonalAgentController.class.getDeclaredMethods();
        assertThat(Arrays.stream(methods).filter(m -> m.isAnnotationPresent(AuthCheck.class))).isEmpty();
    }

    private MockHttpSession login(String account) throws Exception {
        MvcResult result = mockMvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userAccount\":\"" + account + "\",\"userPassword\":\"" + PASSWORD + "\"}"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private void assertUserOneIsStillNotAdmin() {
        Boolean isAdmin = jdbc.queryForObject("SELECT is_admin FROM users WHERE user_id = 1", Boolean.class);
        assertThat(isAdmin).isFalse();
    }
}
