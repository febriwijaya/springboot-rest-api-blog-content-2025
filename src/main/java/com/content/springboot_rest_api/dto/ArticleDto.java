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
public class ArticleDto {
    private Long id;

    @NotBlank(message = "title cannot be empty")
    private String title;

    private String slug;

    @NotBlank(message = "content cannot be empty")
    private String content;

    @JsonProperty("thumbnail_url")
    private String thumbnailUrl;

    @JsonProperty("author_id")
    private Long authorId;

    @JsonProperty("author_name")
    private String authorName;

    @JsonProperty("category_id")
    private Long categoryId;

    @JsonProperty("category_name")
    private String categoryName;

    @JsonProperty("is_approve")
    private String isApprove;
}
