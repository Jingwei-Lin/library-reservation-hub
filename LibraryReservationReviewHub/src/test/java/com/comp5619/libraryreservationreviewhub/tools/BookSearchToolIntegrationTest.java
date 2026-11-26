package com.comp5619.libraryreservationreviewhub.tools;

import com.comp5619.libraryreservationreviewhub.common.PageResult;
import com.comp5619.libraryreservationreviewhub.exception.BusinessException;
import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
import com.comp5619.libraryreservationreviewhub.model.vo.BookVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The AI assistant's book tools, called directly against the seed data (101 books, book 1 "The Hobbit").
 */
@SpringBootTest
@Transactional
class BookSearchToolIntegrationTest {

    @Autowired
    private BookSearchTool bookSearchTool;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void searchFindsTheBooksMatchingTheKeyword() {
        PageResult<BookVO> result = bookSearchTool.searchBooks("Hobbit", true, 1, 5);

        assertThat(result.getRecords()).extracting(BookVO::getTitle).containsExactly("The Hobbit");
        assertThat(result.getTotal()).isEqualTo(1);
    }

    @Test
    void searchWithNoMatchReturnsAnEmptyPage() {
        PageResult<BookVO> result = bookSearchTool.searchBooks("non_existing_keyword", true, 1, 5);

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getTotal()).isZero();
    }

    @Test
    void theGenreToolReturnsOnlyThatGenre() {
        List<BookVO> books = bookSearchTool.getBooksByGenre("Fantasy");

        assertThat(books).isNotEmpty()
                .extracting(BookVO::getGenre).containsOnly("Fantasy");
        assertThat(books).extracting(BookVO::getTitle).contains("The Hobbit");
    }

    @Test
    void randomBooksAreFiveDifferentBooksWithACopyAvailable() {
        List<BookVO> books = bookSearchTool.getRandomBooks();

        assertThat(books).hasSize(5);
        assertThat(books).extracting(BookVO::getBookId).doesNotHaveDuplicates();
        assertThat(books).allSatisfy(book -> {
            assertThat(book.getTitle()).isNotBlank();
            assertThat(book.getAvailable()).isTrue();
        });
    }

    @Test
    void aBookIsReturnedWithItsDetails() {
        BookVO book = bookSearchTool.getBookById(1L);

        assertThat(book.getTitle()).isEqualTo("The Hobbit");
        assertThat(book.getAuthor()).isEqualTo("J.R.R. Tolkien");
        assertThat(book.getGenre()).isEqualTo("Fantasy");
        assertThat(book.getDescription()).containsIgnoringCase("Bilbo").containsIgnoringCase("dragon");
    }

    @Test
    void aDeletedBookIsNotFound() {
        jdbcTemplate.update("UPDATE book SET is_delete = 1 WHERE book_id = 1");

        assertThatThrownBy(() -> bookSearchTool.getBookById(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.NOT_FOUND_ERROR.getCode()));
    }

    @Test
    void availabilityFollowsTheShelf() {
        // two books, so MyBatis' session cache can't hand back a row read before the update
        jdbcTemplate.update("UPDATE book SET quantity = 0 WHERE book_id = 1");

        assertThat(bookSearchTool.isBookAvailable(1L)).isFalse();
        assertThat(bookSearchTool.isBookAvailable(2L)).isTrue();
    }
}
