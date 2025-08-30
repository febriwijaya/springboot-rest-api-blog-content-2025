package com.content.springboot_rest_api.controller;

import com.content.springboot_rest_api.dto.CategoryDto;
import com.content.springboot_rest_api.exception.ErrorDetails;
import com.content.springboot_rest_api.exception.GlobalAPIException;
import com.content.springboot_rest_api.service.CategoryService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("api/categories")
@AllArgsConstructor
@Slf4j //-- tambah logger untuk tangkap error
public class CategoryController {

    private CategoryService categoryService;

    @PostMapping
    public ResponseEntity<?> addCategory(@Valid @RequestBody CategoryDto categoryDto) {
        try {
            CategoryDto savedCategory = categoryService.addCategory(categoryDto);
            return new ResponseEntity<>(savedCategory, HttpStatus.CREATED);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while creating category", apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while creating category", e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllCategories() {
        try {
            List<CategoryDto> categories = categoryService.getAllCategories();
            return ResponseEntity.ok(categories);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while fetching all category", apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while fetching all category", e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("{id}")
    public ResponseEntity<?> getTodo(@PathVariable("id") Long id) {
        try {
            CategoryDto categoryDto = categoryService.getCategory(id);
            return new ResponseEntity<>(categoryDto, HttpStatus.OK);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while fetching category with id {}", id, apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while fetching category with id {}", id, e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("{id}")
    public  ResponseEntity<?> updateCategory(
            @PathVariable("id") Long id,
            @Valid @RequestBody CategoryDto categoryDto
    ) {
        try {
            CategoryDto updated = categoryService.updateCategory(id, categoryDto);
            return ResponseEntity.ok(updated);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while updating category with id {}", id, apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while updating category with id {}", id, e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("{id}")
    public  ResponseEntity<?> deleteCategory(@PathVariable("id") Long id) {
        try {
             categoryService.deleteCategory(id);
             return ResponseEntity.ok("Category deleted Successfully!");
        }  catch (GlobalAPIException apiEx) {
            log.error("Error while deleting category with id {}", id, apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while deleting category with id {}", id, e);
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
