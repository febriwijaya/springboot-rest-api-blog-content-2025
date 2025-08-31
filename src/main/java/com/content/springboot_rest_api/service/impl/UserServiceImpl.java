package com.content.springboot_rest_api.service.impl;

import com.content.springboot_rest_api.dto.UserRegisterDto;
import com.content.springboot_rest_api.dto.UserResponseDto;
import com.content.springboot_rest_api.entity.Role;
import com.content.springboot_rest_api.entity.User;
import com.content.springboot_rest_api.exception.GlobalAPIException;
import com.content.springboot_rest_api.repository.RoleRepository;
import com.content.springboot_rest_api.repository.UserRepository;
import com.content.springboot_rest_api.service.UserService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
//@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    @Value("${app.upload.user-photo-dir}")
    private String uploadDir;

    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           ModelMapper modelMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
    }

    private static final long MAX_SIZE = 2 * 1024 * 1024; // 2MB

    // mapping ekstensi -> MIME type yang diperbolehkan
    private static final Map<String, String> ALLOWED_TYPES = Map.ofEntries(
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("heic", "image/heic"),
            Map.entry("heif", "image/heif")
    );

    @Transactional
    @Override
    public UserResponseDto register(UserRegisterDto dto, MultipartFile foto) {
        //  validasi username
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST, "Username sudah terdaftar");
        }

        //  validasi email
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST, "Email sudah terdaftar");
        }

        //  validasi phone
        if (userRepository.findByPhone(dto.getPhone()).isPresent()) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST, "Nomor Hp sudah terdaftar");
        }

        // Validasi dan simpan foto
        String fotoPath = null;
        if(foto != null && !foto.isEmpty()) {
            validateFile(foto);
            fotoPath = saveFile(foto, dto.getUsername());
        }

        // mapping dto -> entity
        User user = modelMapper.map(dto, User.class);
        // encode password
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setFoto(fotoPath);


        //  set default role = ROLE_USER
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Role USER belum tersedia"));
        user.setRoles(Collections.singleton(userRole));

        //  simpan ke database
        User savedUser = userRepository.save(user);

        // mapping entity -> response dto
       UserResponseDto response = modelMapper.map(savedUser, UserResponseDto.class);
       response.setRoles(Set.of("ROLE_USER"));

       return response;
    }

    @Transactional
    @Override
    public UserResponseDto update(Long id, UserRegisterDto dto, MultipartFile foto) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "User tidak ditemukan"));

        // Update field lain dari DTO (selain password yg sudah di-handle)
        modelMapper.map(dto, user);

        // Update foto
        if (foto != null && !foto.isEmpty()) {
            validateFile(foto);

            // hapus foto lama jika ada
            if (user.getFoto() != null && !user.getFoto().isBlank()) {
                try {
                    // ambil nama file saja dari path DB
                    String existingFileName = Paths.get(user.getFoto()).getFileName().toString();
                    Path existingFilePath = Paths.get(uploadDir).toAbsolutePath().resolve(existingFileName);

                    Files.deleteIfExists(existingFilePath);
                } catch (IOException e) {
                    log.warn("Gagal menghapus file foto lama: {}", user.getFoto(), e);
                }
            }

            // simpan foto baru
            String fotoPath = saveFile(foto, dto.getUsername());
            user.setFoto(fotoPath);
        }

        // update password jika ada
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        User updated = userRepository.save(user);
        UserResponseDto response = modelMapper.map(updated, UserResponseDto.class);
        response.setRoles(
                updated.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
        );
        return response;
    }

    @Override
    public List<UserResponseDto> getAll() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(user -> {
                    UserResponseDto dto = modelMapper.map(user, UserResponseDto.class);
                    dto.setRoles(user.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet()));
                    return dto;
                })
                .toList();
    }

    @Override
    public UserResponseDto getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "User tidak ditemukan"));
        UserResponseDto dto = modelMapper.map(user, UserResponseDto.class);
        dto.setRoles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
        return dto;
    }

    @Transactional
    @Override
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "User tidak ditemukan"));

        // hapus foto jika ada
        if (user.getFoto() != null && !user.getFoto().isBlank()) {
            try {
                // ambil nama file saja dari path DB
                String existingFileName = Paths.get(user.getFoto()).getFileName().toString();
                Path existingFilePath = Paths.get(uploadDir).toAbsolutePath().resolve(existingFileName);

                Files.deleteIfExists(existingFilePath);
            } catch (IOException e) {
                log.warn("Gagal menghapus file foto: {}", user.getFoto(), e);
            }
        }

        // hapus data user
        userRepository.delete(user);
    }

    // helper methods
    private void validateFile(MultipartFile file) {
        log.info("Validating file: name={}, size={}, mime={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        // ukuran max
        if (file.getSize() > MAX_SIZE) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST, "Ukuran photo maksimal 2MB");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || !originalFileName.contains(".")) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST, "Format file tidak valid");
        }

        // ambil ekstensi
        String extension = originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toLowerCase();

        // cek apakah ada di daftar ekstensi yang diperbolehkan
        if (!ALLOWED_TYPES.containsKey(extension)) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST,
                    "Format file tidak valid. Hanya boleh: " + String.join(", ", ALLOWED_TYPES.keySet()));
        }

        // cek mime type dari file yang diupload
        String mimeType = file.getContentType();
        if (mimeType == null || !mimeType.equalsIgnoreCase(ALLOWED_TYPES.get(extension))) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST,
                    "MIME type tidak sesuai dengan ekstensi file (" + extension + ")");
        }
    }

    // helper method : simpan file
    private String saveFile(MultipartFile file, String username) {
        try {
            // Buat path absolut untuk folder upload
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath();

            // Buat folder jika belum ada
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Ambil ekstensi file
            String originalFileName = Objects.requireNonNull(file.getOriginalFilename());
            String extension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase();

            // Buat nama file unik: username_timestamp.ext
            String fileName = username + "_" + System.currentTimeMillis() + extension;

            // Full path tempat file akan disimpan
            Path filePath = uploadPath.resolve(fileName);

            // Simpan file
            file.transferTo(filePath.toFile());

            // Kembalikan relative path sesuai WebConfig
            // Jadi nantinya bisa diakses via /photos/users/filename
            return "/uploads/photos/users/" + fileName;

        } catch (IOException e) {
            log.error("Error saat menyimpan file foto: {}", e.getMessage(), e);
            throw new GlobalAPIException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Gagal menyimpan file foto: " + e.getMessage());
        }
    }
}