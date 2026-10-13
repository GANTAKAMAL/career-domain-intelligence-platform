package com.careerdomain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.careerdomain.entity.JobDescription;

@Repository
public interface JobDescriptionRepository
        extends JpaRepository<JobDescription, Long> {

    List<JobDescription> findByTitleContainingIgnoreCase(String title);
}