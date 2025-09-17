package com.content.springboot_rest_api.service;

import com.content.springboot_rest_api.dto.ArticleDto;
import com.content.springboot_rest_api.dto.TagDto;

import java.util.List;

public interface TagService {

    TagDto createTags(TagDto tagDto);

    List<TagDto> getAllTags();

    TagDto getTagsById(Long id);

    TagDto updateTags(Long id, TagDto tagDto);

    void deleteTag(Long id);

    List<ArticleDto> getArticlesByTagSlug(String slug);

    TagDto approveOrRejected(Long id, TagDto tagDto);

    List<TagDto> getAllTagsByCurrentUser();

    TagDto getTagsBySlug(String slug);
}
