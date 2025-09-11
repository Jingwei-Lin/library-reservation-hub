package com.comp5619.libraryreservationreviewhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comp5619.libraryreservationreviewhub.model.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 图书数据访问层
 * Book Mapper interface for database operations
 */
@Mapper
public interface BookMapper extends BaseMapper<Book> {

    /**
     * 根据类型ID查询图书列表
     */
    @Select("SELECT * FROM book WHERE genreId = #{genreId} AND isDelete = 0")
    List<Book> selectByGenreId(Long genreId);

    /**
     * 根据状态查询图书列表
     */
    @Select("SELECT * FROM book WHERE bookStatus = #{status} AND isDelete = 0")
    List<Book> selectByStatus(String status);

    /**
     * 查询可借阅图书
     */
    @Select("SELECT * FROM book WHERE availableQuantity > 0 AND bookStatus = 'AVAILABLE' AND isDelete = 0")
    List<Book> selectAvailableBooks();
}