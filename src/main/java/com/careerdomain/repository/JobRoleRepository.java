package com.careerdomain.repository;

import com.careerdomain.entity.JobRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobRoleRepository
        extends JpaRepository<JobRole, Long> {

    List<JobRole> findByDomainId(Long domainId);

    Optional<JobRole> findByNameIgnoreCase(
            String name
    );

    List<JobRole> findByNameContainingIgnoreCase(
            String name
    );
}