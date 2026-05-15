package com.gastracker.user_service.dao.repository;

import com.gastracker.user_service.dao.entity.Dealer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DealerRepository extends JpaRepository<Dealer, String> {
    Optional<Dealer> findByUserId(String userId);
    boolean existsByUserId(String userId);
    boolean existsByBusinessRegNo(String businessRegNo);

    @Query(value = """
        SELECT * FROM (
            SELECT id, user_id, business_name, business_reg_no, address, latitude, longitude,
                   (6371 * acos(LEAST(1.0,
                       cos(radians(:lat)) * cos(radians(CAST(latitude AS float8)))
                       * cos(radians(CAST(longitude AS float8)) - radians(:lng))
                       + sin(radians(:lat)) * sin(radians(CAST(latitude AS float8)))
                   ))) AS distance_km
            FROM dealers
            WHERE latitude IS NOT NULL AND longitude IS NOT NULL
        ) sub
        WHERE distance_km <= :radiusKm
        ORDER BY distance_km ASC
        """, nativeQuery = true)
    List<Object[]> findNearbyDealers(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm
    );
}
