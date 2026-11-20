package com.nittrichy.ordertoaudit.repository;

import com.nittrichy.ordertoaudit.entity.PackagingMaterial;
import com.nittrichy.ordertoaudit.enums.PackagingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface PackagingMaterialRepository extends JpaRepository<PackagingMaterial, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PackagingMaterial p where p.id = :id")
    Optional<PackagingMaterial> findByIdForUpdate(@Param("id") Long id);
    Optional<PackagingMaterial> findByMaterialCode(String materialCode);
    Optional<PackagingMaterial> findByName(String name);
    List<PackagingMaterial> findByType(PackagingType type);
    boolean existsByMaterialCode(String materialCode);
}

