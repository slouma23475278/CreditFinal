package com.medilink.notification.dto;

import com.medilink.notification.entity.NotificationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {
    private String id;
    private Long userId;
    private String recipientName;
    private String type;
    private String message;
    private NotificationStatus status;
    private Long ordonnanceId;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
