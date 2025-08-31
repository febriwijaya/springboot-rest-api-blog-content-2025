package com.content.springboot_rest_api.service.impl;

import com.content.springboot_rest_api.dto.ArticleDto;
import com.content.springboot_rest_api.entity.Article;
import com.content.springboot_rest_api.entity.Category;
import com.content.springboot_rest_api.entity.User;
import com.content.springboot_rest_api.exception.GlobalAPIException;
import com.content.springboot_rest_api.repository.ArticlesRepository;
import com.content.springboot_rest_api.repository.CategoryRepository;
import com.content.springboot_rest_api.repository.UserRepository;
import com.content.springboot_rest_api.service.ArticleService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ArticleServiceImpl implements ArticleService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ArticlesRepository articlesRepository;
    private final ModelMapper modelMapper;

    @Value("${app.upload.article-photo-dir}")
    private String uploadDir;

    // Constructor injection untuk repository & modelMapper saja
    public ArticleServiceImpl(UserRepository userRepository,
                              CategoryRepository categoryRepository,
                              ArticlesRepository articlesRepository,
                              ModelMapper modelMapper) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.articlesRepository = articlesRepository;
        this.modelMapper = modelMapper;
    }

    private static final long MAX_SIZE = 2 * 1024 * 1024; // 2 MB
    private static final List<String> ALLOWED_EXTENSIONS =
            List.of("png", "jpg", "jpeg", "gif", "webp", "svg", "heic", "heif");
    private static final List<String> ALLOWED_MIME_TYPES =
            List.of("image/png", "image/jpeg", "image/gif",
                    "image/webp", "image/svg+xml", "image/heic", "image/heif");

    // ---------------- CREATE ----------------
    @Transactional
    @Override
    public ArticleDto createArticle(ArticleDto articleDto, MultipartFile thumbnail, Long authorId) throws IOException {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Author not found"));

        Category category = categoryRepository.findById(articleDto.getCategoryId())
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Category not found"));

        Article article = modelMapper.map(articleDto, Article.class);
        article.setAuthor(author);
        article.setCategory(category);
        article.setSlug(generateSlug(articleDto.getTitle()));
        article.setIsApprove("N");

        if (thumbnail != null && !thumbnail.isEmpty()) {
            validateFile(thumbnail);
            String fileName = saveFile(thumbnail);
            article.setThumbnailUrl("thumbnails/" + fileName);
        }

        Article saved = articlesRepository.save(article);
        return mapToResponse(saved);
    }

    // ---------------- READ ----------------
    @Override
    public List<ArticleDto> getAllArticle() {
        return articlesRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ArticleDto getArticleById(Long id) {
        Article article = articlesRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Article not found"));
        return mapToResponse(article);
    }

    // ---------------- UPDATE ----------------
    @Transactional
    @Override
    public ArticleDto updateArticle(Long id, ArticleDto dto, MultipartFile thumbnail) throws IOException {
        Article article = articlesRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Article not found"));

        // Mapping sebagian field (manual karena update opsional)
        if (dto.getTitle() != null) {
            article.setTitle(dto.getTitle());
            article.setSlug(generateSlug(dto.getTitle()));
        }
        if (dto.getContent() != null) {
            article.setContent(dto.getContent());
        }
        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Category not found"));
            article.setCategory(category);
        }

        // Update thumbnail
        if (thumbnail != null && !thumbnail.isEmpty()) {
            validateFile(thumbnail);
            if (article.getThumbnailUrl() != null) {
                deleteFile(article.getThumbnailUrl());
            }
            String fileName = saveFile(thumbnail);
            article.setThumbnailUrl("thumbnails/" + fileName);
        }

        Article updated = articlesRepository.save(article);
        return mapToResponse(updated);
    }

    // ---------------- DELETE ----------------
    @Transactional
    @Override
    public void deleteArticle(Long id) {
        Article article = articlesRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Article not found"));

        if (article.getThumbnailUrl() != null) {
            deleteFile(article.getThumbnailUrl());
        }

        articlesRepository.delete(article);
        log.info("Article with id {} deleted successfully", id);
    }

    // ---------------- Helper Methods ----------------
    private void validateFile(MultipartFile file) {
        if (file.getSize() > MAX_SIZE) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST,
                    "File size exceeds the maximum allowed size (2MB)");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || !originalFileName.contains(".")) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST, "File name is invalid");
        }

        String extension = originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST,
                    "Invalid file type. Allowed: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST,
                    "Invalid file type. Allowed MIME types: " + String.join(", ", ALLOWED_MIME_TYPES));
        }
    }

    private String saveFile(MultipartFile file) {
        try {
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            File dest = Paths.get(uploadDir, fileName).toFile();
            file.transferTo(dest);

            return fileName;
        } catch (IOException e) {
            log.error("Error saving file: {}", e.getMessage());
            throw new GlobalAPIException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store file: " + e.getMessage());
        }
    }

    private void deleteFile(String filePath) {
        File file = new File("uploads/photos/" + filePath);
        if (file.exists() && !file.delete()) {
            log.warn("Failed to delete file: {}", filePath);
        }
    }

    private String generateSlug(String title) {
        return title.toLowerCase().replaceAll("[^a-z0-9]+", "-");
    }

    private ArticleDto mapToResponse(Article article) {
        ArticleDto dto = modelMapper.map(article, ArticleDto.class);
        dto.setAuthorId(article.getAuthor().getId());
        dto.setAuthorName(article.getAuthor().getFullName());
        dto.setCategoryId(article.getCategory().getId());
        dto.setCategoryName(article.getCategory().getName());

        if (article.getThumbnailUrl() != null) {
            dto.setThumbnailUrl("/photos/" + article.getThumbnailUrl()); // URL publik
        }
        return dto;
    }
}
