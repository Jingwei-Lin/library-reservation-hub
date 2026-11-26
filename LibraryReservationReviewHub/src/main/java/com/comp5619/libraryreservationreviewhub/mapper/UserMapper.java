package com.comp5619.libraryreservationreviewhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import org.apache.ibatis.annotations.Mapper;


/**
 * User Mapper interface for database operations
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}