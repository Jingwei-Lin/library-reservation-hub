package com.comp5619.libraryreservationreviewhub.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 预订视图对象
 */
@Data
public class ReservationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 预订ID
     */
    private Long reservationId;

    /**
     * 用户信息
     */
    private String userName;
    private String userAccount;

    /**
     * 图书信息
     */
    private String bookTitle;
    private String bookAuthor;
    private String bookCoverUrl;

    /**
     * 预订状态
     */
    private String status;

    /**
     * 预订日期
     */
    private Date reservationDate;

    /**
     * 取书日期
     */
    private Date pickupDate;

    /**
     * 应还日期
     */
    private Date dueDate;

    /**
     * 实际归还日期
     */
    private Date returnDate;

    /**
     * 逾期费用
     */
    private BigDecimal overdueFee;

    /**
     * 创建时间
     */
    private Date createTime;
}