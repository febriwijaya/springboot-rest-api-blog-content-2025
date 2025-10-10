package com.content.springboot_rest_api.service.impl;

import com.content.springboot_rest_api.dto.ArticleDto;
import com.content.springboot_rest_api.dto.AuthorizeReqDto;
import com.content.springboot_rest_api.dto.CategoryDto;
import com.content.springboot_rest_api.dto.CategoryDtoTmp;
import com.content.springboot_rest_api.entity.*;
import com.content.springboot_rest_api.exception.GlobalAPIException;
import com.content.springboot_rest_api.repository.ArticlesRepository;
import com.content.springboot_rest_api.repository.CategoryRepository;
import com.content.springboot_rest_api.repository.CategoryTmpRepository;
import com.content.springboot_rest_api.repository.UserRepository;
import com.content.springboot_rest_api.service.CategoryService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private CategoryRepository categoryRepository;
    private CategoryTmpRepository categoryTmpRepository;
    private ArticlesRepository articlesRepository;
    private UserRepository userRepository;
    private ModelMapper modelMapper;

    @Override
    public CategoryDtoTmp addCategory(CategoryDtoTmp categoryDtoTmp) {
        // Mapping otomatis dari DTO ke Entity TMP
        CategoryTmp categoryTmp = modelMapper.map(categoryDtoTmp, CategoryTmp.class);

        // Generate slug
        String slug = categoryDtoTmp.getName()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        categoryTmp.setSlug(slug);

        // Cek slug duplikat di tabel utama maupun TMP
        if (categoryRepository.existsBySlug(slug) || categoryTmpRepository.existsBySlug(slug)) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST, "Slug already exists: " + slug);
        }

        // Ambil username login
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        // Set metadata
        categoryTmp.setCreatedBy(username);
        categoryTmp.setCreatedAt(LocalDateTime.now());
        categoryTmp.setAuthCode("P"); // Pending approval
        categoryTmp.setActionCode("A"); // Add action

        // Simpan ke tabel TMP
        CategoryTmp savedTmp = categoryTmpRepository.save(categoryTmp);

        // Mapping balik ke DTO response
        return modelMapper.map(savedTmp, CategoryDtoTmp.class);
    }

    @Override
    public List<CategoryDto> getAllCategories() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        // Ambil kategori utama
        List<Category> mainCategories = isAdmin
                ? categoryRepository.findAll()
                : categoryRepository.findByCreatedBy(username);

        // Ambil data temporary (pending/edit/delete)
        List<CategoryTmp> latestTmp = categoryTmpRepository.findLatestTmpPerCategory();
        List<CategoryTmp> newCategories = categoryTmpRepository.findLatestTmpNewCategories();

        // Filter hanya data milik user (kalau bukan admin)
        if (!isAdmin) {
            latestTmp = latestTmp.stream()
                    .filter(tmp -> username.equals(tmp.getCreatedBy()) || username.equals(tmp.getUpdatedBy()))
                    .toList();
            newCategories = newCategories.stream()
                    .filter(tmp -> username.equals(tmp.getCreatedBy()) || username.equals(tmp.getUpdatedBy()))
                    .toList();
        }

        // Mapping id_category -> tmp terbaru
        Map<Long, CategoryTmp> tmpMap = latestTmp.stream()
                .filter(tmp -> tmp.getIdCategory() != null)
                .collect(Collectors.toMap(CategoryTmp::getIdCategory, tmp -> tmp));

        List<CategoryDto> result = new ArrayList<>();

        for (Category category : mainCategories) {
            CategoryTmp tmp = tmpMap.get(category.getId());

            // Jika ada versi tmp
            if (tmp != null) {
                // Jika auth_code = "A" dan id_category tidak ada di main → skip
                if ("A".equals(tmp.getAuthCode()) && category.getId() == null) {
                    continue;
                }

                // Jika pending → tampilkan versi tmp
                if ("P".equals(tmp.getAuthCode())) {
                    result.add(convertToDtoMerged(category, tmp));

                    // Jika rejected → tampilkan versi main
                } else if ("R".equals(tmp.getAuthCode())) {
                    result.add(convertToDtoFromMain(category));

                    // Jika approved → tetap tampilkan main tapi isi auth/action dari tmp
                } else if ("A".equals(tmp.getAuthCode())) {
                    result.add(convertToDtoMerged(category, tmp));

                    // Default
                } else {
                    result.add(convertToDtoFromMain(category));
                }

            } else {
                // Tidak ada versi tmp → tampilkan main
                result.add(convertToDtoFromMain(category));
            }
        }

        // Tambahkan kategori baru (yang belum ada di main)
        for (CategoryTmp tmp : newCategories) {
            // skip kalau auth_code = "A" tapi id_category tidak ada di main
            if ("A".equals(tmp.getAuthCode()) && tmp.getIdCategory() == null) {
                continue;
            }
            result.add(convertToDtoFromTmp(tmp));
        }

        // Urutkan berdasarkan createdAt terbaru
        result.sort(Comparator.comparing(CategoryDto::getCreatedAt).reversed());

        return result;
    }

    // 🔹 Jika hanya ada Category (tanpa tmp)
    private CategoryDto convertToDtoFromMain(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setSlug(category.getSlug());
        dto.setAuthCode(null);
        dto.setActionCode(null);
        dto.setCreatedBy(category.getCreatedBy());
        dto.setUpdatedBy(category.getUpdatedBy());
        dto.setCreatedAt(category.getCreatedAt());
        dto.setUpdatedAt(category.getUpdatedAt());
        return dto;
    }

    // 🔹 Jika hanya ada CategoryTmp (kategori baru)
    private CategoryDto convertToDtoFromTmp(CategoryTmp tmp) {
        CategoryDto dto = new CategoryDto();
        dto.setId(tmp.getIdCategory() != null ? tmp.getIdCategory() : tmp.getIdTmp());
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

    // 🔹 Gabungan data Category + CategoryTmp (untuk menampilkan auth/action dari tmp)
    private CategoryDto convertToDtoMerged(Category category, CategoryTmp tmp) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(tmp.getName() != null ? tmp.getName() : category.getName());
        dto.setSlug(tmp.getSlug() != null ? tmp.getSlug() : category.getSlug());
        dto.setAuthCode(tmp.getAuthCode());
        dto.setActionCode(tmp.getActionCode());
        dto.setCreatedBy(category.getCreatedBy());
        dto.setUpdatedBy(tmp.getUpdatedBy() != null ? tmp.getUpdatedBy() : category.getUpdatedBy());
        dto.setCreatedAt(category.getCreatedAt());
        dto.setUpdatedAt(tmp.getUpdatedAt() != null ? tmp.getUpdatedAt() : category.getUpdatedAt());
        return dto;
    }

    @Override
    public CategoryDto getCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->  new GlobalAPIException(HttpStatus.NOT_FOUND,
                        "Category not found with id : " + id));
        return modelMapper.map(category, CategoryDto.class);
    }

    @Override
    public CategoryDtoTmp updateCategory(Long id, CategoryDtoTmp categoryDtoTmp) {
        // Cari data category asli
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND,
                        "Category not found with id : " + id));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.UNAUTHORIZED,
                        "The currently logged in user was not found."));

        Set<String> roles = currentUser.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        // Validasi hak akses (user hanya boleh ubah miliknya sendiri)
        if (roles.contains("ROLE_USER") && !currentUser.getUsername().equals(category.getCreatedBy())) {
            throw new GlobalAPIException(HttpStatus.FORBIDDEN, "You may not update other users' data");
        }

        // Generate slug baru
        String slug = categoryDtoTmp.getName()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        // Cek jika slug sudah ada di TMP atau Category lain
        if (categoryRepository.existsBySlug(slug) || categoryTmpRepository.existsBySlug(slug)) {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST, "Slug already exists: " + slug);
        }

        // Simpan ke tabel TMP (bukan langsung ubah Category utama)
        CategoryTmp tmp = modelMapper.map(categoryDtoTmp, CategoryTmp.class);
        tmp.setIdCategory(category.getId());
        tmp.setSlug(slug);
        tmp.setAuthCode("P");
        tmp.setActionCode("E");
        tmp.setCreatedBy(category.getCreatedBy());
        tmp.setUpdatedBy(username);
        tmp.setCreatedAt(category.getCreatedAt());
        tmp.setUpdatedAt(LocalDateTime.now());

        CategoryTmp savedTmp = categoryTmpRepository.save(tmp);

        return modelMapper.map(savedTmp, CategoryDtoTmp.class);
    }


    @Override
    @Transactional
    public void deleteCategory(Long id) {
        //  Cari data category berdasarkan ID
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND,
                        "Category not found with id : " + id));

        //  Ambil username dari user yang sedang login
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.UNAUTHORIZED,
                        "The currently logged in user was not found."));

        Set<String> roles = currentUser.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        // Validasi hak akses (ROLE_USER hanya boleh hapus datanya sendiri)
        if (roles.contains("ROLE_USER") && !currentUser.getUsername().equals(category.getCreatedBy())) {
            throw new GlobalAPIException(HttpStatus.FORBIDDEN,
                    "You cannot delete other users' data");
        }

        //  Mapping otomatis dari Category ke CategoryTmp menggunakan ModelMapper
        CategoryTmp categoryTmp = modelMapper.map(category, CategoryTmp.class);

        //  Tambahkan metadata action
        categoryTmp.setIdCategory(category.getId());
        categoryTmp.setUpdatedBy(username);
        categoryTmp.setActionCode("D"); // D = Delete
        categoryTmp.setAuthCode("P");   // P = Pending approval

        // Simpan dulu ke tabel category_tmp
        categoryTmpRepository.save(categoryTmp);

        // Jangan langsung hapus dari tabel utama, tunggu approval
        // categoryRepository.delete(category);
    }


    @Override
    public List<ArticleDto> getArticlesByCategorySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new GlobalAPIException(
                        HttpStatus.NOT_FOUND,
                        "Category not found with slug : " + slug
                ));

        List<Article> articles = articlesRepository.findByCategory(category);

        if (articles.isEmpty()) {
            throw new GlobalAPIException(
                    HttpStatus.NOT_FOUND,
                    "No articles found for category: " + slug
            );
        }

        return articles.stream()
                .map(article -> modelMapper.map(article, ArticleDto.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategoryDtoTmp approveOrRejectCategory(Long tmpId, AuthorizeReqDto req) {
        CategoryTmp tmp = categoryTmpRepository.findById(tmpId)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND,
                        "Category TMP not found with id : " + tmpId));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        String authCode = req.getAuthCode();
        if ("A".equalsIgnoreCase(authCode)) {
            // Approve
            tmp.setAuthCode("A");
            tmp.setUpdatedBy(username);
            tmp.setUpdatedAt(java.time.LocalDateTime.now());
            categoryTmpRepository.save(tmp); // persist perubahan status

            String action = tmp.getActionCode();

            // ADD
            if ("A".equalsIgnoreCase(action)) {
                // cek slug double di main table
                if (categoryRepository.existsBySlug(tmp.getSlug())) {
                    throw new GlobalAPIException(HttpStatus.BAD_REQUEST,
                            "Slug already exists in main table: " + tmp.getSlug());
                }

                Category newCategory = new Category();
                newCategory.setName(tmp.getName());
                newCategory.setSlug(tmp.getSlug());
                newCategory.setCreatedBy(tmp.getCreatedBy());
                newCategory.setCreatedAt(tmp.getCreatedAt() != null ? tmp.getCreatedAt() : java.time.LocalDateTime.now());
                newCategory.setUpdatedBy(username);
                newCategory.setUpdatedAt(java.time.LocalDateTime.now());

                Category saved = categoryRepository.save(newCategory);

                // simpan id referensi ke tmp (opsional tapi berguna)
                tmp.setIdCategory(saved.getId());
                CategoryTmp tmp1 =  categoryTmpRepository.save(tmp);

                return modelMapper.map(tmp1, CategoryDtoTmp.class);
            }

            // EDIT
            else if ("E".equalsIgnoreCase(action)) {
                Category existing = categoryRepository.findById(tmp.getIdCategory())
                        .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND,
                                "Original category not found with id : " + tmp.getIdCategory()));

                // bila slug di tmp berubah dan sudah dipakai oleh category lain → reject
                if (!existing.getSlug().equals(tmp.getSlug()) && categoryRepository.existsBySlug(tmp.getSlug())) {
                    throw new GlobalAPIException(HttpStatus.BAD_REQUEST,
                            "Slug already exists in main table: " + tmp.getSlug());
                }

                existing.setName(tmp.getName());
                existing.setSlug(tmp.getSlug());
                existing.setUpdatedBy(username);
                existing.setUpdatedAt(java.time.LocalDateTime.now());

                Category updated = categoryRepository.save(existing);

                // catatan: kita menyimpan status approve di tmp (sudah dilakukan di atas)
                return modelMapper.map(tmp, CategoryDtoTmp.class);
            }

            // DELETE
            else if ("D".equalsIgnoreCase(action)) {
                Category toDelete = categoryRepository.findById(tmp.getIdCategory())
                        .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND,
                                "Category to delete not found with id : " + tmp.getIdCategory()));

                categoryRepository.delete(toDelete);

                // simpan status approve di tmp (sudah di-set di atas)
                return null;
            }

            else {
                throw new GlobalAPIException(HttpStatus.BAD_REQUEST,
                        "Invalid actionCode in tmp (expected A, E or D).");
            }
        }

        else if ("R".equalsIgnoreCase(authCode)) {
            // Reject: hanya update tmp -> jangan ubah main table
            tmp.setAuthCode("R");
            tmp.setUpdatedBy(username);
            tmp.setUpdatedAt(java.time.LocalDateTime.now());
            CategoryTmp rejected = categoryTmpRepository.save(tmp);

            return modelMapper.map(rejected, CategoryDtoTmp.class);
        }

        else {
            throw new GlobalAPIException(HttpStatus.BAD_REQUEST,
                    "Invalid auth_code. Only 'A' (approve) or 'R' (reject) allowed.");
        }
    }


    @Override
    @Transactional
    public List<CategoryDto> getCategoriesByLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        List<Category> categories = categoryRepository.findByCreatedBy(username);

        if (categories.isEmpty()) {
            throw new GlobalAPIException(HttpStatus.NOT_FOUND,
                    "No categories found for user: " + username);
        }

        return categories.stream()
                .map(category -> modelMapper.map(category, CategoryDto.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategoryDto getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND,
                        "Category not found with slug : " + slug));

        return modelMapper.map(category, CategoryDto.class);
    }

    @Override
    public List<CategoryDto> getAllApprovedCategories() {
        // Ambil semua kategori dari tabel utama (category)
        List<Category> categories = categoryRepository.findAll();

        if (categories.isEmpty()) {
            throw new GlobalAPIException(HttpStatus.NOT_FOUND, "No approved categories found.");
        }

        // Mapping menggunakan ModelMapper
        return categories.stream()
                .map(category -> modelMapper.map(category, CategoryDto.class))
                .sorted(Comparator.comparing(CategoryDto::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryDtoTmp> getAllCategoriesTmp() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // Hanya admin yang boleh akses data kategori TMP
        if (!isAdmin) {
            throw new GlobalAPIException(HttpStatus.FORBIDDEN, "Only admin can view temporary categories.");
        }

        // Ambil semua data category_tmp diurutkan dari yang terbaru
        List<CategoryTmp> tmpList = categoryTmpRepository.findAllByOrderByCreatedAtDesc();

        if (tmpList.isEmpty()) {
            throw new GlobalAPIException(HttpStatus.NOT_FOUND, "No temporary categories found.");
        }

        // Mapping ke DTO
        return tmpList.stream()
                .map(tmp -> modelMapper.map(tmp, CategoryDtoTmp.class))
                .toList();
    }

    // ============================================================
    // HELPER MAPPER METHOD
    // ============================================================


//    @Override
//    public List<CategoryDto> getApprovedCategories() {
//        List<Category> categories = categoryRepository.findByAuthCode("A");
//
//        if (categories.isEmpty()) {
//            throw new GlobalAPIException(HttpStatus.NOT_FOUND,
//                    "No approved categories found");
//        }
//
//        return categories.stream()
//                .map(category -> modelMapper.map(category, CategoryDto.class))
//                .collect(Collectors.toList());
//    }
}
