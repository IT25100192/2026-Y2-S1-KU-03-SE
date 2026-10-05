package com.starvoicelanka.common.helper;

import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

@Component("helpers")
public class WebHelpers {

    private static final Locale LK_LOCALE = new Locale("en", "LK");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.UK);
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.UK);

    private static final Map<String, String> STATUS_TONE = Map.ofEntries(
            Map.entry("OPEN", "good"),
            Map.entry("ACTIVE", "good"),
            Map.entry("SUCCESS", "good"),
            Map.entry("SENT", "good"),
            Map.entry("PAID", "good"),
            Map.entry("ADVANCED", "good"),
            Map.entry("WINNER", "good"),
            Map.entry("DRAFT", "muted"),
            Map.entry("PENDING", "warn"),
            Map.entry("QUEUED", "muted"),
            Map.entry("REGISTERED", "muted"),
            Map.entry("ISSUED", "muted"),
            Map.entry("CLOSED", "warn"),
            Map.entry("OVERDUE", "warn"),
            Map.entry("RESULTS_PUBLISHED", "info"),
            Map.entry("SUSPENDED", "bad"),
            Map.entry("FAILED", "bad"),
            Map.entry("REFUNDED", "bad"),
            Map.entry("ELIMINATED", "bad"),
            Map.entry("TERMINATED", "bad"),
            Map.entry("EXPIRED", "bad"),
            Map.entry("CANCELLED", "bad")
    );

    public String money(Number n) {
        if (n == null) return "-";
        return "LKR " + NumberFormat.getNumberInstance(LK_LOCALE).format(n);
    }

    public String number(Number n) {
        if (n == null) return "0";
        return NumberFormat.getNumberInstance(LK_LOCALE).format(n);
    }

    public String date(LocalDateTime d) {
        if (d == null) return "-";
        return d.format(DATE_FMT);
    }

    public String dateTime(LocalDateTime d) {
        if (d == null) return "-";
        return d.format(DATE_TIME_FMT);
    }

    public String date(Date d) {
        if (d == null) return "-";
        return date(d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
    }

    public String dateTime(Date d) {
        if (d == null) return "-";
        return dateTime(d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
    }

    public String ago(LocalDateTime d) {
        if (d == null) return "-";
        Duration duration = Duration.between(d, LocalDateTime.now());
        long seconds = duration.getSeconds();
        if (seconds < 60) return "just now";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        long days = hours / 24;
        if (days < 30) return days + " day" + (days == 1 ? "" : "s") + " ago";
        long months = days / 30;
        if (months < 12) return months + " month" + (months == 1 ? "" : "s") + " ago";
        long years = months / 12;
        return years + " year" + (years == 1 ? "" : "s") + " ago";
    }

    public String tone(Object status) {
        if (status == null) return "muted";
        String s = (status instanceof Enum<?>) ? ((Enum<?>) status).name() : status.toString();
        return STATUS_TONE.getOrDefault(s.toUpperCase(), "muted");
    }

    public String titleCase(Object s) {
        if (s == null) return "";
        String str = (s instanceof Enum<?>) ? ((Enum<?>) s).name() : s.toString();
        if (str.isEmpty()) return "";
        String[] words = str.replace('_', ' ').toLowerCase().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    public String attr(Object s) {
        if (s == null) return "";
        return s.toString().replace("\"", "&quot;");
    }
}
