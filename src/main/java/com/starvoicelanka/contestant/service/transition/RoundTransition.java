package com.starvoicelanka.contestant.service.transition;

import com.starvoicelanka.contestant.entity.RoundStatus;

/**
 * Common interface for the rule of one round status:
 * "from this status, which statuses can the round move to?".
 */
public interface RoundTransition {

    boolean canMoveTo(RoundStatus target);
}
