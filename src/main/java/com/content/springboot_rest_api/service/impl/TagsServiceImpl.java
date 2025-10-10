package com.content.springboot_rest_api.service.impl;

import com.content.springboot_rest_api.dto.ArticleDto;
import com.content.springboot_rest_api.dto.AuthorizeReqDto;
import com.content.springboot_rest_api.dto.TagDto;
import com.content.springboot_rest_api.dto.TagDtoTmp;
import com.content.springboot_rest_api.entity.*;
import com.content.springboot_rest_api.exception.GlobalAPIException;
import com.content.springboot_rest_api.repository.ArticlesRepository;
import com.content.springboot_rest_api.repository.TagRepository;
import com.content.springboot_rest_api.repository.TagTmpRepository;
import com.content.springboot_rest_api.repository.UserRepository;
import com.content.springboot_rest_api.service.TagService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class TagsServiceImpl implements TagService {

    private TagRepository tagRepository;
    private UserRepository userRepository;
    private TagTmpRepository tagTmpRepository;
    private ModelMapper modelMapper;

    @Override
    public TagDtoTmp createTags(TagDto tagDto) {
        String slug = tagDto.getName()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        if (tagRepository.existsBySlug(slug)) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST, "Slug already exists: " + slug);
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        TagTmp tmp = new TagTmp();
        tmp.setName(tagDto.getName());
        tmp.setSlug(slug);
        tmp.setCreatedBy(username);
        tmp.setCreatedAt(java.time.LocalDateTime.now());
        tmp.setAuthCode("P"); // Pending
        tmp.setActionCode("A"); // Add

        TagTmp saved = tagTmpRepository.save(tmp);
        return convertToTmpDto(saved);
    }

    @Override
    public List<TagDto> getAllTags() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        // Admin bisa lihat semua tag, user hanya miliknya
        List<Tag> mainTags = isAdmin
                ? tagRepository.findAll()
                : tagRepository.findByCreatedBy(username);

        // Ambil data temporary (pending / edited / deleted)
        List<TagTmp> latestTmp = tagTmpRepository.findLatestTmpPerTag();
        List<TagTmp> newTags = tagTmpRepository.findLatestTmpNewTags();

        // Filter untuk user biasa
        if (!isAdmin) {
            latestTmp = latestTmp.stream()
                    .filter(tmp -> username.equals(tmp.getCreatedBy()) || username.equals(tmp.getUpdatedBy()))
                    .toList();
            newTags = newTags.stream()
                    .filter(tmp -> username.equals(tmp.getCreatedBy()) || username.equals(tmp.getUpdatedBy()))
                    .toList();
        }

        // Mapping id_tag → TagTmp terbaru
        Map<Long, TagTmp> tmpMap = latestTmp.stream()
                .filter(tmp -> tmp.getIdTag() != null)
                .collect(Collectors.toMap(TagTmp::getIdTag, tmp -> tmp));

        List<TagDto> result = new ArrayList<>();

        // Gabungkan data utama dan temporary
        for (Tag tag : mainTags) {
            if (tmpMap.containsKey(tag.getId())) {
                TagTmp tmp = tmpMap.get(tag.getId());

                if ("P".equals(tmp.getAuthCode())) {
                    // Pending → tampilkan dari tmp
                    result.add(convertToDtoFromTmpWithCodes(tmp));

                } else if ("R".equals(tmp.getAuthCode())) {
                    // Rejected → kalau idTag null tampilkan tmp, kalau tidak tampilkan main
                    if (tmp.getIdTag() == null) {
                        result.add(convertToDtoFromTmpWithCodes(tmp));
                    } else {
                        TagDto dto = convertToDtoFromMain(tag);
                        dto.setAuthCode(tmp.getAuthCode());
                        dto.setActionCode(tmp.getActionCode());
                        result.add(dto);
                    }

                } else {
                    // Approved → tampilkan dari main
                    TagDto dto = convertToDtoFromMain(tag);
                    dto.setAuthCode(tmp.getAuthCode());
                    dto.setActionCode(tmp.getActionCode());
                    result.add(dto);
                }
            } else {
                // Tidak ada versi tmp → tampilkan main (auth/action null)
                result.add(convertToDtoFromMain(tag));
            }
        }

        // Tambahkan tag baru dari tabel tmp (belum ada di main)
        newTags.forEach(tmp -> result.add(convertToDtoFromTmpWithCodes(tmp)));

        // Urutkan berdasarkan createdAt terbaru
        result.sort(Comparator.comparing(TagDto::getCreatedAt).reversed());

        return result;
    }


    @Override
    public TagDto getTagsById(Long id) {

        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND,
                        "Tags not found with id : " + id));

        return modelMapper.map(tag, TagDto.class);
    }

    @Override
    public TagDtoTmp updateTags(Long id, TagDto tagDto) {
        Tag existing = tagRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Tag not found with id : " + id));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.UNAUTHORIZED, "User not found."));

        boolean isUserOwner = existing.getCreatedBy().equals(username);
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));

        if (!isAdmin && !isUserOwner) {
            throw new GlobalAPIException(HttpStatus.FORBIDDEN, "You may not edit other users' tags.");
        }

        String slug = tagDto.getName()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        if (tagRepository.existsBySlug(slug)) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST, "Slug already exists: " + slug);
        }

        TagTmp tmp = new TagTmp();
        tmp.setIdTag(existing.getId());
        tmp.setName(tagDto.getName());
        tmp.setSlug(slug);
        tmp.setCreatedBy(existing.getCreatedBy());
        tmp.setCreatedAt(existing.getCreatedAt());
        tmp.setUpdatedBy(username);
        tmp.setUpdatedAt(java.time.LocalDateTime.now());
        tmp.setAuthCode("P");
        tmp.setActionCode("E");

        TagTmp saved = tagTmpRepository.save(tmp);
        return convertToTmpDto(saved);
    }

    @Override
    public void deleteTag(Long id) {
        Tag existing = tagRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Tag not found with id : " + id));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.UNAUTHORIZED, "User not found."));

        boolean isUserOwner = existing.getCreatedBy().equals(username);
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));

        if (!isAdmin && !isUserOwner) {
            throw new GlobalAPIException(HttpStatus.FORBIDDEN, "You cannot delete other users' tags.");
        }

        TagTmp tmp = new TagTmp();
        tmp.setIdTag(existing.getId());
        tmp.setName(existing.getName());
        tmp.setSlug(existing.getSlug());
        tmp.setCreatedBy(existing.getCreatedBy());
        tmp.setCreatedAt(existing.getCreatedAt());
        tmp.setUpdatedBy(username);
        tmp.setUpdatedAt(java.time.LocalDateTime.now());
        tmp.setAuthCode("P");
        tmp.setActionCode("D");

        tagTmpRepository.save(tmp);
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
    public TagDto approveOrRejected(Long tmpId, AuthorizeReqDto req) {
        TagTmp tmp = tagTmpRepository.findById(tmpId)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Temporary tag not found."));

        String authCode = req.getAuthCode();
        String actionCode = req.getActionCode();

        if ("A".equalsIgnoreCase(authCode)) {
            switch (actionCode) {
                case "D" -> {
                    tagRepository.deleteById(tmp.getIdTag());
                    tmp.setAuthCode("A");
                    tmp.setActionCode("D");
                }
                case "E" -> {
                    Tag existing = tagRepository.findById(tmp.getIdTag())
                            .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Original tag not found."));
                    existing.setName(tmp.getName());
                    existing.setSlug(tmp.getSlug());
                    existing.setUpdatedBy(tmp.getUpdatedBy());
                    existing.setUpdatedAt(java.time.LocalDateTime.now());
                    tagRepository.save(existing);
                    tmp.setAuthCode("A");
                    tmp.setActionCode("E");
                }
                case "A" -> {
                    Tag newTag = new Tag();
                    newTag.setName(tmp.getName());
                    newTag.setSlug(tmp.getSlug());
                    newTag.setCreatedBy(tmp.getCreatedBy());
                    newTag.setCreatedAt(tmp.getCreatedAt());
                    Tag saved = tagRepository.save(newTag);
                    tmp.setIdTag(saved.getId());
                    tmp.setAuthCode("A");
                    tmp.setActionCode("A");
                }
                default -> throw new GlobalAPIException(HttpStatus.BAD_REQUEST, "Invalid action code.");
            }
        } else if ("R".equalsIgnoreCase(authCode)) {
            tmp.setAuthCode("R");
            tmp.setActionCode(actionCode);
        } else {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST, "Invalid authorization code.");
        }

        TagTmp savedTmp = tagTmpRepository.save(tmp);
        return convertToDtoFromTmpWithCodes(savedTmp);
    }


    @Override
    public List<TagDto> getApprovedTags() {
        List<Tag> tags = tagRepository.findAllByOrderByCreatedAtDesc();

        if (tags.isEmpty()) {
            throw new GlobalAPIException(HttpStatus.NOT_FOUND, "No approved tags found");
        }

        return tags.stream()
                .map(tag -> modelMapper.map(tag, TagDto.class))
                .toList();
    }

    @Override
    public List<TagDtoTmp> getAllTagsTmp() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new GlobalAPIException(HttpStatus.FORBIDDEN, "Only admin can view temporary tags.");
        }

        List<TagTmp> tmpList = tagTmpRepository.findAllByOrderByCreatedAtDesc();

        if (tmpList.isEmpty()) {
            throw new GlobalAPIException(HttpStatus.NOT_FOUND, "No temporary tags found.");
        }

        return tmpList.stream()
                .map(this::convertToTmpDto)
                .toList();
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

    // ==============================================
    // HELPER MAPPER METHODS
    // ==============================================
    private TagDto convertToDtoFromMain(Tag tag) {
        TagDto dto = new TagDto();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        dto.setSlug(tag.getSlug());
        dto.setCreatedBy(tag.getCreatedBy());
        dto.setUpdatedBy(tag.getUpdatedBy());
        dto.setCreatedAt(tag.getCreatedAt());
        dto.setUpdatedAt(tag.getUpdatedAt());
        return dto;
    }

    private TagDto convertToDtoFromTmp(TagTmp tmp) {
        TagDto dto = new TagDto();
        dto.setId(tmp.getIdTag());
        dto.setName(tmp.getName());
        dto.setSlug(tmp.getSlug());
        dto.setCreatedBy(tmp.getCreatedBy());
        dto.setUpdatedBy(tmp.getUpdatedBy());
        dto.setCreatedAt(tmp.getCreatedAt());
        dto.setUpdatedAt(tmp.getUpdatedAt());
        return dto;
    }

    private TagDtoTmp convertToTmpDto(TagTmp tmp) {
        TagDtoTmp dto = new TagDtoTmp();
        dto.setIdTmp(tmp.getIdTmp());
        dto.setIdTag(tmp.getIdTag());
        dto.setName(tmp.getName());
        dto.setSlug(tmp.getSlug());
        dto.setAuthCode(tmp.getAuthCode());
        dto.setActionCode(tmp.getActionCode());
        dto.setCreatedBy(tmp.getCreatedBy());
        dto.setUpdatedBy(tmp.getUpdatedBy());
        dto.setCreatedAt(tmp.getCreatedAt());
        dto.setUpdatedAt(tmp.getUpdatedAt());
        return dto;
    }

    private TagDto convertToDtoFromTmpWithCodes(TagTmp tmp) {
        TagDto dto = new TagDto();
        dto.setId(tmp.getIdTag());
        dto.setName(tmp.getName());
        dto.setSlug(tmp.getSlug());
        dto.setCreatedBy(tmp.getCreatedBy());
        dto.setUpdatedBy(tmp.getUpdatedBy());
        dto.setCreatedAt(tmp.getCreatedAt());
        dto.setUpdatedAt(tmp.getUpdatedAt());
        dto.setAuthCode(tmp.getAuthCode());
        dto.setActionCode(tmp.getActionCode());
        return dto;
    }


}
