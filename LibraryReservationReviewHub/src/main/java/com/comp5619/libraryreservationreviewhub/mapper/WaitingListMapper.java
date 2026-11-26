package com.comp5619.libraryreservationreviewhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comp5619.libraryreservationreviewhub.model.Enum.WaitingStatusEnum;
import com.comp5619.libraryreservationreviewhub.model.entity.WaitingList;
import com.comp5619.libraryreservationreviewhub.model.vo.NotifyEligibilityVO;
import com.comp5619.libraryreservationreviewhub.model.vo.WaitingListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface WaitingListMapper extends BaseMapper<WaitingList> {

    List<WaitingListVO> listByUser(@Param("userId") Long userId);

    int updateStatus(@Param("waitId") Long waitId,
                     @Param("status") WaitingStatusEnum status);

    /** The user's WAITING entries with their place in each queue and the book's stock. Read-only. */
    List<NotifyEligibilityVO> selectNotifyEligibilityForUser(@Param("userId") Long userId);

    /** Notified entries whose hold is older than {@code days}: candidates for StockService.expireHold */
    @Select("""
        SELECT wait_id AS waitId, user_id AS userId, book_id AS bookId, status
        FROM waiting_list
        WHERE status = 'notified' AND notification_date < NOW() - INTERVAL #{days} DAY
        ORDER BY notification_date
        """)
    List<WaitingList> selectExpiredHolds(@Param("days") int days);

    /** Ends a hold, if it is still notified and still older than {@code days} (called with the book locked) */
    @Update("""
        UPDATE waiting_list SET status = 'cancelled', is_notified = FALSE
        WHERE wait_id = #{waitId} AND status = 'notified' AND notification_date < NOW() - INTERVAL #{days} DAY
        """)
    int expireHold(@Param("waitId") Long waitId, @Param("days") int days);

    /** Ends a deleted book's queue: its waiting and notified entries are cancelled */
    @Update("""
        UPDATE waiting_list
        SET status = 'cancelled', is_notified = FALSE
        WHERE book_id = #{bookId} AND status IN ('waiting', 'notified')
        """)
    int cancelActiveForBook(@Param("bookId") Long bookId);

    /*
     * The queue. These are only called with the book row locked (StockService), so the reads are locking reads:
     * they see the latest rows, not a snapshot from before the lock.
     */

    /** The first person waiting for the book: the oldest join, then the lowest id */
    @Select("""
        SELECT wait_id AS waitId, user_id AS userId, book_id AS bookId, status
        FROM waiting_list
        WHERE book_id = #{bookId} AND status = 'waiting'
        ORDER BY join_date, wait_id
        LIMIT 1
        FOR UPDATE
        """)
    WaitingList selectNextWaitingForUpdate(@Param("bookId") Long bookId);

    /** The user's waiting or notified entry for the book */
    @Select("""
        SELECT wait_id AS waitId, user_id AS userId, book_id AS bookId, status
        FROM waiting_list
        WHERE user_id = #{userId} AND book_id = #{bookId} AND status IN ('waiting', 'notified')
        ORDER BY wait_id
        LIMIT 1
        FOR UPDATE
        """)
    WaitingList selectActiveForUpdate(@Param("userId") Long userId, @Param("bookId") Long bookId);

    /** A copy is held for this entry from now on */
    @Update("""
        UPDATE waiting_list
        SET status = 'notified', is_notified = TRUE, notification_date = NOW()
        WHERE wait_id = #{waitId} AND status = 'waiting'
        """)
    int markNotified(@Param("waitId") Long waitId);

    /** The user reserved the copy held for them */
    @Update("""
        UPDATE waiting_list SET status = 'fulfilled'
        WHERE user_id = #{userId} AND book_id = #{bookId} AND status = 'notified'
        """)
    int fulfilNotified(@Param("userId") Long userId, @Param("bookId") Long bookId);

    /** The user reserved a shelf copy of a book they were still waiting for */
    @Update("""
        UPDATE waiting_list SET status = 'fulfilled'
        WHERE user_id = #{userId} AND book_id = #{bookId} AND status = 'waiting'
        """)
    int fulfilWaiting(@Param("userId") Long userId, @Param("bookId") Long bookId);

    @Update("""
        UPDATE waiting_list SET status = 'cancelled', is_notified = FALSE
        WHERE wait_id = #{waitId} AND status IN ('waiting', 'notified')
        """)
    int cancelEntry(@Param("waitId") Long waitId);
}

