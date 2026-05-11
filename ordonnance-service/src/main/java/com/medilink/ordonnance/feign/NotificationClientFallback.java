package com.medilink.ordonnance.feign;

import com.medilink.ordonnance.dto.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Fallback implementation when Notification Service is unavailable.
 * Ensures resilience — ordonnance operations still succeed even if notifications fail.
 */
@Component
public class NotificationClientFallback implements NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClientFallback.class);

    @Override
    public ResponseEntity<Void> sendNotification(NotificationRequest request) {
        log.warn("[FALLBACK] Notification service unavailable. Could not send notification to userId={}, type={}",
                request.getUserId(), request.getType());
        return ResponseEntity.ok().build();
    }
}
