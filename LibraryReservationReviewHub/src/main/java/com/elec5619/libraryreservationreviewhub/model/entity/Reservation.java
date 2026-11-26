package com.elec5619.libraryreservationreviewhub.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * A reservation
 */
@TableName(value = "reservation")
@Data
public class Reservation implements Serializable {
    /**
     * ID
     */
    @TableId(value = "reservation_id")
    private Long reservationId;

    @TableField("user_id")
    private Long userId;

    @TableId(value = "book_id")
    private Long bookId;

    @TableId(value = "reservation_date")
    private Date reservationTime;

    /**
     * When the user collects the book
     */

    @TableId(value = "pickup_date")
    private Date pickupTime;

    /**
     * When the book is due back
     */
    @TableId(value = "due_date")
    private Date dueTime;

    /**
     * When it was returned
     */
    @TableId(value = "return_date")
    private Date returnTime;

    /**
     * Status: reserved/borrowed/returned/overdue/cancelled
     */
    @TableId(value = "status")
    private String reservationStatus;

    @TableId(value = "create_time")
    private Date createTime;

    @TableId(value = "update_time")
    private Date updateTime;

    @TableLogic
    @TableId(value = "is_delete")
    private Boolean isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}