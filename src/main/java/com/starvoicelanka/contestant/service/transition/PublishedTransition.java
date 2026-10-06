package com.starvoicelanka.contestant.service.transition;

import com.starvoicelanka.contestant.entity.RoundStatus;

/** Once results are published the round is final. */
public class PublishedTransition implements RoundTransition {
    @Override
    public boolean canMoveTo(RoundStatus target) {
        return false;
    }
}
