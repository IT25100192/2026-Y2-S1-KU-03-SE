package com.starvoicelanka.contestant.service.transition;

import com.starvoicelanka.contestant.entity.RoundStatus;

/** A CLOSED round can have its results published, or be re-opened. */
public class ClosedTransition implements RoundTransition {
    @Override
    public boolean canMoveTo(RoundStatus target) {
        return target == RoundStatus.RESULTS_PUBLISHED || target == RoundStatus.OPEN;
    }
}
