package com.starvoicelanka.user.service.policy;

import com.starvoicelanka.common.exception.ValidationException;

/** Concrete Strategy 2 - admins and sponsor managers: stricter, 10+ characters with upper, lower and a digit. */
public class StaffPasswordPolicy implements PasswordPolicy {

    @Override
    public void validate(String password) {
        if (password == null || password.isEmpty()) {
            throw new ValidationException("Password is required");
        }
        if (password.length() < 10) {
            throw new ValidationException("Staff passwords must be at least 10 characters");
        }
        if (password.length() > 100) {
            throw new ValidationException("Password must be at most 100 characters");
        }
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) upper = true;
            else if (Character.isLowerCase(c)) lower = true;
            else if (Character.isDigit(c)) digit = true;
        }
        if (!(upper && lower && digit)) {
            throw new ValidationException("Staff passwords need an upper-case letter, a lower-case letter and a digit");
        }
    }
}
