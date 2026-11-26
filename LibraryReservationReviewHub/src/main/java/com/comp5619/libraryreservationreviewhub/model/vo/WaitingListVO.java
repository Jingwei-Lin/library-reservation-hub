package com.comp5619.libraryreservationreviewhub.model.vo;

import com.comp5619.libraryreservationreviewhub.model.Enum.WaitingStatusEnum;
import com.comp5619.libraryreservationreviewhub.service.StockService;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

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

    /** When the copy held for a notified user passes on; null unless notified */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public LocalDateTime getHoldExpiresAt() {
        return status == WaitingStatusEnum.NOTIFIED && notificationDate != null
                ? notificationDate.plusDays(StockService.HOLD_DAYS)
                : null;
    }
}
