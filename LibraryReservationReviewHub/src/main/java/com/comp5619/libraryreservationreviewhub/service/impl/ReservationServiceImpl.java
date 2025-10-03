package com.comp5619.libraryreservationreviewhub.service.impl;

import com.comp5619.libraryreservationreviewhub.common.PageRequest;
import com.comp5619.libraryreservationreviewhub.common.PageResult;
import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
import com.comp5619.libraryreservationreviewhub.exception.ThrowUtils;
import com.comp5619.libraryreservationreviewhub.mapper.BookMapper;
import com.comp5619.libraryreservationreviewhub.mapper.ReservationMapper;
import com.comp5619.libraryreservationreviewhub.model.dto.reservation.ReservationAddRequest;
import com.comp5619.libraryreservationreviewhub.model.entity.Book;
import com.comp5619.libraryreservationreviewhub.model.entity.Reservation;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.vo.ReservationVO;
import com.comp5619.libraryreservationreviewhub.service.BookService;
import com.comp5619.libraryreservationreviewhub.service.ReservationService;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationServiceImpl implements ReservationService {

    private final ReservationMapper reservationMapper;
    private final BookService bookService;
    private final BookMapper bookMapper;
    private final UserService userService;

    @Override
    @Transactional
    public Long createReservation(Long bookId, ReservationAddRequest req, User user) {
        log.info("Creating reservation for bookId: {}, userId: {}", bookId, user.getId());
        ThrowUtils.throwIf(bookId == null || bookId <= 0, ErrorCode.PARAMS_ERROR, "Invalid book ID");
        ThrowUtils.throwIf(user == null, ErrorCode.NO_AUTH_ERROR, "User not logged in");
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR, "Reservation details are required");

        // Validate dates
        Date today = new Date();
        ThrowUtils.throwIf(req.getPickupTime() == null || req.getPickupTime().before(today),
                ErrorCode.PARAMS_ERROR, "Pickup date cannot be in the past");
        ThrowUtils.throwIf(req.getDueTime() == null || req.getDueTime().before(req.getPickupTime()),
                ErrorCode.PARAMS_ERROR, "Due date must be after pickup date");

        // Check if book is available
        ThrowUtils.throwIf(!bookService.isBookAvailable(bookId), ErrorCode.OPERATION_ERROR, "Book is not available");

        // Check if user already has an active reservation for this book
        List<Reservation> activeReservations = reservationMapper.selectActiveByBookId(bookId);
        log.info("Active reservations for bookId {}: {}", bookId, activeReservations);
        boolean hasActiveReservation = activeReservations.stream()
                .anyMatch(r -> {
                    if (r == null || r.getUserId() == null || r.getReservationStatus() == null) {
                        log.warn("Invalid reservation data: {}", r);
                        return false;
                    }
                    return r.getUserId().equals(user.getId()) &&
                            List.of("reserved", "borrowed").contains(r.getReservationStatus());
                });
        ThrowUtils.throwIf(hasActiveReservation, ErrorCode.OPERATION_ERROR,
                "You have already reserved this book");

        // Create reservation
        Reservation reservation = new Reservation();
        reservation.setUserId(user.getId());
        reservation.setBookId(bookId);
        reservation.setReservationTime(new Date());
        reservation.setPickupTime(req.getPickupTime());
        reservation.setDueTime(req.getDueTime());
        reservation.setReservationStatus("reserved");
        reservation.setCreateTime(new Date());
        log.info("Reservation object: {}", reservation);

        // Decrease book quantity
        Book book = bookService.getBookById(bookId);
        ThrowUtils.throwIf(book == null, ErrorCode.NOT_FOUND_ERROR, "Book not found");
        book.setQuantity(book.getQuantity() - 1);
        book.setAvailable(book.getQuantity() > 0);
        bookMapper.updateBook(book);
        log.info("Updated book quantity: {}", book.getQuantity());

        // Insert reservation
        int rows = reservationMapper.insertReservation(reservation);
        log.info("Inserted reservation, rows affected: {}", rows);
        ThrowUtils.throwIf(rows <= 0, ErrorCode.OPERATION_ERROR, "Failed to create reservation");
        return reservation.getReservationId();
    }

    @Override
    @Transactional
    public boolean cancelReservation(Long reservationId, User user) {
        log.info("Attempting to cancel reservationId: {} for userId: {}", reservationId, user.getId());
        ThrowUtils.throwIf(reservationId == null || reservationId <= 0, ErrorCode.PARAMS_ERROR, "Invalid reservation ID");
        ThrowUtils.throwIf(user == null, ErrorCode.NO_AUTH_ERROR, "User not logged in");

        Reservation reservation = reservationMapper.selectById(reservationId);
        log.info("Fetched reservation: {}", reservation);
        ThrowUtils.throwIf(reservation == null || reservation.getIsDelete(), ErrorCode.NOT_FOUND_ERROR, "Reservation not found or already cancelled");
        ThrowUtils.throwIf(!reservation.getUserId().equals(user.getId()) && !userService.isAdmin(user),
                ErrorCode.NO_AUTH_ERROR, "Not authorized to cancel this reservation");

        // Update book quantity if reservation is active
        String status = reservation.getReservationStatus();
        if (status != null && List.of("reserved", "borrowed").contains(status)) {
            Book book = bookService.getBookById(reservation.getBookId());
            ThrowUtils.throwIf(book == null, ErrorCode.NOT_FOUND_ERROR, "Book not found");
            book.setQuantity(book.getQuantity() + 1);
            book.setAvailable(true);
            bookMapper.updateBook(book);
            log.info("Updated book quantity for bookId {}: {}", reservation.getBookId(), book.getQuantity());
        } else {
            log.info("Reservation status {} does not require book quantity update", status);
        }

        // Update reservation status to 'cancelled'
        int rows = reservationMapper.cancelById(reservationId);
        log.info("Updated reservation status to 'cancelled', rows affected: {}", rows);
        ThrowUtils.throwIf(rows <= 0, ErrorCode.OPERATION_ERROR, "Failed to cancel reservation in database");
        return true;
    }

    @Override
    @Transactional
    public boolean updateReservationStatus(Long reservationId, String status, User user) {
        ThrowUtils.throwIf(reservationId == null || reservationId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(!userService.isAdmin(user), ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(!List.of("reserved", "borrowed", "returned", "overdue", "cancelled").contains(status),
                ErrorCode.PARAMS_ERROR, "Invalid status");

        Reservation reservation = reservationMapper.selectById(reservationId);
        ThrowUtils.throwIf(reservation == null || reservation.getIsDelete(), ErrorCode.NOT_FOUND_ERROR);

        if ("returned".equals(status) && "borrowed".equals(reservation.getReservationStatus())) {
            Book book = bookService.getBookById(reservation.getBookId());
            book.setQuantity(book.getQuantity() + 1);
            book.setAvailable(true);
            bookMapper.updateBook(book);
            reservation.setReturnTime(new Date());
        } else if ("borrowed".equals(status) && "reserved".equals(reservation.getReservationStatus())) {
            reservation.setPickupTime(new Date());
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
        log.info("Fetched reservations for userId {}: {}", userId, reservations);
        return reservations.stream()
                .map(this::toReservationVO)
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<ReservationVO> getAllReservations(PageRequest pageRequest, User operator) {
        ThrowUtils.throwIf(!userService.isAdmin(operator), ErrorCode.NO_AUTH_ERROR);
        List<Reservation> reservations = reservationMapper.selectByUserId(null);
        log.info("Fetched all reservations: {}", reservations);
        return new PageResult<>(pageRequest.getCurrent(), pageRequest.getPageSize(),
                reservations.size(), reservations.stream().map(this::toReservationVO).collect(Collectors.toList()));
    }

    @Override
    public ReservationVO toReservationVO(Reservation reservation) {
        ReservationVO vo = new ReservationVO();
        BeanUtils.copyProperties(reservation, vo, "reservationTime", "pickupTime", "dueTime", "reservationStatus");
        vo.setReservationId(reservation.getReservationId());
        vo.setUserId(reservation.getUserId());
        vo.setBookId(reservation.getBookId());
        vo.setReservationTime(reservation.getReservationTime());
        vo.setPickupTime(reservation.getPickupTime());
        vo.setDueTime(reservation.getDueTime());
        vo.setReservationStatus(reservation.getReservationStatus());
        Book book = bookService.getBookById(reservation.getBookId());
        vo.setBookTitle(book != null ? book.getTitle() : "Unknown");
        log.info("Mapped ReservationVO: {}", vo);
        return vo;
    }

    @Override
    public boolean hasActiveReservation(Long userId, Long bookId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "Invalid user ID");
        ThrowUtils.throwIf(bookId == null || bookId <= 0, ErrorCode.PARAMS_ERROR, "Invalid book ID");
        List<Reservation> activeReservations = reservationMapper.selectActiveByBookId(bookId);
        log.info("Checking active reservations for userId {}, bookId {}: {}", userId, bookId, activeReservations);
        return activeReservations.stream()
                .anyMatch(r -> r != null && r.getUserId() != null && r.getReservationStatus() != null &&
                        r.getUserId().equals(userId) &&
                        List.of("reserved", "borrowed").contains(r.getReservationStatus()));
    }
}