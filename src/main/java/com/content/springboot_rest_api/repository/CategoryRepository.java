package com.content.springboot_rest_api.repository;

import com.content.springboot_rest_api.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsBySlug(String slug);

    Optional<Category> findBySlug(String slug);

    List<Category> findByAuthCodeAndCreatedAtBefore(String authCode, LocalDateTime createdAt);

    List<Category> findByCreatedBy(String createdBy);

    List<Category> findByAuthCode(String authCode);

}
