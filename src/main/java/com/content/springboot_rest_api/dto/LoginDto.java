package com.content.springboot_rest_api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginDto {

    @NotBlank(message = "Username cannot be empty")
    private String username;

    @NotBlank(message = "The password cannot be empty")
    private String password;
}
