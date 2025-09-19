package com.comp5619.libraryreservationreviewhub.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 等待列表实体类
 */
@TableName(value = "waiting_list")
@Data
public class WaitingList implements Serializable {



    /**
     * 等待ID - 主键
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
     * 加入等待列表时间
     */
    private Date joinTime;

    /**
     * 通知时间
     */
    private Date notificationTime;

    /**
     * 是否已通知
     */
    private Integer isNotified;

    /**
     * 等待状态：waiting/notified/expired/cancelled
     */
    private String waitingStatus;

    /**
     * 优先级
     */
    private Integer priority;

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