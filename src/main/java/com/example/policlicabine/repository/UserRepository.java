package com.example.policlicabine.repository;

import com.example.policlicabine.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findWithRolesAndPermissionsByUsername(String username);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findWithRolesAndPermissionsByUserId(UUID userId);

    @EntityGraph(attributePaths = {"roles", "doctorProfile", "patientProfile", "managerProfile"})
    Optional<User> findWithProfileByUserId(UUID userId);

    @EntityGraph(attributePaths = {"roles", "roles.permissions", "doctorProfile", "patientProfile", "managerProfile"})
    @Query("SELECT u FROM User u WHERE u.username = :username")
    Optional<User> findWithRolesPermissionsAndProfiles(@Param("username") String username);
}
