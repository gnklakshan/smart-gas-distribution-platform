package com.gastracker.allocation_service.dao.repository;

import com.gastracker.allocation_service.dao.entity.Allocation;
import com.gastracker.allocation_service.enums.AllocationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AllocationRepository extends JpaRepository<Allocation, String> {

    List<Allocation> findByStatus(AllocationStatus status);

    List<Allocation> findByDealerId(String dealerId);

    List<Allocation> findByDealerIdOrderByRequestedAtDesc(String dealerId);
}
