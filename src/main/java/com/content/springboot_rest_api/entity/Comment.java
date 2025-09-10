package com.content.springboot_rest_api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment extends BaseEntity{

    // relasi ke article
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    // relasi ke user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Column(length = 100)
    private String name;

    @Column(length = 100)
    private String email;

    @Column(columnDefinition = "TEXT", nullable = false)
    private  String content;

}
