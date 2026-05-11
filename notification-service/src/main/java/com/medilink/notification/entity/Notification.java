package com.medilink.notification.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

/**
 * MongoDB Document - Technology change from JPA/MySQL to MongoDB NoSQL.
 * Stores notifications with flexible schema, perfect for varied notification types.
 */
@Document(collection = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    private String id;   // MongoDB uses String ObjectId

    @Indexed
    private Long userId;

    private String recipientName;

    private String type;    // ORDONNANCE_CREATED, ORDONNANCE_UPDATED, ORDONNANCE_CANCELLED

    private String message;

    @Indexed
    private NotificationStatus status;

    private Long ordonnanceId;

    @CreatedDate
    private LocalDateTime createdAt;

    private LocalDateTime readAt;
}
