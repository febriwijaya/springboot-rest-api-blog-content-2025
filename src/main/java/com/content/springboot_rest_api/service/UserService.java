package com.content.springboot_rest_api.service;

import com.content.springboot_rest_api.dto.UserRegisterDto;
import com.content.springboot_rest_api.dto.UserResponseDto;
import com.content.springboot_rest_api.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {
    UserResponseDto register(UserRegisterDto dto, MultipartFile foto);

    UserResponseDto update(Long id, UserRegisterDto dto, MultipartFile foto);

    List<UserResponseDto> getAll();

    UserResponseDto getById(Long id);

    void delete(Long id);

    }
