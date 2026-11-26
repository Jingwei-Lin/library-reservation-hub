package com.elec5619.libraryreservationreviewhub.mapper;

import com.elec5619.libraryreservationreviewhub.model.entity.Reservation;
import org.apache.ibatis.annotations.*;

import java.util.Date;
import java.util.List;

@Mapper
public interface ReservationMapper {

    @Insert("""
        INSERT INTO reservation (user_id, book_id, reservation_date, pickup_date, due_date, status, create_time)
        VALUES (#{userId}, #{bookId}, #{reservationTime}, #{pickupTime}, #{dueTime}, #{reservationStatus}, #{createTime})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "reservationId", keyColumn = "reservation_id")
    int insertReservation(Reservation reservation);

    @Select("""
        SELECT reservation_id, user_id, book_id, reservation_date AS reservationTime, 
               pickup_date AS pickupTime, due_date AS dueTime, return_date AS returnTime, 
               status AS reservationStatus, create_time, update_time, is_delete
        FROM reservation 
        WHERE reservation_id = #{id} AND is_delete = FALSE
        """)
    Reservation selectById(@Param("id") Long id);

    /**
     * Changes the status only if it is still {@code from}, so of two changes at the same moment only one wins.
     * A return is dated. Returns 0 when the reservation changed in the meantime.
     */
    @Update("""
        UPDATE reservation
        SET status = #{to},
            update_time = CURRENT_TIMESTAMP,
            return_date = IF(#{to} = 'returned', CURRENT_TIMESTAMP, return_date)
        WHERE reservation_id = #{reservationId} AND is_delete = FALSE AND status = #{from}
        """)
    int changeStatus(@Param("reservationId") Long reservationId, @Param("from") String from, @Param("to") String to);

    @Select("""
        SELECT reservation_id, user_id, book_id, reservation_date AS reservationTime, 
               pickup_date AS pickupTime, due_date AS dueTime, return_date AS returnTime, 
               status AS reservationStatus, create_time, update_time, is_delete
        FROM reservation 
        WHERE user_id = #{userId} AND is_delete = FALSE 
        ORDER BY reservation_date DESC
        """)
    List<Reservation> selectByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT reservation_id, user_id, book_id, reservation_date AS reservationTime, 
               pickup_date AS pickupTime, due_date AS dueTime, return_date AS returnTime, 
               status AS reservationStatus, create_time, update_time, is_delete
        FROM reservation 
        WHERE is_delete = FALSE 
        ORDER BY reservation_date DESC
        """)
    List<Reservation> selectAll();

    @Select("""
        SELECT reservation_id, user_id, book_id, reservation_date AS reservationTime, 
               pickup_date AS pickupTime, due_date AS dueTime, return_date AS returnTime, 
               status AS reservationStatus, create_time, update_time, is_delete
        FROM reservation 
        WHERE book_id = #{bookId} AND status IN ('reserved', 'borrowed') AND is_delete = FALSE
        """)
    List<Reservation> selectActiveByBookId(@Param("bookId") Long bookId);

    @Update("""
        UPDATE reservation 
        SET pickup_date = #{pickupTime}, update_time = CURRENT_TIMESTAMP
        WHERE reservation_id = #{reservationId} AND is_delete = FALSE
        """)
    int updatePickupTime(@Param("reservationId") Long reservationId, @Param("pickupTime") Date pickupTime);

    @Select("""
        SELECT COUNT(*) FROM reservation
        WHERE user_id = #{userId} AND book_id = #{bookId} AND status IN ('reserved', 'borrowed') AND is_delete = FALSE
        """)
    long countActive(@Param("userId") Long userId, @Param("bookId") Long bookId);

}