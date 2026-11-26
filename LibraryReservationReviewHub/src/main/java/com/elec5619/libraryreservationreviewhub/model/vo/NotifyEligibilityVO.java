package com.elec5619.libraryreservationreviewhub.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotifyEligibilityVO {
    private Long waitId;
    private Long userId;

    private Long bookId;
    private String bookTitle;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime joinDate;

    /** The user's place in the book's queue, from 1 */
    private Integer position;

    /** People waiting for the book (status=waiting) */
    private Long totalWaiting;

    /** Stock and availability */
    private Integer quantity;
    private Integer heldQuantity;
    private Boolean available;

    /** Users already notified (status=notified) */
    private Integer notifiedCount;

    /** Whether this user can be notified */
    private Boolean canNotify;
}
