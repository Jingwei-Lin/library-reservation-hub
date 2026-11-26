package com.comp5619.libraryreservationreviewhub.model.dto.waitingList;

import com.comp5619.libraryreservationreviewhub.model.Enum.WaitingStatusEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/** A waiting-list update (fields left out are unchanged) */
@Data
public class WaitingListUpdateDTO {

    @NotNull(message = "waitId can not be null")
    private Long waitId;

    private WaitingStatusEnum status;
    private Boolean isNotified;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime notificationDate;
}
