package com.comp5619.libraryreservationreviewhub.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 图书视图对象
 */
@Data
public class BookVO implements Serializable {

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
     * 平均评分
     */
    private BigDecimal averageRating;

    /**
     * 评分数量
     */
    private Integer ratingCount;

    /**
     * 总库存
     */
    private Integer quantity;

    /**
     * 可用库存
     */
    private Integer availableQuantity;

    /**
     * 图书类型
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

    /**
     * 图书状态
     */
    private String status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 最新评论列表
     */
    private List<ReviewVO> recentReviews;
}