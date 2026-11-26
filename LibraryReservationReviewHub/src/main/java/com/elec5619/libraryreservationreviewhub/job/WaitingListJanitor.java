package com.elec5619.libraryreservationreviewhub.job;

import com.elec5619.libraryreservationreviewhub.service.WaitingListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WaitingListJanitor {

    private final WaitingListService waitingListService;

    /**
     * Every hour at :15, ends the holds that ran out (3 days after the notification) and passes each copy to
     * the next person waiting. Hourly, so a hold lasts 3 days and at most an hour more.
     */
    @Scheduled(cron = "0 15 * * * ?")
    public void cancelOverdue() {
        int ended = waitingListService.expireHolds();
        log.info("WaitingListJanitor ran: {} holds ended.", ended);
    }
}
