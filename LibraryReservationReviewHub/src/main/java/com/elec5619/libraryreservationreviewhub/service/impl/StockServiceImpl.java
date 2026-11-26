package com.elec5619.libraryreservationreviewhub.service.impl;

import com.elec5619.libraryreservationreviewhub.exception.ErrorCode;
import com.elec5619.libraryreservationreviewhub.exception.ThrowUtils;
import com.elec5619.libraryreservationreviewhub.mapper.BookMapper;
import com.elec5619.libraryreservationreviewhub.mapper.WaitingListMapper;
import com.elec5619.libraryreservationreviewhub.model.enums.WaitingStatusEnum;
import com.elec5619.libraryreservationreviewhub.model.entity.Book;
import com.elec5619.libraryreservationreviewhub.model.entity.WaitingList;
import com.elec5619.libraryreservationreviewhub.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * See {@link StockService}. Reads after the book lock are locking reads or conditional UPDATEs: a plain SELECT
 * could answer from a snapshot taken before the lock was granted.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockServiceImpl implements StockService {

    private final BookMapper bookMapper;
    private final WaitingListMapper waitingListMapper;

    @Override
    @Transactional
    public Book lockBook(Long bookId) {
        ThrowUtils.throwIf(bookId == null || bookId <= 0, ErrorCode.PARAMS_ERROR, "Invalid book ID");
        Book book = bookMapper.selectByIdForUpdate(bookId);
        ThrowUtils.throwIf(book == null, ErrorCode.NOT_FOUND_ERROR, "Book not found");
        return book;
    }

    @Override
    @Transactional
    public void giveBackCopy(Long bookId) {
        passOn(lockBook(bookId));
    }

    @Override
    @Transactional
    public void takeCopy(Long bookId, Long userId) {
        Book book = lockBook(bookId);
        ThrowUtils.throwIf(Boolean.TRUE.equals(book.getIsDelete()), ErrorCode.NOT_FOUND_ERROR, "Book not found");

        if (waitingListMapper.fulfilNotified(userId, bookId) > 0) {
            // the copy held for them. held_quantity always matches the notified entries; if an older database
            // doesn't, a shelf copy will do
            if (bookMapper.decrementHeldQuantity(bookId) > 0 || bookMapper.takeAvailableCopy(bookId) > 0) {
                log.info("User {} took the copy of book {} held for them", userId, bookId);
                return;
            }
            ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "No available copies");
        }
        ThrowUtils.throwIf(bookMapper.takeAvailableCopy(bookId) == 0, ErrorCode.OPERATION_ERROR, "No available copies");
        // reserving a book you were still waiting for ends the wait
        waitingListMapper.fulfilWaiting(userId, bookId);
    }

    @Override
    @Transactional
    public void cancelWaiting(Long bookId, Long userId) {
        Book book = lockBook(bookId);
        WaitingList entry = waitingListMapper.selectActiveForUpdate(userId, bookId);
        ThrowUtils.throwIf(entry == null, ErrorCode.NOT_FOUND_ERROR, "No cancelable waiting record was found.");

        waitingListMapper.cancelEntry(entry.getWaitId());
        if (entry.getStatus() == WaitingStatusEnum.NOTIFIED && bookMapper.decrementHeldQuantity(bookId) > 0) {
            passOn(book);
        }
        log.info("User {} left the queue for book {} (was {})", userId, bookId, entry.getStatus());
    }

    @Override
    @Transactional
    public void addCopies(Long bookId, int count) {
        Book book = lockBook(bookId);
        for (int i = 0; i < count; i++) {
            passOn(book);
        }
    }

    @Override
    @Transactional
    public boolean expireHold(Long waitId, Long bookId) {
        Book book = lockBook(bookId);
        if (waitingListMapper.expireHold(waitId, HOLD_DAYS) == 0) {
            return false;
        }
        if (bookMapper.decrementHeldQuantity(bookId) > 0) {
            passOn(book);
        }
        log.info("Hold {} on book {} ran out after {} days", waitId, bookId, HOLD_DAYS);
        return true;
    }

    /** Gives one copy to the first person waiting, or puts it on the shelf. The caller holds the book lock. */
    private void passOn(Book book) {
        Long bookId = book.getBookId();
        if (!Boolean.TRUE.equals(book.getIsDelete())) {
            WaitingList next = waitingListMapper.selectNextWaitingForUpdate(bookId);
            if (next != null) {
                waitingListMapper.markNotified(next.getWaitId());
                bookMapper.incrementHeldQuantity(bookId);
                log.info("Held a copy of book {} for user {} (wait {})", bookId, next.getUserId(), next.getWaitId());
                return;
            }
        }
        bookMapper.returnCopy(bookId);
        log.info("Put a copy of book {} back on the shelf", bookId);
    }
}
