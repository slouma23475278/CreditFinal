package com.medilink.ordonnance.repository;

import com.medilink.ordonnance.entity.Ordonnance;
import com.medilink.ordonnance.entity.OrdonnanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrdonnanceRepository extends JpaRepository<Ordonnance, Long> {

    List<Ordonnance> findByPatientId(Long patientId);

    List<Ordonnance> findByDoctorId(Long doctorId);

    List<Ordonnance> findByStatus(OrdonnanceStatus status);

    List<Ordonnance> findByPatientIdAndStatus(Long patientId, OrdonnanceStatus status);
}
