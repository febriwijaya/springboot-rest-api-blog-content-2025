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

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ArticleServiceImpl implements ArticleService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ArticlesRepository articlesRepository;
    private final ModelMapper modelMapper;

    @Value("${app.upload.article-photo-dir}")
    private String thumbnailDir; // e.g. "uploads/photos/thumbnails"

    public ArticleServiceImpl(UserRepository userRepository,
                              CategoryRepository categoryRepository,
                              ArticlesRepository articlesRepository,
                              ModelMapper modelMapper) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.articlesRepository = articlesRepository;
        this.modelMapper = modelMapper;
    }

    // === VALIDATION CONST (disamakan dgn UserServiceImpl) ===
    private static final long MAX_SIZE = 2 * 1024 * 1024; // 2MB
    private static final Map<String, String> ALLOWED_TYPES = Map.ofEntries(
            Map.entry("png",  "image/png"),
            Map.entry("jpg",  "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif",  "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("svg",  "image/svg+xml"),
            Map.entry("heic", "image/heic"),
            Map.entry("heif", "image/heif")
    );

    // ---------------- CREATE ----------------
    @Transactional
    @Override
    public ArticleDto createArticle(ArticleDto articleDto, MultipartFile thumbnail, Long authorId) {
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
            String path = saveFile(thumbnail, article.getSlug());
            article.setThumbnailUrl(path); // simpan relative URL
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
    public ArticleDto updateArticle(Long id, ArticleDto dto, MultipartFile thumbnail) {
        Article article = articlesRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Article not found"));

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

        // === samakan dengan UserServiceImpl: hapus lama -> simpan baru ===
        if (thumbnail != null && !thumbnail.isEmpty()) {
            validateFile(thumbnail);

            // hapus file lama jika ada
            if (article.getThumbnailUrl() != null && !article.getThumbnailUrl().isBlank()) {
                try {
                    String existingFileName = Paths.get(article.getThumbnailUrl()).getFileName().toString();
                    Path existingFilePath = Paths.get(thumbnailDir).toAbsolutePath().resolve(existingFileName);
                    Files.deleteIfExists(existingFilePath);
                } catch (IOException e) {
                    log.warn("Gagal menghapus thumbnail lama: {}", article.getThumbnailUrl(), e);
                }
            }

            // simpan file baru
            String path = saveFile(thumbnail, article.getSlug() != null ? article.getSlug() : "article");
            article.setThumbnailUrl(path);
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

        // hapus file jika ada (pola sama seperti UserServiceImpl.delete)
        if (article.getThumbnailUrl() != null && !article.getThumbnailUrl().isBlank()) {
            try {
                String existingFileName = Paths.get(article.getThumbnailUrl()).getFileName().toString();
                Path existingFilePath = Paths.get(thumbnailDir).toAbsolutePath().resolve(existingFileName);
                Files.deleteIfExists(existingFilePath);
            } catch (IOException e) {
                log.warn("Gagal menghapus thumbnail: {}", article.getThumbnailUrl(), e);
            }
        }

        articlesRepository.delete(article);
        log.info("Article with id {} deleted successfully", id);
    }

    // ---------------- Helper Methods (COPY STYLE UserServiceImpl) ----------------
    private void validateFile(MultipartFile file) {
        log.info("Validating thumbnail: name={}, size={}, mime={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        if (file.getSize() > MAX_SIZE) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST, "Ukuran thumbnail maksimal 2MB");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || !originalFileName.contains(".")) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST, "Format file tidak valid");
        }

        String ext = originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_TYPES.containsKey(ext)) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST,
                    "Format file tidak valid. Hanya boleh: " + String.join(", ", ALLOWED_TYPES.keySet()));
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !mimeType.equalsIgnoreCase(ALLOWED_TYPES.get(ext))) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST,
                    "MIME type tidak sesuai dengan ekstensi file (" + ext + ")");
        }
    }

    // simpan file -> return relative URL (persis seperti UserServiceImpl)
    private String saveFile(MultipartFile file, String slugSeed) {
        try {
            Path uploadPath = Paths.get(thumbnailDir).toAbsolutePath();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFileName = Objects.requireNonNull(file.getOriginalFilename());
            String extWithDot = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase();

            String safeSeed = (slugSeed == null || slugSeed.isBlank()) ? "article" : slugSeed;
            String fileName = safeSeed + "_" + System.currentTimeMillis() + extWithDot;

            Path filePath = uploadPath.resolve(fileName);
            file.transferTo(filePath.toFile());

            // URL publik sesuai WebConfig
            return "/uploads/photos/thumbnails/" + fileName;

        } catch (IOException e) {
            log.error("Error saat menyimpan thumbnail: {}", e.getMessage(), e);
            throw new GlobalAPIException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Gagal menyimpan thumbnail: " + e.getMessage());
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

        // sudah tersimpan sebagai relative URL; langsung pakai
        dto.setThumbnailUrl(article.getThumbnailUrl());
        return dto;
    }
}
