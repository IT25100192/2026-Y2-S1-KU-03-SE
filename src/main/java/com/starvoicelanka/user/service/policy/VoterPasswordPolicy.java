package com.starvoicelanka.user.service.policy;

import com.starvoicelanka.common.exception.ValidationException;

/** Concrete Strategy 1 - ordinary voters: 8 to 100 characters. */
public class VoterPasswordPolicy implements PasswordPolicy {

    @Override
    public void validate(String password) {
        if (password == null || password.isEmpty()) {
            throw new ValidationException("Password is required");
        }
        if (password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters");
        }
        if (password.length() > 100) {
            throw new ValidationException("Password must be at most 100 characters");
        }
    }
}
