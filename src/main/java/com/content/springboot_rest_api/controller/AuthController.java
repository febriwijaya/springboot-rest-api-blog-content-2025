package com.content.springboot_rest_api.controller;

import com.content.springboot_rest_api.dto.LoginDto;
import com.content.springboot_rest_api.dto.LoginResponseDto;
import com.content.springboot_rest_api.dto.UserRegisterDto;
import com.content.springboot_rest_api.dto.UserResponseDto;
import com.content.springboot_rest_api.exception.ErrorDetails;
import com.content.springboot_rest_api.exception.GlobalAPIException;
import com.content.springboot_rest_api.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
@Slf4j //-- tambah logger untuk tangkap error
public class AuthController {

    private UserService userService;

    @PostMapping(value = "/register", consumes = {"multipart/form-data"})
    public ResponseEntity<?> registerUser(
            @Valid @RequestPart("data") UserRegisterDto dto,
            @RequestPart(value = "foto", required = false) MultipartFile foto
    ) {
        try {
            UserResponseDto response = userService.register(dto, foto);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while registering user", apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while registering user", e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDto loginDto) {
        try {
            LoginResponseDto response = userService.login(loginDto);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while login user", apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while login user", e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PutMapping(value = "/users/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @Valid @RequestPart("data") UserRegisterDto dto,
            @RequestPart(value = "foto", required = false) MultipartFile foto
    ) {
        try {
            UserResponseDto response = userService.update(id, dto, foto);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while updating user", apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while updating user", e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping(value = "/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<UserResponseDto> response = userService.getAll();
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while fetching all users", apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while fetching all users", e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping(value = "/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            UserResponseDto response = userService.getById(id);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while get user by id", apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while get user by id", e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUserById(@PathVariable Long id) {
        try {
            userService.delete(id);
            return ResponseEntity.ok("User successfully deleted");
        } catch (GlobalAPIException apiEx) {
            log.error("Error while deleting user", apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while deleting user", e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    //  Helper method biar engga copy-paste error response
    private ResponseEntity<ErrorDetails> buildErrorResponse(String message, String details, HttpStatus status) {
        ErrorDetails errorDetails = new ErrorDetails(
                LocalDateTime.now(),
                message,
                details
        );
        return ResponseEntity.status(status).body(errorDetails);
    }
}
