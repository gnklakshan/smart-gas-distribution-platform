package com.gastracker.queue_service.service;

import com.gastracker.queue_service.dao.entity.CitizenQueue;
import com.gastracker.queue_service.dao.repository.CitizenQueueRepository;
import com.gastracker.queue_service.dto.request.JoinQueueRequest;
import com.gastracker.queue_service.dto.response.QueueResponse;
import com.gastracker.queue_service.enums.QueueStatus;
import com.gastracker.queue_service.event.QueueCancelledEvent;
import com.gastracker.queue_service.event.QueueCompletedEvent;
import com.gastracker.queue_service.event.QueueJoinedEvent;
import com.gastracker.queue_service.event.QueueReadyEvent;
import com.gastracker.queue_service.exception.DuplicateResourceException;
import com.gastracker.queue_service.exception.InvalidStateException;
import com.gastracker.queue_service.exception.ResourceNotFoundException;
import com.gastracker.queue_service.service.transformer.QueueTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {

    private static final String TOPIC_QUEUE_JOINED = "queue.joined";
    private static final String TOPIC_QUEUE_READY = "queue.ready";
    private static final String TOPIC_QUEUE_COMPLETED = "queue.completed";
    private static final String TOPIC_QUEUE_CANCELLED = "queue.cancelled";

    private final CitizenQueueRepository queueRepository;
    private final QueueTransformer queueTransformer;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ── CITIZEN: join a queue ──────────────────────────────────────────────
    @Transactional
    public QueueResponse joinQueue(String userId, JoinQueueRequest request) {
        // Check if citizen already has an active queue entry for this dealer + cylinder type
        boolean alreadyInQueue = queueRepository.existsByUserIdAndDealerIdAndCylinderTypeIdAndStatusIn(
                userId, request.getDealerId(), request.getCylinderTypeId(),
                List.of(QueueStatus.WAITING, QueueStatus.READY_FOR_PICKUP));

        if (alreadyInQueue) {
            throw new DuplicateResourceException("You already have an active queue entry for this dealer and cylinder type");
        }

        String tokenNumber = generateTokenNumber();

        CitizenQueue queue = CitizenQueue.builder()
                .userId(userId)
                .dealerId(request.getDealerId())
                .cylinderTypeId(request.getCylinderTypeId())
                .tokenNumber(tokenNumber)
                .status(QueueStatus.WAITING)
                .build();

        queue = queueRepository.save(queue);

        publishEvent(TOPIC_QUEUE_JOINED, QueueJoinedEvent.builder()
                .queueId(queue.getId())
                .userId(queue.getUserId())
                .dealerId(queue.getDealerId())
                .cylinderTypeId(queue.getCylinderTypeId())
                .tokenNumber(queue.getTokenNumber())
                .build());

        return queueTransformer.toResponse(queue);
    }

    // ── CITIZEN: get my queue entries ──────────────────────────────────────
    public List<QueueResponse> getMyQueues(String userId) {
        return queueRepository.findByUserIdOrderByRequestedAtDesc(userId).stream()
                .map(queueTransformer::toResponse)
                .toList();
    }

    // ── DEALER: get queue for my shop ──────────────────────────────────────
    public List<QueueResponse> getDealerQueue(String dealerId) {
        return queueRepository.findByDealerIdOrderByRequestedAtAsc(dealerId).stream()
                .map(queueTransformer::toResponse)
                .toList();
    }

    // ── DEALER: mark citizen as ready for pickup ──────────────────────────
    @Transactional
    public QueueResponse markReady(String id, String dealerId) {
        CitizenQueue queue = findById(id);
        verifyDealerOwnership(queue, dealerId);

        if (queue.getStatus() != QueueStatus.WAITING) {
            throw new InvalidStateException("Only WAITING entries can be marked as ready. Current: " + queue.getStatus());
        }

        queue.setStatus(QueueStatus.READY_FOR_PICKUP);
        queueRepository.save(queue);

        publishEvent(TOPIC_QUEUE_READY, QueueReadyEvent.builder()
                .queueId(queue.getId())
                .userId(queue.getUserId())
                .dealerId(queue.getDealerId())
                .tokenNumber(queue.getTokenNumber())
                .build());

        return queueTransformer.toResponse(queue);
    }

    // ── DEALER: mark pickup as completed ──────────────────────────────────
    @Transactional
    public QueueResponse markCompleted(String id, String dealerId) {
        CitizenQueue queue = findById(id);
        verifyDealerOwnership(queue, dealerId);

        if (queue.getStatus() != QueueStatus.READY_FOR_PICKUP) {
            throw new InvalidStateException("Only READY_FOR_PICKUP entries can be completed. Current: " + queue.getStatus());
        }

        queue.setStatus(QueueStatus.COMPLETED);
        queue.setFulfilledAt(LocalDateTime.now());
        queueRepository.save(queue);

        // This event is consumed by inventory-service to subtract stock
        publishEvent(TOPIC_QUEUE_COMPLETED, QueueCompletedEvent.builder()
                .queueId(queue.getId())
                .userId(queue.getUserId())
                .dealerId(queue.getDealerId())
                .cylinderTypeId(queue.getCylinderTypeId())
                .tokenNumber(queue.getTokenNumber())
                .build());

        return queueTransformer.toResponse(queue);
    }

    // ── CITIZEN or DEALER: cancel a queue entry ───────────────────────────
    @Transactional
    public QueueResponse cancelQueue(String id, String userId) {
        CitizenQueue queue = findById(id);

        // Either the citizen who owns it or the dealer can cancel
        if (!queue.getUserId().equals(userId) && !queue.getDealerId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to cancel this queue entry");
        }

        if (queue.getStatus() == QueueStatus.COMPLETED || queue.getStatus() == QueueStatus.CANCELLED) {
            throw new InvalidStateException("Cannot cancel a " + queue.getStatus() + " queue entry");
        }

        queue.setStatus(QueueStatus.CANCELLED);
        queueRepository.save(queue);

        publishEvent(TOPIC_QUEUE_CANCELLED, QueueCancelledEvent.builder()
                .queueId(queue.getId())
                .userId(queue.getUserId())
                .dealerId(queue.getDealerId())
                .tokenNumber(queue.getTokenNumber())
                .build());

        return queueTransformer.toResponse(queue);
    }

    // ── Get single queue entry ────────────────────────────────────────────
    public QueueResponse getById(String id) {
        return queueTransformer.toResponse(findById(id));
    }

    // ── Private helpers ───────────────────────────────────────────────────
    private CitizenQueue findById(String id) {
        return queueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found: " + id));
    }

    private void verifyDealerOwnership(CitizenQueue queue, String dealerId) {
        if (!queue.getDealerId().equals(dealerId)) {
            throw new AccessDeniedException("This queue entry does not belong to your shop");
        }
    }

    private String generateTokenNumber() {
        return "TKN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void publishEvent(String topic, Object event) {
        kafkaTemplate.send(topic, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish to {}: {}", topic, ex.getMessage());
                    } else {
                        log.info("Published event to {}", topic);
                    }
                });
    }
}
