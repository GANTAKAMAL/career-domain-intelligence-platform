package com.careerdomain.repository;

import com.careerdomain.entity.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {

    List<UserSkill> findByUserId(Long userId);

    Optional<UserSkill> findByUserIdAndSkillNameIgnoreCase(
            Long userId,
            String skillName
    );

    void deleteByUserIdAndSkillNameIgnoreCase(
            Long userId,
            String skillName
    );
}
