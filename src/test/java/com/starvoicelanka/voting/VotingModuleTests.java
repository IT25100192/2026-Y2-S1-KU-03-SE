package com.starvoicelanka.voting;

import com.starvoicelanka.contestant.entity.Contestant;
import com.starvoicelanka.contestant.entity.Round;
import com.starvoicelanka.contestant.repository.ContestantRepository;
import com.starvoicelanka.contestant.repository.RoundRepository;
import com.starvoicelanka.user.entity.User;
import com.starvoicelanka.user.repository.UserRepository;
import com.starvoicelanka.voting.dto.VotingDtos;
import com.starvoicelanka.voting.service.VotingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class VotingModuleTests {

    @Autowired
    private VotingService votingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private ContestantRepository contestantRepository;

    @Test
    void testEligibilityAndCastVote() {
        User voter = userRepository.findByEmailIgnoreCase("voter1@starvoice.lk").orElseThrow();
        Round round = roundRepository.findAll().get(0);
        Contestant contestant = contestantRepository.findAll().get(0);

        VotingDtos.EligibilityDto eligibility = votingService.checkEligibility(voter.getId(), round.getId(), 1);
        assertNotNull(eligibility);
        assertTrue(eligibility.isCanVote());

        Map<String, Object> result = votingService.castVote(
                voter.getId(),
                round.getId(),
                contestant.getId(),
                1,
                "127.0.0.1",
                "junit-agent"
        );

        assertNotNull(result);
        assertEquals(1, result.get("accepted"));
    }

    @Test
    void testGetRoundTally() {
        Round round = roundRepository.findAll().get(0);
        VotingDtos.RoundTallyDto tally = votingService.getRoundTally(round.getId());

        assertNotNull(tally);
        assertNotNull(tally.getStandings());
        assertFalse(tally.getStandings().isEmpty());
        assertTrue(tally.getTotalVotes() > 0);
    }
}
