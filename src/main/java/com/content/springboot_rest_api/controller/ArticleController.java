package com.content.springboot_rest_api.controller;

import com.content.springboot_rest_api.dto.ArticleDto;
import com.content.springboot_rest_api.dto.AuthorizeReqDto;
import com.content.springboot_rest_api.exception.ErrorDetails;
import com.content.springboot_rest_api.exception.GlobalAPIException;
import com.content.springboot_rest_api.service.ArticleService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/articles")
@AllArgsConstructor
@Slf4j //-- tambah logger untuk tangkap error
public class ArticleController {

    private final ArticleService articleService;


    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PostMapping(
            value = "/add",
            consumes = {"multipart/form-data"}
    )
    public ResponseEntity<?> addArticle(
            @RequestPart("data") @Valid ArticleDto dto,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
            ) {
        try {
           ArticleDto response = articleService.createArticle(dto, thumbnail);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while creating article", apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while creating article", e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PutMapping(
            value = "/update/{id}",
            consumes = { "multipart/form-data" }
    )
    public ResponseEntity<?> updateArticle(
            @PathVariable("id") Long id,
            @RequestPart("data") @Valid ArticleDto dto,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
    ) {
        try {
            ArticleDto response = articleService.updateArticle(id, dto, thumbnail);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while updating article with id {}", id, apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while updating article with id {}", id, e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @DeleteMapping("/{id}")
    public  ResponseEntity<?> deleteArticle(@PathVariable("id") Long id) {
        try {
            articleService.deleteArticle(id);
            return ResponseEntity.ok("The article will be deleted and will be queued. Please wait for it to be authorized by the admin.!");
        }  catch (GlobalAPIException apiEx) {
            log.error("Error while deleting article with id {}", id, apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while deleting article with id {}", id, e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllArticles() {
        try {
            List<ArticleDto> articleDto = articleService.getAllArticle();
            return ResponseEntity.ok(articleDto);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while fetching all articles", apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while fetching all articles", e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getArticleById(@PathVariable("id") Long id) {
        try {
            ArticleDto articleDto = articleService.getArticleById(id);
            return new ResponseEntity<>(articleDto, HttpStatus.OK);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while fetching article with id {}", id, apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while fetching article with id {}", id, e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<?> getArticleBySlug(@PathVariable("slug") String slug) {
        try {
            ArticleDto articleDto = articleService.getArticleBySlug(slug);
            return new ResponseEntity<>(articleDto, HttpStatus.OK);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while fetching article with slug {}", slug, apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while fetching article with id {}", slug, e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    // GET all articles by current user
    @GetMapping("/my-all-articles")
    public ResponseEntity<?> getArticlesByCurrentUser() {
        try {
            List<ArticleDto> articles = articleService.getArticlesByCurrentUser();
            return ResponseEntity.ok(articles);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while fetching articles by current user", apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while fetching articles by current user", e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET all approved articles
    @GetMapping("/approved-articles")
    public ResponseEntity<?> getApprovedArticles() {
        try {
            List<ArticleDto> articles = articleService.getApprovedArticles();
            return ResponseEntity.ok(articles);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while fetching approved articles", apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while fetching approved articles", e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    // APPROVE
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveArticle(
            @PathVariable("id") Long id,
            @RequestBody @Valid AuthorizeReqDto dto
    ) {
        try {
            ArticleDto response = articleService.approveArticle(id, dto);
            if (response == null) {
                return ResponseEntity.ok("Article deleted successfully (approved + delete)");
            }
            return ResponseEntity.ok(response);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while approving article with id {}", id, apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while approving article with id {}", id, e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    // REJECT Article
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectArticle(
            @PathVariable("id") Long id,
            @RequestBody @Valid AuthorizeReqDto dto
    ) {
        try {
            ArticleDto response = articleService.rejectArticle(id, dto);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while rejecting article with id {}", id, apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while rejecting article with id {}", id, e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    //  Helper method biar engga copy-paste error response
    private ResponseEntity<ErrorDetails> buildErrorResponse(String message, String details, HttpStatus status) {
        ErrorDetails errorDetails = new ErrorDetails(
                LocalDateTime.now(),
                message,
                details
        );
        return ResponseEntity.status(status).body(errorDetails);
    }
}
