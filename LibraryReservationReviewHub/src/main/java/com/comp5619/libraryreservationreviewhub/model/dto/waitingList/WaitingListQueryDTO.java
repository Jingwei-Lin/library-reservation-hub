package com.comp5619.libraryreservationreviewhub.model.dto.waitingList;

import com.comp5619.libraryreservationreviewhub.model.Enum.WaitingStatusEnum;
import lombok.Data;

/** Waiting-list filters, for lists and pages */
@Data
public class WaitingListQueryDTO {

    private Long userId;
    private Long bookId;
    private WaitingStatusEnum status;
    private Boolean isNotified;
    /** Only entries still in the queue (waiting or notified): what a user sees of their own list */
    private Boolean activeOnly;

    private Integer pageNo = 1;
    private Integer pageSize = 10;
    private String orderBy; // eg "join_date desc"
}
