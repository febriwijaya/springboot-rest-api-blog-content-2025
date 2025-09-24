package com.content.springboot_rest_api.service.impl;

import com.content.springboot_rest_api.dto.ArticleDto;
import com.content.springboot_rest_api.entity.*;
import com.content.springboot_rest_api.exception.GlobalAPIException;
import com.content.springboot_rest_api.repository.ArticlesRepository;
import com.content.springboot_rest_api.repository.CategoryRepository;
import com.content.springboot_rest_api.repository.TagRepository;
import com.content.springboot_rest_api.repository.UserRepository;
import com.content.springboot_rest_api.service.ArticleService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final TagRepository tagRepository;
    private final ModelMapper modelMapper;

    @Value("${app.upload.article-photo-dir}")
    private String thumbnailDir; // e.g. "uploads/photos/thumbnails"

    public ArticleServiceImpl(UserRepository userRepository,
                              CategoryRepository categoryRepository,
                              ArticlesRepository articlesRepository,
                              TagRepository tagRepository,
                              ModelMapper modelMapper) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.articlesRepository = articlesRepository;
        this.tagRepository = tagRepository;
        this.modelMapper = modelMapper;
    }

    // === VALIDATION CONST ===
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
        article.setAuthCode("P"); // default pending
        article.setActionCode("A"); // Add

        // handle tags
        if (articleDto.getTagIds() != null && !articleDto.getTagIds().isEmpty()) {
            Set<Tag> tags = new HashSet<>(tagRepository.findAllById(articleDto.getTagIds()));
            article.setTags(tags);
        }

        if (thumbnail != null && !thumbnail.isEmpty()) {
            validateFile(thumbnail);
            String path = saveFile(thumbnail, article.getSlug());
            article.setThumbnailUrlPending(path);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        article.setCreatedBy(authentication.getName());

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
    @Transactional
    public ArticleDto getArticleById(Long id) {
        Article article = articlesRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Article not found"));

        if (article.getViews() == null) {
            article.setViews(0L);
        }
        article.setViews(article.getViews() + 1);
        articlesRepository.save(article);

        return mapToResponse(article);
    }

    @Override
    @Transactional
    public ArticleDto getArticleBySlug(String slug) {
        Article article = articlesRepository.findBySlug(slug)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Article not found"));

        if (article.getViews() == null) {
            article.setViews(0L);
        }
        article.setViews(article.getViews() + 1);
        articlesRepository.save(article);

        return mapToResponse(article);
    }

    @Override
    public List<ArticleDto> getArticlesByCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "User not found"));

        List<Article> articles = articlesRepository.findByAuthor(user);
        if (articles.isEmpty()) {
            throw new GlobalAPIException(HttpStatus.NOT_FOUND, "There are no articles belonging to this user");
        }

        return articles.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ArticleDto> getApprovedArticles() {
        List<Article> articles = articlesRepository.findByAuthCode("A");
        if (articles.isEmpty()) {
            throw new GlobalAPIException(HttpStatus.NOT_FOUND, "There are no articles with approved status");
        }

        return articles.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ---------------- UPDATE ----------------
    @Transactional
    @Override
    public ArticleDto updateArticle(Long id, ArticleDto dto, MultipartFile thumbnail) {
        Article article = articlesRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Article not found"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.UNAUTHORIZED, "The currently logged in user was not found."));

        Set<String> roles = currentUser.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        //  Validasi hak akses
        if (roles.contains("ROLE_USER") && !currentUser.getUsername().equals(article.getCreatedBy())) {
            throw new GlobalAPIException(HttpStatus.FORBIDDEN, "You may not update other users' data");
        }

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
        if (dto.getTagIds() != null) {
            Set<Tag> tags = new HashSet<>(tagRepository.findAllById(dto.getTagIds()));
            article.setTags(tags);
        }

        if (thumbnail != null && !thumbnail.isEmpty()) {
            validateFile(thumbnail);

            if (article.getThumbnailUrlPending() != null) {
                deleteThumbnail(article.getThumbnailUrlPending());
            }

            String path = saveFile(thumbnail, article.getSlug());
            article.setThumbnailUrlPending(path);
        }


        article.setUpdatedBy(username);
        article.setAuthCode("P"); // kembali pending
        article.setActionCode("E"); // Edit

        Article updated = articlesRepository.save(article);
        return mapToResponse(updated);
    }

    // ---------------- DELETE ----------------
    @Transactional
    @Override
    public void deleteArticle(Long id) {
        Article article = articlesRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Article not found"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.UNAUTHORIZED, "The currently logged in user was not found."));

        Set<String> roles = currentUser.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        //  Validasi hak akses
        if (roles.contains("ROLE_USER") && !currentUser.getUsername().equals(article.getCreatedBy())) {
            throw new GlobalAPIException(HttpStatus.FORBIDDEN, "You cannot delete other users' data");
        }

        article.setAuthCode("P"); // pending delete
        article.setActionCode("D");

        articlesRepository.save(article);
    }

    // ---------------- APPROVE ----------------
    @Transactional
    @Override
    public ArticleDto approveArticle(Long id, ArticleDto dto) {
        Article article = articlesRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Article not found"));

        // FE harus kirim auth_code = "A"
        if (!"A".equalsIgnoreCase(dto.getAuthCode())) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST,
                    "Invalid request: auth_code must be 'A' for approval");
        }

        String action = dto.getActionCode();
        if (action == null) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST, "action code cannot be empty");
        }

        switch (action) {
            case "A": // First Add
                if (article.getThumbnailUrlPending() != null) {
                    article.setThumbnailUrlApprove(article.getThumbnailUrlPending());
                    article.setThumbnailUrlPending(null);
                }
                break;

            case "E": // Edit
                if (article.getThumbnailUrlApprove() != null) {
                    deleteThumbnail(article.getThumbnailUrlApprove());
                }
                if (article.getThumbnailUrlPending() != null) {
                    article.setThumbnailUrlApprove(article.getThumbnailUrlPending());
                    article.setThumbnailUrlPending(null);
                }
                break;

            case "D": // Delete
                deleteThumbnail(article.getThumbnailUrlPending());
                deleteThumbnail(article.getThumbnailUrlApprove());
                articlesRepository.delete(article);
                return null;

            default:
                throw new GlobalAPIException(HttpStatus.BAD_REQUEST,
                        "Invalid action_code for approval: " + action);
        }

        article.setAuthCode("A"); // Approved
        article.setActionCode(action);
        article.setUpdatedBy(SecurityContextHolder.getContext().getAuthentication().getName());
        article.setUpdatedAt(java.time.LocalDateTime.now());

        Article saved = articlesRepository.save(article);
        return mapToResponse(saved);
    }

    // ---------------- REJECT ----------------
    @Transactional
    @Override
    public ArticleDto rejectArticle(Long id, ArticleDto dto) {
        Article article = articlesRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Article not found"));

        // FE harus kirim auth_code = "R"
        if (!"R".equalsIgnoreCase(dto.getAuthCode())) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST,
                    "Invalid request: auth_code must be 'R' for rejection");
        }

        // kalau ada pending thumbnail → hapus
        if (article.getThumbnailUrlPending() != null) {
            deleteThumbnail(article.getThumbnailUrlPending());
            article.setThumbnailUrlPending(null);
        }

        article.setAuthCode("R"); // Rejected
        article.setActionCode(dto.getActionCode());
        article.setUpdatedBy(SecurityContextHolder.getContext().getAuthentication().getName());
        article.setUpdatedAt(java.time.LocalDateTime.now());

        Article saved = articlesRepository.save(article);
        return mapToResponse(saved);
    }

    // ---------------- Helper Methods ----------------
    private void validateFile(MultipartFile file) {
        if (file.getSize() > MAX_SIZE) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST, "Maximum thumbnail size 2 MB");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || !originalFileName.contains(".")) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST, "Invalid file format");
        }

        String ext = originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_TYPES.containsKey(ext)) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST,
                    "Invalid file format. Just can : " + String.join(", ", ALLOWED_TYPES.keySet()));
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !mimeType.equalsIgnoreCase(ALLOWED_TYPES.get(ext))) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST,
                    "MIME type does not match file extension (" + ext + ")");
        }
    }

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

            return "/uploads/photos/thumbnails/" + fileName;
        } catch (IOException e) {
            throw new GlobalAPIException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to save thumbnail: " + e.getMessage());
        }
    }

    private void deleteThumbnail(String filePath) {
        if (filePath == null) return;
        try {
            String fileName = Paths.get(filePath).getFileName().toString();
            Path existingFilePath = Paths.get(thumbnailDir).toAbsolutePath().resolve(fileName);
            Files.deleteIfExists(existingFilePath);
        } catch (IOException e) {
            log.warn("Failed to delete thumbnail file: {}", filePath, e);
        }
    }

    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    private ArticleDto mapToResponse(Article article) {
        ArticleDto dto = modelMapper.map(article, ArticleDto.class);
        dto.setAuthorId(article.getAuthor().getId());
        dto.setAuthorName(article.getAuthor().getFullName());
        dto.setCategoryId(article.getCategory().getId());
        dto.setCategoryName(article.getCategory().getName());

        dto.setThumbnailUrlPending(article.getThumbnailUrlPending());
        dto.setThumbnailUrlApprove(article.getThumbnailUrlApprove());

        if (article.getTags() != null && !article.getTags().isEmpty()) {
            dto.setTagIds(article.getTags().stream().map(Tag::getId).toList());
            dto.setTagNames(article.getTags().stream().map(Tag::getName).toList());
        } else {
            dto.setTagIds(Collections.emptyList());
            dto.setTagNames(Collections.emptyList());
        }
        return dto;
    }

    /**
     * Cron job jalan tiap malam jam 00:00
     * Hapus artikel dengan auth_code = 'R' lebih dari 3 hari,
     * termasuk file thumbnail pending/approve.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void deleteExpiredRejectedArticles() {
        var threeDaysAgo = java.time.LocalDateTime.now().minusDays(3);
        var expiredArticles = articlesRepository.findByAuthCodeAndCreatedAtBefore("R", threeDaysAgo);

        for (Article article : expiredArticles) {
            deleteThumbnail(article.getThumbnailUrlPending());
            deleteThumbnail(article.getThumbnailUrlApprove());
            articlesRepository.delete(article);
        }
    }
}
