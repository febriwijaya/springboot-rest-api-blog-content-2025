package com.content.springboot_rest_api.service;

import com.content.springboot_rest_api.dto.ArticleDto;
import com.content.springboot_rest_api.dto.AuthorizeReqDto;
import com.content.springboot_rest_api.dto.TagDto;
import com.content.springboot_rest_api.dto.TagDtoTmp;

import java.util.List;

public interface TagService {


    TagDtoTmp createTags(TagDto tagDto);

    List<TagDto> getAllTags();

    TagDto getTagsById(Long id);

    TagDto getTagsBySlug(String slug);

    List<TagDto> getAllTagsByCurrentUser();

    TagDtoTmp updateTags(Long id, TagDto tagDto);

    void deleteTag(Long id);

    List<ArticleDto> getArticlesByTagSlug(String slug);

    TagDto approveOrRejected(Long tmpId, AuthorizeReqDto req);

    List<TagDto> getApprovedTags();

    List<TagDtoTmp> getAllTagsTmp();

}
