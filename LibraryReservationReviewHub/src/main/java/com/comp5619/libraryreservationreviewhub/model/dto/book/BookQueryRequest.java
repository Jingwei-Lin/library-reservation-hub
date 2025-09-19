package com.comp5619.libraryreservationreviewhub.model.dto.book;

import lombok.Data;

import java.io.Serializable;

/**
 * 图书查询请求
 */
@Data
public class BookQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图书ID
     */
    private Long bookId;

    /**
     * 书名
     */
    private String title;

    /**
     * 作者
     */
    private String author;

    /**
     * 出版社
     */
    private String publisher;

    /**
     * ISBN
     */
    private String isbn;

    /**
     * 图书类型ID
     */
    private Long genreId;

    /**
     * 图书状态
     */
    private String status;

    /**
     * 搜索关键词
     */
    private String searchText;

    /**
     * 当前页号
     */
    private long current = 1;

    /**
     * 页面大小
     */
    private long pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（升序/降序）
     */
    private String sortOrder;

}