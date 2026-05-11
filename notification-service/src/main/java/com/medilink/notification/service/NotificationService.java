package com.medilink.notification.service;

import com.medilink.notification.dto.NotificationDTO;
import com.medilink.notification.dto.NotificationRequest;
import com.medilink.notification.entity.Notification;
import com.medilink.notification.entity.NotificationStatus;
import com.medilink.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    /**
     * Called by Feign client from Ordonnance Service (synchronous).
     */
    public NotificationDTO sendNotification(NotificationRequest request) {
        log.info("[FEIGN-RECEIVED] Notification request: type={}, userId={}", request.getType(), request.getUserId());

        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .recipientName(request.getRecipientName())
                .type(request.getType())
                .message(request.getMessage())
                .status(NotificationStatus.SENT)
                .ordonnanceId(request.getOrdonnanceId())
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("[MONGO] Notification saved with id={}", saved.getId());
        return toDTO(saved);
    }

    public List<NotificationDTO> getAllNotifications() {
        return notificationRepository.findAll().stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public List<NotificationDTO> getByUserId(Long userId) {
        return notificationRepository.findByUserId(userId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public List<NotificationDTO> getUnreadByUser(Long userId) {
        return notificationRepository.findByUserIdAndStatus(userId, NotificationStatus.SENT).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public NotificationDTO markAsRead(String id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée: " + id));
        notification.setStatus(NotificationStatus.READ);
        notification.setReadAt(LocalDateTime.now());
        return toDTO(notificationRepository.save(notification));
    }

    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.SENT);
    }

    public void deleteNotification(String id) {
        notificationRepository.deleteById(id);
    }

    private NotificationDTO toDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .recipientName(n.getRecipientName())
                .type(n.getType())
                .message(n.getMessage())
                .status(n.getStatus())
                .ordonnanceId(n.getOrdonnanceId())
                .createdAt(n.getCreatedAt())
                .readAt(n.getReadAt())
                .build();
    }
}
