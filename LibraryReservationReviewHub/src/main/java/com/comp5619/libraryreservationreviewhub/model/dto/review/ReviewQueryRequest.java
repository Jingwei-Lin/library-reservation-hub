package com.comp5619.libraryreservationreviewhub.model.dto.review;

import com.comp5619.libraryreservationreviewhub.common.PageRequest;

import lombok.Data;

@Data
public class ReviewQueryRequest extends PageRequest {
    private Long bookId;
    private Long userId;
    private Integer minRating;
    private Integer maxRating;
}
