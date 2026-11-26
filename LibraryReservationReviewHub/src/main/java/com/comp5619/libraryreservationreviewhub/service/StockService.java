package com.comp5619.libraryreservationreviewhub.service;

import com.comp5619.libraryreservationreviewhub.model.entity.Book;

/**
 * Every change to a book's stock and its waiting list goes through here.
 *
 * <p>The model: {@code quantity} is the shelf (copies anyone can reserve), {@code held_quantity} the copies set
 * aside for notified users, one per NOTIFIED entry. A copy that comes back goes to the first person waiting
 * (oldest join, then lowest id), who is notified at once; with nobody waiting it goes on the shelf.
 *
 * <p>Each method locks the book row first and only then reads or changes waiting-list rows, so changes to one
 * book run one at a time and always take their locks in the same order. Callers that read anything about the
 * book's stock or queue must do so after {@link #lockBook}.
 */
public interface StockService {

    /** How long a copy stays held for a notified user before it passes on (plan D3) */
    int HOLD_DAYS = 3;

    /** Reads the book and locks its row until the transaction ends. Not found → NOT_FOUND_ERROR. */
    Book lockBook(Long bookId);

    /** A copy came back (a cancel or a return): the next person waiting gets it, or it goes on the shelf. */
    void giveBackCopy(Long bookId);

    /**
     * Takes a copy for the user: the copy held for them if they are notified, otherwise one from the shelf.
     * Their waiting-list entry, if any, becomes FULFILLED. No copy → OPERATION_ERROR "No available copies".
     */
    void takeCopy(Long bookId, Long userId);

    /** Cancels the user's waiting or notified entry; a copy held for them passes to the next person. */
    void cancelWaiting(Long bookId, Long userId);

    /** New copies from an admin: each goes to the next person waiting, the rest to the shelf. */
    void addCopies(Long bookId, int count);

    /**
     * Ends one hold that is older than {@link #HOLD_DAYS}: the entry is cancelled and the copy passes on.
     * Returns false when the entry changed in the meantime (reserved, cancelled, or not yet expired).
     */
    boolean expireHold(Long waitId, Long bookId);
}
