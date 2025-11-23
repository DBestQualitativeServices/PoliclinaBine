package com.example.policlicabine.repository;

import com.example.policlicabine.entity.Manager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ManagerRepository extends JpaRepository<Manager, UUID> {

    Optional<Manager> findByUserUserId(UUID userId);

    boolean existsByUserUserId(UUID userId);
}
