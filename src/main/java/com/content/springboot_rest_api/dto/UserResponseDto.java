package com.content.springboot_rest_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {

    private Long id;

    @JsonProperty("full_name")
    private String fullName;

    private String username;

    private String email;

    private String phone;

    @JsonProperty("birth_date")
    private LocalDate birthDate;

    @JsonProperty("job_title")
    private String jobTitle;

    private String location;

    private String gender;

    private String foto;

    private Set<String> roles; // biar simple, hanya nama rolenya
}
