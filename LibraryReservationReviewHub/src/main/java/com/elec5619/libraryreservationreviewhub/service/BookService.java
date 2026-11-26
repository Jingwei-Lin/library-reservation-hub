package com.elec5619.libraryreservationreviewhub.service;

import com.elec5619.libraryreservationreviewhub.common.PageRequest;
import com.elec5619.libraryreservationreviewhub.common.PageResult;
import com.elec5619.libraryreservationreviewhub.model.dto.book.BookAddRequest;
import com.elec5619.libraryreservationreviewhub.model.entity.Book;
import com.elec5619.libraryreservationreviewhub.model.entity.User;
import com.elec5619.libraryreservationreviewhub.model.vo.BookStatsVO;
import com.elec5619.libraryreservationreviewhub.model.vo.BookVO;

import java.util.List;

public interface BookService {
    Long addBook(BookAddRequest req, User operator);
    boolean deleteBook(Long id, User operator);
    boolean updateBook(Book book, User operator);
    Book getBookById(Long id);

    /** Several books by id in one query, deleted ones included; ids with no book are left out */
    List<Book> getBooksByIds(java.util.Collection<Long> ids);

    BookVO getBookVO(Book book);
    List<BookVO> getBookVOList(List<Book> books);

    /** Search for users: deleted books are left out */
    PageResult<BookVO> searchBooks(String keyword, PageRequest pageReq, Boolean available);

    /** Search that can include deleted books, for the admin page */
    PageResult<BookVO> searchBooks(String keyword, PageRequest pageReq, Boolean available, boolean includeDeleted);
    List<BookVO> getBooksByGenre(String genre);

    boolean restoreBook(Long id, User operator);
    boolean isBookAvailable(Long id);
    List<BookVO> getRandomBooks(int limit);

    /** Counts for the Dashboard: listed books, those with a copy on the shelf, and genres */
    BookStatsVO getStats();

}
