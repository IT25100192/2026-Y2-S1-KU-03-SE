package com.starvoicelanka.contestant;

import com.starvoicelanka.common.exception.ConflictException;
import com.starvoicelanka.contestant.entity.Season;
import com.starvoicelanka.contestant.service.ContestantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class SeasonCrudTests {

    @Autowired
    private ContestantService contestantService;

    @Test
    void createUpdateAndDeleteSeason() {
        Season created = contestantService.createSeason("Test Season", 2027, false);
        assertThat(created.getId()).isNotNull();

        Season updated = contestantService.updateSeason(created.getId(), "Test Season Renamed", 2028);
        assertThat(updated.getName()).isEqualTo("Test Season Renamed");
        assertThat(updated.getYear()).isEqualTo(2028);

        contestantService.deleteSeason(created.getId());
        assertThat(contestantService.listSeasons()).noneMatch(s -> s.getId().equals(created.getId()));
    }

    @Test
    void switchingCurrentSeasonKeepsOnlyOneCurrent() {
        Season other = contestantService.createSeason("Another Season", 2027, false);
        contestantService.setCurrentSeason(other.getId());

        assertThat(contestantService.getCurrentSeason().getId()).isEqualTo(other.getId());
        assertThat(contestantService.listSeasons().stream().filter(Season::isCurrent).count()).isEqualTo(1);
    }

    @Test
    void currentSeasonCannotBeDeleted() {
        Season current = contestantService.getCurrentSeason();
        assertThatThrownBy(() -> contestantService.deleteSeason(current.getId()))
                .isInstanceOf(ConflictException.class);
    }
}
