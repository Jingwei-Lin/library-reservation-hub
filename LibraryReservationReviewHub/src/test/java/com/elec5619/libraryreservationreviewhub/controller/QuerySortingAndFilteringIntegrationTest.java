package com.elec5619.libraryreservationreviewhub.controller;

import com.elec5619.libraryreservationreviewhub.exception.BusinessException;
import com.elec5619.libraryreservationreviewhub.model.dto.waitingList.WaitingListQueryDTO;
import com.elec5619.libraryreservationreviewhub.service.WaitingListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Sorting and filtering in the admin user list (POST /user/list/page/vo) and the waiting-list query:
 * sort fields come from a whitelist (no SQL injection), and the filters use the real column names.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QuerySortingAndFilteringIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WaitingListService waitingListService;

    private MockHttpSession admin;

    @BeforeEach
    void logInAdmin() throws Exception {
        MvcResult result = mockMvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userAccount\":\"admin@admin.com\",\"userPassword\":\"123456789\"}"))
                .andExpect(jsonPath("$.code").value(0)).andReturn();
        admin = (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test
    void anSqlExpressionAsSortFieldIsRefused() throws Exception {
        // the boolean-blind injection from the 24/11 security review (02-H3)
        list("{\"sortField\":\"(CASE WHEN (SELECT SUBSTR(password,1,1) FROM users WHERE user_id=3)='0' "
                + "THEN email ELSE phone END)\"}")
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void sortingByAWhitelistedFieldFollowsTheRequestedOrder() throws Exception {
        list("{\"sortField\":\"userAccount\",\"sortOrder\":\"ascend\"}")
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].userAccount").value("admin@admin.com"));
        list("{\"sortField\":\"userAccount\",\"sortOrder\":\"descend\"}")
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].userAccount").value("jingwei.lin2@example.com"));
    }

    @Test
    void filteringByNameRoleOrIdUsesTheRealColumns() throws Exception {
        list("{\"firstName\":\"Hanchen\"}").andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].userAccount").value("hanchen.wang@example.com"));
        list("{\"isAdmin\":true}").andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].userAccount").value("admin@admin.com"));
        list("{\"id\":1}").andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void theAdminPageRequestStillReturnsEveryUser() throws Exception {
        // exactly what AdminDashboard sends today
        list("{\"current\":1,\"pageSize\":20}")
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(3));
    }

    @Test
    void waitingListOrderByAcceptsOnlyWhitelistedColumnsAndDirections() {
        WaitingListQueryDTO injected = new WaitingListQueryDTO();
        injected.setOrderBy("join_date desc; DELETE FROM waiting_list");
        assertThatThrownBy(() -> waitingListService.listByQuery(injected)).isInstanceOf(BusinessException.class);

        WaitingListQueryDTO valid = new WaitingListQueryDTO();
        valid.setOrderBy("join_date DESC, wait_id asc");
        assertThatNoException().isThrownBy(() -> waitingListService.listByQuery(valid));
    }

    private ResultActions list(String body) throws Exception {
        return mockMvc.perform(post("/user/list/page/vo").contentType(MediaType.APPLICATION_JSON)
                .content(body).session(admin));
    }
}
