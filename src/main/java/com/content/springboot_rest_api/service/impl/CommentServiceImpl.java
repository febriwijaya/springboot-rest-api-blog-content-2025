package com.content.springboot_rest_api.service.impl;

import com.content.springboot_rest_api.dto.CommentDto;
import com.content.springboot_rest_api.entity.Article;
import com.content.springboot_rest_api.entity.Comment;
import com.content.springboot_rest_api.entity.User;
import com.content.springboot_rest_api.exception.GlobalAPIException;
import com.content.springboot_rest_api.repository.ArticlesRepository;
import com.content.springboot_rest_api.repository.CommentRepository;
import com.content.springboot_rest_api.repository.UserRepository;
import com.content.springboot_rest_api.service.CommentService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final ArticlesRepository articlesRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Transactional
    @Override
    public CommentDto addComment(CommentDto commentDto) {
        // pastikan artikel ada
        Article article = articlesRepository.findById(commentDto.getArticleId())
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Article Not Found"));

        // validasi content
        if (commentDto.getContent() == null || commentDto.getContent().isBlank()) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST, "Comment content cannot be empty");
        }

        // ambil user dari DB berdasarkan userId yang dikirim dari FE / diambil dari token
        User user = userRepository.findById(commentDto.getUserId())
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "User Not Found"));

        // buat comment baru
        Comment comment = new Comment();
        comment.setArticle(article);
        comment.setUser(user);
        comment.setContent(commentDto.getContent());

        // audit field
        comment.setCreatedAt(LocalDateTime.now());
        comment.setCreatedBy(user.getUsername());

        Comment saved = commentRepository.save(comment);

        // mapping ke DTO hasil
        CommentDto response = modelMapper.map(saved, CommentDto.class);
        response.setArticleId(article.getId());
        response.setUserId(user.getId());

        return response;
    }

    @Override
    public CommentDto getComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Comment not found"));

        CommentDto dto = modelMapper.map(comment, CommentDto.class);
        dto.setArticleId(comment.getArticle() != null ? comment.getArticle().getId() : null);
        dto.setUserId(comment.getUser() != null ? comment.getUser().getId() : null);
        return dto;
    }

    @Override
    public List<CommentDto> getCommentsByArticle(Long articleId) {
        // pastikan article ada (opsional, bisa juga langsung return empty list
        articlesRepository.findById(articleId)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Article not found"));


        return commentRepository.findByArticleId(articleId).stream()
                .map(comment -> {
                    CommentDto dto = modelMapper.map(comment, CommentDto.class);
                    dto.setArticleId(comment.getArticle() != null ? comment.getArticle().getId() : null);
                    dto.setUserId(comment.getUser() != null ? comment.getUser().getId() : null);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public CommentDto updateComment(Long id, CommentDto commentDto) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Comment not found"));

        // ambil user yang sedang login
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        // validasi kepemilikan comment
        if (comment.getUser() == null || !comment.getUser().getUsername().equals(currentUsername)) {
            throw new GlobalAPIException(HttpStatus.FORBIDDEN, "Can't update other people's comments");
        }

        // hanya update content
        if (commentDto.getContent() != null && !commentDto.getContent().isBlank()) {
            comment.setContent(commentDto.getContent());
        } else {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST, "Content cannot be empty");
        }

        // audit fields
        comment.setUpdatedAt(LocalDateTime.now());
        comment.setUpdatedBy(currentUsername);

        Comment updated = commentRepository.save(comment);

        CommentDto dto = modelMapper.map(updated, CommentDto.class);
        dto.setArticleId(updated.getArticle() != null ? updated.getArticle().getId() : null);
        dto.setUserId(updated.getUser() != null ? updated.getUser().getId() : null);

        return dto;
    }

    @Transactional
    @Override
    public void deleteComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Comment not found"));

        // ambil user yang sedang login
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        // validasi kepemilikan comment
        if (comment.getUser() == null || !comment.getUser().getUsername().equals(currentUsername)) {
            throw new GlobalAPIException(HttpStatus.FORBIDDEN, "Can't delete other people's comments");
        }

        commentRepository.delete(comment);
    }

}
