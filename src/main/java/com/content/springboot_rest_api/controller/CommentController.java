package com.content.springboot_rest_api.controller;

import com.content.springboot_rest_api.dto.CommentDto;
import com.content.springboot_rest_api.exception.ErrorDetails;
import com.content.springboot_rest_api.exception.GlobalAPIException;
import com.content.springboot_rest_api.service.CommentService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("api/comment")
@AllArgsConstructor
@Slf4j //-- tambah logger untuk tangkap error
public class CommentController {

    private CommentService commentService;

    @PostMapping
    public ResponseEntity<?> addComment(@Valid @RequestBody CommentDto commentDto) {
        try {
            CommentDto saved = commentService.addComment(commentDto);
            return new ResponseEntity<>(saved, HttpStatus.CREATED);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while creating comment", apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while creating comment", e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("{id}")
    public ResponseEntity<?> getCommentById(@PathVariable("id") Long id) {
        try {
            CommentDto dto = commentService.getComment(id);
            return ResponseEntity.ok(dto);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while fetching comment {}", id, apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while fetching comment {}", id, e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("article/{articleId}")
    public ResponseEntity<?> getCommentsByArticle(@PathVariable("articleId") Long articleId) {
        try {
            List<CommentDto> comments = commentService.getCommentsByArticle(articleId);
            return ResponseEntity.ok(comments);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while fetching comments for article {}", articleId, apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while fetching comments for article {}", articleId, e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("{id}")
    public ResponseEntity<?> updateCommentArticle(
            @PathVariable("id") Long id,
            @Valid @RequestBody CommentDto commentDto
    ) {
        try {
            CommentDto updated = commentService.updateComment(id, commentDto);
            return ResponseEntity.ok(updated);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while updating comment {}", id, apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while updating comment {}", id, e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("{id}")
    public ResponseEntity<?> deleteComment(@PathVariable("id") Long id) {
        try {
            commentService.deleteComment(id);
            return ResponseEntity.ok("Comment deleted successfully!");
        } catch (GlobalAPIException apiEx) {
            log.error("Error while deleting comment {}", id, apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while deleting comment {}", id, e);
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
