package com.comp5619.libraryreservationreviewhub.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.comp5619.libraryreservationreviewhub.common.PageRequest;
import com.comp5619.libraryreservationreviewhub.common.PageResult;
import com.comp5619.libraryreservationreviewhub.model.dto.waitingList.*;
import com.comp5619.libraryreservationreviewhub.model.entity.WaitingList;
import com.comp5619.libraryreservationreviewhub.model.vo.NotifyEligibilityVO;
import com.comp5619.libraryreservationreviewhub.model.vo.WaitingListVO;

import java.util.List;

/**
 * 等待列表业务接口
 */
public interface WaitingListService extends IService<WaitingList> {

    /** 创建等待记录 */
    Long create(WaitingListCreateDTO dto);

    /** 更新等待记录状态 */
    boolean updateStatus(WaitingListUpdateDTO dto);

    /** 删除等待记录 */
    boolean deleteById(Long id);

    /** 按ID查询 */
    WaitingListVO getById(Long id);

    /** 条件查询列表 */
    List<WaitingListVO> listByQuery(WaitingListQueryDTO dto);

    List<WaitingListVO> listByUserWithDetails(Long userId);

    /**分页**/
    PageResult<WaitingListVO> pageByQuery(WaitingListQueryDTO query, PageRequest pageReq);

    /**
     * The user's WAITING entries with their place in each queue. Read-only: copies are handed out when they
     * come back (StockService), not when someone looks.
     */
    List<NotifyEligibilityVO> listQueuePositionsForUser(Long userId);

    /**
     * Joins the queue for a book. Refused when the book is missing or deleted, when a copy is on the shelf
     * (reserve it instead), and when the user already has an active reservation or entry for it.
     */
    Long join(Long userId, Long bookId);

    /** Leaves the queue; a copy held for the user passes to the next person */
    void leave(Long userId, Long bookId);

    /**
     * Ends the holds that are older than {@link com.comp5619.libraryreservationreviewhub.service.StockService#HOLD_DAYS}
     * days; each copy passes to the next person waiting. Returns how many ended.
     */
    int expireHolds();
}
