package com.starvoicelanka.voting.service.fraud;

import com.starvoicelanka.config.AppProperties;
import com.starvoicelanka.voting.dto.VotingDtos;
import com.starvoicelanka.voting.entity.Vote;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Concrete Strategy 2 - one account casting faster than a person plausibly can. */
public class VoteBurstRule implements FraudRule {

    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final AppProperties properties;

    public VoteBurstRule(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public String name() {
        return "VOTE_BURST";
    }

    @Override
    public List<VotingDtos.FraudFlagDto> check(List<Vote> votes) {
        List<VotingDtos.FraudFlagDto> flags = new ArrayList<>();

        // Group by (voterId + minute)
        Map<String, List<Vote>> byVoterMinute = new HashMap<>();
        for (Vote v : votes) {
            String minuteKey = v.getVoter().getId() + "@" + v.getCreatedAt().format(MINUTE);
            byVoterMinute.computeIfAbsent(minuteKey, k -> new ArrayList<>()).add(v);
        }
        for (Map.Entry<String, List<Vote>> entry : byVoterMinute.entrySet()) {
            List<Vote> burstVotes = entry.getValue();
            if (burstVotes.size() > properties.getFraudBurstPerMinute()) {
                Vote first = burstVotes.get(0);
                Long voterId = first.getVoter().getId();
                String minute = first.getCreatedAt().format(MINUTE);
                int totalCount = burstVotes.stream().mapToInt(Vote::getCount).sum();
                List<Long> voteIds = burstVotes.stream().map(Vote::getId).toList();
                String severity = burstVotes.size() >= properties.getFraudBurstPerMinute() * 3 ? "HIGH" : "MEDIUM";
                String desc = String.format("%d separate vote submissions in one minute (%d votes) at %s",
                        burstVotes.size(), totalCount, minute);
                flags.add(new VotingDtos.FraudFlagDto(name(), severity, String.valueOf(voterId), desc, List.of(voterId), voteIds, totalCount));
            }
        }
        return flags;
    }
}
