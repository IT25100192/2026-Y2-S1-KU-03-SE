package com.starvoicelanka.sponsor.repository;

import com.starvoicelanka.sponsor.entity.InvoiceStatus;
import com.starvoicelanka.sponsor.entity.SponsorInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SponsorInvoiceRepository extends JpaRepository<SponsorInvoice, Long>, JpaSpecificationExecutor<SponsorInvoice> {

    Optional<SponsorInvoice> findByInvoiceNo(String invoiceNo);

    List<SponsorInvoice> findByAgreementIdOrderByIssuedAtAsc(Long agreementId);

    List<SponsorInvoice> findByAgreementIdAndStatusNot(Long agreementId, InvoiceStatus status);

    List<SponsorInvoice> findByStatusAndDueAtBefore(InvoiceStatus status, LocalDateTime now);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM SponsorInvoice i WHERE i.agreement.id = :agreementId")
    void deleteByAgreementId(@Param("agreementId") Long agreementId);
}
