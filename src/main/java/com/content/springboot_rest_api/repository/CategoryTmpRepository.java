package com.content.springboot_rest_api.repository;

import com.content.springboot_rest_api.entity.CategoryTmp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryTmpRepository extends JpaRepository<CategoryTmp, Long> {

    // ✅ Cek apakah slug sudah digunakan di data temporary
    boolean existsBySlug(String slug);

    // ✅ Ambil data berdasarkan id kategori utama
    List<CategoryTmp> findByIdCategory(Long idCategory);

    // ✅ Ambil data berdasarkan pembuat (user)
    List<CategoryTmp> findByCreatedBy(String createdBy);

    // ✅ Ambil data berdasarkan status auth_code (misal: 'P' = pending, 'R' = rejected)
    List<CategoryTmp> findByAuthCode(String authCode);

    // ✅ Ambil semua data temporary yang diurutkan dari terbaru
    List<CategoryTmp> findAllByOrderByCreatedAtDesc();

    // ✅ Ambil tmp terbaru untuk setiap category (misal untuk membandingkan versi terbaru)
    @Query("""
           SELECT t FROM CategoryTmp t 
           WHERE t.updatedAt = (
               SELECT MAX(t2.updatedAt) 
               FROM CategoryTmp t2 
               WHERE t2.idCategory = t.idCategory
           )
           """)
    List<CategoryTmp> findLatestTmpPerCategory();

    // ✅ Ambil data temporary baru yang belum memiliki category utama (idCategory = null)
    @Query("""
           SELECT t FROM CategoryTmp t 
           WHERE t.idCategory IS NULL 
           ORDER BY t.createdAt DESC
           """)
    List<CategoryTmp> findLatestTmpNewCategories();
}
