package com.careerdomain.repository;

import com.careerdomain.entity.Domain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DomainRepository extends JpaRepository<Domain, Long> {

    Optional<Domain> findByNameIgnoreCase(String name);
}