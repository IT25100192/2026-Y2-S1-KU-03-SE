package com.starvoicelanka.payment.repository;

import com.starvoicelanka.payment.entity.Payment;
import com.starvoicelanka.payment.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {
    Optional<Payment> findByReceiptNo(String receiptNo);
    Optional<Payment> findByReference(String reference);
    Optional<Payment> findByWebhookEventId(String webhookEventId);
    Page<Payment> findByUserId(Long userId, Pageable pageable);
    Page<Payment> findByUserIdAndStatus(Long userId, PaymentStatus status, Pageable pageable);
    List<Payment> findByCreatedAtBetweenAndStatusNot(LocalDateTime start, LocalDateTime end, PaymentStatus status);

    boolean existsByBundleId(Long bundleId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM Payment p WHERE p.user.id = :userId")
    void deleteByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}
