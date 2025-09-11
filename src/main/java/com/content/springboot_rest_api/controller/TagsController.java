package com.content.springboot_rest_api.controller;


import com.content.springboot_rest_api.dto.ArticleDto;
import com.content.springboot_rest_api.dto.TagDto;
import com.content.springboot_rest_api.exception.ErrorDetails;
import com.content.springboot_rest_api.exception.GlobalAPIException;
import com.content.springboot_rest_api.service.TagService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/tags")
@AllArgsConstructor
@Slf4j //-- tambah logger untuk tangkap error

public class TagsController {

    private TagService tagService;

    @PostMapping
    public ResponseEntity<?> addTags(@Valid @RequestBody TagDto tagDto) {
        try {
            TagDto savedTags = tagService.createTags(tagDto);
            return new ResponseEntity<>(savedTags, HttpStatus.CREATED);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while creating Tags", apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while creating tags", e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllTags() {
        try {
            List<TagDto> tags = tagService.getAllTags();
            return ResponseEntity.ok(tags);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while fetching all tags", apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while fetching all tags", e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{slug}/articles")
    public ResponseEntity<?> getAllArticleByTag(
            @PathVariable String slug
    ) {
        try {
            List<ArticleDto> tags = tagService.getArticlesByTagSlug(slug);
            return ResponseEntity.ok(tags);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while fetching all article by tags", apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while fetching all article by tags", e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTagById(@PathVariable("id") Long id) {
        try {
            TagDto tagDto = tagService.getTagsById(id);
            return new ResponseEntity<>(tagDto, HttpStatus.OK);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while fetching tag with id {}", id, apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while fetching tag with id {}", id, e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTags (
            @PathVariable("id") Long id,
            @Valid @RequestBody TagDto tagDto
    ) {
        try {
            TagDto updated = tagService.updateTags(id, tagDto);
            return ResponseEntity.ok(updated);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while updating tag with id {}", id, apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while updating tag with id {}", id, e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTagsById(@PathVariable("id") Long id) {

        try {
            tagService.deleteTag(id);
            return ResponseEntity.ok("Tag deleted successfully");
        } catch (GlobalAPIException apiEx) {
            log.error("Error while deleting tag with id {}", id, apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while deleting tag with id {}", id, e);
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
