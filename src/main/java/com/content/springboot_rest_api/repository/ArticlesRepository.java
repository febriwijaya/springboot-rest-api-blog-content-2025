package com.content.springboot_rest_api.repository;

import com.content.springboot_rest_api.entity.Article;
import com.content.springboot_rest_api.entity.Category;
import com.content.springboot_rest_api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ArticlesRepository extends JpaRepository<Article, Long> {
    // Cari artikel berdasarkan Category
    List<Article> findByCategory(Category category);

    Optional<Article> findBySlug(String slug);

    List<Article> findByAuthCodeAndCreatedAtBefore(String authCode, LocalDateTime dateTime);

    // Tambahan
    List<Article> findByAuthor(User author);
    List<Article> findByAuthCode(String authCode);
}
