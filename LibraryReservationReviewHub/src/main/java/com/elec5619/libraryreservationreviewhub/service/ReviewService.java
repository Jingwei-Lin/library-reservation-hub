package com.elec5619.libraryreservationreviewhub.service;

import java.util.List;

import com.elec5619.libraryreservationreviewhub.model.dto.review.ReviewAddRequest;
import com.elec5619.libraryreservationreviewhub.model.dto.review.ReviewQueryRequest;
import com.elec5619.libraryreservationreviewhub.model.dto.review.ReviewUpdateRequest;
import com.elec5619.libraryreservationreviewhub.model.entity.Review;
import com.elec5619.libraryreservationreviewhub.model.entity.User;
import com.elec5619.libraryreservationreviewhub.model.vo.ReviewVO;

import com.elec5619.libraryreservationreviewhub.common.PageResult;

public interface ReviewService {
    /**
     * Adds a review; the user must be signed in
     *
     * @param bookId
     * @param req
     * @param user
     * @return the new review's id
     */
    long addReview(Long bookId, ReviewAddRequest req, User user);

    /**
     * Updates a review
     * 
     * @param req
     * @param user
     * @return whether it was updated
     */
    boolean updateReview(ReviewUpdateRequest req, User user);

    /**
     * Deletes a review
     * 
     * @param reviewId
     * @return whether it was deleted
     */
    boolean deleteReview(Long reviewId, User user);

    /**
     * A review by id
     * 
     * @param reviewId
     * @return the review
     */
    Review getReviewById(long reviewId);

    /**
     * All of one user's reviews
     *
     * @param userId the user's id
     * @return the reviews
     */
    List<ReviewVO> getReviewsByUserId(Long userId);



    /**
     * A page of reviews
     * 
     * @param req
     * @return pageResult
     */
    PageResult<ReviewVO> listByPage(ReviewQueryRequest req);

    /**
     * A review as a ReviewVO
     * 
     * @param review
     * @return the view
     */
    ReviewVO getReviewVO(Review review);

    /**
     * Reviews as ReviewVOs
     * 
     * @param reviewList
     * @return the views
     */
    List<ReviewVO> getReviewVOList(List<Review> reviewList);

    /**
     * One book's reviews
     *
     * @param bookId
     * @return the views
     */
    List<ReviewVO> getReviewsByBookId(long bookId);

    String getUserNameByReviewId(long reviewId);
}
