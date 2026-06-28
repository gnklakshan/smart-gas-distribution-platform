package com.gastracker.allocation_service.dao.repository;

import com.gastracker.allocation_service.dao.entity.Allocation;
import com.gastracker.allocation_service.enums.AllocationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AllocationRepository extends JpaRepository<Allocation, String> {

    List<Allocation> findByStatus(AllocationStatus status);

    List<Allocation> findByDealerId(String dealerId);

    List<Allocation> findByDealerIdOrderByRequestedAtDesc(String dealerId);

    long countByDealerId(String dealerId);

    long countByDealerIdAndStatus(String dealerId, AllocationStatus status);

    long countByDealerIdAndStatusIn(String dealerId, List<AllocationStatus> statuses);

    @Query("SELECT COALESCE(SUM(a.approvedQuantity), 0) FROM Allocation a " +
            "WHERE a.dealerId = :dealerId AND a.status IN ('APPROVED', 'DELIVERED') AND a.resolvedAt >= :since")
    Integer sumApprovedQuantitySince(@Param("dealerId") String dealerId, @Param("since") LocalDateTime since);

    @Query("SELECT a.dealerId, COALESCE(SUM(a.approvedQuantity), 0) FROM Allocation a " +
            "WHERE a.status IN ('APPROVED', 'DELIVERED') AND a.resolvedAt >= :since " +
            "GROUP BY a.dealerId")
    List<Object[]> sumApprovedQuantitySinceGroupedByDealer(@Param("since") LocalDateTime since);
}
