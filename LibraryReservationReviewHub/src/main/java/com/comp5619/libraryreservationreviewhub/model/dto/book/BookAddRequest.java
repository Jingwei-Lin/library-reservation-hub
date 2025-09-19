package com.comp5619.libraryreservationreviewhub.model.dto.book;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 图书添加请求
 */
@Data
public class BookAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

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
     * 出版年份
     */
    private Integer publicationYear;

    /**
     * ISBN
     */
    private String isbn;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 库存数量
     */
    private Integer quantity;

    /**
     * 图书类型名称
     */
    private String genreName;

    /**
     * 封面URL
     */
    private String coverUrl;

    /**
     * 图书描述
     */
    private String description;
}