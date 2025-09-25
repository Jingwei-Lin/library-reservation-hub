package com.comp5619.libraryreservationreviewhub.model.dto.review;

import java.io.Serializable;

import lombok.Data;

@Data
public class ReviewUpdateRequest implements Serializable {
    private long reviewId;
    private Integer rating;
    private String comment;
}
