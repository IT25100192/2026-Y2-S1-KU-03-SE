package com.starvoicelanka.common.validation;

import com.starvoicelanka.common.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Shared input validation for the whole project.
 *
 * Every service calls these helpers, so the same rules protect the REST API
 * and the web forms. A failed check throws ValidationException (HTTP 400) with
 * a plain message, which the web controllers show as a red message on the page
 * and the API returns as JSON.
 */
public final class InputValidator {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");
    /** Sri Lankan mobile: 07XXXXXXXX or +947XXXXXXXX */
    private static final Pattern SL_MOBILE = Pattern.compile("^(?:\\+94|0)7\\d{8}$");
    /** Any phone (mobile or landline): 9 to 15 digits, optional leading + */
    private static final Pattern PHONE = Pattern.compile("^\\+?\\d{9,15}$");
    /** Old NIC (9 digits + V/X) or new NIC (12 digits) */
    private static final Pattern NIC = Pattern.compile("^(?:\\d{9}[VvXx]|\\d{12})$");
    private static final Pattern CODE = Pattern.compile("^[A-Za-z0-9_\\-]+$");
    private static final Pattern CARD = Pattern.compile("^\\d{12,19}$");

    private InputValidator() {
    }

    /* ---- text ------------------------------------------------------------ */

    /** Required text: not blank, trimmed length between min and max. Returns the trimmed value. */
    public static String requireText(String value, String field, int min, int max) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(field + " is required");
        }
        String v = value.trim();
        if (v.length() < min) {
            throw new ValidationException(field + " must be at least " + min + " characters");
        }
        if (v.length() > max) {
            throw new ValidationException(field + " must be at most " + max + " characters");
        }
        return v;
    }

    /** Optional text: null or blank is fine (returns null), otherwise it must fit max. */
    public static String optionalText(String value, String field, int max) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String v = value.trim();
        if (v.length() > max) {
            throw new ValidationException(field + " must be at most " + max + " characters");
        }
        return v;
    }

    /** A short code such as a bundle code: letters, digits, dash and underscore only. */
    public static String requireCode(String value, String field, int max) {
        String v = requireText(value, field, 2, max);
        if (!CODE.matcher(v).matches()) {
            throw new ValidationException(field + " can only contain letters, digits, - and _");
        }
        return v;
    }

    /* ---- contact details ------------------------------------------------- */

    public static String requireEmail(String value, String field) {
        return requireEmail(value, field, 180);
    }

    public static String requireEmail(String value, String field, int max) {
        String v = requireText(value, field, 5, max);
        if (!EMAIL.matcher(v).matches()) {
            throw new ValidationException("Enter a valid " + field.toLowerCase() + " (for example name@example.com)");
        }
        return v.toLowerCase();
    }

    public static String requireMobile(String value, String field) {
        String v = requireText(value, field, 9, 25).replaceAll("[\\s\\-]", "");
        if (!SL_MOBILE.matcher(v).matches()) {
            throw new ValidationException(field + " must be a Sri Lankan mobile number like 0771234567");
        }
        return v;
    }

    /** Optional phone (mobile or landline). */
    public static String optionalPhone(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String v = value.trim().replaceAll("[\\s\\-()]", "");
        if (!PHONE.matcher(v).matches()) {
            throw new ValidationException(field + " must be 9 to 15 digits, for example 0112345678");
        }
        return v;
    }

    public static String optionalNic(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String v = value.trim();
        if (!NIC.matcher(v).matches()) {
            throw new ValidationException("NIC must be 9 digits followed by V or X, or 12 digits");
        }
        return v.toUpperCase();
    }

    /** Optional link: must start with http://, https:// or / (a path inside this site). */
    public static String optionalUrl(String value, String field, int max) {
        String v = optionalText(value, field, max);
        if (v == null) {
            return null;
        }
        String lower = v.toLowerCase();
        if (!(lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("/"))) {
            throw new ValidationException(field + " must start with http:// or https://");
        }
        if (v.contains(" ")) {
            throw new ValidationException(field + " cannot contain spaces");
        }
        return v;
    }

    /* ---- numbers --------------------------------------------------------- */

    public static int requireRange(Integer value, String field, int min, int max) {
        if (value == null) {
            throw new ValidationException(field + " is required");
        }
        if (value < min || value > max) {
            throw new ValidationException(field + " must be between " + min + " and " + max);
        }
        return value;
    }

    /** Optional whole number with a range; null means "not given". */
    public static Integer optionalRange(Integer value, String field, int min, int max) {
        if (value == null) {
            return null;
        }
        return requireRange(value, field, min, max);
    }

    public static double requireMoney(Double value, String field, boolean allowZero, double max) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            throw new ValidationException(field + " is required");
        }
        if (value < 0 || (!allowZero && value == 0)) {
            throw new ValidationException(field + (allowZero ? " cannot be negative" : " must be more than zero"));
        }
        if (value > max) {
            throw new ValidationException(field + " is too large (maximum " + (long) max + ")");
        }
        return value;
    }

    /* ---- other ----------------------------------------------------------- */

    public static Long requireId(Long value, String field) {
        if (value == null || value < 1) {
            throw new ValidationException("Choose a valid " + field);
        }
        return value;
    }

    public static void requireDateOrder(LocalDateTime start, LocalDateTime end, String startField, String endField) {
        if (start != null && end != null && !end.isAfter(start)) {
            throw new ValidationException(endField + " must be after " + startField);
        }
    }

    /** Card number: digits only (spaces and dashes are ignored), 12 to 19 digits. Returns digits only. */
    public static String requireCardNumber(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException("Card number is required for card payments");
        }
        String digits = value.replaceAll("[\\s\\-]", "");
        if (!CARD.matcher(digits).matches()) {
            throw new ValidationException("Card number must be 12 to 19 digits");
        }
        return digits;
    }
}
