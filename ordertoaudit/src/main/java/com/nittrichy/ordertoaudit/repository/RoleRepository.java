package com.nittrichy.ordertoaudit.repository;

import com.nittrichy.ordertoaudit.entity.Role;
import com.nittrichy.ordertoaudit.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}

