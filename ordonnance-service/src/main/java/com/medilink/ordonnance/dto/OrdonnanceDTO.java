package com.medilink.ordonnance.dto;

import com.medilink.ordonnance.entity.OrdonnanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdonnanceDTO {

    private Long id;

    @NotNull(message = "doctorId is required")
    private Long doctorId;

    @NotNull(message = "patientId is required")
    private Long patientId;

    @NotBlank(message = "patientName is required")
    private String patientName;

    @NotBlank(message = "doctorName is required")
    private String doctorName;

    private LocalDate dateCreation;

    @NotBlank(message = "diagnosis is required")
    private String diagnosis;

    @NotNull(message = "medications list is required")
    private List<String> medications;

    private String notes;

    private OrdonnanceStatus status;
}
