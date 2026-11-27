package com.elec5619.libraryreservationreviewhub.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.elec5619.libraryreservationreviewhub.common.PageRequest;
import com.elec5619.libraryreservationreviewhub.common.PageResult;
import com.elec5619.libraryreservationreviewhub.exception.BusinessException;
import com.elec5619.libraryreservationreviewhub.exception.ErrorCode;
import com.elec5619.libraryreservationreviewhub.exception.ThrowUtils;
import com.elec5619.libraryreservationreviewhub.mapper.ReservationMapper;
import com.elec5619.libraryreservationreviewhub.mapper.WaitingListMapper;
import com.elec5619.libraryreservationreviewhub.model.enums.WaitingStatusEnum;
import com.elec5619.libraryreservationreviewhub.model.dto.waitingList.WaitingListCreateDTO;
import com.elec5619.libraryreservationreviewhub.model.dto.waitingList.WaitingListQueryDTO;
import com.elec5619.libraryreservationreviewhub.model.dto.waitingList.WaitingListUpdateDTO;
import com.elec5619.libraryreservationreviewhub.model.entity.Book;
import com.elec5619.libraryreservationreviewhub.model.entity.WaitingList;
import com.elec5619.libraryreservationreviewhub.model.vo.NotifyEligibilityVO;
import com.elec5619.libraryreservationreviewhub.model.vo.WaitingListVO;
import com.elec5619.libraryreservationreviewhub.service.StockService;
import com.elec5619.libraryreservationreviewhub.service.WaitingListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The waiting list
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WaitingListServiceImpl
        extends ServiceImpl<WaitingListMapper, WaitingList>
        implements WaitingListService {

    /* ---------- paging and sorting (sort fields from a whitelist) ---------- */
    private record PageCalc(int current, int size, String sortField, boolean asc) {}

    /** The only fields a list may be sorted by (column names) */
    private static final Set<String> SORT_WHITELIST = Set.of(
            "wait_id", "user_id", "book_id", "join_date", "notification_date", "is_notified", "status"
    );

    private final StockService stockService;
    private final ReservationMapper reservationMapper;

    /** Paging and sorting from the request (wait_id DESC by default) */
    private PageCalc calc(PageRequest pr) {
        int current = Math.max(1, pr.getCurrent());
        int size = Math.min(Math.max(1, pr.getPageSize()), PageRequest.MAX_PAGE_SIZE);

        String sortField = "wait_id";
        boolean asc = false;

        if (pr.getSortField() != null && SORT_WHITELIST.contains(pr.getSortField())) {
            sortField = pr.getSortField();
            asc = !"descend".equalsIgnoreCase(String.valueOf(pr.getSortOrder()));
        }
        return new PageCalc(current, size, sortField, asc);
    }

    /* ===================== CRUD ===================== */

    @Override
    public Long create(WaitingListCreateDTO dto) {
        ThrowUtils.throwIf(dto == null, ErrorCode.PARAMS_ERROR);

        WaitingList entity = new WaitingList();
        BeanUtils.copyProperties(dto, entity);

        entity.setJoinDate(LocalDateTime.now());
        entity.setStatus(WaitingStatusEnum.WAITING);
        entity.setIsNotified(Boolean.FALSE);

        int rows = baseMapper.insert(entity);
        ThrowUtils.throwIf(rows <= 0 || entity.getWaitId() == null, ErrorCode.OPERATION_ERROR);
        return entity.getWaitId();
    }

    @Override
    public boolean updateStatus(WaitingListUpdateDTO dto) {
        ThrowUtils.throwIf(dto == null || dto.getWaitId() == null, ErrorCode.PARAMS_ERROR);

        WaitingList entity = baseMapper.selectById(dto.getWaitId());
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR, "Waiting-list entry not found");

        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        if (dto.getIsNotified() != null) {
            entity.setIsNotified(dto.getIsNotified());
        }
        if (dto.getNotificationDate() != null) {
            entity.setNotificationDate(dto.getNotificationDate());
        }

        int rows = baseMapper.updateById(entity);
        ThrowUtils.throwIf(rows <= 0, ErrorCode.OPERATION_ERROR);
        return true;
    }

    @Override
    public boolean deleteById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        return baseMapper.deleteById(id) > 0;
    }

    @Override
    public WaitingListVO getById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        WaitingList entity = baseMapper.selectById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);

        WaitingListVO vo = new WaitingListVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /* ===================== Queries ===================== */

    @Override
    public List<WaitingListVO> listByQuery(WaitingListQueryDTO dto) {
        QueryWrapper<WaitingList> wrapper = buildWrapper(dto);

        if (dto.getOrderBy() != null && !dto.getOrderBy().isBlank()) {
            // Parsed into (column, direction) pairs: only whitelisted columns and ASC/DESC reach the SQL.
            // The old check looked at the first word only and appended the whole string.
            for (String part : dto.getOrderBy().split(",")) {
                String[] tokens = part.trim().toLowerCase(Locale.ROOT).split("\\s+");
                ThrowUtils.throwIf(tokens.length == 0 || tokens.length > 2 || !SORT_WHITELIST.contains(tokens[0]),
                        ErrorCode.PARAMS_ERROR, "Unsupported orderBy");
                String direction = tokens.length == 2 ? tokens[1] : "asc";
                ThrowUtils.throwIf(!direction.equals("asc") && !direction.equals("desc"),
                        ErrorCode.PARAMS_ERROR, "Unsupported orderBy");
                wrapper.orderBy(true, direction.equals("asc"), tokens[0]);
            }
        } else {
            wrapper.orderByDesc("wait_id");
        }

        List<WaitingList> list = baseMapper.selectList(wrapper);
        if (CollUtil.isEmpty(list)) return Collections.emptyList();

        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<WaitingListVO> listByUserWithDetails(Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);
        List<WaitingListVO> list = baseMapper.listByUser(userId);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream().collect(Collectors.toList());
    }

    /**
     * A page of entries
     */
    public PageResult<WaitingListVO> pageByQuery(WaitingListQueryDTO dto, PageRequest pageReq) {
        PageCalc p = calc(pageReq);
        QueryWrapper<WaitingList> wrapper = buildWrapper(dto);

        if (p.asc) {
            wrapper.orderByAsc(p.sortField);
        } else {
            wrapper.orderByDesc(p.sortField);
        }

        long total = baseMapper.selectCount(wrapper);

        // the page's window
        if (total == 0) {
            return new PageResult<>(p.current(), p.size(), 0, List.of());
        }

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<WaitingList> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(p.current, p.size, total);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<WaitingList> entityPage =
                baseMapper.selectPage(page, wrapper);

        List<WaitingListVO> records = entityPage.getRecords()
                .stream().map(this::toVO).collect(Collectors.toList());

        return new PageResult<>(p.current(), p.size(), entityPage.getTotal(), records);
    }

    /* ===================== Helpers ===================== */

    private WaitingListVO toVO(WaitingList entity) {
        WaitingListVO vo = new WaitingListVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private QueryWrapper<WaitingList> buildWrapper(WaitingListQueryDTO dto) {
        QueryWrapper<WaitingList> wrapper = new QueryWrapper<>();
        if (dto == null) return wrapper;

        if (dto.getUserId() != null) {
            wrapper.eq("user_id", dto.getUserId());
        }
        if (dto.getBookId() != null) {
            wrapper.eq("book_id", dto.getBookId());
        }
        if (dto.getStatus() != null) {
            wrapper.eq("status", dto.getStatus());
        }
        if (dto.getIsNotified() != null) {
            wrapper.eq("is_notified", dto.getIsNotified());
        }
        if (Boolean.TRUE.equals(dto.getActiveOnly())) {
            // in the query, so the page and its total count the same rows
            wrapper.notIn("status", WaitingStatusEnum.CANCELLED.getValue(), WaitingStatusEnum.FULFILLED.getValue());
        }
        return wrapper;
    }


    @Override
    public List<NotifyEligibilityVO> listQueuePositionsForUser(Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);
        List<NotifyEligibilityVO> rows = baseMapper.selectNotifyEligibilityForUser(userId);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        // a WAITING entry has no copy held for it; once one is, the entry is NOTIFIED and leaves this list
        rows.forEach(vo -> vo.setCanNotify(false));
        return rows;
    }

    @Override
    @Transactional
    public Long join(Long userId, Long bookId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);
        // the book lock first, so a copy coming back at the same moment is seen (and handed out) consistently
        Book book = stockService.lockBook(bookId);
        ThrowUtils.throwIf(Boolean.TRUE.equals(book.getIsDelete()), ErrorCode.NOT_FOUND_ERROR, "Book not found");
        ThrowUtils.throwIf(book.getQuantity() != null && book.getQuantity() > 0, ErrorCode.OPERATION_ERROR,
                "A copy is available: reserve it instead");
        ThrowUtils.throwIf(reservationMapper.countActive(userId, bookId) > 0, ErrorCode.OPERATION_ERROR,
                "You have already reserved this book");

        long exists = lambdaQuery()
                .eq(WaitingList::getUserId, userId)
                .eq(WaitingList::getBookId, bookId)
                .in(WaitingList::getStatus, WaitingStatusEnum.WAITING, WaitingStatusEnum.NOTIFIED)
                .count();
        ThrowUtils.throwIf(exists > 0, ErrorCode.PARAMS_ERROR, "This book is currently on the waiting list.");

        WaitingListCreateDTO dto = new WaitingListCreateDTO();
        dto.setUserId(userId);
        dto.setBookId(bookId);
        try {
            return create(dto);   // the database allows one active entry per user and book
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "This book is currently on the waiting list.");
        }
    }

    @Override
    @Transactional
    public void leave(Long userId, Long bookId) {
        stockService.cancelWaiting(bookId, userId);
    }

    /** Each hold ends in its own transaction (StockService.expireHold), so one failure doesn't undo the rest */
    @Override
    public int expireHolds() {
        int ended = 0;
        for (WaitingList hold : baseMapper.selectExpiredHolds(StockService.HOLD_DAYS)) {
            try {
                if (stockService.expireHold(hold.getWaitId(), hold.getBookId())) {
                    ended++;
                }
            } catch (RuntimeException e) {
                log.warn("Could not end hold {} on book {}", hold.getWaitId(), hold.getBookId(), e);
            }
        }
        log.info("Ended {} holds older than {} days", ended, StockService.HOLD_DAYS);
        return ended;
    }

}
