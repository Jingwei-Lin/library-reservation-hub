package com.comp5619.libraryreservationreviewhub.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.comp5619.libraryreservationreviewhub.common.PageRequest;
import com.comp5619.libraryreservationreviewhub.common.PageResult;
import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
import com.comp5619.libraryreservationreviewhub.exception.ThrowUtils;
import com.comp5619.libraryreservationreviewhub.mapper.ReservationMapper;
import com.comp5619.libraryreservationreviewhub.mapper.WaitingListMapper;
import com.comp5619.libraryreservationreviewhub.model.Enum.WaitingStatusEnum;
import com.comp5619.libraryreservationreviewhub.model.dto.waitingList.WaitingListCreateDTO;
import com.comp5619.libraryreservationreviewhub.model.dto.waitingList.WaitingListQueryDTO;
import com.comp5619.libraryreservationreviewhub.model.dto.waitingList.WaitingListUpdateDTO;
import com.comp5619.libraryreservationreviewhub.model.entity.Book;
import com.comp5619.libraryreservationreviewhub.model.entity.WaitingList;
import com.comp5619.libraryreservationreviewhub.model.vo.NotifyEligibilityVO;
import com.comp5619.libraryreservationreviewhub.model.vo.WaitingListVO;
import com.comp5619.libraryreservationreviewhub.service.StockService;
import com.comp5619.libraryreservationreviewhub.service.WaitingListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 等待列表业务实现类（参照 BookServiceImpl 风格）
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WaitingListServiceImpl
        extends ServiceImpl<WaitingListMapper, WaitingList>
        implements WaitingListService {

    /* ---------- 分页与排序拼装（带字段白名单） ---------- */
    private record PageCalc(int current, int size, String sortField, boolean asc) {}

    /** 仅允许这些字段参与排序（列名与数据库一致） */
    private static final Set<String> SORT_WHITELIST = Set.of(
            "wait_id", "user_id", "book_id", "join_date", "notification_date", "is_notified", "status"
    );

    private final StockService stockService;
    private final ReservationMapper reservationMapper;

    /** 计算分页与排序参数（默认 wait_id DESC） */
    private PageCalc calc(PageRequest pr) {
        int current = Math.max(1, pr.getCurrent());
        int size = Math.min(Math.max(1, pr.getPageSize()), 101);

        String sortField = "wait_id";
        boolean asc = false; // 默认 DESC

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
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR, "等待记录不存在");

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

    /* ===================== 查询 ===================== */

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
     * 分页查询
     */
    public PageResult<WaitingListVO> pageByQuery(WaitingListQueryDTO dto, PageRequest pageReq) {
        PageCalc p = calc(pageReq);
        QueryWrapper<WaitingList> wrapper = buildWrapper(dto);

        // 排序
        if (p.asc) {
            wrapper.orderByAsc(p.sortField);
        } else {
            wrapper.orderByDesc(p.sortField);
        }

        // 统计总数
        long total = baseMapper.selectCount(wrapper);

        // 计算分页窗口
        if (total == 0) {
            return new PageResult<>(pageReq.getCurrent(), pageReq.getPageSize(), 0, List.of());
        }

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<WaitingList> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(p.current, p.size, total);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<WaitingList> entityPage =
                baseMapper.selectPage(page, wrapper);

        List<WaitingListVO> records = entityPage.getRecords()
                .stream().map(this::toVO).collect(Collectors.toList());

        return new PageResult<>(pageReq.getCurrent(), pageReq.getPageSize(), entityPage.getTotal(), records);
    }

    /* ===================== 私有工具 ===================== */

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
        return create(dto);
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
