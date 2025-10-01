package com.comp5619.libraryreservationreviewhub.mapper;

import com.comp5619.libraryreservationreviewhub.model.entity.Review;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReviewMapper {

    /**
     * Select all reviews for a specific book, ordered by review date (newest first)
     */
    @Select("SELECT review_id AS reviewId, user_id AS userId, book_id AS bookId, rating, comment, review_date AS reviewDate " +
            "FROM Review WHERE book_id = #{bookId} ORDER BY review_date DESC")
    List<Review> selectByBookId(@Param("bookId") Long bookId);

    /* ================= 基础 CRUD ================= */

}