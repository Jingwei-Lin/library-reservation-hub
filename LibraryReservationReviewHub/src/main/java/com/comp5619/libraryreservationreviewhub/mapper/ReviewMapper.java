package com.comp5619.libraryreservationreviewhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comp5619.libraryreservationreviewhub.model.entity.Review;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {
    // Add custom methods if needed (e.g., find reviews by bookId)
    // Example:
    // List<Review> selectByBookId(Long bookId);
}