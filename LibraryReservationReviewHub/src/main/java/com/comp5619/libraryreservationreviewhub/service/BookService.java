package com.comp5619.libraryreservationreviewhub.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.comp5619.libraryreservationreviewhub.model.dto.book.BookAddRequest;
import com.comp5619.libraryreservationreviewhub.model.dto.book.BookQueryRequest;
import com.comp5619.libraryreservationreviewhub.model.dto.book.ReviewAddRequest;
import com.comp5619.libraryreservationreviewhub.model.entity.Book;
import com.comp5619.libraryreservationreviewhub.model.entity.Genre;
import com.comp5619.libraryreservationreviewhub.model.entity.Review;
import com.comp5619.libraryreservationreviewhub.model.vo.BookVO;
import com.comp5619.libraryreservationreviewhub.model.vo.ReviewVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 图书服务接口 - 整合图书、类型、评论管理
 */
public interface BookService extends IService<Book> {

    // ========== 图书管理 ==========
    
    /**
     * 添加图书
     */
    long addBook(BookAddRequest bookAddRequest, HttpServletRequest request);

    /**
     * 删除图书
     */
    boolean deleteBook(long id, HttpServletRequest request);

    /**
     * 更新图书
     */
    boolean updateBook(Book book, HttpServletRequest request);

    /**
     * 根据ID获取图书
     */
    Book getBookById(long id);

    /**
     * 获取图书VO
     */
    BookVO getBookVO(Book book, HttpServletRequest request);

    /**
     * 获取图书VO列表
     */
    List<BookVO> getBookVOList(List<Book> bookList, HttpServletRequest request);

    /**
     * 分页获取图书VO
     */
    Page<BookVO> getBookVOPage(Page<Book> bookPage, HttpServletRequest request);

    /**
     * 获取查询条件
     */
    QueryWrapper<Book> getQueryWrapper(BookQueryRequest bookQueryRequest);

    /**
     * 搜索图书（支持标题、作者、ISBN搜索）
     */
    Page<BookVO> searchBooks(String keyword, int current, int size, HttpServletRequest request);

    // ========== 图书类型管理 ==========
    
    /**
     * 获取所有图书类型
     */
    List<Genre> getAllGenres();

    /**
     * 根据类型ID获取图书
     */
    List<BookVO> getBooksByGenre(Long genreId, HttpServletRequest request);

    // ========== 评论管理 ==========
    
    /**
     * 添加评论
     */
    long addReview(ReviewAddRequest reviewAddRequest, HttpServletRequest request);

    /**
     * 删除评论
     */
    boolean deleteReview(long id, HttpServletRequest request);

    /**
     * 获取图书的评论列表
     */
    List<ReviewVO> getBookReviews(Long bookId, HttpServletRequest request);

    /**
     * 获取评论VO
     */
    ReviewVO getReviewVO(Review review, HttpServletRequest request);

    /**
     * 获取评论VO列表
     */
    List<ReviewVO> getReviewVOList(List<Review> reviewList, HttpServletRequest request);

    /**
     * 更新图书平均评分
     */
    void updateBookRating(Long bookId);
}