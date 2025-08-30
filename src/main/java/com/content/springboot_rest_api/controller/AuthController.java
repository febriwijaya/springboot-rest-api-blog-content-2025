package com.content.springboot_rest_api.controller;

import com.content.springboot_rest_api.dto.ArticleDto;
import com.content.springboot_rest_api.dto.UserRegisterDto;
import com.content.springboot_rest_api.entity.User;
import com.content.springboot_rest_api.exception.ErrorDetails;
import com.content.springboot_rest_api.exception.GlobalAPIException;
import com.content.springboot_rest_api.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

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
            User response = userService.register(dto, foto);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while creating article", apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while creating article", e);
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
