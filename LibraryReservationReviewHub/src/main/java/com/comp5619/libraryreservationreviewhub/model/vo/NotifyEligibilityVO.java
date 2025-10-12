package com.comp5619.libraryreservationreviewhub.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotifyEligibilityVO {
    private Long bookId;
    private String bookTitle;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime joinDate;

    /** 当前用户在该书等待队列中的位置（从1开始） */
    private Integer position;

    /** 该书等待中总人数（status=waiting） */
    private Long totalWaiting;

    /** 库存与可用性 */
    private Integer quantity;
    private Boolean available;

    /** 是否可以通知给当前用户 */
    private Boolean canNotify;
}
