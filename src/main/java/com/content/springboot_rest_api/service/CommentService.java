package com.content.springboot_rest_api.service;

import com.content.springboot_rest_api.dto.CommentDto;

import java.util.List;

public interface CommentService {

    CommentDto addComment(CommentDto commentDto);

    CommentDto getComment(Long id);

    List<CommentDto> getCommentsByArticle(Long articleId);

    CommentDto updateComment(Long id, CommentDto commentDto);

    void deleteComment(Long id);

}
