package com.comp5619.libraryreservationreviewhub.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 图书实体类
 */
@TableName(value = "book")
@Data
public class Book implements Serializable {



    /**
     * 图书ID - 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

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
     * 总数量
     */
    private Integer quantity;

    /**
     * 可借数量
     */
    private Integer availableQuantity;

    /**
     * 类型ID
     */
    private Long genreId;

    /**
     * 封面URL
     */
    private String coverUrl;

    /**
     * 描述
     */
    private String description;

    /**
     * 图书状态：available/unavailable/maintenance/deleted
     */
    private String bookStatus;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}