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
public class TagDto {
    private Long id;
    @NotBlank(message = "name cannot be empty")
    private String name;
    private String slug;
}
