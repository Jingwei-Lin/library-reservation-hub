package com.comp5619.libraryreservationreviewhub.mapper;

import com.comp5619.libraryreservationreviewhub.model.entity.Review;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReviewMapper {

    @Select("SELECT * FROM review WHERE id = #{id}")
    Review selectById(Long id);

    @Select("""
    SELECT COUNT(1) FROM review
    WHERE userId = #{userId} AND bookId = #{bookId}
    """)
    long countByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);

    @Insert("""
    INSERT INTO review
      (bookId, userId, rating, comment, reviewStatus, likeCount, createTime, updateTime, isDelete)
    VALUES
      (#{bookId}, #{userId}, #{rating}, #{comment}, #{reviewStatus}, #{likeCount}, #{createTime}, #{updateTime}, 0)
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertReview(Review r);

    @Delete("DELETE FROM review WHERE id = #{id}")
    int deleteById(Long id);

    @Select("""
    SELECT * FROM review
    WHERE bookId = #{bookId} AND reviewStatus = #{status}
    ORDER BY createTime DESC
    """)
    List<Review> selectByBookAndStatus(@Param("bookId") Long bookId, @Param("status") String reviewStatus);
}
