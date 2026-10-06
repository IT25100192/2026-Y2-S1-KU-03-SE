package com.starvoicelanka.contestant.service.transition;

import com.starvoicelanka.contestant.entity.RoundStatus;

/** A DRAFT round can only be opened. */
public class DraftTransition implements RoundTransition {
    @Override
    public boolean canMoveTo(RoundStatus target) {
        return target == RoundStatus.OPEN;
    }
}
