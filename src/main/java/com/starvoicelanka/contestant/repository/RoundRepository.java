package com.starvoicelanka.contestant.repository;

import com.starvoicelanka.contestant.entity.Round;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoundRepository extends JpaRepository<Round, Long> {
    List<Round> findBySeasonIdOrderBySequenceAsc(Long seasonId);
    Optional<Round> findBySeasonIdAndSequence(Long seasonId, int sequence);
    long countBySeasonId(Long seasonId);
}
