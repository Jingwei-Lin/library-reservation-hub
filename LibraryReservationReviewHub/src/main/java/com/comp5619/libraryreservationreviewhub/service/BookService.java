package com.comp5619.libraryreservationreviewhub.service;

import com.comp5619.libraryreservationreviewhub.common.PageRequest;
import com.comp5619.libraryreservationreviewhub.common.PageResult;
import com.comp5619.libraryreservationreviewhub.model.dto.book.BookAddRequest;
import com.comp5619.libraryreservationreviewhub.model.entity.Book;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.vo.BookVO;

import java.util.List;

public interface BookService {
    long addBook(BookAddRequest req, User operator);
    boolean deleteBook(long id, User operator);
    boolean updateBook(Book book, User operator);
    Book getBookById(long id);

    BookVO getBookVO(Book book);
    List<BookVO> getBookVOList(List<Book> books);

    PageResult<BookVO> searchBooks(String keyword, PageRequest pageReq, Boolean available);
    List<BookVO> getBooksByGenre(String genre);
    boolean restoreBook(long id, User operator);
    boolean isBookAvailable(Long id);
}
