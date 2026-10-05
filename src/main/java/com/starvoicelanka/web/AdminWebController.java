package com.starvoicelanka.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.starvoicelanka.contestant.entity.Season;
import com.starvoicelanka.contestant.service.ContestantService;
import com.starvoicelanka.notification.entity.Notification;
import com.starvoicelanka.notification.entity.NotificationChannel;
import com.starvoicelanka.notification.entity.NotificationStatus;
import com.starvoicelanka.notification.service.NotificationService;
import com.starvoicelanka.payment.entity.Payment;
import com.starvoicelanka.payment.entity.PaymentStatus;
import com.starvoicelanka.payment.service.PaymentService;
import com.starvoicelanka.sponsor.dto.SponsorDtos;
import com.starvoicelanka.sponsor.entity.*;
import com.starvoicelanka.sponsor.service.SponsorService;
import com.starvoicelanka.user.entity.Role;
import com.starvoicelanka.user.entity.User;
import com.starvoicelanka.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SPONSOR_MANAGER')")
public class AdminWebController {

    private final UserService userService;
    private final ContestantService contestantService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final SponsorService sponsorService;
    private final com.starvoicelanka.voting.service.VotingService votingService;
    private final ObjectMapper mapper;

    public AdminWebController(UserService userService,
                              ContestantService contestantService,
                              PaymentService paymentService,
                              NotificationService notificationService,
                              SponsorService sponsorService,
                              com.starvoicelanka.voting.service.VotingService votingService) {
        this.userService = userService;
        this.contestantService = contestantService;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
        this.sponsorService = sponsorService;
        this.votingService = votingService;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }

    @GetMapping
    public String adminIndex(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null && userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/admin/rounds";
        }
        return "redirect:/admin/agreements";
    }

    /* ---- Seasons ---- */

    @GetMapping("/seasons")
    @PreAuthorize("hasRole('ADMIN')")
    public String seasons(Model model) {
        List<java.util.Map<String, Object>> rows = new java.util.ArrayList<>();
        for (Season s : contestantService.listSeasons()) {
            java.util.Map<String, Object> row = new java.util.HashMap<>();
            row.put("season", s);
            row.put("rounds", contestantService.countRoundsInSeason(s.getId()));
            row.put("contestants", contestantService.countContestantsInSeason(s.getId()));
            rows.add(row);
        }
        model.addAttribute("title", "Seasons - Admin");
        model.addAttribute("section", "Seasons");
        model.addAttribute("rows", rows);
        return "admin/seasons";
    }

    @PostMapping("/seasons")
    @PreAuthorize("hasRole('ADMIN')")
    public String createSeason(@RequestParam("name") String name,
                               @RequestParam("year") int year,
                               @RequestParam(name = "makeCurrent", defaultValue = "false") boolean makeCurrent,
                               RedirectAttributes redirect) {
        try {
            Season created = contestantService.createSeason(name, year, false);
            if (makeCurrent) {
                contestantService.setCurrentSeason(created.getId());
            }
            redirect.addFlashAttribute("ok", "Season '" + created.getName() + "' created.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/seasons";
    }

    @PostMapping("/seasons/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editSeason(@PathVariable("id") Long id,
                             @RequestParam("name") String name,
                             @RequestParam("year") int year,
                             RedirectAttributes redirect) {
        try {
            contestantService.updateSeason(id, name, year);
            redirect.addFlashAttribute("ok", "Season updated.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/seasons";
    }

    @PostMapping("/seasons/{id}/current")
    @PreAuthorize("hasRole('ADMIN')")
    public String makeSeasonCurrent(@PathVariable("id") Long id, RedirectAttributes redirect) {
        try {
            Season s = contestantService.setCurrentSeason(id);
            redirect.addFlashAttribute("ok", "'" + s.getName() + "' is now the current season.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/seasons";
    }

    @PostMapping("/seasons/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteSeason(@PathVariable("id") Long id, RedirectAttributes redirect) {
        try {
            contestantService.deleteSeason(id);
            redirect.addFlashAttribute("ok", "Season deleted.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/seasons";
    }

    @GetMapping("/rounds")
    @PreAuthorize("hasRole('ADMIN')")
    public String rounds(Model model) {
        Season season = null;
        try { season = contestantService.getCurrentSeason(); } catch (Exception ignored) {}
        List<com.starvoicelanka.contestant.entity.Round> rounds = season != null ? contestantService.listRounds(season.getId()) : java.util.Collections.emptyList();
        long activeContestants = season != null ? contestantService.countActiveContestants(season.getId()) : 0;

        model.addAttribute("title", "Rounds - Admin");
        model.addAttribute("section", "Rounds");
        model.addAttribute("season", season);
        model.addAttribute("rounds", rounds);
        model.addAttribute("activeContestants", activeContestants);
        return "admin/rounds";
    }

    @PostMapping("/rounds")
    @PreAuthorize("hasRole('ADMIN')")
    public String createRound(@org.springframework.web.bind.annotation.RequestParam("name") String name,
                              @org.springframework.web.bind.annotation.RequestParam(name = "sequence", defaultValue = "1") int sequence,
                              @org.springframework.web.bind.annotation.RequestParam(name = "advanceCount", defaultValue = "2") int advanceCount,
                              org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        try {
            com.starvoicelanka.contestant.dto.ContestantDtos.CreateRoundRequest req = new com.starvoicelanka.contestant.dto.ContestantDtos.CreateRoundRequest();
            req.setName(name);
            req.setSequence(sequence);
            req.setAdvanceCount(advanceCount);
            contestantService.createRound(req);
            redirect.addFlashAttribute("ok", "Round '" + name + "' created successfully.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/rounds";
    }

    @PostMapping("/rounds/{id}/open")
    @PreAuthorize("hasRole('ADMIN')")
    public String openRound(@PathVariable("id") Long id, org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        try {
            votingService.openRound(id, true);
            redirect.addFlashAttribute("ok", "Round opened successfully! Public voting is now live.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/rounds";
    }

    @PostMapping("/rounds/{id}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public String closeRound(@PathVariable("id") Long id, org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        try {
            votingService.closeRound(id);
            redirect.addFlashAttribute("ok", "Round closed successfully. Voting is now paused.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/rounds";
    }

    @PostMapping("/rounds/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public String publishResults(@PathVariable("id") Long id, org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        try {
            contestantService.publishResults(id);
            redirect.addFlashAttribute("ok", "Results published successfully! Final standings and eliminations processed.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/rounds";
    }

    @GetMapping("/rounds/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String roundDetail(@PathVariable("id") Long id, Model model) {
        com.starvoicelanka.contestant.entity.Round round = contestantService.getRound(id);
        List<com.starvoicelanka.contestant.entity.RoundEntry> entries = contestantService.getRoundLineUp(id);
        Season season = round.getSeason();
        List<com.starvoicelanka.contestant.entity.Contestant> availableContestants = season != null ?
                contestantService.listContestants(season.getId(), com.starvoicelanka.contestant.entity.ContestantStatus.ACTIVE, PageRequest.of(0, 100)).getContent() :
                java.util.Collections.emptyList();

        model.addAttribute("title", round.getName() + " - Admin");
        model.addAttribute("round", round);
        model.addAttribute("entries", entries);
        model.addAttribute("availableContestants", availableContestants);
        return "admin/round-detail";
    }

    @PostMapping("/rounds/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public String assignLineUp(@PathVariable("id") Long id,
                               @org.springframework.web.bind.annotation.RequestParam(name = "contestantIds", required = false) List<Long> contestantIds,
                               org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        try {
            if (contestantIds != null && !contestantIds.isEmpty()) {
                contestantService.assignContestantsToRound(id, contestantIds);
                redirect.addFlashAttribute("ok", "Line-up updated successfully.");
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/rounds/" + id;
    }

    @PostMapping("/rounds/{id}/contestants/{contestantId}/media")
    @PreAuthorize("hasRole('ADMIN')")
    public String uploadMedia(@PathVariable("id") Long id,
                              @PathVariable("contestantId") Long contestantId,
                              @org.springframework.web.bind.annotation.RequestParam("media") org.springframework.web.multipart.MultipartFile file,
                              @org.springframework.web.bind.annotation.RequestParam(value = "performanceTitle", required = false) String performanceTitle,
                              org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        try {
            contestantService.uploadPerformanceMedia(id, contestantId, file, performanceTitle);
            redirect.addFlashAttribute("ok", "Performance media clip uploaded successfully.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/rounds/" + id;
    }

    @PostMapping("/rounds/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteRound(@PathVariable("id") Long id, RedirectAttributes redirect) {
        try {
            contestantService.deleteRound(id);
            redirect.addFlashAttribute("ok", "Round and associated entries/votes deleted successfully.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/rounds";
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String users(
            @org.springframework.web.bind.annotation.RequestParam(name = "search", required = false) String search,
            @org.springframework.web.bind.annotation.RequestParam(name = "role", required = false) com.starvoicelanka.user.entity.Role role,
            @org.springframework.web.bind.annotation.RequestParam(name = "status", required = false) com.starvoicelanka.user.entity.UserStatus status,
            Model model) {
        List<User> users = userService.listUsers(PageRequest.of(0, 100), role, status, search).getContent();
        model.addAttribute("title", "Users - Admin");
        model.addAttribute("section", "Users");
        model.addAttribute("users", users);
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("selectedRole", role != null ? role.name() : "");
        model.addAttribute("selectedStatus", status != null ? status.name() : "");
        return "admin/users";
    }

    @PostMapping("/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public String changeUserStatus(@PathVariable("id") Long id,
                                   @org.springframework.web.bind.annotation.RequestParam("status") com.starvoicelanka.user.entity.UserStatus status,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   jakarta.servlet.http.HttpServletRequest req,
                                   org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        try {
            User actor = userDetails != null ? userService.getUserByEmail(userDetails.getUsername()) : null;
            userService.setStatus(id, status, actor, "Admin status alteration", req.getRemoteAddr());
            redirect.addFlashAttribute("ok", "Account status updated to " + status);
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public String changeUserRole(@PathVariable("id") Long id,
                                 @org.springframework.web.bind.annotation.RequestParam("role") Role role,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 jakarta.servlet.http.HttpServletRequest req,
                                 org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        try {
            User actor = userDetails != null ? userService.getUserByEmail(userDetails.getUsername()) : null;
            userService.changeRole(id, role, actor, "Admin role update", req.getRemoteAddr());
            redirect.addFlashAttribute("ok", "Account role changed to " + role);
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String createUser(
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam(name = "mobile", required = false) String mobile,
            @RequestParam(name = "password", required = false) String password,
            @RequestParam(name = "role", defaultValue = "VOTER") Role role,
            @RequestParam(name = "status", defaultValue = "ACTIVE") com.starvoicelanka.user.entity.UserStatus status,
            @AuthenticationPrincipal UserDetails userDetails,
            jakarta.servlet.http.HttpServletRequest req,
            RedirectAttributes redirect) {
        try {
            User actor = userDetails != null ? userService.getUserByEmail(userDetails.getUsername()) : null;
            User created = userService.createUser(fullName, email, mobile, password, role, status, actor, req.getRemoteAddr());
            redirect.addFlashAttribute("ok", "User account created successfully: " + created.getEmail());
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteUser(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            jakarta.servlet.http.HttpServletRequest req,
            RedirectAttributes redirect) {
        try {
            User actor = userDetails != null ? userService.getUserByEmail(userDetails.getUsername()) : null;
            userService.deleteUser(id, actor, req.getRemoteAddr());
            redirect.addFlashAttribute("ok", "User account successfully deleted.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/contestants")
    @PreAuthorize("hasRole('ADMIN')")
    public String contestants(Model model) {
        List<com.starvoicelanka.contestant.entity.Contestant> contestants = contestantService.listContestants(null, null, PageRequest.of(0, 100)).getContent();
        model.addAttribute("title", "Contestants - Admin");
        model.addAttribute("section", "Contestants");
        model.addAttribute("contestants", contestants);
        return "admin/contestants";
    }

    @PostMapping("/contestants")
    @PreAuthorize("hasRole('ADMIN')")
    public String createContestant(@org.springframework.web.bind.annotation.RequestParam("fullName") String fullName,
                                   @org.springframework.web.bind.annotation.RequestParam(value = "stageName", required = false) String stageName,
                                   @org.springframework.web.bind.annotation.RequestParam(value = "age", required = false) Integer age,
                                   @org.springframework.web.bind.annotation.RequestParam("district") String district,
                                   org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        try {
            com.starvoicelanka.contestant.dto.ContestantDtos.RegisterContestantRequest req = new com.starvoicelanka.contestant.dto.ContestantDtos.RegisterContestantRequest();
            req.setFullName(fullName);
            req.setStageName(stageName);
            req.setAge(age);
            req.setDistrict(district);
            contestantService.registerContestant(req);
            redirect.addFlashAttribute("ok", "Contestant registered successfully.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/contestants";
    }

    @PostMapping("/contestants/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateContestantStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") com.starvoicelanka.contestant.entity.ContestantStatus status,
            RedirectAttributes redirect) {
        try {
            contestantService.setContestantStatus(id, status);
            redirect.addFlashAttribute("ok", "Contestant status updated to " + status);
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/contestants";
    }

    @PostMapping("/contestants/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteContestant(@PathVariable("id") Long id, RedirectAttributes redirect) {
        try {
            contestantService.deleteContestant(id);
            redirect.addFlashAttribute("ok", "Contestant and associated records deleted successfully.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/contestants";
    }

    /* ---- Contestants: update ---------------------------------------------- */

    @GetMapping("/contestants/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editContestantPage(@PathVariable("id") Long id, Model model) {
        model.addAttribute("title", "Edit contestant - Admin");
        model.addAttribute("section", "Contestants");
        model.addAttribute("contestant", contestantService.getContestant(id));
        return "admin/contestant-edit";
    }

    @PostMapping("/contestants/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateContestant(@PathVariable("id") Long id,
                                   @RequestParam("fullName") String fullName,
                                   @RequestParam(value = "stageName", required = false) String stageName,
                                   @RequestParam(value = "age", required = false) Integer age,
                                   @RequestParam("district") String district,
                                   @RequestParam(value = "bio", required = false) String bio,
                                   RedirectAttributes redirect) {
        try {
            if (fullName == null || fullName.isBlank()) throw new IllegalArgumentException("Full name is required");
            if (district == null || district.isBlank()) throw new IllegalArgumentException("District is required");
            if (age != null && (age < 16 || age > 99)) throw new IllegalArgumentException("Age must be between 16 and 99");
            com.starvoicelanka.contestant.dto.ContestantDtos.UpdateContestantRequest req =
                    new com.starvoicelanka.contestant.dto.ContestantDtos.UpdateContestantRequest();
            req.setFullName(fullName.trim());
            req.setStageName(stageName);
            req.setAge(age);
            req.setDistrict(district.trim());
            req.setBio(bio);
            contestantService.updateContestant(id, req);
            redirect.addFlashAttribute("ok", "Contestant details updated successfully.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/contestants/" + id + "/edit";
        }
        return "redirect:/admin/contestants";
    }

    /* ---- Rounds: update and line-up removal ------------------------------- */

    @PostMapping("/rounds/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateRound(@PathVariable("id") Long id,
                              @RequestParam("name") String name,
                              @RequestParam(name = "sequence", required = false) Integer sequence,
                              @RequestParam(name = "advanceCount", required = false) Integer advanceCount,
                              RedirectAttributes redirect) {
        try {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Round name is required");
            contestantService.updateRound(id, name, sequence, advanceCount);
            redirect.addFlashAttribute("ok", "Round updated successfully.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/rounds/" + id;
    }

    @PostMapping("/rounds/{id}/contestants/{contestantId}/remove")
    @PreAuthorize("hasRole('ADMIN')")
    public String removeFromLineUp(@PathVariable("id") Long id,
                                   @PathVariable("contestantId") Long contestantId,
                                   RedirectAttributes redirect) {
        try {
            contestantService.removeFromLineUp(id, contestantId);
            redirect.addFlashAttribute("ok", "Contestant removed from the line-up.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/rounds/" + id;
    }

    /* ---- Votes: read, void (update) and delete ---------------------------- */

    @GetMapping("/votes")
    @PreAuthorize("hasRole('ADMIN')")
    public String votes(@RequestParam(name = "page", defaultValue = "1") int page, Model model) {
        int pageIndex = Math.max(0, page - 1);
        Page<com.starvoicelanka.voting.entity.Vote> votesPage =
                votingService.listAllVotes(PageRequest.of(pageIndex, 50, Sort.by(Sort.Direction.DESC, "createdAt")));
        model.addAttribute("title", "Votes - Admin");
        model.addAttribute("section", "Votes");
        model.addAttribute("votes", votesPage.getContent());
        model.addAttribute("total", votesPage.getTotalElements());
        model.addAttribute("page", page);
        model.addAttribute("limit", 50);
        return "admin/votes";
    }

    @PostMapping("/votes/{id}/void")
    @PreAuthorize("hasRole('ADMIN')")
    public String voidVote(@PathVariable("id") Long id,
                           @RequestParam(name = "reason", defaultValue = "Voided by admin") String reason,
                           RedirectAttributes redirect) {
        try {
            votingService.voidVote(id, reason);
            redirect.addFlashAttribute("ok", "Vote voided and removed from the tally.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/votes";
    }

    @PostMapping("/votes/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteVote(@PathVariable("id") Long id, RedirectAttributes redirect) {
        try {
            votingService.deleteVote(id);
            redirect.addFlashAttribute("ok", "Vote record deleted.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/votes";
    }

    /* ---- Payment bundles: full CRUD --------------------------------------- */

    @GetMapping("/bundles")
    @PreAuthorize("hasRole('ADMIN')")
    public String bundles(Model model) {
        model.addAttribute("title", "Vote bundles - Admin");
        model.addAttribute("section", "Bundles");
        model.addAttribute("bundles", paymentService.listAllBundles());
        return "admin/bundles";
    }

    @PostMapping("/bundles")
    @PreAuthorize("hasRole('ADMIN')")
    public String createBundle(@RequestParam("code") String code,
                               @RequestParam("name") String name,
                               @RequestParam("voteCredits") Integer voteCredits,
                               @RequestParam("priceLKR") Double priceLKR,
                               RedirectAttributes redirect) {
        try {
            if (code == null || code.isBlank()) throw new IllegalArgumentException("Bundle code is required");
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Bundle name is required");
            if (voteCredits == null || voteCredits < 1) throw new IllegalArgumentException("A bundle needs at least 1 vote credit");
            if (priceLKR == null || priceLKR < 0) throw new IllegalArgumentException("Price cannot be negative");
            com.starvoicelanka.payment.dto.PaymentDtos.CreateBundleRequest req =
                    new com.starvoicelanka.payment.dto.PaymentDtos.CreateBundleRequest();
            req.setCode(code);
            req.setName(name);
            req.setVoteCredits(voteCredits);
            req.setPriceLKR(priceLKR);
            req.setIsActive(true);
            paymentService.createBundle(req);
            redirect.addFlashAttribute("ok", "Bundle '" + name + "' created.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/bundles";
    }

    @PostMapping("/bundles/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateBundle(@PathVariable("id") Long id,
                               @RequestParam("name") String name,
                               @RequestParam("voteCredits") Integer voteCredits,
                               @RequestParam("priceLKR") Double priceLKR,
                               RedirectAttributes redirect) {
        try {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Bundle name is required");
            paymentService.updateBundle(id, name, voteCredits, priceLKR, null);
            redirect.addFlashAttribute("ok", "Bundle updated.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/bundles";
    }

    @PostMapping("/bundles/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public String toggleBundle(@PathVariable("id") Long id, RedirectAttributes redirect) {
        try {
            com.starvoicelanka.payment.entity.VoteBundle b = paymentService.getBundle(id);
            paymentService.updateBundle(id, null, null, null, !b.isActive());
            redirect.addFlashAttribute("ok", b.isActive() ? "Bundle deactivated." : "Bundle activated.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/bundles";
    }

    @PostMapping("/bundles/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteBundle(@PathVariable("id") Long id, RedirectAttributes redirect) {
        try {
            paymentService.deleteBundle(id);
            redirect.addFlashAttribute("ok", "Bundle deleted.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/bundles";
    }

    /* ---- Sponsorship packages: create, update, delete --------------------- */

    @PostMapping("/packages")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SPONSOR_MANAGER')")
    public String createPackage(@RequestParam("tier") SponsorshipTier tier,
                                @RequestParam("name") String name,
                                @RequestParam("priceLKR") Double priceLKR,
                                @RequestParam(name = "guaranteedImpressions", defaultValue = "0") int guaranteedImpressions,
                                @RequestParam(name = "bannerSlotsPerRound", defaultValue = "1") int bannerSlotsPerRound,
                                @RequestParam(name = "logoOnLeaderboard", defaultValue = "false") boolean logoOnLeaderboard,
                                @RequestParam(name = "namingRights", defaultValue = "false") boolean namingRights,
                                RedirectAttributes redirect) {
        try {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Package name is required");
            if (priceLKR == null || priceLKR < 0) throw new IllegalArgumentException("Price cannot be negative");
            SponsorDtos.CreatePackageRequest req = new SponsorDtos.CreatePackageRequest();
            req.setTier(tier);
            req.setName(name);
            req.setPriceLKR(priceLKR);
            req.setGuaranteedImpressions(guaranteedImpressions);
            req.setBannerSlotsPerRound(bannerSlotsPerRound);
            req.setLogoOnLeaderboard(logoOnLeaderboard);
            req.setNamingRights(namingRights);
            sponsorService.createPackage(req);
            redirect.addFlashAttribute("ok", "Package '" + name + "' created.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/sponsors";
    }

    @PostMapping("/packages/{id}/edit")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SPONSOR_MANAGER')")
    public String updatePackage(@PathVariable("id") Long id,
                                @RequestParam("name") String name,
                                @RequestParam("priceLKR") Double priceLKR,
                                @RequestParam(name = "guaranteedImpressions", defaultValue = "0") int guaranteedImpressions,
                                @RequestParam(name = "bannerSlotsPerRound", defaultValue = "1") int bannerSlotsPerRound,
                                @RequestParam(name = "logoOnLeaderboard", defaultValue = "false") boolean logoOnLeaderboard,
                                @RequestParam(name = "namingRights", defaultValue = "false") boolean namingRights,
                                RedirectAttributes redirect) {
        try {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Package name is required");
            sponsorService.updatePackage(id, name, priceLKR, guaranteedImpressions, bannerSlotsPerRound, logoOnLeaderboard, namingRights);
            redirect.addFlashAttribute("ok", "Package updated.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/sponsors";
    }

    @PostMapping("/packages/{id}/delete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SPONSOR_MANAGER')")
    public String deletePackage(@PathVariable("id") Long id, RedirectAttributes redirect) {
        try {
            sponsorService.deletePackage(id);
            redirect.addFlashAttribute("ok", "Package deleted.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/sponsors";
    }

    @GetMapping("/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public String audit(Model model) {
        List<com.starvoicelanka.user.entity.AuditLog> logs;
        try {
            logs = userService.listAuditLog(null, null, PageRequest.of(0, 100)).getContent();
        } catch (Exception e) {
            logs = java.util.Collections.emptyList();
        }
        model.addAttribute("title", "Audit Log - Admin");
        model.addAttribute("section", "Audit Log");
        model.addAttribute("logs", logs);
        return "admin/audit";
    }

    @GetMapping("/fraud")
    @PreAuthorize("hasRole('ADMIN')")
    public String fraud(Model model) {
        model.addAttribute("title", "Fraud & Anomalies - Admin");
        model.addAttribute("section", "Fraud & anomalies");
        model.addAttribute("data", "Fraud detection is accessible via API or automated sweeps.");
        return "admin/placeholder";
    }

    @GetMapping("/payments")
    @PreAuthorize("hasRole('ADMIN')")
    public String payments(
            @RequestParam(name = "status", required = false) PaymentStatus status,
            @RequestParam(name = "page", defaultValue = "1") int page,
            Model model) {
        int pageIndex = Math.max(0, page - 1);
        Page<Payment> paymentsPage = paymentService.listPayments(null, status, PageRequest.of(pageIndex, 50, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<Payment> all = paymentService.listPayments(null, null, PageRequest.of(0, 1000)).getContent();
        double totalRevenue = all.stream().filter(p -> p.getStatus() == PaymentStatus.SUCCESS).mapToDouble(Payment::getAmountLKR).sum();
        long successCount = all.stream().filter(p -> p.getStatus() == PaymentStatus.SUCCESS).count();
        long refundCount = all.stream().filter(p -> p.getStatus() == PaymentStatus.REFUNDED).count();

        model.addAttribute("title", "Payments - Admin");
        model.addAttribute("section", "Payments");
        model.addAttribute("payments", paymentsPage.getContent());
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalCount", all.size());
        model.addAttribute("successCount", successCount);
        model.addAttribute("refundCount", refundCount);
        model.addAttribute("selectedStatus", status != null ? status.name() : "ALL");
        return "admin/payments";
    }

    @PostMapping("/payments/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public String refundPayment(
            @PathVariable("id") Long id,
            @RequestParam(name = "reason", defaultValue = "Admin discretionary refund") String reason,
            RedirectAttributes redirect) {
        try {
            paymentService.refundPayment(id, reason);
            redirect.addFlashAttribute("ok", "Payment refunded successfully.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/payments";
    }

    @PostMapping("/payments/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deletePayment(@PathVariable("id") Long id, RedirectAttributes redirect) {
        try {
            paymentService.deletePayment(id);
            redirect.addFlashAttribute("ok", "Payment transaction record deleted successfully.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/payments";
    }

    @GetMapping("/reconcile")
    @PreAuthorize("hasRole('ADMIN')")
    public String reconcile(Model model) {
        model.addAttribute("title", "Reconciliation - Admin");
        model.addAttribute("section", "Reconciliation");
        model.addAttribute("data", "Daily reconciliation endpoint: POST /api/payments/reconcile");
        return "admin/placeholder";
    }

    @GetMapping("/notifications")
    @PreAuthorize("hasRole('ADMIN')")
    public String notifications(
            @RequestParam(name = "channel", required = false) NotificationChannel channel,
            @RequestParam(name = "status", required = false) NotificationStatus status,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", defaultValue = "1") int page,
            Model model) {
        int pageIndex = Math.max(0, page - 1);
        Page<Notification> notificationPage = notificationService.listAll(channel, status, search, PageRequest.of(pageIndex, 50, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<Notification> all = notificationService.listAll(null, null, null, PageRequest.of(0, 1000)).getContent();
        long sentCount = all.stream().filter(n -> n.getStatus() == NotificationStatus.SENT).count();
        long failedCount = all.stream().filter(n -> n.getStatus() == NotificationStatus.FAILED).count();
        long queuedCount = all.stream().filter(n -> n.getStatus() == NotificationStatus.QUEUED).count();

        List<User> users = userService.listUsers(PageRequest.of(0, 200), null, null, null).getContent();

        model.addAttribute("title", "Notifications - Admin");
        model.addAttribute("section", "Notifications");
        model.addAttribute("notifications", notificationPage.getContent());
        model.addAttribute("totalCount", all.size());
        model.addAttribute("sentCount", sentCount);
        model.addAttribute("failedCount", failedCount);
        model.addAttribute("queuedCount", queuedCount);
        model.addAttribute("selectedChannel", channel != null ? channel.name() : "ALL");
        model.addAttribute("selectedStatus", status != null ? status.name() : "ALL");
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("users", users);
        return "admin/notifications";
    }

    @PostMapping("/notifications")
    @PreAuthorize("hasRole('ADMIN')")
    public String createNotification(
            @RequestParam(name = "recipientUserId", required = false) Long recipientUserId,
            @RequestParam(name = "channel", defaultValue = "IN_APP") NotificationChannel channel,
            @RequestParam("subject") String subject,
            @RequestParam("body") String body,
            RedirectAttributes redirect) {
        try {
            notificationService.createNotification(recipientUserId, channel, subject, body);
            redirect.addFlashAttribute("ok", "Notification created and dispatched successfully.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/notifications";
    }

    @PostMapping("/notifications/{id}/resend")
    @PreAuthorize("hasRole('ADMIN')")
    public String resendNotification(@PathVariable("id") Long id, RedirectAttributes redirect) {
        try {
            notificationService.resend(id);
            redirect.addFlashAttribute("ok", "Notification re-dispatched successfully.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/notifications";
    }

    @PostMapping("/notifications/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateNotification(
            @PathVariable("id") Long id,
            @RequestParam("subject") String subject,
            @RequestParam("body") String body,
            @RequestParam(name = "status", required = false) NotificationStatus status,
            RedirectAttributes redirect) {
        try {
            notificationService.updateNotification(id, subject, body, status);
            redirect.addFlashAttribute("ok", "Notification updated successfully.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/notifications";
    }

    @PostMapping("/notifications/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteNotification(@PathVariable("id") Long id, RedirectAttributes redirect) {
        try {
            notificationService.deleteNotification(id);
            redirect.addFlashAttribute("ok", "Notification record deleted successfully.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/notifications";
    }

    @PostMapping("/notifications/clear-failed")
    @PreAuthorize("hasRole('ADMIN')")
    public String clearFailedNotifications(RedirectAttributes redirect) {
        try {
            int count = notificationService.deleteFailedNotifications();
            redirect.addFlashAttribute("ok", "Purged " + count + " failed notification records.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/notifications";
    }

    @GetMapping("/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public String broadcast() {
        return "redirect:/admin/notifications";
    }

    @GetMapping("/failed-notifications")
    @PreAuthorize("hasRole('ADMIN')")
    public String failedNotifications() {
        return "redirect:/admin/notifications?status=FAILED";
    }

    @GetMapping("/sponsors")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SPONSOR_MANAGER')")
    public String sponsors(Model model) {
        List<Sponsor> sponsors = sponsorService.listSponsors(null, PageRequest.of(0, 100)).getContent();
        List<SponsorshipPackage> packages = sponsorService.listPackages();
        model.addAttribute("title", "Sponsors & Packages - Admin");
        model.addAttribute("section", "Sponsors");
        model.addAttribute("sponsors", sponsors);
        model.addAttribute("packages", packages);
        return "admin/sponsors";
    }

    @PostMapping("/sponsors")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SPONSOR_MANAGER')")
    public String registerSponsor(
            @RequestParam("companyName") String companyName,
            @RequestParam("contactName") String contactName,
            @RequestParam("contactEmail") String contactEmail,
            @RequestParam(name = "industry", required = false) String industry,
            @RequestParam(name = "website", required = false) String website,
            @RequestParam(name = "contactPhone", required = false) String contactPhone,
            @RequestParam(name = "logoUrl", required = false) String logoUrl,
            RedirectAttributes redirect) {
        try {
            SponsorDtos.RegisterSponsorRequest req = new SponsorDtos.RegisterSponsorRequest();
            req.setCompanyName(companyName);
            req.setContactName(contactName);
            req.setContactEmail(contactEmail);
            req.setIndustry(industry);
            req.setWebsite(website);
            req.setContactPhone(contactPhone);
            req.setLogoUrl(logoUrl);
            sponsorService.registerSponsor(req);
            redirect.addFlashAttribute("ok", "Sponsor company '" + companyName + "' registered successfully.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/sponsors";
    }

    @PostMapping("/sponsors/{id}/edit")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SPONSOR_MANAGER')")
    public String updateSponsor(
            @PathVariable("id") Long id,
            @RequestParam("companyName") String companyName,
            @RequestParam("contactName") String contactName,
            @RequestParam("contactEmail") String contactEmail,
            @RequestParam(name = "industry", required = false) String industry,
            @RequestParam(name = "website", required = false) String website,
            @RequestParam(name = "contactPhone", required = false) String contactPhone,
            RedirectAttributes redirect) {
        try {
            SponsorDtos.UpdateSponsorRequest req = new SponsorDtos.UpdateSponsorRequest();
            req.setCompanyName(companyName);
            req.setContactName(contactName);
            req.setContactEmail(contactEmail);
            req.setIndustry(industry);
            req.setWebsite(website);
            req.setContactPhone(contactPhone);
            sponsorService.updateSponsor(id, req);
            redirect.addFlashAttribute("ok", "Sponsor details updated successfully.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/sponsors";
    }

    @PostMapping("/sponsors/{id}/delete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SPONSOR_MANAGER')")
    public String deleteSponsor(@PathVariable("id") Long id, RedirectAttributes redirect) {
        try {
            sponsorService.deleteSponsor(id);
            redirect.addFlashAttribute("ok", "Sponsor company and related agreements deleted successfully.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/sponsors";
    }

    @GetMapping("/agreements")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SPONSOR_MANAGER')")
    public String agreements(
            @RequestParam(name = "status", required = false) String statusParam,
            Model model) {
        AgreementStatus status = null;
        if (statusParam != null && !statusParam.isBlank() && !"ALL".equalsIgnoreCase(statusParam)) {
            try {
                status = AgreementStatus.valueOf(statusParam.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                status = null;   // unknown filter value: just show everything
            }
        }
        List<SponsorshipAgreement> agreements = sponsorService.listAgreements(null, status, PageRequest.of(0, 100)).getContent();
        List<SponsorshipAgreement> all = sponsorService.listAgreements(null, null, PageRequest.of(0, 500)).getContent();
        double totalContractValue = all.stream().mapToDouble(SponsorshipAgreement::getContractValueLKR).sum();
        long activeCount = all.stream().filter(a -> a.getStatus() == AgreementStatus.ACTIVE).count();

        List<Sponsor> sponsors = sponsorService.listSponsors(true, PageRequest.of(0, 100)).getContent();
        List<SponsorshipPackage> packages = sponsorService.listPackages();

        model.addAttribute("title", "Agreements - Admin");
        model.addAttribute("section", "Sponsors");
        model.addAttribute("agreements", agreements);
        model.addAttribute("totalContractValue", totalContractValue);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("totalAgreements", all.size());
        model.addAttribute("sponsors", sponsors);
        model.addAttribute("packages", packages);
        model.addAttribute("selectedStatus", status != null ? status.name() : "ALL");
        return "admin/agreements";
    }

    @PostMapping("/agreements")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SPONSOR_MANAGER')")
    public String createAgreement(
            @RequestParam("sponsorId") Long sponsorId,
            @RequestParam("packageTier") SponsorshipTier tier,
            @RequestParam(name = "contractValueLKR", required = false) Double value,
            @RequestParam(name = "clickThroughUrl", required = false) String clickThroughUrl,
            @RequestParam(name = "bannerImageUrl", required = false) String bannerImageUrl,
            RedirectAttributes redirect) {
        try {
            if (value != null && value < 0) throw new IllegalArgumentException("Contract value cannot be negative");
            SponsorDtos.CreateAgreementRequest req = new SponsorDtos.CreateAgreementRequest();
            req.setSponsorId(sponsorId);
            req.setPackageTier(tier);
            req.setContractValueLKR(value);
            req.setStartsAt(LocalDateTime.now());
            req.setEndsAt(LocalDateTime.now().plusMonths(6));
            req.setClickThroughUrl(clickThroughUrl);
            req.setBannerImageUrl(bannerImageUrl);
            SponsorshipAgreement agreement = sponsorService.createAgreement(req);
            redirect.addFlashAttribute("ok", "Agreement " + agreement.getAgreementNo() + " created successfully.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/agreements";
    }

    @GetMapping("/agreements/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SPONSOR_MANAGER')")
    public String agreementDetail(@PathVariable("id") Long id, Model model, RedirectAttributes redirect) {
        try {
            SponsorshipAgreement agreement = sponsorService.getAgreement(id);
            SponsorDtos.FinancialsDto financials = sponsorService.getAgreementFinancials(id);
            SponsorDtos.ExposureReportDto exposure = sponsorService.getExposureReport(id);

            model.addAttribute("title", "Agreement " + agreement.getAgreementNo() + " - Admin");
            model.addAttribute("section", "Sponsors");
            model.addAttribute("agreement", agreement);
            model.addAttribute("financials", financials);
            model.addAttribute("exposure", exposure);
            return "admin/agreement-detail";
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Could not load agreement details: " + e.getMessage());
            return "redirect:/admin/agreements";
        }
    }

    @PostMapping("/agreements/{id}/invoices")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SPONSOR_MANAGER')")
    public String issueInvoice(
            @PathVariable("id") Long id,
            @RequestParam(name = "amountLKR", required = false) Double amountLKR,
            @RequestParam(name = "description", required = false) String description,
            RedirectAttributes redirect) {
        try {
            SponsorDtos.IssueInvoiceRequest req = new SponsorDtos.IssueInvoiceRequest();
            req.setAmountLKR(amountLKR);
            req.setDescription(description);
            sponsorService.issueInvoice(id, req);
            redirect.addFlashAttribute("ok", "Invoice issued successfully.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/agreements/" + id;
    }

    @PostMapping("/invoices/{invoiceId}/pay")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SPONSOR_MANAGER')")
    public String markInvoicePaid(
            @PathVariable("invoiceId") Long invoiceId,
            @RequestParam(name = "agreementId") Long agreementId,
            @RequestParam(name = "paymentRef", defaultValue = "BANK-TRANSFER") String paymentRef,
            RedirectAttributes redirect) {
        try {
            SponsorDtos.RecordInvoicePaymentRequest req = new SponsorDtos.RecordInvoicePaymentRequest();
            req.setPaymentReference(paymentRef);
            req.setPaidAt(LocalDateTime.now());
            sponsorService.recordInvoicePayment(invoiceId, req);
            redirect.addFlashAttribute("ok", "Invoice payment recorded successfully.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/agreements/" + agreementId;
    }

    @PostMapping("/agreements/{id}/activate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SPONSOR_MANAGER')")
    public String activateAgreement(@PathVariable("id") Long id,
                                    @RequestParam(name = "from", required = false) String from,
                                    RedirectAttributes redirect) {
        try {
            SponsorshipAgreement a = sponsorService.activateAgreement(id);
            redirect.addFlashAttribute("ok", "Agreement " + a.getAgreementNo() + " is now active.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "detail".equals(from) ? "redirect:/admin/agreements/" + id : "redirect:/admin/agreements";
    }

    @PostMapping("/agreements/{id}/edit")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SPONSOR_MANAGER')")
    public String editAgreement(@PathVariable("id") Long id,
                                @RequestParam("packageTier") SponsorshipTier tier,
                                @RequestParam(name = "contractValueLKR", required = false) Double value,
                                RedirectAttributes redirect) {
        try {
            if (value != null && value < 0) throw new IllegalArgumentException("Contract value cannot be negative");
            sponsorService.updateAgreement(id, tier, value);
            redirect.addFlashAttribute("ok", "Agreement updated.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/agreements";
    }

    @PostMapping("/agreements/{id}/terminate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SPONSOR_MANAGER')")
    public String terminateAgreement(
            @PathVariable("id") Long id,
            @RequestParam(name = "reason", defaultValue = "Terminated by admin") String reason,
            @RequestParam(name = "from", required = false) String from,
            RedirectAttributes redirect) {
        try {
            sponsorService.terminateAgreement(id, reason);
            redirect.addFlashAttribute("ok", "Agreement terminated.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "list".equals(from) ? "redirect:/admin/agreements" : "redirect:/admin/agreements/" + id;
    }

    @PostMapping("/agreements/{id}/renew")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SPONSOR_MANAGER')")
    public String renewAgreement(
            @PathVariable("id") Long id,
            @RequestParam(name = "upliftPercent", defaultValue = "10.0") Double upliftPercent,
            RedirectAttributes redirect) {
        try {
            SponsorDtos.RenewAgreementRequest req = new SponsorDtos.RenewAgreementRequest();
            req.setUpliftPercent(upliftPercent);
            req.setStartsAt(LocalDateTime.now());
            req.setEndsAt(LocalDateTime.now().plusMonths(6));
            req.setActivate(true);
            SponsorshipAgreement renewed = sponsorService.renewAgreement(id, req);
            redirect.addFlashAttribute("ok", "Agreement renewed successfully as " + renewed.getAgreementNo() + " with " + upliftPercent + "% uplift.");
            return "redirect:/admin/agreements/" + renewed.getId();
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/agreements/" + id;
        }
    }

    @PostMapping("/agreements/{id}/delete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SPONSOR_MANAGER')")
    public String deleteAgreement(@PathVariable("id") Long id, RedirectAttributes redirect) {
        try {
            sponsorService.deleteAgreement(id);
            redirect.addFlashAttribute("ok", "Sponsorship agreement and associated records deleted successfully.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/agreements";
    }
}
