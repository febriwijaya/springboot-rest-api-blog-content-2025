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

        Comment comment = new Comment();
        comment.setArticle(article);

        // cek apakah ada user yang login
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean hasAuthenticatedUser = auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);

        if (hasAuthenticatedUser) {
            String username = auth.getName();
            // ambil user dari DB, kalau ada set ke comment
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "User Not Found"));

            comment.setUser(user);
            // optional: biarkan name/email null untuk logged in user
            comment.setName(null);
            comment.setEmail(null);
        } else {
            // guest : wajib ada name & email di DTO
            if (commentDto.getName() == null || commentDto.getName().isBlank()) {
                throw new GlobalAPIException(HttpStatus.BAD_REQUEST, "Guest name is required");
            }

            if(commentDto.getEmail() == null || commentDto.getEmail().isBlank()) {
                throw new GlobalAPIException(HttpStatus.BAD_REQUEST, "Guest email is required");
            }

            comment.setUser(null);
            comment.setName(commentDto.getName());
            comment.setEmail(commentDto.getEmail());
        }

        // isi content wajib ada

        if(commentDto.getContent() == null || commentDto.getContent().isBlank()) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST, "Comment content can not empty");
        }

        comment.setContent(commentDto.getContent());

        Comment saved = commentRepository.save(comment);

        // mapping ke DTO hasil
        CommentDto response = modelMapper.map(saved, CommentDto.class);
        response.setArticleId(saved.getArticle() != null ? saved.getArticle().getId() : null);
        response.setUserId(saved.getUser() != null ? saved.getUser().getId() : null);

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

        // hanya update content (dan mungkin name/email jika guest)
        if (commentDto.getContent() != null && !commentDto.getContent().isBlank()) {
            comment.setContent(commentDto.getContent());
        }

        if (comment.getUser() == null) { // guest comment -> allow update name/email
            if (commentDto.getName() != null) comment.setName(commentDto.getName());
            if (commentDto.getEmail() != null) comment.setEmail(commentDto.getEmail());
        }

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
        commentRepository.delete(comment);
    }

}
