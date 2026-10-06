package com.starvoicelanka.contestant.service.transition;

import com.starvoicelanka.contestant.entity.RoundStatus;

/**
 * FACTORY PATTERN - Contestant module.
 *
 * ContestantService asks the factory for the transition rule of the round's
 * current status and gets back a RoundTransition. It no longer needs a switch
 * on the status, and a new status only needs a new class plus one case here.
 */
public class RoundTransitionFactory {

    public RoundTransition createTransition(RoundStatus current) {
        switch (current) {
            case DRAFT:
                return new DraftTransition();
            case OPEN:
                return new OpenTransition();
            case CLOSED:
                return new ClosedTransition();
            case RESULTS_PUBLISHED:
                return new PublishedTransition();
            default:
                throw new IllegalArgumentException("Unknown round status: " + current);
        }
    }
}
