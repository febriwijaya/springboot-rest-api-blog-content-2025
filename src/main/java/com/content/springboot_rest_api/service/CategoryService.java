package com.content.springboot_rest_api.service;

import com.content.springboot_rest_api.dto.ArticleDto;
import com.content.springboot_rest_api.dto.CategoryDto;

import java.util.List;

public interface CategoryService {

    CategoryDto addCategory(CategoryDto categoryDto);

    List<CategoryDto> getAllCategories();

    CategoryDto getCategory(Long id);

    CategoryDto updateCategory(Long id, CategoryDto categoryDto);

    void deleteCategory(Long id);

    List<ArticleDto> getArticlesByCategorySlug(String slug);

    //  approval dan reject sekarang digabung jadi satu method
    CategoryDto approveOrRejectCategory(Long id, CategoryDto categoryDto);

    List<CategoryDto> getCategoriesByLoggedInUser();

    CategoryDto getCategoryBySlug(String slug);

}
