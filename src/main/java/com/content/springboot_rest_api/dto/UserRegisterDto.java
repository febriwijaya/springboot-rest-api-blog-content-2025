package com.content.springboot_rest_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterDto {

    @JsonProperty("full_name")
    @NotBlank(message = "Full Name cannot be empty")
    @Pattern(
            regexp = "^[\\p{L} ]+$",
            message = "full name must only contain letters and spaces"
    )
    private String fullName;

    @NotBlank(message = "username cannot be empty")
    private String username;

    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Password must be at least 8 characters and contain letters, numbers, and special characters"
    )
    @NotBlank(message = "Password cannot be empty")
    private String password;

    @NotBlank(message = "email cannot be empty")
    @Email(message = "invalid email format")
    private String email;

    @NotBlank(message = "Phone cannot be empty")
    @Pattern(regexp = "^[+]?\\d{9,15}$", message = "Phone hanya boleh + dan angka, 9–15 digit")
    private String phone;

    @JsonProperty("birth_date")
    @NotNull(message = "Birth date cannot be empty")
    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;

    @JsonProperty("job_title")
    @NotBlank(message = "Job title cannot be empty")
    private String jobTitle;

    @NotBlank(message = "Location cannot be empty")
    private String location;

    @NotBlank(message = "Gender cannot be empty")
    private String gender;

}
