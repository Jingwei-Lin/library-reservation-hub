package com.elec5619.libraryreservationreviewhub.tools;


import com.elec5619.libraryreservationreviewhub.common.PageRequest;
import com.elec5619.libraryreservationreviewhub.common.PageResult;
import com.elec5619.libraryreservationreviewhub.exception.ErrorCode;
import com.elec5619.libraryreservationreviewhub.exception.ThrowUtils;
import com.elec5619.libraryreservationreviewhub.model.entity.Book;
import com.elec5619.libraryreservationreviewhub.model.vo.BookVO;
import com.elec5619.libraryreservationreviewhub.service.BookService;
import com.elec5619.libraryreservationreviewhub.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * BookSearchTool
 *
 * The book tools the AI assistant can call:
 *  - search, genres, recommendations, details and availability;
 *  - every method is a @Tool, which the model picks from the user's request.
 */
@Component
@Slf4j
public class BookSearchTool {

    @Autowired
    private BookService bookService;


    /**
     * Search
     * @param keyword keyword (title, author or genre)
     * @param available only books with a copy available
     * @param current page number
     * @param pageSize page size
     * @return PageResult<BookVO>
     */
    @Tool(
            name = "search_books",
            description = "Search for books by title, author name, or genre keyword. " +
                    "Use this when the user provides a keyword, name, or topic."
    )
    public PageResult<BookVO> searchBooks(String keyword, Boolean available, int current, int pageSize) {
        log.info("ToolCall: search_books, keyword={}", keyword);
        PageRequest pageRequest = new PageRequest();
        pageRequest.setCurrent(current);
        pageRequest.setPageSize(pageSize);
        return bookService.searchBooks(keyword, pageRequest, available);
    }

    /**
     * The books in one genre
     */
    @Tool(
            name = "get_books_by_genre",
            description = "List all books that belong to a specific genre, such as 'Fantasy' or 'Romance'. " +
                    "Use only when the user explicitly mentions a genre name."
    )
    public List<BookVO> getBooksByGenre(String genre) {
        log.info("ToolCall: get_books_by_genre, genre={}", genre);
        return bookService.getBooksByGenre(genre);
    }

    /**
     * Five random books to recommend, with everything the reply needs to name them. It used to return the top 10's
     * ids and ratings only, so the model had nothing to name and invented titles.
     */
    @Tool(
            name = "get_random_books",
            description = "Return 5 random books from the library catalog that have a copy available now, each with " +
                    "its title, author, genre, description and cover path. Use this when the user asks for general, " +
                    "non-specific book recommendations such as 'recommend me some books' or 'popular books' " +
                    "without giving any genre or author."
    )
    public List<BookVO> getRandomBooks() {
        List<BookVO> books = bookService.getRandomBooks(5);
        log.info("ToolCall: get_random_books returned {} books", books.size());
        return books;
    }



    /**
     * One book's details (a deleted book is not found)
     */
    @Tool(name = "get_book_by_id", description = "Get detailed information for a book by ID")
    public BookVO getBookById(Long id) {
        log.info("ToolCall: get_book_by_id, id={}", id);
        Book book = bookService.getBookById(id);
        ThrowUtils.throwIf(Boolean.TRUE.equals(book.getIsDelete()), ErrorCode.NOT_FOUND_ERROR);
        return bookService.getBookVO(book);
    }

    /**
     * Whether a copy of the book can be reserved now
     */
    @Tool(name = "check_book_availability", description = "Check whether a specific book is available for borrowing")
    public boolean isBookAvailable(Long id) {
        log.info("ToolCall: check_book_availability, id={}", id);
        return bookService.isBookAvailable(id);
    }
}