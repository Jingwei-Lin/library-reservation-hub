package com.comp5619.libraryreservationreviewhub.controller;

import com.comp5619.libraryreservationreviewhub.annotation.AuthCheck;
import com.comp5619.libraryreservationreviewhub.common.BaseResponse;
import com.comp5619.libraryreservationreviewhub.common.PageRequest;
import com.comp5619.libraryreservationreviewhub.common.PageResult;
import com.comp5619.libraryreservationreviewhub.common.ResultUtils;
import com.comp5619.libraryreservationreviewhub.constant.UserConstant;
import com.comp5619.libraryreservationreviewhub.exception.BusinessException;
import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
import com.comp5619.libraryreservationreviewhub.exception.ThrowUtils;
import com.comp5619.libraryreservationreviewhub.model.dto.book.BookAddRequest;
import com.comp5619.libraryreservationreviewhub.model.entity.Book;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.vo.BookStatsVO;
import com.comp5619.libraryreservationreviewhub.model.vo.BookVO;
import com.comp5619.libraryreservationreviewhub.service.BookService;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/book")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final UserService userService;

    /* ============ Admin ============ */

    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> add(@Valid @RequestBody BookAddRequest req, HttpServletRequest request) {
        User operator = userService.getLoginUser(request);
        return ResultUtils.success(bookService.addBook(req, operator));
    }

    @DeleteMapping("/{id}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> delete(@PathVariable long id, HttpServletRequest request) {
        User operator = userService.getLoginUser(request);
        return ResultUtils.success(bookService.deleteBook(id, operator));
    }

    @PutMapping("/{id}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> update(@PathVariable Long id,
                                        @RequestBody Book book,
                                        HttpServletRequest request) {
        // the entity's id field is bookId, not id
        book.setBookId(id.longValue());
        User operator = userService.getLoginUser(request);
        return ResultUtils.success(bookService.updateBook(book, operator));
    }

    @PutMapping("/{id}/restore")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> restore(@PathVariable long id, HttpServletRequest request) {
        User operator = userService.getLoginUser(request);
        return ResultUtils.success(bookService.restoreBook(id, operator));
    }

    /* ============ Queries ============ */

    /** The Dashboard's statistics, counted over every listed book (the list itself is paged) */
    @GetMapping("/stats")
    public BaseResponse<BookStatsVO> stats() {
        return ResultUtils.success(bookService.getStats());
    }

    /** A deleted book is "not found" for everyone but admins */
    @GetMapping("/{id}")
    public BaseResponse<BookVO> get(@PathVariable long id, HttpServletRequest request) {
        Book book = bookService.getBookById(id);
        ThrowUtils.throwIf(Boolean.TRUE.equals(book.getIsDelete()) && !isAdmin(request), ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(bookService.getBookVO(book));
    }

    /**
     * Search:
     * GET /book/search?keyword=xxx&available=true&current=1&pageSize=10&sortField=title&sortOrder=asc
     * Deleted books are left out; {@code includeDeleted=true} adds them, for admins only (the admin page).
     */
    @GetMapping("/search")
    public BaseResponse<PageResult<BookVO>> search(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Boolean available,
                                                   @RequestParam(defaultValue = "false") boolean includeDeleted,
                                                   PageRequest pageReq,
                                                   HttpServletRequest request) {
        return ResultUtils.success(bookService.searchBooks(keyword, pageReq, available,
                includeDeleted && isAdmin(request)));
    }

    /**
     * By genre:/book/genre/Fantasy
     */
    @GetMapping("/genre/{genre}")
    public BaseResponse<List<BookVO>> byGenre(@PathVariable String genre) {
        return ResultUtils.success(bookService.getBooksByGenre(genre));
    }

    /** These reads are open to guests, so "not signed in" just means "not an admin" */
    private boolean isAdmin(HttpServletRequest request) {
        try {
            return userService.isAdmin(userService.getLoginUser(request));
        } catch (BusinessException e) {
            return false;
        }
    }
}
