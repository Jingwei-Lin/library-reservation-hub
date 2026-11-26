package com.elec5619.libraryreservationreviewhub.model.dto.review;

import com.elec5619.libraryreservationreviewhub.common.PageRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;


@EqualsAndHashCode(callSuper = true)
@Data
public class ReviewQueryRequest extends PageRequest {
    private Long bookId;
    private Long userId;
    private Integer minRating; // with maxRating: a rating range
    private Integer maxRating;
}
