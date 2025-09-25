package com.comp5619.libraryreservationreviewhub.service;

import com.comp5619.libraryreservationreviewhub.model.dto.book.ReviewAddRequest;
import com.comp5619.libraryreservationreviewhub.model.entity.User;

public interface ReviewService {
    /**
     * 添加review，必须是登录后的
     * 
     * @param req
     * @param loginUser
     * @return
     */
    long addReview(ReviewAddRequest req, User loginUser);

}
