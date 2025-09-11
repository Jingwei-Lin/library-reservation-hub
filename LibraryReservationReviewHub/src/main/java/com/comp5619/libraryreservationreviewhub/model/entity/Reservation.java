package com.comp5619.libraryreservationreviewhub.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 预订实体类
 */
@TableName(value = "reservation")
@Data
public class Reservation implements Serializable {



    /**
     * 预订ID - 主键
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
     * 预订时间
     */
    private Date reservationTime;

    /**
     * 取书时间
     */
    private Date pickupTime;

    /**
     * 到期时间
     */
    private Date dueTime;

    /**
     * 归还时间
     */
    private Date returnTime;

    /**
     * 预订状态：reserved/borrowed/returned/overdue/cancelled
     */
    private String reservationStatus;

    /**
     * 逾期费用
     */
    private BigDecimal overdueFee;

    /**
     * 备注
     */
    private String remarks;

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