//package com.comp5619.libraryreservationreviewhub.controller;
//
//import com.baomidou.mybatisplus.core.metadata.IPage;
//import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
//import com.comp5619.libraryreservationreviewhub.common.BaseResponse;
//import com.comp5619.libraryreservationreviewhub.common.ResultUtils;
//import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
//import com.comp5619.libraryreservationreviewhub.model.dto.book.BookAddRequest;
//import com.comp5619.libraryreservationreviewhub.model.entity.Book;
//import com.comp5619.libraryreservationreviewhub.model.vo.BookVO;
//import com.comp5619.libraryreservationreviewhub.service.BookService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//import jakarta.validation.Valid;
//import java.util.List;
//import jakarta.servlet.http.HttpServletRequest;
//
///**
// * 图书管理控制器
// * Book Management Controller
// */
//@RestController
//@RequestMapping("/api/books")
//public class BookController {
//
//    @Autowired
//    private BookService bookService;
//
//    /**
//     * 添加图书
//     * Add a new book
//     */
//    @PostMapping
//    public BaseResponse<Book> addBook(@Valid @RequestBody BookAddRequest bookAddRequest, HttpServletRequest request) {
//        long bookId = bookService.addBook(bookAddRequest, request);
//        Book createdBook = bookService.getBookById(bookId);
//        return ResultUtils.success(createdBook);
//    }
//
//    /**
//     * 根据ID获取图书
//     * Get book by ID
//     */
//    @GetMapping("/{id}")
//    public BaseResponse<Book> getBookById(@PathVariable Long id) {
//        Book book = bookService.getBookById(id);
//        if (book == null) {
//            return (BaseResponse<Book>) ResultUtils.error(ErrorCode.BOOK_NOT_FOUND_ERROR);
//        }
//        return ResultUtils.success(book);
//    }
//
//    /**
//     * 更新图书信息
//     * Update book information
//     */
//    @PutMapping("/{id}")
//    public BaseResponse<Book> updateBook(@PathVariable Long id, @Valid @RequestBody Book book, HttpServletRequest request) {
//        book.setId(id);
//        boolean updated = bookService.updateBook(book, request);
//        if (!updated) {
//            return (BaseResponse<Book>) ResultUtils.error(ErrorCode.OPERATION_ERROR);
//        }
//        Book updatedBook = bookService.getBookById(id);
//        return ResultUtils.success(updatedBook);
//    }
//
//    /**
//     * 删除图书
//     * Delete book
//     */
//    @DeleteMapping("/{id}")
//    public BaseResponse<Boolean> deleteBook(@PathVariable Long id, HttpServletRequest request) { // Ensure request is included
//        boolean result = bookService.deleteBook(id, request); // Pass both id and request
//        if (!result) {
//            return (BaseResponse<Boolean>) ResultUtils.error(ErrorCode.OPERATION_ERROR); // Cast to match return type
//        }
//        return ResultUtils.success(result);
//    }
//
//    /**
//     * 分页查询图书
//     * Get books with pagination
//     */
//    @GetMapping
//    public BaseResponse<Page<BookVO>> getBooks( // Changed to Page<BookVO>
//                                                @RequestParam(defaultValue = "1") int current,
//                                                @RequestParam(defaultValue = "10") int size,
//                                                @RequestParam(required = false) String keyword,
//                                                HttpServletRequest request) {
//
//        Page<Book> page = new Page<>(current, size);
//        Page<BookVO> result; // Changed to Page<BookVO>
//
//        if (keyword != null && !keyword.trim().isEmpty()) {
//            result = bookService.searchBooks(keyword, current, size, request); // Matches service signature
//        } else {
//            result = bookService.getBookVOPage(page, request); // Use getBookVOPage
//        }
//
//        return ResultUtils.success(result);
//    }
//
//    /**
//     * 根据分类获取图书
//     * Get books by genre
//     */
//    @GetMapping("/genre/{genreId}")
//    public BaseResponse<List<Book>> getBooksByGenre(@PathVariable Long genreId, HttpServletRequest request) { // Add HttpServletRequest parameter
//        List<Book> books = bookService.getBooksByGenre(genreId, request); // Pass both genreId and request
//        if (books == null || books.isEmpty()) {
//            return (BaseResponse<List<Book>>) ResultUtils.error(ErrorCode.NOT_FOUND_ERROR); // Use NOT_FOUND_ERROR for no results
//        }
//        return ResultUtils.success(books);
//    }
//
////    /**
////     * 检查图书是否可用
////     * Check book availability
////     */
////    @GetMapping("/{id}/availability")
////    public BaseResponse<Boolean> checkBookAvailability(@PathVariable Long id) {
////        boolean available = bookService.isBookAvailable(id);
////        return ResultUtils.success(available);
////    }
//
//    /**
//     * 更新图书库存
//     * Update book stock
//     */
////    @PutMapping("/{id}/stock")
////    public BaseResponse<Boolean> updateBookStock(
////            @PathVariable Long id,
////            @RequestParam Integer quantity) {
////        boolean result = bookService.updateBookStock(id, quantity);
////        return ResultUtils.success(result);
////    }
//
//    /**
//     * 获取可用图书列表
//     * Get available books
//     */
////    @GetMapping("/available")
////    public BaseResponse<List<Book>> getAvailableBooks() {
////        List<Book> books = bookService.getAvailableBooks();
////        return ResultUtils.success(books);
////    }
//}