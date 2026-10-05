package com.starvoicelanka.voting.repository;

import com.starvoicelanka.voting.entity.Vote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long>, JpaSpecificationExecutor<Vote> {

    Page<Vote> findByVoterIdAndVoidedFalse(Long voterId, Pageable pageable);

    Page<Vote> findByVoterIdAndRoundIdAndVoidedFalse(Long voterId, Long roundId, Pageable pageable);

    Page<Vote> findByRoundIdAndVoidedFalse(Long roundId, Pageable pageable);

    List<Vote> findByRoundIdAndVoidedFalse(Long roundId);

    @Query("SELECT COUNT(v) FROM Vote v WHERE v.voter.id = :voterId AND v.voided = false")
    long countByVoterIdAndVoidedFalse(@Param("voterId") Long voterId);

    @Query("SELECT v FROM Vote v WHERE v.round.id = :roundId AND v.voided = false AND v.ipAddress IS NOT NULL AND v.ipAddress != ''")
    List<Vote> findNonVoidedWithIpByRoundId(@Param("roundId") Long roundId);

    @Query("SELECT v FROM Vote v JOIN FETCH v.voter WHERE v.round.id = :roundId AND v.voided = false")
    List<Vote> findWithVoterByRoundIdAndVoidedFalse(@Param("roundId") Long roundId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM Vote v WHERE v.voter.id = :voterId")
    void deleteByVoterId(@org.springframework.data.repository.query.Param("voterId") Long voterId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM Vote v WHERE v.contestant.id = :contestantId")
    void deleteByContestantId(@org.springframework.data.repository.query.Param("contestantId") Long contestantId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM Vote v WHERE v.round.id = :roundId")
    void deleteByRoundId(@org.springframework.data.repository.query.Param("roundId") Long roundId);
}
