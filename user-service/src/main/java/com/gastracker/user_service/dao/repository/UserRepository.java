package com.gastracker.user_service.dao.repository;

import com.gastracker.user_service.dao.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByNic(String nic);
    boolean existsByNic(String nic);
}
