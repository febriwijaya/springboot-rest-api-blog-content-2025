package com.content.springboot_rest_api.service;

import com.content.springboot_rest_api.dto.UserRegisterDto;
import com.content.springboot_rest_api.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    User register(UserRegisterDto dto, MultipartFile foto);
}
