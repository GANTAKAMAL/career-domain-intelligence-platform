package com.careerdomain.controller;

import java.time.Instant;
import java.util.Map;
import com.careerdomain.exception.DuplicateResourceException;
import com.careerdomain.exception.InvalidCredentialsException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<?> badRequest(IllegalArgumentException e) {
        HttpStatus status = e.getMessage() != null && e.getMessage().toLowerCase().contains("not found")
                ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return response(status, e.getMessage());
    }
    @ExceptionHandler(DuplicateResourceException.class) ResponseEntity<?> conflict(DuplicateResourceException e) { return response(HttpStatus.CONFLICT, e.getMessage()); }
    @ExceptionHandler(InvalidCredentialsException.class) ResponseEntity<?> unauthorized(InvalidCredentialsException e) { return response(HttpStatus.UNAUTHORIZED, e.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(MethodArgumentNotValidException e) { String message = e.getBindingResult().getFieldErrors().stream().findFirst().map(x -> x.getField() + " " + x.getDefaultMessage()).orElse("Invalid request"); return response(HttpStatus.BAD_REQUEST, message); }
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class) ResponseEntity<?> forbidden() { return response(HttpStatus.FORBIDDEN, "Access denied"); }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class) ResponseEntity<?> conflict() { return response(HttpStatus.CONFLICT, "This record conflicts with existing data."); }
    @ExceptionHandler(org.neo4j.driver.exceptions.Neo4jException.class)
    ResponseEntity<?> graphUnavailable(org.neo4j.driver.exceptions.Neo4jException e) {
        return response(HttpStatus.SERVICE_UNAVAILABLE,
                "Knowledge Graph is unavailable. Check that Neo4j is running and verify NEO4J_URI, NEO4J_USERNAME and NEO4J_PASSWORD.");
    }
    @ExceptionHandler(Exception.class) ResponseEntity<?> internalError(Exception e) { return response(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected server error occurred."); }
    private ResponseEntity<Map<String, Object>> response(HttpStatus status, String message) { return ResponseEntity.status(status).body(Map.of("timestamp", Instant.now().toString(), "status", status.value(), "message", message == null ? "Request failed" : message)); }
}
