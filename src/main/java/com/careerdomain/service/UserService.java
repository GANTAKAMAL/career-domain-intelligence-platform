package com.careerdomain.service;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.careerdomain.repository.UserRepository;
import com.careerdomain.entity.User;
import com.careerdomain.exception.DuplicateResourceException;
import com.careerdomain.exception.InvalidCredentialsException;

@Service
public class UserService {

    private final UserRepository userRepository; private final PasswordEncoder passwordEncoder;
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) { this.userRepository = userRepository; this.passwordEncoder = passwordEncoder; }

    public User registerUser(User user) {

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }
    public User register(String name, String email, String password) {
        String normalized = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalized)) throw new DuplicateResourceException("An account already exists for that email address.");
        return registerUser(new User(name.trim(), normalized, password));
    }
    public User login(String email, String password) {
        User user = loginUser(email.trim().toLowerCase(), password);
        if (user == null) throw new InvalidCredentialsException();
        return user;
    }

    public User loginUser(String email, String password) {

        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null &&
            passwordEncoder.matches(password, user.getPassword())) {

            return user;
        }

        return null;
    }
    public boolean userExists(String email) {
        return userRepository.existsByEmail(email);
    }
}
