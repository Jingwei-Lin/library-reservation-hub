package com.elec5619.libraryreservationreviewhub.mapper;

import com.elec5619.libraryreservationreviewhub.model.entity.Review;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReviewMapper {

    /**
     * Select all reviews for a specific book, ordered by review date (newest first)
     */
    @Select("SELECT review_id AS reviewId, user_id AS userId, book_id AS bookId, rating, comment, review_date AS reviewDate " +
            "FROM review WHERE book_id = #{bookId} ORDER BY review_date DESC, review_id DESC")
    List<Review> selectByBookId(@Param("bookId") Long bookId);

    /* ================= CRUD ================= */

    /**
     * Insert a new review
     */
    @Insert("INSERT INTO review (user_id, book_id, rating, comment, review_date) " +
            "VALUES (#{userId}, #{bookId}, #{rating}, #{comment}, #{reviewDate})")
    @Options(useGeneratedKeys = true, keyProperty = "reviewId", keyColumn = "review_id")
    int insertReview(Review review);

    /**
     * Select a review by its ID
     */
    @Select("SELECT review_id AS reviewId, user_id AS userId, book_id AS bookId, rating, comment, review_date AS reviewDate " +
            "FROM review WHERE review_id = #{reviewId}")
    Review selectById(@Param("reviewId") Long reviewId);

    /**
     * All of one user's reviews, newest first
     */
    @Select("SELECT review_id AS reviewId, user_id AS userId, book_id AS bookId, rating, comment, review_date AS reviewDate " +
            "FROM review WHERE user_id = #{userId} ORDER BY review_date DESC, review_id DESC")
    List<Review> selectByUserId(@Param("userId") Long userId);

    /**
     * Update an existing review's rating and comment
     */
    @Update("UPDATE review SET rating = #{rating}, comment = #{comment} " +
            "WHERE review_id = #{reviewId}")
    int updateReview(Review review);

    /**
     * Delete a review by its ID
     */
    @Delete("DELETE FROM review WHERE review_id = #{reviewId}")
    int deleteById(@Param("reviewId") Long reviewId);

    /**
     * Count reviews for a given book
     */
    @Select("SELECT COUNT(1) FROM review WHERE book_id = #{bookId}")
    long countByBookId(@Param("bookId") Long bookId);

    /* ================= Pagination for site-wide reviews ================= */

    /**
     * Count reviews by optional filters (site-wide)
     */
    @Select({
        "<script>",
        "SELECT COUNT(1) FROM review",
        "<where>",
        "  <if test=\"bookId != null\">AND book_id = #{bookId}</if>",
        "  <if test=\"userId != null\">AND user_id = #{userId}</if>",
        "  <if test=\"minRating != null\">AND rating &gt;= #{minRating}</if>",
        "  <if test=\"maxRating != null\">AND rating &lt;= #{maxRating}</if>",
        "</where>",
        "</script>"
    })
    long countAllByQuery(@Param("bookId") Long bookId,
                         @Param("userId") Long userId,
                         @Param("minRating") Integer minRating,
                         @Param("maxRating") Integer maxRating);

    /**
     * Select reviews by optional filters with pagination (site-wide)
     */
    @Select({
        "<script>",
        "SELECT review_id AS reviewId, user_id AS userId, book_id AS bookId, rating, comment, review_date AS reviewDate",
        "FROM review",
        "<where>",
        "  <if test=\"bookId != null\">AND book_id = #{bookId}</if>",
        "  <if test=\"userId != null\">AND user_id = #{userId}</if>",
        "  <if test=\"minRating != null\">AND rating &gt;= #{minRating}</if>",
        "  <if test=\"maxRating != null\">AND rating &lt;= #{maxRating}</if>",
        "</where>",
        "ORDER BY review_date DESC, review_id DESC",
        "LIMIT #{limit} OFFSET #{offset}",
        "</script>"
    })
    List<Review> selectAllByQuery(@Param("bookId") Long bookId,
                                  @Param("userId") Long userId,
                                  @Param("minRating") Integer minRating,
                                  @Param("maxRating") Integer maxRating,
                                  @Param("offset") long offset,
                                  @Param("limit") long limit);
}