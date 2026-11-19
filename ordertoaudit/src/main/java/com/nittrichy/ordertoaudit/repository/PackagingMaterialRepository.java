package com.nittrichy.ordertoaudit.repository;

import com.nittrichy.ordertoaudit.entity.PackagingMaterial;
import com.nittrichy.ordertoaudit.enums.PackagingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PackagingMaterialRepository extends JpaRepository<PackagingMaterial, Long> {
    Optional<PackagingMaterial> findByMaterialCode(String materialCode);
    Optional<PackagingMaterial> findByName(String name);
    List<PackagingMaterial> findByType(PackagingType type);
    boolean existsByMaterialCode(String materialCode);
}
