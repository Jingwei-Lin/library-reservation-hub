package com.comp5619.libraryreservationreviewhub.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 评论视图对象
 */
@Data
public class ReviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    private Long reviewId;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 图书标题
     */
    private String bookTitle;

    /**
     * 评分
     */
    private Integer rating;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 创建时间
     */
    private Date createTime;
}