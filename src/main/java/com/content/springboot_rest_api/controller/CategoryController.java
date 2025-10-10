package com.content.springboot_rest_api.controller;

import com.content.springboot_rest_api.dto.AuthorizeReqDto;
import com.content.springboot_rest_api.dto.CategoryDto;
import com.content.springboot_rest_api.dto.CategoryDtoTmp;
import com.content.springboot_rest_api.exception.ErrorDetails;
import com.content.springboot_rest_api.exception.GlobalAPIException;
import com.content.springboot_rest_api.service.CategoryService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("api/categories")
@AllArgsConstructor
@Slf4j //-- tambah logger untuk tangkap error
public class CategoryController {

    private CategoryService categoryService;

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PostMapping
    public ResponseEntity<?> addCategory(@Valid @RequestBody CategoryDtoTmp categoryDtoTmp) {
        try {
            CategoryDtoTmp savedCategory = categoryService.addCategory(categoryDtoTmp);
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
    public ResponseEntity<?> getCategoryById(@PathVariable("id") Long id) {
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

    @GetMapping("/approved")
    public ResponseEntity<?> getAllApprovedCategories() {
        try {
            List<CategoryDto> categories = categoryService.getAllApprovedCategories();
            return ResponseEntity.ok(categories);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while fetching list categories", apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while fetching list categories", e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/list-auth")
    public ResponseEntity<?> getListAuth() {
        try {
            List<CategoryDtoTmp> listAuth = categoryService.getAllCategoriesTmp();
            return ResponseEntity.ok(listAuth);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while fetching list auth category", apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while fetching list auth category", e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PutMapping("{id}")
    public  ResponseEntity<?> updateCategory(
            @PathVariable("id") Long id,
            @Valid @RequestBody CategoryDtoTmp categoryDtoTmp
    ) {
        try {
            CategoryDtoTmp updated = categoryService.updateCategory(id, categoryDtoTmp);
            return ResponseEntity.ok(updated);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while updating category with id {}", id, apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while updating category with id {}", id, e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @DeleteMapping("{id}")
    public  ResponseEntity<?> deleteCategory(@PathVariable("id") Long id) {
        try {
             categoryService.deleteCategory(id);
             return ResponseEntity.ok("The category is currently in the queue and will be authorized by the admin.!");
        }  catch (GlobalAPIException apiEx) {
            log.error("Error while deleting category with id {}", id, apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while deleting category with id {}", id, e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("{slug}/articles")
    public ResponseEntity<?> getArticlesByCategorySlug(@PathVariable("slug") String slug) {
        try {
            return ResponseEntity.ok(categoryService.getArticlesByCategorySlug(slug));
        } catch (GlobalAPIException apiEx) {
            log.error("Error while fetching articles for category slug {}", slug, apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while fetching articles for category slug {}", slug, e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @PutMapping("{id}/approval")
    public ResponseEntity<?> approveOrRejectCategory(
            @PathVariable("id") Long id,
            @Valid @RequestBody AuthorizeReqDto categoryDto
    ) {
        try {
            CategoryDtoTmp result = categoryService.approveOrRejectCategory(id, categoryDto);
            if (result == null) {
                return ResponseEntity.ok("Category deleted successfully (approved + delete)");
            }
            return ResponseEntity.ok(result);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while approving/rejecting category with id {}", id, apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while approving/rejecting category with id {}", id, e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    //  Get all categories milik user yang sedang login
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/me")
    public ResponseEntity<?> getMyCategories() {
        try {
            List<CategoryDto> categories = categoryService.getCategoriesByLoggedInUser();
            return ResponseEntity.ok(categories);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while fetching categories for current user", apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while fetching categories for current user", e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Get category by slug
    @GetMapping("/slug/{slug}")
    public ResponseEntity<?> getCategoryBySlug(@PathVariable("slug") String slug) {
        try {
            CategoryDto category = categoryService.getCategoryBySlug(slug);
            return ResponseEntity.ok(category);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while fetching category with slug {}", slug, apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while fetching category with slug {}", slug, e);
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
