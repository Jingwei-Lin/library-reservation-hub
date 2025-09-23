package com.comp5619.libraryreservationreviewhub.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 图书视图对象 - 修正字段映射
 */
@Data
public class BookVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图书ID - 改为Integer以匹配Book实体
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
     * 图书类别 - 改为genre以匹配Book实体
     */
    private String genre;

    /**
     * 总库存
     */
    private Integer quantity;

    /**
     * 是否可用
     */
    private Boolean available;

    /**
     * 封面路径
     */
    private String path;

    // 移除不必要的字段，除非数据库中有这些数据
    // private String publisher;
    // private Integer publicationYear;
    // private String isbn;
    // private BigDecimal price;
    // private BigDecimal averageRating;
    // private Integer ratingCount;
    // private Integer availableQuantity;
    // private String genreName;
    // private String coverUrl;
    // private String description;
    // private String status;
    // private Date createTime;
    // private List<ReviewVO> recentReviews;
}