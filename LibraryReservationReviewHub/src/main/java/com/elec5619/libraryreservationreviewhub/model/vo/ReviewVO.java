package com.elec5619.libraryreservationreviewhub.model.vo;

import java.sql.Date;

import lombok.Data;

/**
 * A review, as the API returns it
 */
@Data
public class ReviewVO {
    private static final long serialVersionUID = 1L;
    private Long reviewId;
    private Long userId;
    private Long bookId;
    private Integer rating;
    private String comment;
    private Date reviewDate;
    private String userName;
}