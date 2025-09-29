package com.content.springboot_rest_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizeReqDto {

    @NotBlank(message = "auth code cannot be empty")
    @JsonProperty("auth_code")
    private String authCode;

    @NotBlank(message = "action code cannot be empty")
    @JsonProperty("action_code")
    private String actionCode;
}
