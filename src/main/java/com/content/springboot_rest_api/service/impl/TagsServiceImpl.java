package com.content.springboot_rest_api.service.impl;

import com.content.springboot_rest_api.dto.ArticleDto;
import com.content.springboot_rest_api.dto.TagDto;
import com.content.springboot_rest_api.entity.Article;
import com.content.springboot_rest_api.entity.Tag;
import com.content.springboot_rest_api.exception.GlobalAPIException;
import com.content.springboot_rest_api.repository.TagRepository;
import com.content.springboot_rest_api.service.TagService;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class TagsServiceImpl implements TagService {

    private TagRepository tagRepository;
    private ModelMapper modelMapper;

    @Override
    public TagDto createTags(TagDto tagDto) {
        // Mapping DTO to entity
        Tag tag = modelMapper.map(tagDto, Tag.class);

        // Generate slug dari name
        String slug = tag.getName()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")  // Ganti spasi/karakter spesial dengan "-"
                .replaceAll("(^-|-$)", "");     // Hapus tanda "-" di awal/akhir

        // cek apakah slug sudah ada
        if (tagRepository.existsBySlug(slug)) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST,
                    "Slug already exists: " + slug);
        }
        tag.setSlug(slug);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        tag.setCreatedBy(username);

        // save ke DB
        Tag savedTag = tagRepository.save(tag);

        // balikin response DTO
        return modelMapper.map(savedTag, TagDto.class);
    }

    @Override
    public List<TagDto> getAllTags() {
        List<Tag> tags = tagRepository.findAll();

        return tags.stream()
                .map(tag -> modelMapper.map(tag, TagDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public TagDto getTagsById(Long id) {

        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND,
                        "Tags not found with id : " + id));

        return modelMapper.map(tag, TagDto.class);
    }

    @Override
    public TagDto updateTags(Long id, TagDto tagDto) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND,
                        "Tag not found with id : " + id));

        tag.setName(tagDto.getName());

        // Generate slug dari name
        String slug = tagDto.getName()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")  // Ganti spasi/karakter spesial dengan "-"
                .replaceAll("(^-|-$)", "");     // Hapus tanda "-" di awal/akhir

        // cek apakah slug sudah ada
        if (tagRepository.existsBySlug(slug)) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST,
                    "Slug already exists: " + slug);
        }

        tag.setSlug(slug);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        tag.setUpdatedBy(username);

        Tag updated = tagRepository.save(tag);
        return modelMapper.map(updated, TagDto.class);

    }

    @Override
    public void deleteTag(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND,
                        "Tag not found with id : " + id));

        tagRepository.delete(tag);
    }

    @Override
    public List<ArticleDto> getArticlesByTagSlug(String slug) {
        Tag tag = tagRepository.findBySlug(slug)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Tag not found with slug : " + slug));

        Set<Article> articles = tag.getArticles();

        if(articles == null || articles.isEmpty()) {
            throw new GlobalAPIException(HttpStatus.NOT_FOUND, "No articles found for tag : " + slug);
        }

        return articles.stream()
                .map(article -> modelMapper.map(article, ArticleDto.class))
                .collect(Collectors.toList());
    }
}
