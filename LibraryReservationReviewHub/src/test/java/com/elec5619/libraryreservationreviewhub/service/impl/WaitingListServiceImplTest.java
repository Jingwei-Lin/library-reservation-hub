package com.elec5619.libraryreservationreviewhub.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.elec5619.libraryreservationreviewhub.common.PageRequest;
import com.elec5619.libraryreservationreviewhub.common.PageResult;
import com.elec5619.libraryreservationreviewhub.mapper.WaitingListMapper;
import com.elec5619.libraryreservationreviewhub.model.enums.WaitingStatusEnum;
import com.elec5619.libraryreservationreviewhub.model.dto.waitingList.WaitingListCreateDTO;
import com.elec5619.libraryreservationreviewhub.model.dto.waitingList.WaitingListQueryDTO;
import com.elec5619.libraryreservationreviewhub.model.entity.WaitingList;
import com.elec5619.libraryreservationreviewhub.model.vo.NotifyEligibilityVO;
import com.elec5619.libraryreservationreviewhub.model.vo.WaitingListVO;
import com.elec5619.libraryreservationreviewhub.mapper.ReservationMapper;
import com.elec5619.libraryreservationreviewhub.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WaitingListServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class WaitingListServiceImplTest {

    @Mock
    private WaitingListMapper waitingListMapper;

    @Mock
    private StockService stockService;

    @Mock
    private ReservationMapper reservationMapper;

    @InjectMocks
    private WaitingListServiceImpl service;

    @BeforeEach
    void injectBaseMapper() {
        // baseMapper lives in the ServiceImpl superclass, so it is set by reflection
        ReflectionTestUtils.setField(service, "baseMapper", waitingListMapper);
    }

    /* -------------------- create -------------------- */

    @Test
    void create_success() {
        WaitingListCreateDTO dto = new WaitingListCreateDTO();
        dto.setUserId(1L);
        dto.setBookId(100L);

        // the insert also sets the entity's id
        doAnswer(inv -> {
            WaitingList e = inv.getArgument(0);
            e.setWaitId(10L);
            return 1;
        }).when(waitingListMapper).insert(any(WaitingList.class));

        Long id = service.create(dto);
        assertEquals(10L, id);
        verify(waitingListMapper, times(1)).insert(any(WaitingList.class));
    }

    /* -------------------- getById -------------------- */

    @Test
    void getById_success() {
        WaitingList e = new WaitingList();
        e.setWaitId(7L);
        e.setUserId(1L);
        e.setBookId(100L);
        e.setStatus(WaitingStatusEnum.WAITING);
        when(waitingListMapper.selectById(7L)).thenReturn(e);

        var vo = service.getById(7L);
        assertEquals(7L, vo.getWaitId());
        assertEquals(1L, vo.getUserId());
        assertEquals(100L, vo.getBookId());
        assertEquals(WaitingStatusEnum.WAITING, vo.getStatus());
    }

    /* -------------------- listByQuery -------------------- */

    @Test
    void listByQuery_success() {
        WaitingList e1 = new WaitingList();
        e1.setWaitId(1L); e1.setUserId(1L); e1.setBookId(100L); e1.setStatus(WaitingStatusEnum.WAITING);
        WaitingList e2 = new WaitingList();
        e2.setWaitId(2L); e2.setUserId(1L); e2.setBookId(101L); e2.setStatus(WaitingStatusEnum.CANCELLED);

        when(waitingListMapper.selectList(any())).thenReturn(List.of(e1, e2));

        WaitingListQueryDTO q = new WaitingListQueryDTO();
        q.setUserId(1L);

        List<WaitingListVO> list = service.listByQuery(q);
        assertEquals(2, list.size());
        assertEquals(1L, list.get(0).getUserId());
        assertEquals(100L, list.get(0).getBookId());
    }

    /* -------------------- listByUserWithDetails -------------------- */

    @Test
    void listByUserWithDetails_success() {
        WaitingListVO vo1 = new WaitingListVO();
        vo1.setWaitId(11L); vo1.setUserId(1L); vo1.setBookId(100L); vo1.setStatus(WaitingStatusEnum.WAITING);
        WaitingListVO vo2 = new WaitingListVO();
        vo2.setWaitId(12L); vo2.setUserId(1L); vo2.setBookId(101L); vo2.setStatus(WaitingStatusEnum.NOTIFIED);

        when(waitingListMapper.listByUser(1L)).thenReturn(List.of(vo1, vo2));

        List<WaitingListVO> out = service.listByUserWithDetails(1L);
        assertEquals(2, out.size());
        assertEquals(11L, out.get(0).getWaitId());
    }

    /* -------------------- pageByQuery -------------------- */

    @Test
    void pageByQuery_success() {
        // total = 2
        when(waitingListMapper.selectCount(any())).thenReturn(2L);

        WaitingList e1 = new WaitingList();
        e1.setWaitId(1L); e1.setUserId(1L); e1.setBookId(100L); e1.setStatus(WaitingStatusEnum.WAITING);
        WaitingList e2 = new WaitingList();
        e2.setWaitId(2L); e2.setUserId(1L); e2.setBookId(101L); e2.setStatus(WaitingStatusEnum.WAITING);

        Page<WaitingList> mpPage = new Page<>(1, 10, 2);
        mpPage.setRecords(List.of(e1, e2));
        when(waitingListMapper.selectPage(any(), any())).thenReturn(mpPage);

        WaitingListQueryDTO q = new WaitingListQueryDTO();
        q.setUserId(1L);

        PageRequest pr = new PageRequest();
        pr.setCurrent(1);
        pr.setPageSize(10);
        pr.setSortField("wait_id"); // a whitelisted field
        pr.setSortOrder("descend");

        PageResult<WaitingListVO> page = service.pageByQuery(q, pr);
        assertEquals(2L, page.getTotal());
        assertEquals(2, page.getRecords().size());
        assertEquals(1L, page.getRecords().get(0).getUserId());
    }

    /* -------------------- listQueuePositionsForUser (read-only) -------------------- */

    @Test
    void listQueuePositions_noRows() {
        when(waitingListMapper.selectNotifyEligibilityForUser(1L)).thenReturn(List.of());
        assertTrue(service.listQueuePositionsForUser(1L).isEmpty());
    }

    @Test
    void listQueuePositions_onlyReports() {
        NotifyEligibilityVO vo = new NotifyEligibilityVO();
        vo.setBookId(200L);
        vo.setHeldQuantity(2);
        vo.setQuantity(0);
        vo.setPosition(1);
        vo.setNotifiedCount(0);
        when(waitingListMapper.selectNotifyEligibilityForUser(5L)).thenReturn(List.of(vo));

        List<NotifyEligibilityVO> out = service.listQueuePositionsForUser(5L);

        assertEquals(1, out.get(0).getPosition());
        assertEquals(Boolean.FALSE, out.get(0).getCanNotify());
        // looking at the queue writes nothing: copies are handed out when they come back
        verify(waitingListMapper, never()).markNotified(anyLong());
        verify(waitingListMapper, never()).update(any(), any());
    }

    @Test
    void leave_goesThroughTheStockService() {
        service.leave(1L, 200L);
        verify(stockService).cancelWaiting(200L, 1L);
    }

    /* -------------------- expireHolds -------------------- */

    @Test
    void expireHolds_endsEachHoldThroughTheStockService() {
        WaitingList first = new WaitingList();
        first.setWaitId(1L);
        first.setBookId(10L);
        WaitingList second = new WaitingList();
        second.setWaitId(2L);
        second.setBookId(20L);
        when(waitingListMapper.selectExpiredHolds(3)).thenReturn(List.of(first, second));
        when(stockService.expireHold(1L, 10L)).thenReturn(true);
        when(stockService.expireHold(2L, 20L)).thenReturn(false);   // reserved in the meantime

        assertEquals(1, service.expireHolds());
    }

    @Test
    void expireHolds_oneFailureDoesNotStopTheRest() {
        WaitingList first = new WaitingList();
        first.setWaitId(1L);
        first.setBookId(10L);
        WaitingList second = new WaitingList();
        second.setWaitId(2L);
        second.setBookId(20L);
        when(waitingListMapper.selectExpiredHolds(3)).thenReturn(List.of(first, second));
        when(stockService.expireHold(1L, 10L)).thenThrow(new RuntimeException("deadlock"));
        when(stockService.expireHold(2L, 20L)).thenReturn(true);

        assertEquals(1, service.expireHolds());
    }
}
