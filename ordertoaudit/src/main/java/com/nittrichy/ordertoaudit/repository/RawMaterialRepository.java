package com.nittrichy.ordertoaudit.repository;

import com.nittrichy.ordertoaudit.entity.RawMaterial;
import com.nittrichy.ordertoaudit.enums.MaterialCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface RawMaterialRepository extends JpaRepository<RawMaterial, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RawMaterial r where r.id = :id")
    Optional<RawMaterial> findByIdForUpdate(@Param("id") Long id);
    Optional<RawMaterial> findByMaterialCode(String materialCode);
    Optional<RawMaterial> findByName(String name);
    List<RawMaterial> findByCategory(MaterialCategory category);
    boolean existsByMaterialCode(String materialCode);
}

