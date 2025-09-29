package com.comp5619.libraryreservationreviewhub.mapper;

import com.comp5619.libraryreservationreviewhub.model.entity.Reservation;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ReservationMapper {

    @Insert("""
        INSERT INTO Reservation (user_id, book_id, reservation_date, status, create_time)
        VALUES (#{userId}, #{bookId}, #{reservationTime}, #{reservationStatus}, #{createTime})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "reservationId", keyColumn = "reservation_id")
    int insertReservation(Reservation reservation);

    @Select("SELECT * FROM Reservation WHERE reservation_id = #{id} AND is_delete = FALSE")
    Reservation selectById(@Param("id") Long id);

    @Update("""
        UPDATE Reservation 
        SET status = #{reservationStatus}, update_time = CURRENT_TIMESTAMP
        WHERE reservation_id = #{reservationId} AND is_delete = FALSE
        """)
    int updateStatus(@Param("reservationId") Long reservationId, @Param("reservationStatus") String status);

    @Update("UPDATE Reservation SET is_delete = TRUE WHERE reservation_id = #{id}")
    int deleteById(@Param("id") Long id);

    @Select("""
        SELECT * FROM Reservation 
        WHERE user_id = #{userId} AND is_delete = FALSE 
        ORDER BY reservation_date DESC
        """)
    List<Reservation> selectByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT * FROM Reservation 
        WHERE book_id = #{bookId} AND status IN ('active', 'picked_up') AND is_delete = FALSE
        """)
    List<Reservation> selectActiveByBookId(@Param("bookId") Long bookId);
}