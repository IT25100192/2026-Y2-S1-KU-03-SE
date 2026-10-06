package com.starvoicelanka.payment.repository;

import com.starvoicelanka.payment.entity.VoteBundle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoteBundleRepository extends JpaRepository<VoteBundle, Long> {
    List<VoteBundle> findByIsActiveTrueOrderByPriceLKRAsc();
    List<VoteBundle> findAllByOrderByPriceLKRAsc();
    Optional<VoteBundle> findByCodeIgnoreCase(String code);
    Optional<VoteBundle> findByCodeIgnoreCaseAndIsActiveTrue(String code);
}
