package com.comp5619.libraryreservationreviewhub.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * A review
 */
@TableName(value = "review")
@Data
public class Review implements Serializable {

    /**
     * Primary key
     */
    @TableId(value = "review_id", type = IdType.AUTO)
    private Long reviewId;

    @TableField("user_id")
    private Long userId;

    @TableField("book_id")
    private Long bookId;

    /**
     * Rating, 1 to 5
     */
    private Integer rating;

    private String comment;

    @TableField("review_date")
    private Date reviewDate;

    private static final long serialVersionUID = 1L;
}