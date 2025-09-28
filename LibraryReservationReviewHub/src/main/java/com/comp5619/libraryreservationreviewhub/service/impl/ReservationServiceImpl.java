package com.comp5619.libraryreservationreviewhub.service.impl;

import cn.hutool.core.util.StrUtil;
import com.comp5619.libraryreservationreviewhub.common.PageRequest;
import com.comp5619.libraryreservationreviewhub.common.PageResult;
import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
import com.comp5619.libraryreservationreviewhub.exception.ThrowUtils;
import com.comp5619.libraryreservationreviewhub.mapper.BookMapper;
import com.comp5619.libraryreservationreviewhub.mapper.ReservationMapper;
import com.comp5619.libraryreservationreviewhub.model.entity.Book;
import com.comp5619.libraryreservationreviewhub.model.entity.Reservation;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.vo.ReservationVO;
import com.comp5619.libraryreservationreviewhub.service.BookService;
import com.comp5619.libraryreservationreviewhub.service.ReservationService;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationMapper reservationMapper;
    private final BookService bookService;
    private final BookMapper bookMapper;
    private final UserService userService;

    @Override
    @Transactional
    public Long createReservation(Long bookId, User user) {
        ThrowUtils.throwIf(bookId == null || bookId <= 0, ErrorCode.PARAMS_ERROR, "Invalid book ID");
        ThrowUtils.throwIf(user == null, ErrorCode.NO_AUTH_ERROR, "User not logged in");

        // Check if book is available
        ThrowUtils.throwIf(!bookService.isBookAvailable(bookId), ErrorCode.OPERATION_ERROR, "Book is not available");

        // Check if user already has an active reservation for this book
        List<Reservation> activeReservations = reservationMapper.selectActiveByBookId(bookId);
        boolean hasActiveReservation = activeReservations.stream()
                .anyMatch(r -> r.getUserId().equals(user.getId()) && "active".equals(r.getReservationStatus()));
        ThrowUtils.throwIf(hasActiveReservation, ErrorCode.OPERATION_ERROR, "User already reserved this book");

        // Create reservation
        Reservation reservation = new Reservation();
        reservation.setUserId(user.getId());
        reservation.setBookId(bookId);
        reservation.setReservationTime(new Date());
        reservation.setReservationStatus("active");
        reservation.setCreateTime(new Date());

        // Decrease book quantity
        Book book = bookService.getBookById(bookId);
        book.setQuantity(book.getQuantity() - 1);
        bookMapper.updateBook(book);

        // Insert reservation
        int rows = reservationMapper.insertReservation(reservation);
        ThrowUtils.throwIf(rows <= 0, ErrorCode.OPERATION_ERROR, "Failed to create reservation");
        return reservation.getReservationId();
    }

    @Override
    @Transactional
    public boolean cancelReservation(Long reservationId, User user) {
        ThrowUtils.throwIf(reservationId == null || reservationId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(user == null, ErrorCode.NO_AUTH_ERROR);

        Reservation reservation = reservationMapper.selectById(reservationId);
        ThrowUtils.throwIf(reservation == null || reservation.getIsDelete(), ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!reservation.getUserId().equals(user.getId()) && !userService.isAdmin(user),
                ErrorCode.NO_AUTH_ERROR, "Not authorized to cancel this reservation");

        // Update book quantity if reservation is active or picked up
        if ("active".equals(reservation.getReservationStatus()) || "picked_up".equals(reservation.getReservationStatus())) {
            Book book = bookService.getBookById(reservation.getBookId());
            book.setQuantity(book.getQuantity() + 1);
            bookMapper.updateBook(book);
        }

        return reservationMapper.deleteById(reservationId) > 0;
    }

    @Override
    @Transactional
    public boolean updateReservationStatus(Long reservationId, String status, User user) {
        ThrowUtils.throwIf(reservationId == null || reservationId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(!userService.isAdmin(user), ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(!List.of("active", "picked_up", "returned", "cancelled").contains(status),
                ErrorCode.PARAMS_ERROR, "Invalid status");

        Reservation reservation = reservationMapper.selectById(reservationId);
        ThrowUtils.throwIf(reservation == null || reservation.getIsDelete(), ErrorCode.NOT_FOUND_ERROR);

        // Update book quantity for specific status transitions
        if ("returned".equals(status) && "picked_up".equals(reservation.getReservationStatus())) {
            Book book = bookService.getBookById(reservation.getBookId());
            book.setQuantity(book.getQuantity() + 1);
            bookMapper.updateBook(book);
            reservation.setReturnTime(new Date());
        } else if ("picked_up".equals(status) && "active".equals(reservation.getReservationStatus())) {
            reservation.setPickupTime(new Date());
            // Set due date (e.g., 14 days from pickup)
            long dueTimeMillis = System.currentTimeMillis() + 14L * 24 * 60 * 60 * 1000;
            reservation.setDueTime(new Date(dueTimeMillis));
        }

        return reservationMapper.updateStatus(reservationId, status) > 0;
    }

    @Override
    public Reservation getReservationById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Reservation reservation = reservationMapper.selectById(id);
        ThrowUtils.throwIf(reservation == null || reservation.getIsDelete(), ErrorCode.NOT_FOUND_ERROR);
        return reservation;
    }

    @Override
    public List<ReservationVO> getReservationsByUser(Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);
        List<Reservation> reservations = reservationMapper.selectByUserId(userId);
        return reservations.stream().map(this::toReservationVO).collect(Collectors.toList());
    }

    @Override
    public PageResult<ReservationVO> getAllReservations(PageRequest pageRequest, User operator) {
        ThrowUtils.throwIf(!userService.isAdmin(operator), ErrorCode.NO_AUTH_ERROR);
        // Implement pagination logic if needed (similar to BookServiceImpl.searchBooks)
        // For simplicity, return all reservations for admin
        List<Reservation> reservations = reservationMapper.selectByUserId(null); // Modify to support pagination
        return new PageResult<>(pageRequest.getCurrent(), pageRequest.getPageSize(),
                reservations.size(), reservations.stream().map(this::toReservationVO).collect(Collectors.toList()));
    }

    public ReservationVO toReservationVO(Reservation reservation) {
        ReservationVO vo = new ReservationVO();
        BeanUtils.copyProperties(reservation, vo);
        Book book = bookService.getBookById(reservation.getBookId());
        vo.setBookTitle(book.getTitle());
        return vo;
    }
}
