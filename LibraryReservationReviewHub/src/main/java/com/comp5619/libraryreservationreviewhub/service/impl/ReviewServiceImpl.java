package com.comp5619.libraryreservationreviewhub.service.impl;

import java.sql.Date;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
import com.comp5619.libraryreservationreviewhub.exception.ThrowUtils;
import com.comp5619.libraryreservationreviewhub.mapper.ReviewMapper;
import com.comp5619.libraryreservationreviewhub.model.dto.book.ReviewAddRequest;
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
    private UserService userService;

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

    @Override
    public ReviewVO getReviewVO(Review review) {
        if (review == null) return null;
        ReviewVO vo = new ReviewVO();
        BeanUtils.copyProperties(review, vo);
        // 如果 ReviewVO 字段名不同，手动映射：
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
        List<Review> list = reviewMapper.selectByBookId(bookId);
        return getReviewVOList(list);
    }


}
