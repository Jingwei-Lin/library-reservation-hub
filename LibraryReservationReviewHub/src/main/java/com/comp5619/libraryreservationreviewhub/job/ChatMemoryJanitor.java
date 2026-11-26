package com.comp5619.libraryreservationreviewhub.job;

import com.comp5619.libraryreservationreviewhub.chatmemory.FileBasedChatMemory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Deletes AI chat conversations nobody has written to for 7 days (step 5 plan D2) */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatMemoryJanitor {

    static final Duration MAX_IDLE = Duration.ofDays(7);

    private final FileBasedChatMemory chatMemory;

    // every day at 03:45
    @Scheduled(cron = "0 45 3 * * ?")
    public void deleteIdleConversations() {
        int deleted = chatMemory.deleteIdleConversations(MAX_IDLE);
        log.info("ChatMemoryJanitor ran: {} idle conversations deleted.", deleted);
    }
}
