package com.content.springboot_rest_api.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDto {

    private Long id;

    private Long articleId;

    private Long userId;

    private String name;

    private String email;

    private String content;

    private LocalDateTime createdAt;
}
