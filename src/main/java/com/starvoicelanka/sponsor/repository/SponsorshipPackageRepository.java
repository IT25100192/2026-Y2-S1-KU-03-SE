package com.starvoicelanka.sponsor.repository;

import com.starvoicelanka.sponsor.entity.SponsorshipPackage;
import com.starvoicelanka.sponsor.entity.SponsorshipTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SponsorshipPackageRepository extends JpaRepository<SponsorshipPackage, Long> {
    Optional<SponsorshipPackage> findByTier(SponsorshipTier tier);
    List<SponsorshipPackage> findAllByOrderByPriceLKRDesc();
}
