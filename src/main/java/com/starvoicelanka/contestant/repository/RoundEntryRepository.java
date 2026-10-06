package com.starvoicelanka.contestant.repository;

import com.starvoicelanka.contestant.entity.RoundEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoundEntryRepository extends JpaRepository<RoundEntry, Long> {
    Optional<RoundEntry> findByRoundIdAndContestantId(Long roundId, Long contestantId);
    List<RoundEntry> findByRoundIdOrderByVotesInRoundDesc(Long roundId);
    List<RoundEntry> findByContestantId(Long contestantId);
    long countByRoundId(Long roundId);

    @Modifying
    @Query("UPDATE RoundEntry r SET r.votesInRound = r.votesInRound + :count WHERE r.round.id = :roundId AND r.contestant.id = :contestantId")
    void incrementVotes(@Param("roundId") Long roundId, @Param("contestantId") Long contestantId, @Param("count") int count);

    @Modifying
    @Query("DELETE FROM RoundEntry r WHERE r.contestant.id = :contestantId")
    void deleteByContestantId(@Param("contestantId") Long contestantId);

    @Modifying
    @Query("DELETE FROM RoundEntry r WHERE r.round.id = :roundId")
    void deleteByRoundId(@Param("roundId") Long roundId);
}
