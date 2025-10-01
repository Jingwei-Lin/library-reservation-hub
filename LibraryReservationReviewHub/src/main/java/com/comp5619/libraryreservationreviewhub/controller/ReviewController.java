package com.comp5619.libraryreservationreviewhub.controller;

import com.comp5619.libraryreservationreviewhub.common.BaseResponse;
import com.comp5619.libraryreservationreviewhub.common.ResultUtils;
import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
import com.comp5619.libraryreservationreviewhub.exception.ThrowUtils;
import com.comp5619.libraryreservationreviewhub.model.vo.ReviewVO;
import com.comp5619.libraryreservationreviewhub.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 获取某本书的所有书评: GET /book/1/reservations
     */
    @GetMapping("/book/{bookId}")
    public BaseResponse<List<ReviewVO>> get(@PathVariable long bookId) {
        return ResultUtils.success(reviewService.getReviewsByBookId(bookId));
    }

}