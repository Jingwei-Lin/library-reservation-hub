package com.comp5619.libraryreservationreviewhub.controller;

import com.comp5619.libraryreservationreviewhub.annotation.AuthCheck;
import com.comp5619.libraryreservationreviewhub.common.BaseResponse;
import com.comp5619.libraryreservationreviewhub.common.PageRequest;
import com.comp5619.libraryreservationreviewhub.common.PageResult;
import com.comp5619.libraryreservationreviewhub.common.ResultUtils;
import com.comp5619.libraryreservationreviewhub.constant.UserConstant;
import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
import com.comp5619.libraryreservationreviewhub.exception.ThrowUtils;
import com.comp5619.libraryreservationreviewhub.model.dto.waitingList.*;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.Enum.WaitingStatusEnum;
import com.comp5619.libraryreservationreviewhub.model.vo.NotifyEligibilityVO;
import com.comp5619.libraryreservationreviewhub.model.vo.WaitingListVO;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import com.comp5619.libraryreservationreviewhub.service.WaitingListService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/waiting-list")
@RequiredArgsConstructor
public class WaitingListController {

    private final WaitingListService waitingListService;
    private final UserService userService;


    /* ============ Current user: queries ============ */

    /**
     * The signed-in user's waiting-list entries.
     * Users see only those still in the queue (not CANCELLED or FULFILLED); admins see all
     */
    @GetMapping("/my")
    @AuthCheck
    public BaseResponse<List<WaitingListVO>> listMyWaitlist(HttpServletRequest request) {
        User current = userService.getLoginUser(request);
        ThrowUtils.throwIf(current == null, ErrorCode.NOT_LOGIN_ERROR);

        WaitingListQueryDTO query = new WaitingListQueryDTO();
        query.setUserId(current.getId());

        List<WaitingListVO> list = waitingListService.listByUserWithDetails(current.getId());

        // users: only the entries still in the queue
        if (!userService.isAdmin(current)) {
            list.removeIf(vo -> vo.getStatus() == WaitingStatusEnum.CANCELLED);
            list.removeIf(vo -> vo.getStatus() == WaitingStatusEnum.FULFILLED);
        }
        return ResultUtils.success(list);
    }

    /**
     * The signed-in user's entries with one status.
     * Users can't ask for CANCELLED
     * Example:GET /waiting-list/my/status?status=waiting
     */
    @GetMapping("/my/status")
    @AuthCheck
    public BaseResponse<List<WaitingListVO>> listMyByStatus(@RequestParam("status") String status,
                                                            HttpServletRequest request) {
        User current = userService.getLoginUser(request);
        ThrowUtils.throwIf(current == null, ErrorCode.NOT_LOGIN_ERROR);

        WaitingStatusEnum s;
        try {
            s = WaitingStatusEnum.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "Illegal state parameter: " + status);
            return ResultUtils.success(List.of()); // unreachable; the compiler needs a return
        }

        // users can't list their cancelled entries
        ThrowUtils.throwIf(!userService.isAdmin(current) && s == WaitingStatusEnum.CANCELLED,
                ErrorCode.FORBIDDEN_ERROR, "Ordinary users are not allowed to view the cancelled records.");

        WaitingListQueryDTO query = new WaitingListQueryDTO();
        query.setUserId(current.getId());
        query.setStatus(s);

        return ResultUtils.success(waitingListService.listByQuery(query));
    }

    /* ============ Current user: join and cancel ============ */

    /**
     * The current user joins the queue for a book. Only when no copy is on the shelf, and not twice
     * (see {@link WaitingListService#join}).
     */
    @PostMapping("/my/add")
    @AuthCheck
    public BaseResponse<Long> addToMyWaitlist(@Valid @RequestBody AddReq req, HttpServletRequest request) {
        User current = userService.getLoginUser(request);
        ThrowUtils.throwIf(current == null, ErrorCode.NOT_LOGIN_ERROR);

        Long id = waitingListService.join(current.getId(), req.getBookId());
        ThrowUtils.throwIf(id == null, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(id);
    }

    /**
     * The current user leaves the queue for a book (the entry becomes CANCELLED). A copy held for them passes to
     * the next person waiting, or goes on the shelf.
     * Example: DELETE /waiting-list/my/123
     */
    @DeleteMapping("/my/{bookId}")
    @AuthCheck
    public BaseResponse<Boolean> cancelMyWait(@PathVariable("bookId") Long bookId, HttpServletRequest request) {
        User current = userService.getLoginUser(request);
        ThrowUtils.throwIf(current == null, ErrorCode.NOT_LOGIN_ERROR);

        waitingListService.leave(current.getId(), bookId);
        return ResultUtils.success(true);
    }

    /* ============ Admin: by user id ============ */

    /**
     * Admin: all of one user's entries, CANCELLED included
     */
    @GetMapping("/user/{userId}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<WaitingListVO>> listByUserAdmin(@PathVariable("userId") Long userId) {
        WaitingListQueryDTO query = new WaitingListQueryDTO();
        query.setUserId(userId);
        return ResultUtils.success(waitingListService.listByQuery(query));
    }

    /* ============ Paged ============ */

    /**
     * Example:GET /waiting-list/my/page?current=1&pageSize=10
     */
    @GetMapping("/my/page")
    @AuthCheck
    public BaseResponse<PageResult<WaitingListVO>> pageMyWaitlist(PageRequest pageReq,
                                                                  HttpServletRequest request) {
        User current = userService.getLoginUser(request);
        ThrowUtils.throwIf(current == null, ErrorCode.NOT_LOGIN_ERROR);

        // the current user's entries; like /my, a user sees only those still in the queue (an admin sees all).
        // Filtered in the query, so the total matches the rows
        WaitingListQueryDTO query = new WaitingListQueryDTO();
        query.setUserId(current.getId());
        query.setActiveOnly(!userService.isAdmin(current));
        return ResultUtils.success(waitingListService.pageByQuery(query, pageReq));
    }

    /**
     * The user's place in each queue they are waiting in. Read-only, kept for API compatibility: copies are
     * handed out when they come back, so looking here no longer notifies anyone.
     */
    @GetMapping("/my/eligibility")
    @AuthCheck
    public BaseResponse<List<NotifyEligibilityVO>> myNotifyEligibility(HttpServletRequest request) {
        User current = userService.getLoginUser(request);
        ThrowUtils.throwIf(current == null, ErrorCode.NOT_LOGIN_ERROR);
        return ResultUtils.success(waitingListService.listQueuePositionsForUser(current.getId()));
    }


    @Data
    public static class AddReq {
        @NotNull(message = "bookId can not be null")
        private Long bookId;
    }
}