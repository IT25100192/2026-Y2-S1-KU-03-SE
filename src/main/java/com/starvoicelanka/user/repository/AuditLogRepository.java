package com.starvoicelanka.user.repository;

import com.starvoicelanka.user.entity.AuditAction;
import com.starvoicelanka.user.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    Page<AuditLog> findByTargetUserId(Long targetUserId, Pageable pageable);
    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE AuditLog a SET a.targetUser = null WHERE a.targetUser.id = :userId")
    void nullifyTargetUser(@org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE AuditLog a SET a.actor = null WHERE a.actor.id = :userId")
    void nullifyActor(@org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM AuditLog a WHERE a.targetUser.id = :userId")
    void deleteByTargetUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM AuditLog a WHERE a.actor.id = :userId")
    void deleteByActorId(@org.springframework.data.repository.query.Param("userId") Long userId);
}
