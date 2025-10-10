package com.content.springboot_rest_api.repository;

import com.content.springboot_rest_api.entity.TagTmp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface TagTmpRepository extends JpaRepository<TagTmp, Long> {

    List<TagTmp> findAllByOrderByCreatedAtDesc();

    Optional<TagTmp> findBySlug(String slug);

    List<TagTmp> findByCreatedBy(String createdBy);

    List<TagTmp> findByAuthCode(String authCode);

    boolean existsBySlug(String slug);

    @Query("""
        SELECT t FROM TagTmp t
        WHERE t.idTmp IN (
            SELECT MAX(t2.idTmp)
            FROM TagTmp t2
            WHERE t2.idTag IS NOT NULL
            GROUP BY t2.idTag
        )
    """)
    List<TagTmp> findLatestTmpPerTag();

    @Query("""
        SELECT t FROM TagTmp t
        WHERE t.idTag IS NULL
        AND t.idTmp IN (
            SELECT MAX(t2.idTmp)
            FROM TagTmp t2
            WHERE t2.idTag IS NULL
            GROUP BY t2.slug
        )
    """)
    List<TagTmp> findLatestTmpNewTags();

    //  Ambil tmp hanya milik user tertentu
    @Query("""
        SELECT t FROM TagTmp t
        WHERE t.createdBy = :username OR t.updatedBy = :username
    """)
    List<TagTmp> findAllByUser(String username);

    List<TagTmp> findByIdTag(Long idTag);
}
