package com.medilink.notification.messaging;

import com.medilink.notification.entity.Notification;
import com.medilink.notification.entity.NotificationStatus;
import com.medilink.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * RabbitMQ Consumer — listens to ordonnance events from Ordonnance Service.
 * Valeur ajoutée: communication asynchrone via message broker.
 */
@Component
@RequiredArgsConstructor
public class OrdonnanceEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrdonnanceEventConsumer.class);

    private final NotificationRepository notificationRepository;

    @RabbitListener(queues = "ordonnance.notification.queue")
    public void handleOrdonnanceEvent(OrdonnanceEvent event) {
        log.info("[RabbitMQ] Received event: type={}, ordonnanceId={}, patientId={}",
                event.getEventType(), event.getOrdonnanceId(), event.getPatientId());

        String message = buildMessage(event);

        Notification notification = Notification.builder()
                .userId(event.getPatientId())
                .recipientName(event.getPatientName())
                .type("ASYNC_" + event.getEventType())
                .message(message)
                .status(NotificationStatus.SENT)
                .ordonnanceId(event.getOrdonnanceId())
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        log.info("[RabbitMQ] Notification async saved for patient {}", event.getPatientId());
    }

    private String buildMessage(OrdonnanceEvent event) {
        return switch (event.getEventType()) {
            case "CREATED" -> String.format(
                "[ASYNC] Ordonnance #%d créée par Dr. %s. Diagnostic: %s. Médicaments: %s",
                event.getOrdonnanceId(), event.getDoctorName(),
                event.getDiagnosis(), event.getMedications());
            case "UPDATED" -> String.format(
                "[ASYNC] Ordonnance #%d mise à jour par Dr. %s.",
                event.getOrdonnanceId(), event.getDoctorName());
            case "CANCELLED" -> String.format(
                "[ASYNC] Ordonnance #%d annulée.", event.getOrdonnanceId());
            default -> "[ASYNC] Événement ordonnance: " + event.getEventType();
        };
    }
}
