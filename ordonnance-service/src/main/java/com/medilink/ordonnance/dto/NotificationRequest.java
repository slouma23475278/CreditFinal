package com.medilink.ordonnance.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {
    private Long userId;
    private String type;
    private String message;
    private String recipientName;
}
