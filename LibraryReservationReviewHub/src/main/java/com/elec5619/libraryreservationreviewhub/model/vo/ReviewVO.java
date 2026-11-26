package com.elec5619.libraryreservationreviewhub.model.vo;

import java.util.Date;

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
    /** When the review was posted, with the time of day; an edit doesn't change it */
    private Date reviewDate;
    private String userName;
}