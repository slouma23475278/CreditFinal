package com.medilink.ordonnance.feign;

import com.medilink.ordonnance.dto.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign Client for inter-service communication with Notification Service.
 * Used in 3 scenarios:
 *   1. Ordonnance created  -> notify patient
 *   2. Ordonnance updated  -> notify patient of changes
 *   3. Ordonnance cancelled -> notify patient of cancellation
 */
@FeignClient(name = "notification-service", fallback = NotificationClientFallback.class)
public interface NotificationClient {

    @PostMapping("/api/notifications")
    ResponseEntity<Void> sendNotification(@RequestBody NotificationRequest request);
}
