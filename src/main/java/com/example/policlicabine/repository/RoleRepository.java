package com.example.policlicabine.repository;

import com.example.policlicabine.entity.Role;
import com.example.policlicabine.entity.enums.UserRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(UserRole name);

    boolean existsByName(UserRole name);

    Set<Role> findByNameIn(Set<UserRole> names);

    @EntityGraph(attributePaths = {"permissions"})
    Optional<Role> findWithPermissionsByName(UserRole name);

    @EntityGraph(attributePaths = {"permissions"})
    Optional<Role> findWithPermissionsByRoleId(UUID roleId);
}
