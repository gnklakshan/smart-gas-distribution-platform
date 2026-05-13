package com.gastracker.queue_service.dao.repository;

import com.gastracker.queue_service.dao.entity.CitizenQueue;
import com.gastracker.queue_service.enums.QueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CitizenQueueRepository extends JpaRepository<CitizenQueue, String> {

    List<CitizenQueue> findByUserIdOrderByRequestedAtDesc(String userId);

    List<CitizenQueue> findByDealerIdOrderByRequestedAtAsc(String dealerId);

    List<CitizenQueue> findByDealerIdAndStatusOrderByRequestedAtAsc(String dealerId, QueueStatus status);

    boolean existsByUserIdAndDealerIdAndCylinderTypeIdAndStatusIn(
            String userId, String dealerId, String cylinderTypeId, List<QueueStatus> statuses);
}
