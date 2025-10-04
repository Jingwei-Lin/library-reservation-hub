package com.comp5619.libraryreservationreviewhub.service.impl;

import java.sql.Date;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.comp5619.libraryreservationreviewhub.mapper.BookMapper;
import com.comp5619.libraryreservationreviewhub.model.entity.Book;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import com.comp5619.libraryreservationreviewhub.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
import com.comp5619.libraryreservationreviewhub.exception.ThrowUtils;
import com.comp5619.libraryreservationreviewhub.mapper.ReviewMapper;
import com.comp5619.libraryreservationreviewhub.model.dto.review.ReviewAddRequest;
import com.comp5619.libraryreservationreviewhub.model.dto.review.ReviewQueryRequest;
import com.comp5619.libraryreservationreviewhub.model.dto.review.ReviewUpdateRequest;
import com.comp5619.libraryreservationreviewhub.model.entity.Review;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.vo.ReviewVO;
import com.comp5619.libraryreservationreviewhub.service.ReviewService;

import cn.hutool.db.PageResult;
import jakarta.annotation.Resource;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    @Resource
    private ReviewMapper reviewMapper;
    private final BookMapper bookMapper;
    private UserService userService;
    private final BookService bookService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addReview(Long bookId, ReviewAddRequest req, User user) {
        log.info("Adding review for bookId: {}, userId: {}", bookId, user.getId());
        ThrowUtils.throwIf(bookId == null || bookId <= 0, ErrorCode.PARAMS_ERROR, "Invalid book ID");
        ThrowUtils.throwIf(user == null, ErrorCode.NO_AUTH_ERROR, "User not logged in");
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR, "Reservation details are required");

        // Check if book is available
        ThrowUtils.throwIf(!bookService.isBookAvailable(bookId), ErrorCode.OPERATION_ERROR, "Book is not available");

        // Create review
        Review review = new Review();
        review.setUserId(user.getId());
        review.setBookId(bookId);
        review.setRating(req.getRating());
        review.setComment(req.getComment());
        review.setReviewDate(new java.util.Date());
        log.info("Review object: {}", review);

        // Insert reservation
        int rows = reviewMapper.insertReview(review);
        log.info("Inserted review, rows affected: {}", rows);
        ThrowUtils.throwIf(rows <= 0, ErrorCode.OPERATION_ERROR, "Failed to create review");
        return review.getReviewId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteReview(Long reviewId, User user) {
        log.info("Attempting to cancel reviewId: {} for userId: {}", reviewId, user.getId());
        ThrowUtils.throwIf(reviewId == null || reviewId <= 0, ErrorCode.PARAMS_ERROR, "Invalid review ID");
        ThrowUtils.throwIf(user == null, ErrorCode.NO_AUTH_ERROR, "User not logged in");

        Review review = reviewMapper.selectById(reviewId);
        log.info("Fetched review: {}", review);
        ThrowUtils.throwIf(review == null, ErrorCode.NOT_FOUND_ERROR, "Review not found");
        ThrowUtils.throwIf(!review.getUserId().equals(user.getId()) && !userService.isAdmin(user),
                ErrorCode.NO_AUTH_ERROR, "Not authorized to cancel this review");

        int rows = reviewMapper.deleteById(reviewId);
        log.info("Review {} deleted, rows affected: {}", reviewId, rows);
        ThrowUtils.throwIf(rows <= 0, ErrorCode.OPERATION_ERROR, "Failed to delete review in database");
        return true;
    }

    @Override
    public ReviewVO getReviewVO(Review review) {
        if (review == null) return null;
        ReviewVO vo = new ReviewVO();
        BeanUtils.copyProperties(review, vo);

        vo.setBookId(review.getBookId());
        vo.setReviewId(review.getReviewId());
        vo.setReviewDate(new java.sql.Date(review.getReviewDate().getTime()));
        vo.setUserId(review.getUserId());
        vo.setComment(review.getComment());
        vo.setRating(review.getRating());
        // System.out.println(userService.getById(review.getUserId()));
        // vo.setUserName(userService.getUserNameById(review.getUserId()));

        return vo;
    }

    @Override
    public List<ReviewVO> getReviewVOList(List<Review> reviews) {
        if (CollUtil.isEmpty(reviews)) return CollUtil.newArrayList();
        return reviews.stream().map(this::getReviewVO).collect(Collectors.toList());
    }

    @Override
    public List<ReviewVO> getReviewsByBookId(long bookId) {
        ThrowUtils.throwIf(bookId <= 0, ErrorCode.PARAMS_ERROR, "Invalid book ID");
        List<Review> reviews = reviewMapper.selectByBookId(bookId);
        return getReviewVOList(reviews);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateReview(ReviewUpdateRequest req, User user) {
        log.info("Attempting to update reviewId: {} for userId: {}", req.getReviewId(), user.getId());
        ThrowUtils.throwIf(req == null || req.getReviewId() <= 0, ErrorCode.PARAMS_ERROR, "Invalid review ID");
        ThrowUtils.throwIf(user == null || user.getId() == null, ErrorCode.NO_AUTH_ERROR, "User not logged in");
        ThrowUtils.throwIf(req.getRating() == null || req.getRating() < 1 || req.getRating() > 5, ErrorCode.PARAMS_ERROR, "Rating must be between 1 and 5");
        ThrowUtils.throwIf(StrUtil.isBlank(req.getComment()), ErrorCode.PARAMS_ERROR, "Comment cannot be empty");

        Review existing = reviewMapper.selectById(req.getReviewId());
        ThrowUtils.throwIf(existing == null, ErrorCode.NOT_FOUND_ERROR, "Review not found");
        ThrowUtils.throwIf(!existing.getUserId().equals(user.getId()) && !userService.isAdmin(user), ErrorCode.NO_AUTH_ERROR, "Not authorized to update this review");

        Review review = new Review();
        review.setReviewId(req.getReviewId());
        review.setRating(req.getRating());
        review.setComment(req.getComment());
        review.setReviewDate(new java.util.Date());
        int rows = reviewMapper.updateReview(review);
        log.info("Updated reviewId: {}, rows affected: {}", req.getReviewId(), rows);
        ThrowUtils.throwIf(rows <= 0, ErrorCode.OPERATION_ERROR, "Failed to update review");
        return true;
    }

    @Override
    public Review getReviewById(long reviewId) {
        ThrowUtils.throwIf(reviewId <= 0, ErrorCode.PARAMS_ERROR, "Invalid review ID");
        Review review = reviewMapper.selectById(reviewId);
        ThrowUtils.throwIf(review == null, ErrorCode.NOT_FOUND_ERROR, "Review not found");
        return review;
    }

    public PageResult<ReviewVO> listByPage(ReviewQueryRequest req) {
        return new PageResult<>();
    }

    public boolean moderate(long id, String status, User operator) {
        return true;
    }

    public String getUserNameByReviewId(long reviewId) {
        ThrowUtils.throwIf(reviewId <= 0, ErrorCode.PARAMS_ERROR, "Invalid review ID");
        Review review = reviewMapper.selectById(reviewId);
        ThrowUtils.throwIf(review == null, ErrorCode.NOT_FOUND_ERROR, "Review not found");
        User user = userService.getById(review.getUserId());
        return user != null ? user.getFirstName() + " " + user.getLastName() : "Unknown User";
    }




}
