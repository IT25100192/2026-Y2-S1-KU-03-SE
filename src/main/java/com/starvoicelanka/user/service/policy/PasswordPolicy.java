package com.starvoicelanka.user.service.policy;

/**
 * STRATEGY PATTERN (Strategy interface) - User module.
 *
 * "How strong must the password be?" depends on the role of the account, so
 * each rule set is one interchangeable strategy. UserService (the Context)
 * only calls validate() and never checks the role itself.
 */
public interface PasswordPolicy {

    /** Throws ValidationException when the password is too weak. */
    void validate(String password);
}
