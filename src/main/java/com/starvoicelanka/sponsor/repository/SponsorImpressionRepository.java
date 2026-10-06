package com.starvoicelanka.sponsor.repository;

import com.starvoicelanka.sponsor.entity.SponsorImpression;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SponsorImpressionRepository extends JpaRepository<SponsorImpression, Long> {

    Optional<SponsorImpression> findTopByAgreementIdOrderByCreatedAtDesc(Long agreementId);

    @Query("SELECT i FROM SponsorImpression i LEFT JOIN FETCH i.round WHERE i.agreement.id = :agreementId")
    List<SponsorImpression> findByAgreementIdWithRound(@Param("agreementId") Long agreementId);

    long countByAgreementId(Long agreementId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM SponsorImpression i WHERE i.agreement.id = :agreementId")
    void deleteByAgreementId(@Param("agreementId") Long agreementId);

    // round is optional on an impression, so on round deletion we just detach the
    // reference instead of deleting the impression record itself (it still belongs
    // to a sponsorship agreement and should stay for reporting purposes).
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE SponsorImpression i SET i.round = null WHERE i.round.id = :roundId")
    void nullifyRoundId(@Param("roundId") Long roundId);
}
