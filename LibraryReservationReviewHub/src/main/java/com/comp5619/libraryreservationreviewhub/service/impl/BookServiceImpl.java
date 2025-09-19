//package com.comp5619.libraryreservationreviewhub.service.impl;
//
//import cn.hutool.core.collection.CollUtil;
//import cn.hutool.core.util.ObjUtil;
//import cn.hutool.core.util.StrUtil;
//import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
//import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
//import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
//import com.comp5619.libraryreservationreviewhub.exception.BusinessException;
//import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
//import com.comp5619.libraryreservationreviewhub.exception.ThrowUtils;
//import com.comp5619.libraryreservationreviewhub.mapper.BookMapper;
//import com.comp5619.libraryreservationreviewhub.mapper.GenreMapper;
//import com.comp5619.libraryreservationreviewhub.mapper.ReviewMapper;
//import com.comp5619.libraryreservationreviewhub.model.Enum.BookStatusEnum;
//import com.comp5619.libraryreservationreviewhub.model.Enum.ReviewStatusEnum;
//import com.comp5619.libraryreservationreviewhub.model.Enum.UserRoleEnum;
//import com.comp5619.libraryreservationreviewhub.model.dto.book.BookAddRequest;
//import com.comp5619.libraryreservationreviewhub.model.dto.book.BookQueryRequest;
//import com.comp5619.libraryreservationreviewhub.model.dto.book.ReviewAddRequest;
//import com.comp5619.libraryreservationreviewhub.model.entity.Book;
//import com.comp5619.libraryreservationreviewhub.model.entity.Genre;
//import com.comp5619.libraryreservationreviewhub.model.entity.Review;
//import com.comp5619.libraryreservationreviewhub.model.entity.User;
//import com.comp5619.libraryreservationreviewhub.model.vo.BookVO;
//import com.comp5619.libraryreservationreviewhub.model.vo.ReviewVO;
//import com.comp5619.libraryreservationreviewhub.model.vo.UserVO;
//import com.comp5619.libraryreservationreviewhub.service.BookService;
//import com.comp5619.libraryreservationreviewhub.service.UserService;
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.BeanUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
////import javax.annotation.Resource;
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.util.Date;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
///**
// * 图书服务实现类
// */
//@Service
//@Slf4j
//public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {
//
//    @Autowired
//    private UserService userService;
//
//    @Autowired
//    private GenreMapper genreMapper;
//
//    @Autowired
//    private ReviewMapper reviewMapper;
//
//    // ========== 图书管理 ==========
//
//    @Override
//    public long addBook(BookAddRequest bookAddRequest, HttpServletRequest request) {
//        // 1. 校验参数
//        ThrowUtils.throwIf(bookAddRequest == null, ErrorCode.PARAMS_ERROR);
//
//        String title = bookAddRequest.getTitle();
//        String author = bookAddRequest.getAuthor();
//        String isbn = bookAddRequest.getIsbn();
//
//        ThrowUtils.throwIf(StrUtil.hasBlank(title, author, isbn), ErrorCode.PARAMS_ERROR, "图书信息不能为空");
//
//        // 2. 校验权限（只有管理员可以添加图书）
//        User loginUser = userService.getLoginUser(request);
//        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
//
//        // 3. 校验ISBN是否已存在
//        QueryWrapper<Book> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("isbn", isbn);
//        long count = this.baseMapper.selectCount(queryWrapper);
//        ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "ISBN已存在");
//
//        // 4. 创建图书
//        Book book = new Book();
//        BeanUtils.copyProperties(bookAddRequest, book);
//        book.setBookStatus(BookStatusEnum.AVAILABLE.getValue());
//        book.setAverageRating(BigDecimal.ZERO);
//        book.setRatingCount(0);
//        book.setCreateTime(new Date());
//        book.setUpdateTime(new Date());
//
//        // 5. 保存到数据库
//        boolean result = this.save(book);
//        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
//
//        return book.getId();
//    }
//
//    @Override
//    public boolean deleteBook(long id, HttpServletRequest request) {
//        // 1. 校验参数
//        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
//
//        // 2. 校验权限
//        User loginUser = userService.getLoginUser(request);
//        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
//
//        // 3. 校验图书是否存在
//        Book book = this.getById(id);
//        ThrowUtils.throwIf(book == null, ErrorCode.NOT_FOUND_ERROR);
//
//        // 4. 逻辑删除
//        return this.removeById(id);
//    }
//
//    @Override
//    public boolean updateBook(Book book, HttpServletRequest request) {
//        // 1. 校验参数
//        ThrowUtils.throwIf(book == null || book.getId() == null, ErrorCode.PARAMS_ERROR);
//
//        // 2. 校验权限
//        User loginUser = userService.getLoginUser(request);
//        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
//
//        // 3. 校验图书是否存在
//        Book oldBook = this.getById(book.getId());
//        ThrowUtils.throwIf(oldBook == null, ErrorCode.NOT_FOUND_ERROR);
//
//        // 4. 更新时间
//        book.setUpdateTime(new Date());
//
//        // 5. 更新图书
//        return this.updateById(book);
//    }
//
//    @Override
//    public Book getBookById(long id) {
//        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
//        Book book = this.getById(id);
//        ThrowUtils.throwIf(book == null, ErrorCode.NOT_FOUND_ERROR);
//        return book;
//    }
//
//    @Override
//    public BookVO getBookVO(Book book, HttpServletRequest request) {
//        if (book == null) {
//            return null;
//        }
//
//        BookVO bookVO = new BookVO();
//        BeanUtils.copyProperties(book, bookVO);
//
//        // 获取类型信息
//        if (book.getGenreId() != null) {
//            Genre genre = genreMapper.selectById(book.getGenreId());
//            if (genre != null) {
//                bookVO.setGenreName(genre.getName());
//            }
//        }
//
//        return bookVO;
//    }
//
//    @Override
//    public List<BookVO> getBookVOList(List<Book> bookList, HttpServletRequest request) {
//        if (CollUtil.isEmpty(bookList)) {
//            return CollUtil.newArrayList();
//        }
//
//        return bookList.stream().map(book -> getBookVO(book, request)).collect(Collectors.toList());
//    }
//
//    @Override
//    public Page<BookVO> getBookVOPage(Page<Book> bookPage, HttpServletRequest request) {
//        List<Book> bookList = bookPage.getRecords();
//        Page<BookVO> bookVOPage = new Page<>(bookPage.getCurrent(), bookPage.getSize(), bookPage.getTotal());
//        if (CollUtil.isEmpty(bookList)) {
//            return bookVOPage;
//        }
//
//        List<BookVO> bookVOList = getBookVOList(bookList, request);
//        bookVOPage.setRecords(bookVOList);
//        return bookVOPage;
//    }
//
//    @Override
//    public QueryWrapper<Book> getQueryWrapper(BookQueryRequest bookQueryRequest) {
//        QueryWrapper<Book> queryWrapper = new QueryWrapper<>();
//        if (bookQueryRequest == null) {
//            return queryWrapper;
//        }
//
//        String title = bookQueryRequest.getTitle();
//        String author = bookQueryRequest.getAuthor();
//        String isbn = bookQueryRequest.getIsbn();
//        Long genreId = bookQueryRequest.getGenreId();
//        String bookStatus = bookQueryRequest.getBookStatus();
//
//        // 模糊查询
//        queryWrapper.like(StrUtil.isNotBlank(title), "title", title);
//        queryWrapper.like(StrUtil.isNotBlank(author), "author", author);
//        queryWrapper.eq(StrUtil.isNotBlank(isbn), "isbn", isbn);
//        queryWrapper.eq(ObjUtil.isNotNull(genreId), "genreId", genreId);
//        queryWrapper.eq(StrUtil.isNotBlank(bookStatus), "bookStatus", bookStatus);
//
//        // 默认按创建时间降序
//        queryWrapper.orderByDesc("createTime");
//
//        return queryWrapper;
//    }
//
//    @Override
//    public Page<BookVO> searchBooks(String keyword, int current, int size, HttpServletRequest request) {
//        QueryWrapper<Book> queryWrapper = new QueryWrapper<>();
//        if (StrUtil.isNotBlank(keyword)) {
//            queryWrapper.and(qw -> qw
//                .like("title", keyword)
//                .or().like("author", keyword)
//                .or().like("isbn", keyword)
//            );
//        }
//
//        // 只查询可借阅的图书
//        queryWrapper.eq("bookStatus", BookStatusEnum.AVAILABLE.getValue());
//        queryWrapper.orderByDesc("createTime");
//
//        Page<Book> bookPage = this.page(new Page<>(current, size), queryWrapper);
//        return getBookVOPage(bookPage, request);
//    }
//
//    // ========== 图书类型管理 ==========
//
//    @Override
//    public List<Genre> getAllGenres() {
//        QueryWrapper<Genre> queryWrapper = new QueryWrapper<>();
//        queryWrapper.orderByAsc("sortOrder");
//        return genreMapper.selectList(queryWrapper);
//    }
//
//    @Override
//    public List<BookVO> getBooksByGenre(Long genreId, HttpServletRequest request) {
//        ThrowUtils.throwIf(genreId == null || genreId <= 0, ErrorCode.PARAMS_ERROR);
//
//        QueryWrapper<Book> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("genreId", genreId);
//        queryWrapper.eq("bookStatus", BookStatusEnum.AVAILABLE.getValue());
//        queryWrapper.orderByDesc("createTime");
//
//        List<Book> bookList = this.list(queryWrapper);
//        return getBookVOList(bookList, request);
//    }
//
//    // ========== 评论管理 ==========
//
//    @Override
//    public long addReview(ReviewAddRequest reviewAddRequest, HttpServletRequest request) {
//        // 1. 校验参数
//        ThrowUtils.throwIf(reviewAddRequest == null, ErrorCode.PARAMS_ERROR);
//
//        Long bookId = reviewAddRequest.getBookId();
//        Integer rating = reviewAddRequest.getRating();
//        String comment = reviewAddRequest.getComment();
//
//        ThrowUtils.throwIf(bookId == null || bookId <= 0, ErrorCode.PARAMS_ERROR, "图书ID不能为空");
//        ThrowUtils.throwIf(rating == null || rating < 1 || rating > 5, ErrorCode.PARAMS_ERROR, "评分必须在1-5之间");
//        ThrowUtils.throwIf(StrUtil.isBlank(comment), ErrorCode.PARAMS_ERROR, "评论内容不能为空");
//
//        // 2. 校验用户登录
//        User loginUser = userService.getLoginUser(request);
//
//        // 3. 校验图书是否存在
//        Book book = this.getById(bookId);
//        ThrowUtils.throwIf(book == null, ErrorCode.NOT_FOUND_ERROR, "图书不存在");
//
//        // 4. 校验用户是否已经评论过该图书
//        QueryWrapper<Review> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("userId", loginUser.getId());
//        queryWrapper.eq("bookId", bookId);
//        long count = reviewMapper.selectCount(queryWrapper);
//        ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "您已经评论过该图书");
//
//        // 5. 创建评论
//        Review review = new Review();
//        BeanUtils.copyProperties(reviewAddRequest, review);
//        review.setUserId(loginUser.getId());
//        review.setReviewStatus(ReviewStatusEnum.PUBLISHED.getValue());
//        review.setLikeCount(0);
//        review.setCreateTime(new Date());
//        review.setUpdateTime(new Date());
//
//        // 6. 保存评论
//        int result = reviewMapper.insert(review);
//        ThrowUtils.throwIf(result <= 0, ErrorCode.OPERATION_ERROR);
//
//        // 7. 更新图书平均评分
//        updateBookRating(bookId);
//
//        return review.getId();
//    }
//
//    @Override
//    public boolean deleteReview(long id, HttpServletRequest request) {
//        // 1. 校验参数
//        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
//
//        // 2. 校验评论是否存在
//        Review review = reviewMapper.selectById(id);
//        ThrowUtils.throwIf(review == null, ErrorCode.NOT_FOUND_ERROR);
//
//        // 3. 校验权限（只有评论作者或管理员可以删除）
//        User loginUser = userService.getLoginUser(request);
//        if (!review.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
//            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//        }
//
//        // 4. 删除评论
//        int result = reviewMapper.deleteById(id);
//        if (result > 0) {
//            // 5. 更新图书平均评分
//            updateBookRating(review.getBookId());
//        }
//
//        return result > 0;
//    }
//
//    @Override
//    public List<ReviewVO> getBookReviews(Long bookId, HttpServletRequest request) {
//        ThrowUtils.throwIf(bookId == null || bookId <= 0, ErrorCode.PARAMS_ERROR);
//
//        QueryWrapper<Review> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("bookId", bookId);
//        queryWrapper.eq("reviewStatus", ReviewStatusEnum.PUBLISHED.getValue());
//        queryWrapper.orderByDesc("createTime");
//
//        List<Review> reviewList = reviewMapper.selectList(queryWrapper);
//        return getReviewVOList(reviewList, request);
//    }
//
//    @Override
//    public ReviewVO getReviewVO(Review review, HttpServletRequest request) {
//        if (review == null) {
//            return null;
//        }
//
//        ReviewVO reviewVO = new ReviewVO();
//        BeanUtils.copyProperties(review, reviewVO);
//
//        // 获取用户信息
//        User user = userService.getById(review.getUserId());
//        if (user != null) {
//            UserVO userVO = userService.getUserVO(user);
//            reviewVO.setUser(userVO);
//        }
//
//        return reviewVO;
//    }
//
//    @Override
//    public List<ReviewVO> getReviewVOList(List<Review> reviewList, HttpServletRequest request) {
//        if (CollUtil.isEmpty(reviewList)) {
//            return CollUtil.newArrayList();
//        }
//
//        return reviewList.stream().map(review -> getReviewVO(review, request)).collect(Collectors.toList());
//    }
//
//    @Override
//    public void updateBookRating(Long bookId) {
//        if (bookId == null || bookId <= 0) {
//            return;
//        }
//
//        // 查询该图书的所有已发布评论
//        QueryWrapper<Review> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("bookId", bookId);
//        queryWrapper.eq("reviewStatus", ReviewStatusEnum.PUBLISHED.getValue());
//        List<Review> reviewList = reviewMapper.selectList(queryWrapper);
//
//        if (CollUtil.isEmpty(reviewList)) {
//            // 没有评论，设置为0
//            Book book = new Book();
//            book.setId(bookId);
//            book.setAverageRating(BigDecimal.ZERO);
//            book.setRatingCount(0);
//            book.setUpdateTime(new Date());
//            this.updateById(book);
//        } else {
//            // 计算平均评分
//            double avgRating = reviewList.stream()
//                .mapToInt(Review::getRating)
//                .average()
//                .orElse(0.0);
//
//            Book book = new Book();
//            book.setId(bookId);
//            book.setAverageRating(BigDecimal.valueOf(avgRating).setScale(1, RoundingMode.HALF_UP));
//            book.setRatingCount(reviewList.size());
//            book.setUpdateTime(new Date());
//            this.updateById(book);
//        }
//    }
//
//    @Override
//    public boolean isBookAvailable(Long id, HttpServletRequest request) {
//        Book book = this.getById(id); // Assuming getById is from IService
//        return book != null && book.getAvailable() != null && book.getAvailable();
//    }
//}