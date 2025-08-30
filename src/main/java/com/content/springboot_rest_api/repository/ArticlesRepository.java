package com.content.springboot_rest_api.repository;

import com.content.springboot_rest_api.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticlesRepository extends JpaRepository<Article, Long> {
}
