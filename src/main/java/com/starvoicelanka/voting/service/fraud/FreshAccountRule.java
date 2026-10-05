package com.starvoicelanka.voting.service.fraud;

import com.starvoicelanka.config.AppProperties;
import com.starvoicelanka.user.entity.User;
import com.starvoicelanka.voting.dto.VotingDtos;
import com.starvoicelanka.voting.entity.Vote;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Concrete Strategy 3 - an account that registered and voted within a few seconds. */
public class FreshAccountRule implements FraudRule {

    private final AppProperties properties;

    public FreshAccountRule(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public String name() {
        return "FRESH_ACCOUNT";
    }

    @Override
    public List<VotingDtos.FraudFlagDto> check(List<Vote> votes) {
        List<VotingDtos.FraudFlagDto> flags = new ArrayList<>();

        Map<Long, List<Vote>> byVoter = new HashMap<>();
        for (Vote v : votes) {
            byVoter.computeIfAbsent(v.getVoter().getId(), k -> new ArrayList<>()).add(v);
        }
        for (Map.Entry<Long, List<Vote>> entry : byVoter.entrySet()) {
            Long voterId = entry.getKey();
            List<Vote> voterVotes = entry.getValue();
            User vUser = voterVotes.get(0).getVoter();
            if (vUser.getCreatedAt() != null) {
                double fastestSec = Double.MAX_VALUE;
                List<Long> freshVoteIds = new ArrayList<>();
                int totalFreshVotes = 0;
                for (Vote v : voterVotes) {
                    long diffSec = Math.abs(Duration.between(vUser.getCreatedAt(), v.getCreatedAt()).toSeconds());
                    if (diffSec < properties.getFraudFreshAccountSeconds()) {
                        fastestSec = Math.min(fastestSec, diffSec);
                        freshVoteIds.add(v.getId());
                        totalFreshVotes += v.getCount();
                    }
                }
                if (!freshVoteIds.isEmpty()) {
                    String desc = String.format("Account voted %ds after it was created, casting %d votes",
                            Math.round(fastestSec), totalFreshVotes);
                    flags.add(new VotingDtos.FraudFlagDto(name(), "LOW", String.valueOf(voterId), desc, List.of(voterId), freshVoteIds, totalFreshVotes));
                }
            }
        }
        return flags;
    }
}
