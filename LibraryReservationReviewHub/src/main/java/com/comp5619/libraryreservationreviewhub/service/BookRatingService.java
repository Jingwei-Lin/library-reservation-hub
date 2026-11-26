package com.comp5619.libraryreservationreviewhub.service;

import com.comp5619.libraryreservationreviewhub.model.vo.BookRatingVO;
import com.comp5619.libraryreservationreviewhub.model.vo.BookWithRatingVO;

import java.util.List;

public interface BookRatingService {

    BookRatingVO getByBookId(Long bookId);

    List<BookRatingVO> getByBookIds(List<Long> bookIds);

    /** Manually recalculate (generally not necessary, as the trigger will automatically maintain it) */
    void recalc(Long bookId);

    /** Top 10: bookId and avgRating only */
    List<BookRatingVO> top10();

    /** Top 10: the whole book and avgRating */
    List<BookWithRatingVO> top10Books();
}