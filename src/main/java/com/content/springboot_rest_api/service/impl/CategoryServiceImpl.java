package com.content.springboot_rest_api.service.impl;

import com.content.springboot_rest_api.dto.CategoryDto;
import com.content.springboot_rest_api.entity.Category;
import com.content.springboot_rest_api.exception.GlobalAPIException;
import com.content.springboot_rest_api.repository.CategoryRepository;
import com.content.springboot_rest_api.service.CategoryService;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private CategoryRepository categoryRepository;

    private ModelMapper modelMapper;

    @Override
    public CategoryDto addCategory(CategoryDto categoryDto) {
        // Mapping DTO ke Entity
        Category category = modelMapper.map(categoryDto, Category.class);

        // Generate slug dari name
        String slug = category.getName()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")  // Ganti spasi/karakter spesial dengan "-"
                .replaceAll("(^-|-$)", "");     // Hapus tanda "-" di awal/akhir

        // Cek apakah slug sudah ada
        if (categoryRepository.existsBySlug(slug)) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST,
                    "Slug already exists: " + slug);
        }
        category.setSlug(slug);

        // Save ke DB
        Category savedCategory = categoryRepository.save(category);

        // Balikin response DTO
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

        // Generate slug dari name
        String slug = categoryDto.getName()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")  // Ganti spasi/karakter spesial dengan "-"
                .replaceAll("(^-|-$)", "");     // Hapus tanda "-" di awal/akhir

        // Cek apakah slug sudah ada
        if (categoryRepository.existsBySlug(slug)) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST,
                    "Slug already exists: " + slug);
        }
        category.setSlug(slug);

        Category updated = categoryRepository.save(category);
        return modelMapper.map(updated, CategoryDto.class);
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->  new GlobalAPIException(HttpStatus.NOT_FOUND,
                        "Category not found with id : " + id));
        categoryRepository.delete(category);
    }
}
