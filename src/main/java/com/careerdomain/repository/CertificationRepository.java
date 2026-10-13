package com.careerdomain.repository;

import com.careerdomain.entity.Certification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CertificationRepository extends JpaRepository<Certification, Long> {
    Optional<Certification> findByNameIgnoreCase(String name);
}
