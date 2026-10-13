package com.careerdomain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.careerdomain.entity.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}