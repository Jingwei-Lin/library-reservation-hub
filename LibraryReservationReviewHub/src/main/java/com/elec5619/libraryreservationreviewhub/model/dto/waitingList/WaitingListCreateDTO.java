package com.elec5619.libraryreservationreviewhub.model.dto.waitingList;

import com.elec5619.libraryreservationreviewhub.model.enums.WaitingStatusEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** A new waiting-list entry */
@Data
public class WaitingListCreateDTO {

    @NotNull(message = "userId can not be null")
    private Long userId;

    @NotNull(message = "bookId can not be null")
    private Long bookId;

    private WaitingStatusEnum status;   // default WAITING
    private Boolean isNotified;         // default false
}
