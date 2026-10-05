package com.starvoicelanka.payment.repository;

import com.starvoicelanka.payment.entity.CreditAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreditAccountRepository extends JpaRepository<CreditAccount, Long> {
    Optional<CreditAccount> findByUserId(Long userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM CreditAccount c WHERE c.user.id = :userId")
    void deleteByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}
