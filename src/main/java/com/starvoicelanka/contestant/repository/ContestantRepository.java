package com.starvoicelanka.contestant.repository;

import com.starvoicelanka.contestant.entity.Contestant;
import com.starvoicelanka.contestant.entity.ContestantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContestantRepository extends JpaRepository<Contestant, Long>, JpaSpecificationExecutor<Contestant> {
    Optional<Contestant> findBySeasonIdAndFullName(Long seasonId, String fullName);
    long countBySeasonIdAndStatus(Long seasonId, ContestantStatus status);
    long countBySeasonId(Long seasonId);
    List<Contestant> findBySeasonIdOrderByTotalVotesDesc(Long seasonId);
}
