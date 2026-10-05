package com.starvoicelanka.contestant.service.transition;

import com.starvoicelanka.contestant.entity.RoundStatus;

/** An OPEN round can only be closed. */
public class OpenTransition implements RoundTransition {
    @Override
    public boolean canMoveTo(RoundStatus target) {
        return target == RoundStatus.CLOSED;
    }
}
