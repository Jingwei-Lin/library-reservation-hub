package com.comp5619.libraryreservationreviewhub.model.dto.review;

import java.io.Serializable;

import lombok.Data;

@Data
public class ReviewAddRequest implements Serializable {
    private Long bookId;
    private Integer rating;
    private String commnet;
}
