package com.comp5619.libraryreservationreviewhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comp5619.libraryreservationreviewhub.model.entity.Genre;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GenreMapper extends BaseMapper<Genre> {
    // Add custom methods if needed (e.g., find by name)
    // Example:
    // Genre selectByName(String name);
}