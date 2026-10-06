package com.starvoicelanka.sponsor.repository;

import com.starvoicelanka.sponsor.entity.AgreementStatus;
import com.starvoicelanka.sponsor.entity.SponsorshipAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SponsorshipAgreementRepository extends JpaRepository<SponsorshipAgreement, Long>, JpaSpecificationExecutor<SponsorshipAgreement> {

    Optional<SponsorshipAgreement> findByAgreementNo(String agreementNo);

    Optional<SponsorshipAgreement> findByRenewedFromId(Long renewedFromId);

    @Query("SELECT a FROM SponsorshipAgreement a WHERE a.sponsor.id = :sponsorId AND a.season.id = :seasonId AND a.status IN :statuses")
    Optional<SponsorshipAgreement> findClash(
            @Param("sponsorId") Long sponsorId,
            @Param("seasonId") Long seasonId,
            @Param("statuses") Collection<AgreementStatus> statuses
    );

    @Query("SELECT a FROM SponsorshipAgreement a JOIN FETCH a.sponsor JOIN FETCH a.packageRef WHERE a.status = :status AND a.startsAt <= :now AND a.endsAt >= :now")
    List<SponsorshipAgreement> findActiveAgreementsInWindow(
            @Param("status") AgreementStatus status,
            @Param("now") LocalDateTime now
    );

    List<SponsorshipAgreement> findByStatusAndEndsAtBefore(AgreementStatus status, LocalDateTime now);

    boolean existsByPackageRefId(Long packageId);

    List<SponsorshipAgreement> findBySponsorId(Long sponsorId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE SponsorshipAgreement a SET a.renewedFrom = null WHERE a.renewedFrom.id = :agreementId")
    void nullifyRenewedFrom(@Param("agreementId") Long agreementId);
}
