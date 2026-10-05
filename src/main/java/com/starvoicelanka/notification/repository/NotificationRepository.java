package com.starvoicelanka.notification.repository;

import com.starvoicelanka.notification.entity.Notification;
import com.starvoicelanka.notification.entity.NotificationChannel;
import com.starvoicelanka.notification.entity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    Page<Notification> findByUserIdAndChannel(Long userId, NotificationChannel channel, Pageable pageable);

    Page<Notification> findByUserIdAndChannelAndReadAtIsNull(Long userId, NotificationChannel channel, Pageable pageable);

    long countByUserIdAndChannelAndReadAtIsNull(Long userId, NotificationChannel channel);

    long countByUserIdAndChannel(Long userId, NotificationChannel channel);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.user.id = :userId AND n.readAt IS NULL")
    int markAllAsReadForUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.abandonedAt IS NULL AND n.attempts < :maxAttempts AND (n.nextAttemptAt IS NULL OR n.nextAttemptAt <= :now)")
    List<Notification> findDueForRetry(@Param("status") NotificationStatus status, @Param("maxAttempts") int maxAttempts, @Param("now") LocalDateTime now, Pageable pageable);

    Page<Notification> findByStatus(NotificationStatus status, Pageable pageable);

    Page<Notification> findByStatusAndAbandonedAtIsNull(NotificationStatus status, Pageable pageable);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
