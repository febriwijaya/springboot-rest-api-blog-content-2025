package com.content.springboot_rest_api.service;

import com.content.springboot_rest_api.dto.ArticleDto;
import com.content.springboot_rest_api.dto.AuthorizeReqDto;
import com.content.springboot_rest_api.dto.CategoryDto;
import com.content.springboot_rest_api.dto.CategoryDtoTmp;

import java.util.List;

public interface CategoryService {

    // Tambah kategori baru ke tabel TMP
    CategoryDtoTmp addCategory(CategoryDtoTmp categoryDtoTmp);


    List<CategoryDto> getAllCategories();

    CategoryDto getCategory(Long id);

    CategoryDtoTmp updateCategory(Long id, CategoryDtoTmp categoryDtoTmp);

    void deleteCategory(Long id);

    List<ArticleDto> getArticlesByCategorySlug(String slug);

    //  approval dan reject sekarang digabung jadi satu method
    CategoryDtoTmp approveOrRejectCategory(Long id, AuthorizeReqDto categoryDto);

    List<CategoryDto> getCategoriesByLoggedInUser();

    CategoryDto getCategoryBySlug(String slug);

    List<CategoryDto> getAllApprovedCategories();

   List<CategoryDtoTmp> getAllCategoriesTmp();


}
