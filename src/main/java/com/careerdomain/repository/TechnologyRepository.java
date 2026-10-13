package com.careerdomain.repository;

import com.careerdomain.entity.Technology;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TechnologyRepository extends JpaRepository<Technology, Long> {
    Optional<Technology> findByNameIgnoreCase(String name);
}
