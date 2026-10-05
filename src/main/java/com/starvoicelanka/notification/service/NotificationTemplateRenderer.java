package com.starvoicelanka.notification.service;

import com.starvoicelanka.notification.entity.NotificationChannel;
import com.starvoicelanka.notification.entity.NotificationTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class NotificationTemplateRenderer {

    public record RenderedMessage(String subject, String body, List<NotificationChannel> channels) {}

    public RenderedMessage render(NotificationTemplate template, Map<String, Object> p) {
        if (p == null) p = Map.of();

        return switch (template) {
            case ACCOUNT_VERIFICATION -> new RenderedMessage(
                    "Verify your StarVoice Lanka account",
                    String.format("Hi %s, your verification code is %s. It expires in %s minutes.",
                            p.getOrDefault("fullName", "User"),
                            p.getOrDefault("code", ""),
                            p.getOrDefault("expiresInMinutes", 15)),
                    List.of(NotificationChannel.EMAIL, NotificationChannel.SMS)
            );

            case PASSWORD_RESET -> new RenderedMessage(
                    "Reset your StarVoice Lanka password",
                    String.format("Hi %s, use code %s to reset your password. It expires in %s minutes. If this was not you, ignore this message.",
                            p.getOrDefault("fullName", "User"),
                            p.getOrDefault("code", ""),
                            p.getOrDefault("expiresInMinutes", 15)),
                    List.of(NotificationChannel.EMAIL)
            );

            case VOTE_CONFIRMATION -> new RenderedMessage(
                    "Your vote has been counted",
                    String.format("You cast %s %s vote(s) for %s in %s. You have %s free vote(s) left this round.",
                            p.getOrDefault("voteCount", 1),
                            String.valueOf(p.getOrDefault("voteType", "free")).toLowerCase(),
                            p.getOrDefault("contestantName", "Contestant"),
                            p.getOrDefault("roundName", "Round"),
                            p.getOrDefault("freeVotesLeft", 0)),
                    List.of(NotificationChannel.IN_APP, NotificationChannel.SMS)
            );

            case PAYMENT_RECEIPT -> new RenderedMessage(
                    String.format("Receipt %s - StarVoice Lanka", p.getOrDefault("receiptNo", "")),
                    String.format("Payment of LKR %s received for %s vote credits. Reference %s.",
                            p.getOrDefault("amount", 0),
                            p.getOrDefault("voteCredits", 0),
                            p.getOrDefault("reference", "")),
                    List.of(NotificationChannel.EMAIL, NotificationChannel.IN_APP)
            );

            case REFUND_ISSUED -> new RenderedMessage(
                    String.format("Refund processed - %s", p.getOrDefault("receiptNo", "")),
                    String.format("LKR %s has been refunded to your %s account. Reason: %s.",
                            p.getOrDefault("amount", 0),
                            p.getOrDefault("method", "Card"),
                            p.getOrDefault("reason", "Requested by user")),
                    List.of(NotificationChannel.EMAIL)
            );

            case ROUND_OPENED -> new RenderedMessage(
                    String.format("%s voting is open", p.getOrDefault("roundName", "Round")),
                    String.format("Voting for %s is open until %s. %s contestants are in this round.",
                            p.getOrDefault("roundName", "Round"),
                            p.getOrDefault("closesAt", "the end of the show"),
                            p.getOrDefault("contestantCount", 0)),
                    List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL)
            );

            case ELIMINATION_NOTICE -> new RenderedMessage(
                    String.format("%s results", p.getOrDefault("roundName", "Round")),
                    String.format("%s finished with %s votes and has been %s.",
                            p.getOrDefault("contestantName", "Contestant"),
                            p.getOrDefault("totalVotes", 0),
                            String.valueOf(p.getOrDefault("outcome", "ELIMINATED")).toLowerCase()),
                    List.of(NotificationChannel.IN_APP)
            );

            case SPONSOR_AGREEMENT -> new RenderedMessage(
                    String.format("Sponsorship agreement %s", p.getOrDefault("agreementNo", "")),
                    String.format("%s is confirmed as a %s partner for %s. Contract value LKR %s.",
                            p.getOrDefault("sponsorName", "Partner"),
                            p.getOrDefault("tier", "Partner"),
                            p.getOrDefault("seasonName", "current season"),
                            p.getOrDefault("value", 0)),
                    List.of(NotificationChannel.EMAIL)
            );

            case FRAUD_ALERT -> new RenderedMessage(
                    String.format("Suspicious voting in %s", p.getOrDefault("roundName", "Round")),
                    String.format("The fraud sweep flagged %s vote record(s) in %s across %s rule(s). %s were voided automatically. Review the ledger before results are published.",
                            p.getOrDefault("flagged", 0),
                            p.getOrDefault("roundName", "Round"),
                            p.getOrDefault("rules", 0),
                            p.getOrDefault("voided", 0)),
                    List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL)
            );

            case AGREEMENT_RENEWED -> new RenderedMessage(
                    String.format("Sponsorship renewed - %s", p.getOrDefault("newAgreementNo", "")),
                    String.format("%s has been renewed as a %s partner. Previous agreement %s is now expired. New contract value LKR %s (%s%% uplift).",
                            p.getOrDefault("sponsorName", "Partner"),
                            p.getOrDefault("tier", "Partner"),
                            p.getOrDefault("previousAgreementNo", ""),
                            p.getOrDefault("value", 0),
                            p.getOrDefault("upliftPercent", 0)),
                    List.of(NotificationChannel.EMAIL)
            );

            case SPONSOR_INVOICE -> new RenderedMessage(
                    String.format("Invoice %s - LKR %s", p.getOrDefault("invoiceNo", ""), p.getOrDefault("amount", 0)),
                    String.format("Invoice %s for %s has been issued against agreement %s. Amount LKR %s, due %s.",
                            p.getOrDefault("invoiceNo", ""),
                            p.getOrDefault("sponsorName", "Partner"),
                            p.getOrDefault("agreementNo", ""),
                            p.getOrDefault("amount", 0),
                            p.getOrDefault("dueAt", "")),
                    List.of(NotificationChannel.EMAIL)
            );

            case GENERAL_ANNOUNCEMENT -> new RenderedMessage(
                    String.valueOf(p.getOrDefault("subject", "StarVoice Lanka Notice")),
                    String.valueOf(p.getOrDefault("body", "Important update from StarVoice Lanka.")),
                    List.of(NotificationChannel.EMAIL, NotificationChannel.IN_APP, NotificationChannel.SMS)
            );
        };
    }
}
