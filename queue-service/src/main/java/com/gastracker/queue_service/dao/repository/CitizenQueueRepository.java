package com.gastracker.queue_service.dao.repository;

import com.gastracker.queue_service.dao.entity.CitizenQueue;
import com.gastracker.queue_service.enums.QueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CitizenQueueRepository extends JpaRepository<CitizenQueue, String> {

    List<CitizenQueue> findByUserIdOrderByRequestedAtDesc(String userId);

    List<CitizenQueue> findByDealerIdOrderByRequestedAtAsc(String dealerId);

    List<CitizenQueue> findByDealerIdAndStatusOrderByRequestedAtAsc(String dealerId, QueueStatus status);

    long countByDealerIdAndStatus(String dealerId, QueueStatus status);

    long countByDealerIdAndStatusAndFulfilledAtAfter(String dealerId, QueueStatus status, LocalDateTime after);

    List<CitizenQueue> findByDealerIdAndStatusAndFulfilledAtAfter(String dealerId, QueueStatus status, LocalDateTime after);

    boolean existsByUserIdAndDealerIdAndCylinderTypeIdAndStatusIn(
            String userId, String dealerId, String cylinderTypeId, List<QueueStatus> statuses);
}
