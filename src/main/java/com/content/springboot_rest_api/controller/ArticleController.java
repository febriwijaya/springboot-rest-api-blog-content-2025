package com.content.springboot_rest_api.controller;

import com.content.springboot_rest_api.dto.ArticleDto;
import com.content.springboot_rest_api.exception.ErrorDetails;
import com.content.springboot_rest_api.exception.GlobalAPIException;
import com.content.springboot_rest_api.service.ArticleService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/articles")
@AllArgsConstructor
@Slf4j //-- tambah logger untuk tangkap error
public class ArticleController {

    private ArticleService articleService;

    @PostMapping(
            value = "/add",
            consumes = {"multipart/form-data"}
    )
    public ResponseEntity<?> addArticle(
            @RequestPart("data") @Valid ArticleDto dto,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
            ) {
        try {
           ArticleDto response = articleService.createArticle(dto, thumbnail, dto.getAuthorId());
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while creating article", apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while creating article", e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

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

    @DeleteMapping("{id}")
    public  ResponseEntity<?> deleteArticle(@PathVariable("id") Long id) {
        try {
            articleService.deleteArticle(id);
            return ResponseEntity.ok("Article deleted Successfully!");
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

    @GetMapping("{id}")
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
