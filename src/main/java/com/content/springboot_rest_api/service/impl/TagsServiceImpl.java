package com.content.springboot_rest_api.service.impl;

import com.content.springboot_rest_api.dto.ArticleDto;
import com.content.springboot_rest_api.dto.TagDto;
import com.content.springboot_rest_api.entity.Article;
import com.content.springboot_rest_api.entity.Role;
import com.content.springboot_rest_api.entity.Tag;
import com.content.springboot_rest_api.entity.User;
import com.content.springboot_rest_api.exception.GlobalAPIException;
import com.content.springboot_rest_api.repository.ArticlesRepository;
import com.content.springboot_rest_api.repository.TagRepository;
import com.content.springboot_rest_api.repository.UserRepository;
import com.content.springboot_rest_api.service.TagService;
import jakarta.transaction.Transactional;
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
    private UserRepository userRepository;
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
        tag.setAuthCode("P");
        tag.setActionCode("A");

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

        if (tag.getAuthCode().equalsIgnoreCase("P")) {
            throw new GlobalAPIException(HttpStatus.FORBIDDEN, "tag status is still pending");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.UNAUTHORIZED, "The currently logged in user was not found."));

        Set<String> roles = currentUser.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        //  Validasi hak akses
        if (roles.contains("ROLE_USER") && !currentUser.getUsername().equals(tag.getCreatedBy())) {
            throw new GlobalAPIException(HttpStatus.FORBIDDEN, "You may not update other users' data");
        }

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
        tag.setUpdatedBy(username);

        tag.setActionCode("E");
        tag.setAuthCode("P");

        Tag updated = tagRepository.save(tag);
        return modelMapper.map(updated, TagDto.class);

    }

    @Override
    public void deleteTag(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND,
                        "Tag not found with id : " + id));

        if (tag.getAuthCode().equalsIgnoreCase("P")) {
            throw new GlobalAPIException(HttpStatus.FORBIDDEN, "tag status is still pending");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.UNAUTHORIZED, "The currently logged in user was not found."));

        Set<String> roles = currentUser.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        //  Validasi hak akses
        if (roles.contains("ROLE_USER") && !currentUser.getUsername().equals(tag.getCreatedBy())) {
            throw new GlobalAPIException(HttpStatus.FORBIDDEN, "You cannot delete other users' data");
        }

        tag.setActionCode("D");
        tag.setAuthCode("P");

        tagRepository.save(tag);
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

    @Override
    @Transactional
    public TagDto approveOrRejected(Long id, TagDto tagDto) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND,
                        "Tag not found with id : " + id));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        String authCode = tagDto.getAuthCode();   // dari FE
        String actionCode = tagDto.getActionCode(); // dari FE

        if ("A".equalsIgnoreCase(authCode)) {
            // Jika Approve
            if ("D".equalsIgnoreCase(actionCode)) {
                // Jika action = Delete → benar2 delete
                tagRepository.delete(tag);
                return null; // karena datanya sudah dihapus
            } else if ("E".equalsIgnoreCase(actionCode) || "A".equalsIgnoreCase(actionCode)) {
                // Approve update atau create
                tag.setAuthCode("A");
                tag.setUpdatedBy(username);
                tag.setUpdatedAt(java.time.LocalDateTime.now());
                Tag updated = tagRepository.save(tag);
                return modelMapper.map(updated, TagDto.class);
            }
        } else if ("R".equalsIgnoreCase(authCode)) {
            // Jika Rejected
            tag.setAuthCode("R");
            tag.setUpdatedBy(username);
            tag.setUpdatedAt(java.time.LocalDateTime.now());
            Tag rejected = tagRepository.save(tag);
            return modelMapper.map(rejected, TagDto.class);
        }

        throw new GlobalAPIException(HttpStatus.BAD_REQUEST, "Invalid authCode or actionCode combination");
    }

    @Override
    public List<TagDto> getAllTagsByCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        List<Tag> tags = tagRepository.findByCreatedBy(username);

        if (tags.isEmpty()) {
            throw new GlobalAPIException(HttpStatus.NOT_FOUND,
                    "No tags found for user: " + username);
        }

        return tags.stream()
                .map(tag -> modelMapper.map(tag, TagDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public TagDto getTagsBySlug(String slug) {
        Tag tag = tagRepository.findBySlug(slug)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND,
                        "Tag not found with slug : " + slug));

        return modelMapper.map(tag, TagDto.class);
    }

    @Override
    public List<TagDto> getApprovedTags() {
        List<Tag> tags = tagRepository.findByAuthCode("A");

        if (tags.isEmpty()) {
            throw new GlobalAPIException(HttpStatus.NOT_FOUND,
                    "No approved tags found");
        }

        return tags.stream()
                .map(tag -> modelMapper.map(tag, TagDto.class))
                .collect(Collectors.toList());
    }


}
