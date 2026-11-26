package com.elec5619.libraryreservationreviewhub.model.vo;

import com.elec5619.libraryreservationreviewhub.model.enums.WaitingStatusEnum;
import com.elec5619.libraryreservationreviewhub.service.StockService;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** return view object */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitingListVO {

    private Long waitId;
    private Long userId;
    private Long bookId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime joinDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime notificationDate;

    private Boolean isNotified;

    /** waiting / notified / cancelled / fulfilled */
    private WaitingStatusEnum status;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String userName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String bookTitle;

    /**
     * When the copy held for a notified user passes on; null unless notified. An instant (ISO 8601 with a zone,
     * e.g. 2026-11-29T04:08:37Z) rather than the zone-less pattern of the dates above, so the browser can show
     * the deadline in the reader's own time. The dates read from the database are in the server's zone.
     */
    public Instant getHoldExpiresAt() {
        return status == WaitingStatusEnum.NOTIFIED && notificationDate != null
                ? notificationDate.plusDays(StockService.HOLD_DAYS).atZone(ZoneId.systemDefault()).toInstant()
                : null;
    }
}
