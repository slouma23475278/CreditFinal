package com.medilink.ordonnance.messaging;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdonnanceEvent implements Serializable {
    private Long ordonnanceId;
    private Long patientId;
    private Long doctorId;
    private String patientName;
    private String doctorName;
    private String diagnosis;
    private List<String> medications;
    private String eventType;   // CREATED, UPDATED, CANCELLED
    private LocalDate date;
}
