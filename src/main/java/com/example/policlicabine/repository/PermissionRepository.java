package com.example.policlicabine.repository;

import com.example.policlicabine.entity.Permission;
import com.example.policlicabine.entity.enums.PermissionEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByName(PermissionEnum name);

    boolean existsByName(PermissionEnum name);
}
