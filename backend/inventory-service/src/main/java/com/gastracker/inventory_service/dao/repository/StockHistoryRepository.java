package com.gastracker.inventory_service.dao.repository;

import com.gastracker.inventory_service.dao.entity.StockHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface StockHistoryRepository extends JpaRepository<StockHistory, String> {

    List<StockHistory> findByDealerIdAndChangedAtAfterOrderByChangedAtDesc(String dealerId, LocalDateTime after);
}
