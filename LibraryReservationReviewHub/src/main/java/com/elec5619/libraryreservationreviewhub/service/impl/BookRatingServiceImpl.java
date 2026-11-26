package com.elec5619.libraryreservationreviewhub.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elec5619.libraryreservationreviewhub.exception.ErrorCode;
import com.elec5619.libraryreservationreviewhub.exception.ThrowUtils;
import com.elec5619.libraryreservationreviewhub.mapper.BookMapper;
import com.elec5619.libraryreservationreviewhub.mapper.BookRatingMapper;
import com.elec5619.libraryreservationreviewhub.model.entity.Book;
import com.elec5619.libraryreservationreviewhub.model.entity.BookRating;
import com.elec5619.libraryreservationreviewhub.model.vo.BookRatingVO;
import com.elec5619.libraryreservationreviewhub.model.vo.BookWithRatingVO;
import com.elec5619.libraryreservationreviewhub.service.BookRatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookRatingServiceImpl implements BookRatingService {

    private final BookRatingMapper bookRatingMapper;
    private final BookMapper bookMapper;

    @Override
    public BookRatingVO getByBookId(Long bookId) {
        ThrowUtils.throwIf(bookId == null || bookId <= 0, ErrorCode.PARAMS_ERROR, "Invalid book ID");
        BookRating br = bookRatingMapper.selectById(bookId);

        BookRatingVO vo = new BookRatingVO();
        if (br != null) {
            BeanUtils.copyProperties(br, vo);
        } else {
            vo.setBookId(bookId);
            vo.setAvgRating(null);
        }
        return vo;
    }

    @Override
    public List<BookRatingVO> getByBookIds(List<Long> bookIds) {
        if (CollUtil.isEmpty(bookIds)) return Collections.emptyList();

        // drop invalid ids and duplicates, keeping the order they came in, which the result follows
        List<Long> ids = bookIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) return Collections.emptyList();

        // one query: the primary key is book_id
        List<BookRating> list = bookRatingMapper.selectBatchIds(ids);
        Map<Long, BookRating> map = list.stream()
                .collect(Collectors.toMap(BookRating::getBookId, x -> x));

        List<BookRatingVO> res = new ArrayList<>(ids.size());
        for (Long id : ids) {
            BookRating r = map.get(id);
            BookRatingVO vo = new BookRatingVO();
            if (r != null) {
                BeanUtils.copyProperties(r, vo);
            } else {
                vo.setBookId(id);
                vo.setAvgRating(null);
            }
            res.add(vo);
        }
        return res;
    }

    @Override
    public void recalc(Long bookId) {
        ThrowUtils.throwIf(bookId == null || bookId <= 0, ErrorCode.PARAMS_ERROR, "Invalid book ID");
        bookRatingMapper.recalcOne(bookId);
    }

    /** Deleted books never appear in the top 10 */
    private static final String LISTED_BOOK_IDS = "SELECT book_id FROM book WHERE is_delete = FALSE";

    /** Top 10: bookId and avgRating only */
    @Override
    public List<BookRatingVO> top10() {
        LambdaQueryWrapper<BookRating> qw = new LambdaQueryWrapper<>();
        qw.isNotNull(BookRating::getAvgRating)
                .inSql(BookRating::getBookId, LISTED_BOOK_IDS)
                .orderByDesc(BookRating::getAvgRating)
                .last("LIMIT 10");

        List<BookRating> list = bookRatingMapper.selectList(qw);
        return list.stream().map(br -> {
            BookRatingVO vo = new BookRatingVO();
            BeanUtils.copyProperties(br, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    /** Top 10: the whole book and avgRating */
    @Override
    public List<BookWithRatingVO> top10Books() {
        // the 10 best ratings first
        LambdaQueryWrapper<BookRating> qw = new LambdaQueryWrapper<>();
        qw.isNotNull(BookRating::getAvgRating)
                .inSql(BookRating::getBookId, LISTED_BOOK_IDS)
                // ties are in no set order; add .orderByAsc(BookRating::getBookId) to fix one
                .orderByDesc(BookRating::getAvgRating)
                .last("LIMIT 10");

        List<BookRating> ratings = bookRatingMapper.selectList(qw);

        // then each book, in that order (only 10 queries)
        List<BookWithRatingVO> res = new ArrayList<>(ratings.size());
        for (BookRating r : ratings) {
            Book book = bookMapper.selectById(r.getBookId());
            if (book == null) { // a rating without its book
                continue;
            }
            BookWithRatingVO vo = new BookWithRatingVO();
            vo.setBookId(book.getBookId());
            vo.setTitle(book.getTitle());
            vo.setAuthor(book.getAuthor());
            vo.setGenre(book.getGenre());
            vo.setDescription(book.getDescription());
            vo.setPath(book.getPath());
            vo.setQuantity(book.getQuantity());
            vo.setHeldQuantity(book.getHeldQuantity());
            vo.setAvailable(book.getAvailable());
            vo.setAvgRating(r.getAvgRating());
            res.add(vo);
        }
        return res;
    }
}
