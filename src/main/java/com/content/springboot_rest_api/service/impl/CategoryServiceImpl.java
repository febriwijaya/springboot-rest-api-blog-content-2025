package com.content.springboot_rest_api.service.impl;

import com.content.springboot_rest_api.dto.ArticleDto;
import com.content.springboot_rest_api.dto.CategoryDto;
import com.content.springboot_rest_api.entity.Article;
import com.content.springboot_rest_api.entity.Category;
import com.content.springboot_rest_api.exception.GlobalAPIException;
import com.content.springboot_rest_api.repository.ArticlesRepository;
import com.content.springboot_rest_api.repository.CategoryRepository;
import com.content.springboot_rest_api.service.CategoryService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private CategoryRepository categoryRepository;
    private ArticlesRepository articlesRepository;
    private ModelMapper modelMapper;

    @Override
    public CategoryDto addCategory(CategoryDto categoryDto) {
        Category category = modelMapper.map(categoryDto, Category.class);

        String slug = category.getName()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        if (categoryRepository.existsBySlug(slug)) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST,
                    "Slug already exists: " + slug);
        }
        category.setSlug(slug);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        category.setCreatedBy(username);
        category.setActionCode("A");
        category.setAuthCode("P");
        category.setCreatedAt(LocalDateTime.now());

        Category savedCategory = categoryRepository.save(category);
        return modelMapper.map(savedCategory, CategoryDto.class);
    }

    @Override
    public List<CategoryDto> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        return categories.stream()
                .map(category -> modelMapper.map(category, CategoryDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto getCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->  new GlobalAPIException(HttpStatus.NOT_FOUND,
                        "Category not found with id : " + id));
        return modelMapper.map(category, CategoryDto.class);
    }

    @Override
    public CategoryDto updateCategory(Long id, CategoryDto categoryDto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->  new GlobalAPIException(HttpStatus.NOT_FOUND,
                        "Category not found with id : " + id));

        category.setName(categoryDto.getName());

        String slug = categoryDto.getName()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        if (categoryRepository.existsBySlug(slug)) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST,
                    "Slug already exists: " + slug);
        }
        category.setSlug(slug);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        category.setUpdatedBy(username);
        category.setActionCode("E");
        category.setAuthCode("P");

        Category updated = categoryRepository.save(category);
        return modelMapper.map(updated, CategoryDto.class);
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->  new GlobalAPIException(HttpStatus.NOT_FOUND,
                        "Category not found with id : " + id));
        category.setActionCode("D");
        category.setAuthCode("P");
        categoryRepository.save(category);
    }

    @Override
    public List<ArticleDto> getArticlesByCategorySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new GlobalAPIException(
                        HttpStatus.NOT_FOUND,
                        "Category not found with slug : " + slug
                ));

        List<Article> articles = articlesRepository.findByCategory(category);

        if (articles.isEmpty()) {
            throw new GlobalAPIException(
                    HttpStatus.NOT_FOUND,
                    "No articles found for category: " + slug
            );
        }

        return articles.stream()
                .map(article -> modelMapper.map(article, ArticleDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto approveOrRejectCategory(Long id, CategoryDto categoryDto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND,
                        "Category not found with id : " + id));

        // ambil username dari authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        // set siapa yang update
        category.setUpdatedBy(username);

        // logic approve / reject
        if ("A".equalsIgnoreCase(categoryDto.getAuthCode())) {
            // jika action code = D (delete) maka hapus
            if ("D".equalsIgnoreCase(categoryDto.getActionCode())) {
                categoryRepository.delete(category);
                return null; // bisa juga return DTO kosong / pesan sukses
            } else {
                // approve biasa
                category.setAuthCode("A");
                category.setUpdatedAt(java.time.LocalDateTime.now());
                Category approved = categoryRepository.save(category);
                return modelMapper.map(approved, CategoryDto.class);
            }
        } else if ("R".equalsIgnoreCase(categoryDto.getAuthCode())) {
            // reject
            category.setAuthCode("R");
            category.setUpdatedAt(java.time.LocalDateTime.now());
            Category rejected = categoryRepository.save(category);
            return modelMapper.map(rejected, CategoryDto.class);
        } else {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST,
                    "Invalid auth_code. Only 'A' (approve) or 'R' (reject) allowed.");
        }
    }

    @Override
    @Transactional
    public List<CategoryDto> getCategoriesByLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        List<Category> categories = categoryRepository.findByCreatedBy(username);

        if (categories.isEmpty()) {
            throw new GlobalAPIException(HttpStatus.NOT_FOUND,
                    "No categories found for user: " + username);
        }

        return categories.stream()
                .map(category -> modelMapper.map(category, CategoryDto.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategoryDto getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND,
                        "Category not found with slug : " + slug));

        return modelMapper.map(category, CategoryDto.class);
    }



    // ===================== SCHEDULER AUTO DELETE =====================
    @Scheduled(cron = "0 0 0 * * ?") // jalan tiap jam 0 pagi
    public void deleteRejectedCategories() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(3);
        List<Category> rejectedCategories = categoryRepository
                .findByAuthCodeAndCreatedAtBefore("R", cutoff);

        if (!rejectedCategories.isEmpty()) {
            categoryRepository.deleteAll(rejectedCategories);
        }
    }
}
