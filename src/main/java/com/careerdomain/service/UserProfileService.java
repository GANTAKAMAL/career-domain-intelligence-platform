package com.careerdomain.service;

import org.springframework.stereotype.Service;

import com.careerdomain.entity.UserProfile;
import com.careerdomain.repository.UserProfileRepository;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public UserProfile createProfile(UserProfile profile) {
        return userProfileRepository.save(profile);
    }

    public UserProfile getProfileById(Long id) {
        return userProfileRepository.findById(id).orElse(null);
    }

    public UserProfile getProfileByUserId(Long userId) {
        return userProfileRepository.findByUserId(userId).orElse(null);
    }

    public UserProfile updateProfile(Long id, UserProfile profile) {

        UserProfile existing = getProfileById(id);

        if (existing == null) {
            return null;
        }

        existing.setSkills(profile.getSkills());
        existing.setExperience(profile.getExperience());
        existing.setEducation(profile.getEducation());
        existing.setCareerGoal(profile.getCareerGoal());

        return userProfileRepository.save(existing);
    }

    public void deleteProfile(Long id) {
        userProfileRepository.deleteById(id);
    }
}