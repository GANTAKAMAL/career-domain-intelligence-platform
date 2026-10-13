package com.careerdomain.service;

import static org.junit.jupiter.api.Assertions.*;
import com.careerdomain.exception.DuplicateResourceException;
import com.careerdomain.exception.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceTests {
    @Autowired private UserService users;

    @Test
    void registrationHashesPasswordNormalizesEmailAndRejectsDuplicate() {
        var user = users.register(" Student ", " STUDENT@example.test ", "SecurePass123");
        assertEquals("student@example.test", user.getEmail());
        assertNotEquals("SecurePass123", user.getPassword());
        assertThrows(DuplicateResourceException.class, () -> users.register("Student", "student@example.test", "SecurePass123"));
    }

    @Test
    void invalidLoginUsesExplicitDomainError() {
        assertThrows(InvalidCredentialsException.class, () -> users.login("unknown@example.test", "wrong"));
    }
}
