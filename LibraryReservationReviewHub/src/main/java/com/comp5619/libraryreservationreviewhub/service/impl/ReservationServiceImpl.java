package com.comp5619.libraryreservationreviewhub.service.impl;

import com.comp5619.libraryreservationreviewhub.common.PageRequest;
import com.comp5619.libraryreservationreviewhub.common.PageResult;
import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
import com.comp5619.libraryreservationreviewhub.exception.ThrowUtils;
import com.comp5619.libraryreservationreviewhub.mapper.ReservationMapper;
import com.comp5619.libraryreservationreviewhub.model.dto.reservation.ReservationAddRequest;
import com.comp5619.libraryreservationreviewhub.model.dto.reservation.ReservationUpdatePickupRequest;
import com.comp5619.libraryreservationreviewhub.model.entity.Book;
import com.comp5619.libraryreservationreviewhub.model.entity.Reservation;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.vo.ReservationVO;
import com.comp5619.libraryreservationreviewhub.service.BookService;
import com.comp5619.libraryreservationreviewhub.service.ReservationService;
import com.comp5619.libraryreservationreviewhub.service.StockService;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationServiceImpl implements ReservationService {

    private final ReservationMapper reservationMapper;
    private final BookService bookService;
    private final UserService userService;
    private final StockService stockService;

    @Override
    @Transactional
    public Long createReservation(Long bookId, ReservationAddRequest req, User user) {
        log.info("Creating reservation for bookId: {}, userId: {}", bookId, user.getId());
        ThrowUtils.throwIf(bookId == null || bookId <= 0, ErrorCode.PARAMS_ERROR, "Invalid book ID");
        ThrowUtils.throwIf(user == null, ErrorCode.NO_AUTH_ERROR, "User not logged in");
        ThrowUtils.throwIf(user.getStatus() == 0, ErrorCode.USER_BANNED, "User is banned");
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR, "Reservation details are required");

        // Validate dates
        Date pickupTime = req.getPickupTime();
        Date dueTime = req.getDueTime();
        ThrowUtils.throwIf(pickupTime == null, ErrorCode.PARAMS_ERROR, "Pickup date is required");
        ThrowUtils.throwIf(dueTime == null, ErrorCode.PARAMS_ERROR, "Due date is required");

        // Normalize dates to compare only date part
        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        todayCal.set(Calendar.MILLISECOND, 0);
        Date today = todayCal.getTime();

        Calendar pickupCal = Calendar.getInstance();
        pickupCal.setTime(pickupTime);
        pickupCal.set(Calendar.HOUR_OF_DAY, 0);
        pickupCal.set(Calendar.MINUTE, 0);
        pickupCal.set(Calendar.SECOND, 0);
        pickupCal.set(Calendar.MILLISECOND, 0);
        Date pickupDateOnly = pickupCal.getTime();

        // Allow pickup date to be today or future
        ThrowUtils.throwIf(pickupDateOnly.before(today), ErrorCode.PARAMS_ERROR, "Pickup date cannot be in the past");

        // Set dueTime to just before midnight (23:59:59.999)
        Calendar dueCal = Calendar.getInstance();
        dueCal.setTime(dueTime);
        dueCal.set(Calendar.HOUR_OF_DAY, 23);
        dueCal.set(Calendar.MINUTE, 59);
        dueCal.set(Calendar.SECOND, 59);
        dueCal.set(Calendar.MILLISECOND, 999);
        Date adjustedDueTime = dueCal.getTime();

        // Validate due date is after pickup date
        ThrowUtils.throwIf(adjustedDueTime.before(pickupTime) || adjustedDueTime.equals(pickupTime),
                ErrorCode.PARAMS_ERROR, "Due date must be after pickup date");

        // Lock the book row first: requests for the same book then run one at a time, so the checks below see
        // the latest data (a second click waits here and then finds the first reservation), and every stock
        // path takes its locks in the same order (book, then waiting list and reservations).
        Book book = stockService.lockBook(bookId);
        ThrowUtils.throwIf(Boolean.TRUE.equals(book.getIsDelete()), ErrorCode.NOT_FOUND_ERROR, "Book not found");
        ThrowUtils.throwIf(reservationMapper.countActive(user.getId(), bookId) > 0, ErrorCode.OPERATION_ERROR,
                "You have already reserved this book");

        // A notified user takes the copy held for them, everyone else a shelf copy; their waiting-list entry,
        // if any, is fulfilled
        stockService.takeCopy(bookId, user.getId());

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

        // Insert reservation
        int rows = reservationMapper.insertReservation(reservation);
        log.info("Inserted reservation, rows affected: {}", rows);
        ThrowUtils.throwIf(rows <= 0, ErrorCode.OPERATION_ERROR, "Failed to create reservation");

        log.info("Reservation created successfully with ID: {}", reservation.getReservationId());

        return reservation.getReservationId();
    }

    @Override
    @Transactional
    public boolean cancelReservation(Long reservationId, User user) {
        log.info("Attempting to cancel reservationId: {} for userId: {}", reservationId, user.getId());
        ThrowUtils.throwIf(reservationId == null || reservationId <= 0, ErrorCode.PARAMS_ERROR,
                "Invalid reservation ID");
        ThrowUtils.throwIf(user == null, ErrorCode.NO_AUTH_ERROR, "User not logged in");

        Reservation reservation = reservationMapper.selectById(reservationId);
        log.info("Fetched reservation: {}", reservation);
        ThrowUtils.throwIf(reservation == null || reservation.getIsDelete(), ErrorCode.NOT_FOUND_ERROR,
                "Reservation not found or already cancelled");
        ThrowUtils.throwIf(!reservation.getUserId().equals(user.getId()) && !userService.isAdmin(user),
                ErrorCode.NO_AUTH_ERROR, "Not authorized to cancel this reservation");

        // Update book quantity if reservation is active
        String status = reservation.getReservationStatus();
        if (status != null && List.of("reserved", "borrowed").contains(status)) {
            stockService.giveBackCopy(reservation.getBookId());
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
            stockService.giveBackCopy(reservation.getBookId());
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
        List<Reservation> reservations = reservationMapper.selectAll();
        int current = Math.max(1, pageRequest.getCurrent());
        int pageSize = Math.max(1, pageRequest.getPageSize());
        int total = reservations.size();
        int fromIndex = Math.min((current - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<Reservation> pageList = total == 0 ? List.of() : reservations.subList(fromIndex, toIndex);
        return new PageResult<>(current, pageSize,
                total,
                pageList.stream().map(this::toReservationVO).collect(java.util.stream.Collectors.toList()));
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

    @Override
    @Transactional
    public boolean updatePickupTime(ReservationUpdatePickupRequest req, User user) {
        log.info("Attempting to update pickup time for reservationId: {} by userId: {}",
                req.getReservationId(), user.getId());

        // Validate inputs
        ThrowUtils.throwIf(req == null || req.getReservationId() == null || req.getReservationId() <= 0,
                ErrorCode.PARAMS_ERROR, "Invalid reservation ID");
        ThrowUtils.throwIf(req.getPickupTime() == null, ErrorCode.PARAMS_ERROR, "Pickup time is required");
        ThrowUtils.throwIf(user == null, ErrorCode.NO_AUTH_ERROR, "User not logged in");
        ThrowUtils.throwIf(user.getStatus() == 0, ErrorCode.USER_BANNED, "User is banned");

        // Fetch the reservation
        Reservation reservation = reservationMapper.selectById(req.getReservationId());
        ThrowUtils.throwIf(reservation == null || reservation.getIsDelete(),
                ErrorCode.NOT_FOUND_ERROR, "Reservation not found or already cancelled");
        ThrowUtils.throwIf(!reservation.getUserId().equals(user.getId()) && !userService.isAdmin(user),
                ErrorCode.NO_AUTH_ERROR, "Not authorized to update this reservation");

        // Validate reservation status (only allow updates for 'reserved' status)
        ThrowUtils.throwIf(!"reserved".equals(reservation.getReservationStatus()),
                ErrorCode.OPERATION_ERROR, "Can only update pickup time for reserved status");

        // Validate new pickup time
        Date newPickupTime = req.getPickupTime();
        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        todayCal.set(Calendar.MILLISECOND, 0);
        Date today = todayCal.getTime();

        Calendar pickupCal = Calendar.getInstance();
        pickupCal.setTime(newPickupTime);
        pickupCal.set(Calendar.HOUR_OF_DAY, 0);
        pickupCal.set(Calendar.MINUTE, 0);
        pickupCal.set(Calendar.SECOND, 0);
        pickupCal.set(Calendar.MILLISECOND, 0);
        Date pickupDateOnly = pickupCal.getTime();

        // Allow pickup date to be today or future
        ThrowUtils.throwIf(pickupDateOnly.before(today),
                ErrorCode.PARAMS_ERROR, "New pickup date cannot be in the past");

        // Validate new pickup time is before due time
        ThrowUtils.throwIf(newPickupTime.after(reservation.getDueTime()) ||
                        newPickupTime.equals(reservation.getDueTime()),
                ErrorCode.PARAMS_ERROR, "New pickup time must be before due time");

        // Update the reservation
        reservation.setPickupTime(newPickupTime);
        reservation.setUpdateTime(new Date());
        int rows = reservationMapper.updatePickupTime(req.getReservationId(), newPickupTime);
        log.info("Updated pickup time for reservationId {}, rows affected: {}",
                req.getReservationId(), rows);
        ThrowUtils.throwIf(rows <= 0, ErrorCode.OPERATION_ERROR, "Failed to update pickup time");

        return true;
    }

}