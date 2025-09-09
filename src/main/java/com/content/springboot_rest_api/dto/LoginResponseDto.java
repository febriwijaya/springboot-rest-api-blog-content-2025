package com.content.springboot_rest_api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
public class LoginResponseDto {
    private String token;
    private String type = "Bearer";
    private String username;
    private Set<String> roles;
}
