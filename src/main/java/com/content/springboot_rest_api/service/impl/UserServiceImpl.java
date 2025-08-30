package com.content.springboot_rest_api.service.impl;

import com.content.springboot_rest_api.dto.UserRegisterDto;
import com.content.springboot_rest_api.entity.Role;
import com.content.springboot_rest_api.entity.User;
import com.content.springboot_rest_api.exception.GlobalAPIException;
import com.content.springboot_rest_api.repository.RoleRepository;
import com.content.springboot_rest_api.repository.UserRepository;
import com.content.springboot_rest_api.service.UserService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // lokasi folder penyimpanan
    private static final String UPLOAD_DIR = "uploads/photos/users/";
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
    public User register(UserRegisterDto dto, MultipartFile foto) {
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

        //  validasi + simpan foto
        String fotoPath = null;
        if (foto != null && !foto.isEmpty()) {
            validateFile(foto);
            fotoPath = saveFile(foto, dto.getUsername());
        }

        //  buat user baru
        User user = new User();
        user.setFullName(dto.getFullName());
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword())); // encode password
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setBirthDate(dto.getBirthDate());
        user.setJobTitle(dto.getJobTitle());
        user.setLocation(dto.getLocation());
        user.setFoto(fotoPath);
        user.setGender(dto.getGender());

        //  set default role = ROLE_USER
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.INTERNAL_SERVER_ERROR, "Role USER belum tersedia"));
        user.getRoles().add(userRole);

        //  simpan user
        return userRepository.save(user);
    }

    // helper methods
    private void validateFile(MultipartFile file) {
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

    private String saveFile(MultipartFile file, String username) {
        try {
            // buat folder jika belum ada
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // nama file unik
            String originalFileName = file.getOriginalFilename();
            String extension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase();
            String fileName = username + "_" + System.currentTimeMillis() + extension;

            Path filePath = uploadPath.resolve(fileName);
            file.transferTo(filePath.toFile());

            return UPLOAD_DIR + fileName;
        } catch (IOException e) {
            throw new GlobalAPIException(HttpStatus.INTERNAL_SERVER_ERROR, "Gagal menyimpan file foto");
        }
    }
}
