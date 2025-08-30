package com.content.springboot_rest_api.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class GlobalAPIException extends RuntimeException {
    private final HttpStatus status;

    public GlobalAPIException(HttpStatus status, String message) {
        super(message); // simpan message ke RuntimeException
        this.status = status;
    }
}
