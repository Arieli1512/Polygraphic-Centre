---
name: 'springboot4-java25-servlet'
description: 'Enforces coding standards for Java 25, Spring Boot 4, Jakarta Servlet architecture, and strict controller logging.'
version: '1.0.0'
frameworks:
  - 'java@25'
  - 'springboot@4.x'
  - 'jakarta.servlet'
---

# Java 25 & Spring Boot 4 Servlet Code Generation Blueprint

Use this skill whenever generating, editing, or refactoring backend controllers, data transfer objects, service layers, exceptions, or database mapping objects.

## 1. Language & Framework Guardrails (Java 25 & Spring Boot 4)

- **Immutable Data Carriers:** Use Java `record` declarations for all Data Transfer Objects (DTOs), request payloads, and response envelopes. Do not use standard classes with Lombok `@Data` annotations for incoming or outgoing network data.
- **Null Safety Integration:** Enforce Spring Boot 4’s JSpecify standard nullability semantics. Explicitly annotate parameters and return types that are allowed to be null using `org.jspecify.annotations.Nullable`.
- **Modern Control Flow:** Maximize the use of Java 25 pattern matching for `switch` statements and expressions when parsing variations in inputs or handling polymorphic domain exceptions.
- **Concurrency Execution:** Optimize for Servlet-stack Virtual Threads. Never use asynchronous reactive types (`Mono`, `Flux`). Code must remain linear, blocking, and straightforward.

## 2. API Validation & Global Error Trapping

- **Jakarta Pipeline Enforcements:** Every controller `@RequestBody` payload must be prefixed with the `@Valid` or `@Validated` triggers.
- **DTO Safety Limits:** Record fields inside payload signatures must carry strict, defensive Jakarta attributes (`@NotNull`, `@Size`, `@NotBlank`, `@Min`).
- **Global Error Handling:** All validation intercept failures or entity absences must clear through a unified `@RestControllerAdvice` controller. Never let raw server or SQL traces leak out through a 500 error to the React frontend.

## 3. Structural Code Blueprints

### Preferred Architecture: Strict Validation Payloads (Java 25 Record)
```java
package com.example.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Clean immutable record tracking validation boundaries for Spring Boot 4 engines.
 */
public record UserRegistrationRequest(
    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    String username,

    @NotBlank(message = "Password context required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    String password
) {}
```

```java
package com.example.app.controller;

import com.example.app.dto.UserRegistrationRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @PostMapping
    public ResponseEntity<Void> registerUser(@Valid @RequestBody UserRegistrationRequest request) {
        log.info("Processing ingestion pipeline for inbound user payload: {}", request.username());
        
        // Business execution logic goes here
        
        log.info("Successfully established user account for identifier: {}", request.username());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
```

```java
package com.example.app.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationFailures(MethodArgumentNotValidException ex) {
        Map<String, String> errorMapping = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                error -> error.getField(),
                error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid data shape"
            ));
            
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMapping);
    }
}
```