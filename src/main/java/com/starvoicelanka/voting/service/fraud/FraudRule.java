package com.starvoicelanka.voting.service.fraud;

import com.starvoicelanka.voting.dto.VotingDtos;
import com.starvoicelanka.voting.entity.Vote;

import java.util.List;

/**
 * STRATEGY PATTERN (Strategy interface) - Voting module.
 *
 * Each fraud rule is one interchangeable algorithm for the same task:
 * "look at the votes of a round and raise flags". VotingService (the Context)
 * only knows this interface, so a new rule is added by writing a new class,
 * with no change to VotingService.
 */
public interface FraudRule {

    /** Name of the rule, e.g. SHARED_IP. */
    String name();

    /** Inspect the votes of one round and return a flag for every problem found. */
    List<VotingDtos.FraudFlagDto> check(List<Vote> votes);
}
