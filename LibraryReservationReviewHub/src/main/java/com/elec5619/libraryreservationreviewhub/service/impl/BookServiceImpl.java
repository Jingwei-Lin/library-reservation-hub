package com.elec5619.libraryreservationreviewhub.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.elec5619.libraryreservationreviewhub.common.PageRequest;
import com.elec5619.libraryreservationreviewhub.common.PageResult;
import com.elec5619.libraryreservationreviewhub.exception.ErrorCode;
import com.elec5619.libraryreservationreviewhub.exception.ThrowUtils;
import com.elec5619.libraryreservationreviewhub.mapper.BookMapper;
import com.elec5619.libraryreservationreviewhub.mapper.WaitingListMapper;
import com.elec5619.libraryreservationreviewhub.model.dto.book.BookAddRequest;
import com.elec5619.libraryreservationreviewhub.model.entity.Book;
import com.elec5619.libraryreservationreviewhub.model.entity.User;
import com.elec5619.libraryreservationreviewhub.model.vo.BookStatsVO;
import com.elec5619.libraryreservationreviewhub.model.vo.BookVO;
import com.elec5619.libraryreservationreviewhub.service.BookService;
import com.elec5619.libraryreservationreviewhub.service.StockService;
import com.elec5619.libraryreservationreviewhub.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final UserService userService;
    private final BookMapper bookMapper;
    private final WaitingListMapper waitingListMapper;
    private final StockService stockService;

    /* ---------- paging and sorting (sort fields from a whitelist) ---------- */
    private record PageCalc(long offset, long limit, String orderBySql) {}

    private PageCalc calc(PageRequest pr, Set<String> sortWhitelist) {
        int current = Math.max(1, pr.getCurrent());
        int size = Math.min(Math.max(1, pr.getPageSize()), 101);
        long offset = (long) (current - 1) * size;

        // newest first by default
        String orderBySql = "ORDER BY book_id DESC";
        if (StrUtil.isNotBlank(pr.getSortField()) && sortWhitelist.contains(pr.getSortField())) {
            String dir = "descend".equalsIgnoreCase(pr.getSortOrder()) ? "DESC" : "ASC";
            orderBySql = "ORDER BY " + pr.getSortField() + " " + dir;
        }
        return new PageCalc(offset, size, orderBySql);
    }

    /* ===================== Books ===================== */

    @Override
    public Long addBook(BookAddRequest req, User operator) {
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(operator == null || !userService.isAdmin(operator), ErrorCode.NO_AUTH_ERROR);

        String title = StrUtil.trim(req.getTitle());
        String author = StrUtil.trim(req.getAuthor());
        String genre = StrUtil.trim(req.getGenre());

        ThrowUtils.throwIf(StrUtil.hasBlank(title, author, genre), ErrorCode.PARAMS_ERROR, "Book information cannot be left blank.");

        long cnt = bookMapper.countByTag(title, author, genre);
        ThrowUtils.throwIf(cnt > 0, ErrorCode.PARAMS_ERROR, "The book already exists.");

        Book b = new Book();
        b.setTitle(title);
        b.setAuthor(author);
        b.setGenre(genre);

        b.setDescription(StrUtil.emptyToNull(StrUtil.trim(req.getDescription())));
        b.setPath(StrUtil.emptyToNull(StrUtil.trim(req.getPath())));

        b.setQuantity(req.getQuantity() == null ? 0 : req.getQuantity());
        // available follows the stock (the book triggers set it); the request's value is not used
        b.setAvailable(b.getQuantity() > 0);

        int rows = bookMapper.insertBook(b);
        ThrowUtils.throwIf(rows <= 0 || b.getBookId() == null, ErrorCode.OPERATION_ERROR);
        return b.getBookId().longValue();
    }

    /**
     * Soft delete: the book is hidden from users and its queue ends. Copies held for the queue go back to the
     * shelf count, and copies still out come back to it as usual. Deleting an already deleted book is a no-op.
     */
    @Override
    @Transactional
    public boolean deleteBook(Long id, User operator) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(operator == null || !userService.isAdmin(operator), ErrorCode.NO_AUTH_ERROR);

        Book exist = bookMapper.selectByIdForUpdate(id);
        ThrowUtils.throwIf(exist == null, ErrorCode.NOT_FOUND_ERROR);
        if (Boolean.TRUE.equals(exist.getIsDelete())) {
            return true;
        }

        bookMapper.deleteById(id);
        int cancelled = waitingListMapper.cancelActiveForBook(id);
        log.info("Deleted book {}; cancelled {} waiting-list entries", id, cancelled);
        return true;
    }

    /**
     * Admin edit. {@code quantity} is the shelf count: copies added while people are waiting go to them first
     * (each is held for the next person), and lowering it only removes shelf copies, never held ones.
     */
    @Override
    @Transactional
    public boolean updateBook(Book book, User operator) {
        ThrowUtils.throwIf(book == null || book.getBookId() == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(operator == null || !userService.isAdmin(operator), ErrorCode.NO_AUTH_ERROR);

        Book old = bookMapper.selectById(book.getBookId());
        ThrowUtils.throwIf(old == null, ErrorCode.NOT_FOUND_ERROR);

        if (book.getQuantity() != null) {
            ThrowUtils.throwIf(book.getQuantity() < 0, ErrorCode.PARAMS_ERROR, "Quantity cannot be negative");
            Book locked = stockService.lockBook(book.getBookId());
            int added = book.getQuantity() - (locked.getQuantity() == null ? 0 : locked.getQuantity());
            if (added > 0) {
                stockService.addCopies(book.getBookId(), added);
                book.setQuantity(null);   // already written, through the queue
            }
        }

        // `available` follows the stock and can't be set, so an edit may leave nothing to write
        boolean nothingToWrite = book.getTitle() == null && book.getAuthor() == null && book.getGenre() == null
                && book.getDescription() == null && book.getQuantity() == null && book.getPath() == null;
        if (nothingToWrite) {
            return true;
        }
        return bookMapper.updateBook(book) > 0;
    }

    @Override
    public Book getBookById(Long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Book b = bookMapper.selectById((Long) id);
        ThrowUtils.throwIf(b == null, ErrorCode.NOT_FOUND_ERROR);
        return b;
    }

    @Override
    public BookVO getBookVO(Book book) {
        if (book == null) return null;
        BookVO vo = new BookVO();
        BeanUtils.copyProperties(book, vo);
        vo.setBookId(book.getBookId());
        vo.setTitle(book.getTitle());
        vo.setAuthor(book.getAuthor());
        vo.setGenre(book.getGenre());
        vo.setQuantity(book.getQuantity());
        vo.setHeldQuantity(book.getHeldQuantity());
        vo.setAvailable(book.getAvailable());
        vo.setPath(book.getPath());
        vo.setDescription(book.getDescription());
        vo.setDeleted(Boolean.TRUE.equals(book.getIsDelete()));
        return vo;
    }

    @Override
    public List<BookVO> getBookVOList(List<Book> books) {
        if (CollUtil.isEmpty(books)) return CollUtil.newArrayList();
        return books.stream().map(this::getBookVO).collect(Collectors.toList());
    }

    @Override
    public PageResult<BookVO> searchBooks(String keyword, PageRequest pageReq, Boolean available) {
        return searchBooks(keyword, pageReq, available, false);
    }

    @Override
    public PageResult<BookVO> searchBooks(String keyword, PageRequest pageReq, Boolean available, boolean includeDeleted) {
        Set<String> whitelist = Set.of("book_id", "title", "author", "genre", "quantity", "available" , "description");
        PageCalc p = calc(pageReq, whitelist);

        String kw = StrUtil.emptyToDefault(keyword, "");
        long total = bookMapper.countByKeyword(kw, available, includeDeleted);
        List<Book> list = total == 0
                ? List.of()
                : bookMapper.searchByKeywordWithOrder(kw, available, includeDeleted, p.offset(), p.limit(), p.orderBySql());

        return new PageResult<>(pageReq.getCurrent(), pageReq.getPageSize(), total, getBookVOList(list));
    }

    @Override
    public List<BookVO> getBooksByGenre(String genre) {
        ThrowUtils.throwIf(StrUtil.isBlank(genre), ErrorCode.PARAMS_ERROR, "genre can not be null");
        List<Book> list = bookMapper.selectByGenre(genre.trim());
        return getBookVOList(list);
    }

    @Override
    public boolean isBookAvailable(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Book b = bookMapper.selectById(id.longValue());
        return b != null && !Boolean.TRUE.equals(b.getIsDelete()) && Boolean.TRUE.equals(b.getAvailable())
                && b.getQuantity() != null && b.getQuantity() > 0;
    }

    @Override
    public boolean restoreBook(Long id, User operator) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(operator == null || !userService.isAdmin(operator), ErrorCode.NO_AUTH_ERROR);

        Book exist = bookMapper.selectById((long) id);
        ThrowUtils.throwIf(exist == null, ErrorCode.NOT_FOUND_ERROR);

        // restoring a book that isn't deleted is a no-op
        if (!Boolean.TRUE.equals(exist.getIsDelete())) {
            return true;
        }
        return bookMapper.restoreById((long) id) > 0;
    }

    @Override
    public BookStatsVO getStats() {
        return bookMapper.selectStats();
    }

    @Override
    public List<BookVO> getRandomBooks(int limit) {
        // between 1 and 20 books
        int realLimit = Math.min(Math.max(limit, 1), 20);
        log.info("Fetching {} random books as top books (no score field, random selection)", realLimit);

        // pick from the first 100 rather than load the whole table
        PageRequest pr = new PageRequest();
        pr.setCurrent(1);
        pr.setPageSize(100);
        PageResult<BookVO> page = searchBooks("", pr, true);

        List<BookVO> list = page.getRecords();
        if (list.isEmpty()) return List.of();

        Collections.shuffle(list);
        return list.stream().limit(realLimit).collect(Collectors.toList());
    }


}