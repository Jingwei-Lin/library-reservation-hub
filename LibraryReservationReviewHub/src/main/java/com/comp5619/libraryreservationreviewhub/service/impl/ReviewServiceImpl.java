package com.comp5619.libraryreservationreviewhub.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
import com.comp5619.libraryreservationreviewhub.exception.ThrowUtils;
import com.comp5619.libraryreservationreviewhub.mapper.ReviewMapper;
import com.comp5619.libraryreservationreviewhub.mapper.UserMapper;
import com.comp5619.libraryreservationreviewhub.model.dto.book.ReviewAddRequest;
import com.comp5619.libraryreservationreviewhub.model.dto.review.ReviewQueryRequest;
import com.comp5619.libraryreservationreviewhub.model.dto.review.ReviewUpdateRequest;
import com.comp5619.libraryreservationreviewhub.model.entity.Review;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.vo.ReviewVO;
import com.comp5619.libraryreservationreviewhub.service.ReviewService;
import com.comp5619.libraryreservationreviewhub.service.UserService;

import cn.hutool.db.PageResult;
import jakarta.annotation.Resource;

@Service
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements ReviewService {
    @Resource
    private UserService userService;
    private UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class) // 回滚所有异常
    public long addReview(ReviewAddRequest req, User loginUser) {
        // 有效值检查
        // 1. 检查ReviewAddRequest是否为空
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        // 2. 检查loginUser是否为空
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null, ErrorCode.PARAMS_ERROR);
        // 3. 检查传过来的req是否包含bookid
        ThrowUtils.throwIf(req.getBookId() == null, ErrorCode.PARAMS_ERROR);
        // 4. 检查rating是否包含有效值
        ThrowUtils.throwIf(req.getRating() == null, ErrorCode.PARAMS_ERROR, "Rating should be 1~5");
        return 0;
    }

    @Override
    public boolean updateReview(ReviewUpdateRequest req, User user) {
        return true;
    }

    public boolean deleteReview(long id) {
        return true;
    }

    public Review getReviewBtId(long id) {
        Review review = new Review();
        return review;
    }

    public PageResult<ReviewVO> listByPage(ReviewQueryRequest req) {
        return new PageResult<>();
    }

    public boolean moderate(long id, String status, User operator) {
        return true;
    }

    public ReviewVO toVO(Review review) {
        return new ReviewVO();
    }

    public List<ReviewVO> toList(List<Review> reviewList) {
        return null;
    }
}
