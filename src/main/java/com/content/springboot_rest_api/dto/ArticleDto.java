package com.content.springboot_rest_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

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

    @JsonProperty("thumbnail_url_pending")
    private String thumbnailUrlPending;

    @JsonProperty("thumbnail_url_approve")
    private String thumbnailUrlApprove;

    @JsonProperty("author_id")
    private Long authorId;

    @JsonProperty("author_name")
    private String authorName;

    @NotNull(message = "category id cannot be null")
    @Min(value = 1, message = "category id must be greater than 0")
    @JsonProperty("category_id")
    private Long categoryId;

    @JsonProperty("category_name")
    private String categoryName;

    @JsonProperty("auth_code")
    private String authCode;

    @JsonProperty("action_code")
    private String actionCode;

    private Long views = 0L;

    @JsonProperty("tag_ids")
    private List<Long> tagIds;

    @JsonProperty("tag_names")
    private List<String> tagNames;
}
