package com.starvoicelanka.sponsor.repository;

import com.starvoicelanka.sponsor.entity.Sponsor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SponsorRepository extends JpaRepository<Sponsor, Long>, JpaSpecificationExecutor<Sponsor> {
    Optional<Sponsor> findByCompanyNameIgnoreCase(String companyName);
    boolean existsByCompanyNameIgnoreCase(String companyName);
    Page<Sponsor> findByIsActive(boolean isActive, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Sponsor s SET s.accountManager = null WHERE s.accountManager.id = :userId")
    void nullifyAccountManager(@org.springframework.data.repository.query.Param("userId") Long userId);
}
