package com.content.springboot_rest_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDtoTmp {

    private Long idTmp;

    private Long idCategory;

    private String name;

    private String slug;

    @JsonProperty("auth_code")
    private String authCode;

    @JsonProperty("action_code")
    private String actionCode;

    private String createdBy;

    private String updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
