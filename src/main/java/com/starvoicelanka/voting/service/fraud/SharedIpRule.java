package com.starvoicelanka.voting.service.fraud;

import com.starvoicelanka.config.AppProperties;
import com.starvoicelanka.voting.dto.VotingDtos;
import com.starvoicelanka.voting.entity.Vote;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Concrete Strategy 1 - one IP address behind several different accounts. */
public class SharedIpRule implements FraudRule {

    private final AppProperties properties;

    public SharedIpRule(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public String name() {
        return "SHARED_IP";
    }

    @Override
    public List<VotingDtos.FraudFlagDto> check(List<Vote> votes) {
        List<VotingDtos.FraudFlagDto> flags = new ArrayList<>();

        Map<String, List<Vote>> byIp = new HashMap<>();
        for (Vote v : votes) {
            if (v.getIpAddress() != null && !v.getIpAddress().trim().isEmpty()) {
                byIp.computeIfAbsent(v.getIpAddress().trim(), k -> new ArrayList<>()).add(v);
            }
        }
        for (Map.Entry<String, List<Vote>> entry : byIp.entrySet()) {
            String ip = entry.getKey();
            List<Vote> ipVotes = entry.getValue();
            Set<Long> voters = new HashSet<>();
            List<Long> voteIds = new ArrayList<>();
            int totalCount = 0;
            for (Vote v : ipVotes) {
                voters.add(v.getVoter().getId());
                voteIds.add(v.getId());
                totalCount += v.getCount();
            }
            if (voters.size() >= properties.getFraudSharedIpVoters()) {
                String severity = voters.size() >= properties.getFraudSharedIpVoters() * 2 ? "HIGH" : "MEDIUM";
                String desc = String.format("%d accounts voted from %s, casting %d votes", voters.size(), ip, totalCount);
                flags.add(new VotingDtos.FraudFlagDto(name(), severity, ip, desc, new ArrayList<>(voters), voteIds, totalCount));
            }
        }
        return flags;
    }
}
