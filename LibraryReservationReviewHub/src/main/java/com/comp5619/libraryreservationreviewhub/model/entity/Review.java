package com.comp5619.libraryreservationreviewhub.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 评论实体类
 */
@TableName(value = "review")
@Data
public class Review implements Serializable {



    /**
     * 评论ID - 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 图书ID
     */
    private Long bookId;

    /**
     * 评分 (1-5星)
     */
    private Integer rating;

    /**
     * 评论内容
     */
    private String comment;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 评论状态：published/pending/rejected/deleted
     */
    private String reviewStatus;

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