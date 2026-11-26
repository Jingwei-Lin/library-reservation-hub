package com.comp5619.libraryreservationreviewhub.controller;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.comp5619.libraryreservationreviewhub.common.PageRequest;
import com.comp5619.libraryreservationreviewhub.common.PageResult;
import com.comp5619.libraryreservationreviewhub.mapper.BookMapper;
import com.comp5619.libraryreservationreviewhub.model.Enum.WaitingStatusEnum;
import com.comp5619.libraryreservationreviewhub.model.dto.waitingList.WaitingListCreateDTO;
import com.comp5619.libraryreservationreviewhub.model.dto.waitingList.WaitingListQueryDTO;
import com.comp5619.libraryreservationreviewhub.model.dto.waitingList.WaitingListUpdateDTO;
import com.comp5619.libraryreservationreviewhub.model.entity.Book;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.entity.WaitingList;
import com.comp5619.libraryreservationreviewhub.model.vo.NotifyEligibilityVO;
import com.comp5619.libraryreservationreviewhub.model.vo.WaitingListVO;
import com.comp5619.libraryreservationreviewhub.service.BookService;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import com.comp5619.libraryreservationreviewhub.service.WaitingListService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * WaitingListController tests (standalone, JUnit5 + Mockito + MockMvc).
 */
@ExtendWith(MockitoExtension.class)
class WaitingListControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WaitingListService waitingListService;

    @Mock
    private UserService userService;

    @InjectMocks
    private WaitingListController waitingListController;

    private User normalUser;
    private User adminUser;

    private WaitingListVO waitingVO;
    private WaitingListVO cancelledVO;
    private WaitingListVO fulfilledVO;

    @BeforeEach
    void setUp() {
        // so LocalDateTime's @JsonFormat applies
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(waitingListController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(om))
                .build();

        normalUser = new User();
        normalUser.setId(1L);
        normalUser.setStatus(1);

        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setStatus(1);

        waitingVO = new WaitingListVO();
        waitingVO.setWaitId(11L);
        waitingVO.setUserId(1L);
        waitingVO.setBookId(100L);
        waitingVO.setStatus(WaitingStatusEnum.WAITING);

        cancelledVO = new WaitingListVO();
        cancelledVO.setWaitId(12L);
        cancelledVO.setUserId(1L);
        cancelledVO.setBookId(101L);
        cancelledVO.setStatus(WaitingStatusEnum.CANCELLED);

        fulfilledVO = new WaitingListVO();
        fulfilledVO.setWaitId(13L);
        fulfilledVO.setUserId(1L);
        fulfilledVO.setBookId(102L);
        fulfilledVO.setStatus(WaitingStatusEnum.FULFILLED);
    }

    /** ---------- helper: a chain mock, without overload ambiguity ---------- */
    @SuppressWarnings("unchecked")
    private LambdaQueryChainWrapper<WaitingList> mkChain() {
        // eq/ne/in/orderByAsc/last return the mock itself, so calls chain
        return mock(LambdaQueryChainWrapper.class, invocation -> {
            String name = invocation.getMethod().getName();
            if ("eq".equals(name) || "ne".equals(name) || "in".equals(name)
                    || "orderByAsc".equals(name) || "last".equals(name)) {
                return invocation.getMock();
            }
            // everything else gets the default answer (the tests stub one() and count() themselves)
            return org.mockito.Mockito.RETURNS_DEFAULTS.answer(invocation);
        });
    }

    @Test
    void listMyWaitlist_UserFiltersCancelledAndFulfilled() throws Exception {
        when(userService.getLoginUser(any(HttpServletRequest.class))).thenReturn(normalUser);
        when(userService.isAdmin(eq(normalUser))).thenReturn(false);

        List<WaitingListVO> raw = new ArrayList<>();
        raw.add(waitingVO);
        raw.add(cancelledVO);
        raw.add(fulfilledVO);

        when(waitingListService.listByUserWithDetails(1L)).thenReturn(raw);

        mockMvc.perform(get("/waiting-list/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("waiting"));
    }

    @Test
    void listMyWaitlist_AdminSeesAll() throws Exception {
        when(userService.getLoginUser(any(HttpServletRequest.class))).thenReturn(adminUser);
        when(userService.isAdmin(eq(adminUser))).thenReturn(true);

        List<WaitingListVO> raw = List.of(waitingVO, cancelledVO, fulfilledVO);
        when(waitingListService.listByUserWithDetails(2L)).thenReturn(new ArrayList<>(raw));

        mockMvc.perform(get("/waiting-list/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    void listMyByStatus_Success() throws Exception {
        when(userService.getLoginUser(any(HttpServletRequest.class))).thenReturn(normalUser);
        when(userService.isAdmin(eq(normalUser))).thenReturn(false);
        when(waitingListService.listByQuery(any(WaitingListQueryDTO.class)))
                .thenReturn(List.of(waitingVO));

        mockMvc.perform(get("/waiting-list/my/status").param("status", "waiting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("waiting"));
    }

    @Test
    void pageMyWaitlist_NonAdminFiltersCancelled() throws Exception {
        when(userService.getLoginUser(any(HttpServletRequest.class))).thenReturn(normalUser);
        when(userService.isAdmin(eq(normalUser))).thenReturn(false);

        // the finished entries are left out by the query itself, so the total matches the rows
        PageResult<WaitingListVO> page = new PageResult<>(1L, 10L, 1L, List.of(waitingVO));
        when(waitingListService.pageByQuery(argThat(q -> Boolean.TRUE.equals(q.getActiveOnly()) && q.getUserId() == 1L),
                any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/waiting-list/my/page")
                        .param("current", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].status").value("waiting"));
    }

    @Test
    void myNotifyEligibility_Success() throws Exception {
        when(userService.getLoginUser(any(HttpServletRequest.class))).thenReturn(normalUser);

        NotifyEligibilityVO vo = new NotifyEligibilityVO();
        vo.setWaitId(900L);
        vo.setUserId(1L);
        vo.setBookId(100L);
        vo.setBookTitle("Clean Code");
        vo.setJoinDate(LocalDateTime.of(2025, 1, 2, 15, 30, 0)); // "2025-01-02 15:30:00"
        vo.setPosition(2);
        vo.setTotalWaiting(5L);
        vo.setQuantity(0);
        vo.setHeldQuantity(1);
        vo.setAvailable(false);
        vo.setNotifiedCount(1);
        vo.setCanNotify(true);

        when(waitingListService.listQueuePositionsForUser(1L))
                .thenReturn(List.of(vo));

        mockMvc.perform(get("/waiting-list/my/eligibility"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].bookId").value(100))
                .andExpect(jsonPath("$.data[0].bookTitle").value("Clean Code"))
                .andExpect(jsonPath("$.data[0].joinDate").value("2025-01-02 15:30:00"))
                .andExpect(jsonPath("$.data[0].position").value(2))
                .andExpect(jsonPath("$.data[0].totalWaiting").value(5))
                .andExpect(jsonPath("$.data[0].quantity").value(0))
                .andExpect(jsonPath("$.data[0].heldQuantity").value(1))
                .andExpect(jsonPath("$.data[0].available").value(false))
                .andExpect(jsonPath("$.data[0].notifiedCount").value(1))
                .andExpect(jsonPath("$.data[0].canNotify").value(true));
    }

    @Test
    void addToMyWaitlist_Success() throws Exception {
        when(userService.getLoginUser(any(HttpServletRequest.class))).thenReturn(normalUser);
        // the rules for joining (no shelf copy, no duplicate) live in WaitingListService.join
        when(waitingListService.join(1L, 100L)).thenReturn(100L);

        mockMvc.perform(post("/waiting-list/my/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(100));
    }

    @Test
    void cancelMyWait_LeavesTheQueue() throws Exception {
        when(userService.getLoginUser(any(HttpServletRequest.class))).thenReturn(normalUser);

        mockMvc.perform(delete("/waiting-list/my/200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
        // a copy held for the user passes on inside WaitingListService.leave (StockService)
        verify(waitingListService).leave(1L, 200L);
    }

    @Test
    void listByUserAdmin_Success() throws Exception {
        when(waitingListService.listByQuery(any(WaitingListQueryDTO.class)))
                .thenReturn(List.of(waitingVO, cancelledVO, fulfilledVO));

        mockMvc.perform(get("/waiting-list/user/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));
    }
}
