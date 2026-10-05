package com.starvoicelanka.web;

import com.starvoicelanka.common.exception.UnauthorizedException;
import com.starvoicelanka.config.AppProperties;
import com.starvoicelanka.contestant.dto.ContestantDtos;
import com.starvoicelanka.contestant.entity.Round;
import com.starvoicelanka.contestant.entity.RoundStatus;
import com.starvoicelanka.contestant.entity.Season;
import com.starvoicelanka.contestant.service.ContestantService;
import com.starvoicelanka.notification.entity.Notification;
import com.starvoicelanka.notification.entity.NotificationPreference;
import com.starvoicelanka.notification.service.NotificationService;
import com.starvoicelanka.payment.dto.PaymentDtos;
import com.starvoicelanka.payment.entity.CreditAccount;
import com.starvoicelanka.payment.entity.Payment;
import com.starvoicelanka.payment.entity.PaymentMethod;
import com.starvoicelanka.payment.entity.VoteBundle;
import com.starvoicelanka.payment.service.PaymentService;
import com.starvoicelanka.user.entity.User;
import com.starvoicelanka.user.service.UserService;
import com.starvoicelanka.voting.dto.VotingDtos;
import com.starvoicelanka.voting.entity.Vote;
import com.starvoicelanka.voting.entity.VoteQuota;
import com.starvoicelanka.voting.service.VotingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;

@Controller
public class AppWebController {

    private final ContestantService contestantService;
    private final VotingService votingService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final UserService userService;
    private final AppProperties properties;

    public AppWebController(ContestantService contestantService,
                            VotingService votingService,
                            PaymentService paymentService,
                            NotificationService notificationService,
                            UserService userService,
                            AppProperties properties) {
        this.contestantService = contestantService;
        this.votingService = votingService;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
        this.userService = userService;
        this.properties = properties;
    }

    private User getAuthenticatedUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedException("You need to be logged in");
        }
        return userService.getUserByEmail(userDetails.getUsername());
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getAuthenticatedUser(userDetails);
        Season season = null;
        try {
            season = contestantService.getCurrentSeason();
        } catch (Exception ignored) {}

        List<Round> rounds = season != null ? contestantService.listRounds(season.getId()) : Collections.emptyList();
        Round round = rounds.stream().filter(r -> r.getStatus() == RoundStatus.OPEN).findFirst().orElse(null);

        CreditAccount account = paymentService.getAccount(user.getId());
        Page<Vote> votesPage = votingService.listVotes(user.getId(), null, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")));
        long unread = notificationService.countUnread(user.getId());

        int freeLeft = 0;
        if (round != null) {
            VoteQuota quota = votingService.getQuota(user.getId(), round.getId());
            freeLeft = Math.max(0, properties.getFreeVotesPerRound() - quota.getFreeVotesUsed());
        }

        model.addAttribute("title", "Dashboard");
        model.addAttribute("round", round);
        model.addAttribute("account", account);
        model.addAttribute("votes", votesPage.getContent());
        model.addAttribute("voteCount", votesPage.getTotalElements());
        model.addAttribute("unread", unread);
        model.addAttribute("freeLeft", freeLeft);
        return "app/dashboard";
    }

    @GetMapping("/vote/{roundId}")
    public String votePage(
            @PathVariable("roundId") Long roundId,
            @RequestParam(name = "contestant", required = false) Long selectedContestantId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        User user = getAuthenticatedUser(userDetails);
        Round round = contestantService.getRound(roundId);
        ContestantDtos.RoundLineUpDto lineUp = contestantService.getRoundLineUpDto(roundId);
        VotingDtos.EligibilityDto eligibility = votingService.checkEligibility(user.getId(), roundId, 1);
        VotingDtos.RoundTallyDto tally = votingService.getRoundTally(roundId);

        model.addAttribute("title", "Vote - " + round.getName());
        model.addAttribute("round", round);
        model.addAttribute("entries", lineUp.getEntries());
        model.addAttribute("standings", tally.getStandings());
        model.addAttribute("eligibility", eligibility);
        model.addAttribute("selected", selectedContestantId);
        return "app/vote";
    }

    @PostMapping("/vote")
    public String voteSubmit(
            @RequestParam("roundId") Long roundId,
            @RequestParam("contestantId") Long contestantId,
            @RequestParam(name = "count", defaultValue = "1") int count,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest servletRequest,
            RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(userDetails);
        try {
            String ip = getClientIp(servletRequest);
            String userAgent = servletRequest.getHeader("User-Agent");
            votingService.castVote(user.getId(), roundId, contestantId, count, ip, userAgent);
            redirectAttributes.addFlashAttribute("ok", "Votes cast. Thanks for tuning in.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/vote/" + roundId;
    }

    @GetMapping("/my/votes")
    public String myVotes(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(name = "page", defaultValue = "1") int page,
            Model model) {
        User user = getAuthenticatedUser(userDetails);
        int pageIndex = Math.max(0, page - 1);
        Page<Vote> votesPage = votingService.listVotes(user.getId(), null, PageRequest.of(pageIndex, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

        model.addAttribute("title", "My Votes");
        model.addAttribute("items", votesPage.getContent());
        model.addAttribute("total", votesPage.getTotalElements());
        model.addAttribute("page", page);
        model.addAttribute("limit", 20);
        return "app/my-votes";
    }

    /* ---- Voting: update (change) and delete (withdraw) own votes --------- */

    @GetMapping("/my/votes/{id}/change")
    public String changeVotePage(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes,
            Model model) {
        User user = getAuthenticatedUser(userDetails);
        try {
            Vote vote = votingService.getVote(id);
            if (!vote.getVoter().getId().equals(user.getId())) {
                throw new com.starvoicelanka.common.exception.ForbiddenException("You can only change your own votes");
            }
            if (vote.isVoided() || vote.getRound().getStatus() != RoundStatus.OPEN) {
                throw new com.starvoicelanka.common.exception.BadRequestException("This vote can no longer be changed");
            }
            ContestantDtos.RoundLineUpDto lineUp = contestantService.getRoundLineUpDto(vote.getRound().getId());
            model.addAttribute("title", "Change vote");
            model.addAttribute("vote", vote);
            model.addAttribute("entries", lineUp.getEntries());
            return "app/vote-edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/my/votes";
        }
    }

    @PostMapping("/my/votes/{id}/change")
    public String changeVote(
            @PathVariable("id") Long id,
            @RequestParam("contestantId") Long contestantId,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(userDetails);
        try {
            votingService.changeVote(user.getId(), id, contestantId);
            redirectAttributes.addFlashAttribute("ok", "Your vote has been moved to the new contestant.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/my/votes";
    }

    @PostMapping("/my/votes/{id}/withdraw")
    public String withdrawVote(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(userDetails);
        try {
            votingService.retractVote(user.getId(), id);
            redirectAttributes.addFlashAttribute("ok", "Vote withdrawn. Any credits or free votes were returned to you.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/my/votes";
    }

    @GetMapping("/my/wallet")
    public String myWallet(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getAuthenticatedUser(userDetails);
        CreditAccount account = paymentService.getAccount(user.getId());
        List<VoteBundle> bundles = paymentService.listBundles();

        model.addAttribute("title", "Wallet");
        model.addAttribute("account", account);
        model.addAttribute("bundles", bundles);
        return "app/wallet";
    }

    @PostMapping("/my/wallet/purchase")
    public String purchaseSubmit(
            @RequestParam("bundleCode") String bundleCode,
            @RequestParam(name = "method", defaultValue = "CARD") PaymentMethod method,
            @RequestParam(name = "card", required = false) String card,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(userDetails);
        try {
            PaymentDtos.PurchaseResultDto result = paymentService.purchaseBundle(user.getId(), bundleCode, method, card);
            redirectAttributes.addFlashAttribute("ok", "Purchase complete. New balance: " + result.balance() + " credits.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/my/wallet";
    }

    @GetMapping("/my/payments")
    public String myPayments(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(name = "page", defaultValue = "1") int page,
            Model model) {
        User user = getAuthenticatedUser(userDetails);
        int pageIndex = Math.max(0, page - 1);
        Page<Payment> pageResult = paymentService.listPayments(user.getId(), null, PageRequest.of(pageIndex, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

        model.addAttribute("title", "Payments");
        model.addAttribute("items", pageResult.getContent());
        model.addAttribute("total", pageResult.getTotalElements());
        model.addAttribute("page", page);
        model.addAttribute("limit", 20);
        return "app/payments";
    }

    @GetMapping("/my/payments/{id}/receipt")
    public String paymentReceipt(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        User user = getAuthenticatedUser(userDetails);
        PaymentDtos.ReceiptDto receipt = paymentService.getReceipt(id, user);

        model.addAttribute("title", "Receipt " + receipt.receiptNo());
        model.addAttribute("receipt", receipt);
        model.addAttribute("paymentId", id);
        return "app/receipt";
    }

    @GetMapping("/my/payments/{id}/receipt.pdf")
    public ResponseEntity<byte[]> paymentReceiptPdf(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        PaymentDtos.ReceiptDto receipt = paymentService.getReceipt(id, user);
        byte[] pdfBytes = paymentService.renderReceiptPdf(id, user);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + receipt.receiptNo() + ".pdf\"")
                .body(pdfBytes);
    }

    @GetMapping("/my/notifications")
    public String myNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(name = "page", defaultValue = "1") int page,
            Model model) {
        User user = getAuthenticatedUser(userDetails);
        int pageIndex = Math.max(0, page - 1);
        Page<Notification> pageResult = notificationService.listForUser(user.getId(), PageRequest.of(pageIndex, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

        model.addAttribute("title", "Notifications");
        model.addAttribute("items", pageResult.getContent());
        model.addAttribute("total", pageResult.getTotalElements());
        model.addAttribute("page", page);
        model.addAttribute("limit", 20);
        return "app/notifications";
    }

    @PostMapping("/my/notifications/{id}/read")
    public String markRead(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {
        User user = getAuthenticatedUser(userDetails);
        notificationService.markRead(user.getId(), id);
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/my/notifications");
    }

    @PostMapping("/my/notifications/read-all")
    public String markAllRead(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        notificationService.markAllRead(user.getId());
        return "redirect:/my/notifications";
    }

    @GetMapping("/my/preferences")
    public String preferences(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getAuthenticatedUser(userDetails);
        NotificationPreference prefs = notificationService.getPreferences(user.getId());

        model.addAttribute("title", "Notification Preferences");
        model.addAttribute("prefs", prefs);
        return "app/preferences";
    }

    @PostMapping("/my/preferences")
    public String updatePreferences(
            @RequestParam(name = "email", defaultValue = "false") boolean email,
            @RequestParam(name = "sms", defaultValue = "false") boolean sms,
            @RequestParam(name = "inApp", defaultValue = "false") boolean inApp,
            @RequestParam(name = "announcements", defaultValue = "false") boolean announcements,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        User user = getAuthenticatedUser(userDetails);
        NotificationPreference prefs = notificationService.updatePreferences(user.getId(), email, sms, inApp, announcements);

        model.addAttribute("title", "Notification Preferences");
        model.addAttribute("prefs", prefs);
        model.addAttribute("ok", "Preferences saved.");
        return "app/preferences";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getAuthenticatedUser(userDetails);
        model.addAttribute("title", "Profile");
        model.addAttribute("user", user);
        return "app/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @RequestParam("fullName") String fullName,
            @RequestParam("mobile") String mobile,
            @RequestParam(name = "nic", required = false) String nic,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes,
            Model model) {
        User user = getAuthenticatedUser(userDetails);
        try {
            User updated = userService.updateProfile(user.getId(), fullName, mobile, nic);
            model.addAttribute("title", "Profile");
            model.addAttribute("user", updated);
            model.addAttribute("ok", "Profile updated.");
            return "app/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/profile";
        }
    }

    @PostMapping("/profile/password")
    public String changePassword(
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(userDetails);
        try {
            userService.changePassword(user.getId(), currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("ok", "Password changed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/close")
    public String closeAccount(
            @RequestParam("confirmPassword") String confirmPassword,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(userDetails);
        try {
            String ip = getClientIp(request);
            userService.closeAccount(user.getId(), user, "Closed by account holder from the web app", ip);
            request.getSession().invalidate();
            redirectAttributes.addFlashAttribute("ok", "Your account has been closed.");
            return "redirect:/";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/profile";
        }
    }
}
