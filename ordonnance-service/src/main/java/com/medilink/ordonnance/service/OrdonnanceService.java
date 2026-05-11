package com.medilink.ordonnance.service;

import com.medilink.ordonnance.dto.NotificationRequest;
import com.medilink.ordonnance.dto.OrdonnanceDTO;
import com.medilink.ordonnance.entity.Ordonnance;
import com.medilink.ordonnance.entity.OrdonnanceStatus;
import com.medilink.ordonnance.feign.NotificationClient;
import com.medilink.ordonnance.messaging.OrdonnanceEvent;
import com.medilink.ordonnance.messaging.OrdonnanceEventPublisher;
import com.medilink.ordonnance.repository.OrdonnanceRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrdonnanceService {

    private static final Logger log = LoggerFactory.getLogger(OrdonnanceService.class);

    private final OrdonnanceRepository ordonnanceRepository;
    private final NotificationClient notificationClient;         // Feign
    private final OrdonnanceEventPublisher eventPublisher;       // RabbitMQ

    // =====================================================================
    //  SCENARIO 1 - Create ordonnance → notify patient via Feign + RabbitMQ
    // =====================================================================
    @Transactional
    public OrdonnanceDTO createOrdonnance(OrdonnanceDTO dto) {
        Ordonnance ordonnance = toEntity(dto);
        Ordonnance saved = ordonnanceRepository.save(ordonnance);

        // Feign call - synchronous notification
        log.info("[FEIGN] Sending CREATED notification to patient {}", saved.getPatientId());
        notificationClient.sendNotification(NotificationRequest.builder()
                .userId(saved.getPatientId())
                .type("ORDONNANCE_CREATED")
                .message("Votre ordonnance #" + saved.getId() + " a été créée par Dr. " + saved.getDoctorName()
                        + ". Diagnostic: " + saved.getDiagnosis())
                .recipientName(saved.getPatientName())
                .build());

        // RabbitMQ event - asynchronous
        eventPublisher.publishEvent(OrdonnanceEvent.builder()
                .ordonnanceId(saved.getId())
                .patientId(saved.getPatientId())
                .doctorId(saved.getDoctorId())
                .patientName(saved.getPatientName())
                .doctorName(saved.getDoctorName())
                .diagnosis(saved.getDiagnosis())
                .medications(saved.getMedications())
                .eventType("CREATED")
                .date(saved.getDateCreation())
                .build());

        return toDTO(saved);
    }

    // =====================================================================
    //  SCENARIO 2 - Update ordonnance → notify patient of changes via Feign
    // =====================================================================
    @Transactional
    public OrdonnanceDTO updateOrdonnance(Long id, OrdonnanceDTO dto) {
        Ordonnance existing = ordonnanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ordonnance non trouvée: " + id));

        existing.setDiagnosis(dto.getDiagnosis());
        existing.setMedications(dto.getMedications());
        existing.setNotes(dto.getNotes());
        existing.setPatientName(dto.getPatientName());
        existing.setDoctorName(dto.getDoctorName());

        Ordonnance updated = ordonnanceRepository.save(existing);

        // Feign call - notify patient of update
        log.info("[FEIGN] Sending UPDATED notification to patient {}", updated.getPatientId());
        notificationClient.sendNotification(NotificationRequest.builder()
                .userId(updated.getPatientId())
                .type("ORDONNANCE_UPDATED")
                .message("Votre ordonnance #" + updated.getId() + " a été modifiée par Dr. " + updated.getDoctorName()
                        + ". Nouveau diagnostic: " + updated.getDiagnosis())
                .recipientName(updated.getPatientName())
                .build());

        // RabbitMQ async event
        eventPublisher.publishEvent(OrdonnanceEvent.builder()
                .ordonnanceId(updated.getId())
                .patientId(updated.getPatientId())
                .doctorId(updated.getDoctorId())
                .eventType("UPDATED")
                .date(LocalDate.now())
                .build());

        return toDTO(updated);
    }

    // =====================================================================
    //  SCENARIO 3 - Cancel ordonnance → notify patient via Feign
    // =====================================================================
    @Transactional
    public OrdonnanceDTO cancelOrdonnance(Long id) {
        Ordonnance ordonnance = ordonnanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ordonnance non trouvée: " + id));

        ordonnance.setStatus(OrdonnanceStatus.CANCELLED);
        Ordonnance cancelled = ordonnanceRepository.save(ordonnance);

        // Feign call - notify patient of cancellation
        log.info("[FEIGN] Sending CANCELLED notification to patient {}", cancelled.getPatientId());
        notificationClient.sendNotification(NotificationRequest.builder()
                .userId(cancelled.getPatientId())
                .type("ORDONNANCE_CANCELLED")
                .message("Votre ordonnance #" + cancelled.getId() + " a été annulée.")
                .recipientName(cancelled.getPatientName())
                .build());

        // RabbitMQ async event
        eventPublisher.publishEvent(OrdonnanceEvent.builder()
                .ordonnanceId(cancelled.getId())
                .patientId(cancelled.getPatientId())
                .doctorId(cancelled.getDoctorId())
                .eventType("CANCELLED")
                .date(LocalDate.now())
                .build());

        return toDTO(cancelled);
    }

    // =====================================================================
    //  Standard CRUD
    // =====================================================================
    public List<OrdonnanceDTO> getAllOrdonnances() {
        return ordonnanceRepository.findAll().stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public OrdonnanceDTO getOrdonnanceById(Long id) {
        return ordonnanceRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Ordonnance non trouvée: " + id));
    }

    public List<OrdonnanceDTO> getByPatientId(Long patientId) {
        return ordonnanceRepository.findByPatientId(patientId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public List<OrdonnanceDTO> getByDoctorId(Long doctorId) {
        return ordonnanceRepository.findByDoctorId(doctorId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public void deleteOrdonnance(Long id) {
        if (!ordonnanceRepository.existsById(id)) {
            throw new RuntimeException("Ordonnance non trouvée: " + id);
        }
        ordonnanceRepository.deleteById(id);
    }

    // =====================================================================
    //  Mappers
    // =====================================================================
    private OrdonnanceDTO toDTO(Ordonnance o) {
        return OrdonnanceDTO.builder()
                .id(o.getId())
                .doctorId(o.getDoctorId())
                .patientId(o.getPatientId())
                .patientName(o.getPatientName())
                .doctorName(o.getDoctorName())
                .dateCreation(o.getDateCreation())
                .diagnosis(o.getDiagnosis())
                .medications(o.getMedications())
                .notes(o.getNotes())
                .status(o.getStatus())
                .build();
    }

    private Ordonnance toEntity(OrdonnanceDTO dto) {
        return Ordonnance.builder()
                .doctorId(dto.getDoctorId())
                .patientId(dto.getPatientId())
                .patientName(dto.getPatientName())
                .doctorName(dto.getDoctorName())
                .diagnosis(dto.getDiagnosis())
                .medications(dto.getMedications())
                .notes(dto.getNotes())
                .build();
    }
}
