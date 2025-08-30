package com.content.springboot_rest_api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "articles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Article extends BaseEntity{

    @Column(nullable = false)
    private String title;

    @Column(unique = true, nullable = false)
    private String slug;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private  String content;

    private  String thumbnailUrl;

    private String isApprove;

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private  User author;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}
