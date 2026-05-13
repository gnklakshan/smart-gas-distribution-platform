package com.gastracker.user_service.dao.repository;

import com.gastracker.user_service.dao.entity.Dealer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DealerRepository extends JpaRepository<Dealer, String> {
    Optional<Dealer> findByUserId(String userId);
    boolean existsByUserId(String userId);
    boolean existsByBusinessRegNo(String businessRegNo);
}
