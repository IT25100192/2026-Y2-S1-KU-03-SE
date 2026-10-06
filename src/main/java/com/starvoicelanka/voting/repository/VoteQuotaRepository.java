package com.starvoicelanka.voting.repository;

import com.starvoicelanka.voting.entity.VoteQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoteQuotaRepository extends JpaRepository<VoteQuota, Long> {
    Optional<VoteQuota> findByVoterIdAndRoundId(Long voterId, Long roundId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM VoteQuota vq WHERE vq.voter.id = :voterId")
    void deleteByVoterId(@org.springframework.data.repository.query.Param("voterId") Long voterId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM VoteQuota vq WHERE vq.round.id = :roundId")
    void deleteByRoundId(@org.springframework.data.repository.query.Param("roundId") Long roundId);
}
