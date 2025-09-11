package com.content.springboot_rest_api.repository;

import com.content.springboot_rest_api.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    boolean existsBySlug(String slug);
    Optional<Tag> findBySlug(String slug);
}
