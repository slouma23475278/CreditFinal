package com.medilink.ordonnance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "ordonnances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ordonnance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Long doctorId;

    @NotNull
    private Long patientId;

    @NotBlank
    private String patientName;

    @NotBlank
    private String doctorName;

    private LocalDate dateCreation;

    @NotBlank
    private String diagnosis;

    @ElementCollection
    @CollectionTable(name = "ordonnance_medications", joinColumns = @JoinColumn(name = "ordonnance_id"))
    @Column(name = "medication")
    private List<String> medications;

    private String notes;

    @Enumerated(EnumType.STRING)
    private OrdonnanceStatus status;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDate.now();
        if (this.status == null) {
            this.status = OrdonnanceStatus.ACTIVE;
        }
    }
}
