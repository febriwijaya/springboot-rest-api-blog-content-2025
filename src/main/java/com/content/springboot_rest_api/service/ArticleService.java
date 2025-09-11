package com.content.springboot_rest_api.service;

import com.content.springboot_rest_api.dto.ArticleDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ArticleService {

    ArticleDto createArticle(ArticleDto articleDto, MultipartFile thumbnail, Long authorId) throws IOException;

    List<ArticleDto> getAllArticle();

    ArticleDto getArticleById(Long id);

    ArticleDto updateArticle(Long id, ArticleDto dto, MultipartFile thumbnail) throws IOException;

    void deleteArticle(Long id);

    ArticleDto getArticleBySlug(String slug);

}
