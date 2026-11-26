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
 * The waiting list
 */
public interface WaitingListService extends IService<WaitingList> {

    /** Creates an entry */
    Long create(WaitingListCreateDTO dto);

    /** Changes an entry's status */
    boolean updateStatus(WaitingListUpdateDTO dto);

    /** Deletes an entry */
    boolean deleteById(Long id);

    /** An entry by id */
    WaitingListVO getById(Long id);

    /** Entries matching the filters */
    List<WaitingListVO> listByQuery(WaitingListQueryDTO dto);

    List<WaitingListVO> listByUserWithDetails(Long userId);

    /** A page of entries */
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
