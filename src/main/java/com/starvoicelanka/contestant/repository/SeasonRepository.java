package com.starvoicelanka.contestant.repository;

import com.starvoicelanka.contestant.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SeasonRepository extends JpaRepository<Season, Long> {
    Optional<Season> findByIsCurrentTrue();

    @Modifying
    @Query("UPDATE Season s SET s.isCurrent = false WHERE s.isCurrent = true")
    void unsetCurrentSeasons();
}
