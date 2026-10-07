package com.example.oj.learning.tracking;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "learning.tracking.recovery-enabled", havingValue = "true")
@RequiredArgsConstructor
public class TrackingRecovery {
    private final TrackingService tracking;

    @Scheduled(fixedDelayString = "${learning.tracking.recovery-delay-ms:10000}")
    public void recover() {
        tracking.consumeBatch(100);
    }
}
