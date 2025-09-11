package com.comp5619.libraryreservationreviewhub.model.dto.reservation;

import lombok.Data;

import java.io.Serializable;

/**
 * 预订添加请求
 */
@Data
public class ReservationAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图书ID
     */
    private Long bookId;

    /**
     * 预订天数
     */
    private Integer reservationDays;
}