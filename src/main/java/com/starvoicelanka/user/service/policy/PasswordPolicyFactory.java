package com.starvoicelanka.user.service.policy;

import com.starvoicelanka.user.entity.Role;

/**
 * FACTORY PATTERN - User module.
 *
 * Gives back the PasswordPolicy for a role, so UserService does not need an
 * if-else on the role. A new role only needs a new policy class and one case here.
 */
public class PasswordPolicyFactory {

    public PasswordPolicy createPolicy(Role role) {
        if (role == null) {
            return new VoterPasswordPolicy();
        }
        switch (role) {
            case ADMIN:
            case SPONSOR_MANAGER:
                return new StaffPasswordPolicy();
            case VOTER:
            default:
                return new VoterPasswordPolicy();
        }
    }
}
