package com.medilink.ordonnance.controller;

import com.medilink.ordonnance.dto.OrdonnanceDTO;
import com.medilink.ordonnance.service.OrdonnanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ordonnances")
@RequiredArgsConstructor
@Tag(name = "Ordonnance Service", description = "Gestion des ordonnances médicales")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(origins = "*")
public class OrdonnanceController {

    private final OrdonnanceService ordonnanceService;

    @GetMapping
    @Operation(summary = "Liste toutes les ordonnances")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<List<OrdonnanceDTO>> getAllOrdonnances() {
        return ResponseEntity.ok(ordonnanceService.getAllOrdonnances());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer une ordonnance par ID")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT', 'ADMIN')")
    public ResponseEntity<OrdonnanceDTO> getOrdonnanceById(@PathVariable Long id) {
        return ResponseEntity.ok(ordonnanceService.getOrdonnanceById(id));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Ordonnances par patient - SCENARIO 1 Feign")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT', 'ADMIN')")
    public ResponseEntity<List<OrdonnanceDTO>> getByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(ordonnanceService.getByPatientId(patientId));
    }

    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "Ordonnances par médecin")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<List<OrdonnanceDTO>> getByDoctor(@PathVariable Long doctorId) {
        return ResponseEntity.ok(ordonnanceService.getByDoctorId(doctorId));
    }

    @PostMapping
    @Operation(summary = "Créer une ordonnance - SCENARIO 1: notifie le patient via Feign + RabbitMQ")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<OrdonnanceDTO> createOrdonnance(@Valid @RequestBody OrdonnanceDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ordonnanceService.createOrdonnance(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une ordonnance - SCENARIO 2: notifie le patient via Feign")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<OrdonnanceDTO> updateOrdonnance(@PathVariable Long id,
                                                           @Valid @RequestBody OrdonnanceDTO dto) {
        return ResponseEntity.ok(ordonnanceService.updateOrdonnance(id, dto));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Annuler une ordonnance - SCENARIO 3: notifie le patient via Feign")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<OrdonnanceDTO> cancelOrdonnance(@PathVariable Long id) {
        return ResponseEntity.ok(ordonnanceService.cancelOrdonnance(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une ordonnance")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteOrdonnance(@PathVariable Long id) {
        ordonnanceService.deleteOrdonnance(id);
        return ResponseEntity.ok("Ordonnance supprimée avec succès.");
    }
}
