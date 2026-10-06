package com.starvoicelanka.contestant.dto;

import com.starvoicelanka.contestant.entity.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public class ContestantDtos {

    public static class CreateSeasonRequest {
        @NotBlank(message = "Season name is required")
        private String name;

        @NotNull(message = "Year is required")
        @Min(value = 2020, message = "Year must be 2020 or later")
        private Integer year;

        private Boolean isCurrent = false;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }
        public Boolean getIsCurrent() { return isCurrent; }
        public void setIsCurrent(Boolean isCurrent) { this.isCurrent = isCurrent; }
    }

    public static class RegisterContestantRequest {
        private Long seasonId;

        @NotBlank(message = "Full name is required")
        @Size(min = 3, message = "Full name must be at least 3 characters")
        private String fullName;

        private String stageName;
        private Integer age;
        private String district;
        private String bio;
        private String photoUrl;

        public Long getSeasonId() { return seasonId; }
        public void setSeasonId(Long seasonId) { this.seasonId = seasonId; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getStageName() { return stageName; }
        public void setStageName(String stageName) { this.stageName = stageName; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }
        public String getBio() { return bio; }
        public void setBio(String bio) { this.bio = bio; }
        public String getPhotoUrl() { return photoUrl; }
        public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    }

    public static class UpdateContestantRequest {
        private String fullName;
        private String stageName;
        private Integer age;
        private String district;
        private String bio;
        private String photoUrl;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getStageName() { return stageName; }
        public void setStageName(String stageName) { this.stageName = stageName; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }
        public String getBio() { return bio; }
        public void setBio(String bio) { this.bio = bio; }
        public String getPhotoUrl() { return photoUrl; }
        public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    }

    public static class SetContestantStatusRequest {
        @NotNull(message = "Status is required")
        private ContestantStatus status;

        public ContestantStatus getStatus() { return status; }
        public void setStatus(ContestantStatus status) { this.status = status; }
    }

    public static class CreateRoundRequest {
        private Long seasonId;

        @NotBlank(message = "Round name is required")
        @Size(min = 2, message = "Round name must be at least 2 characters")
        private String name;

        @NotNull(message = "Sequence number is required")
        @Min(value = 1, message = "Sequence must be at least 1")
        private Integer sequence;

        private LocalDateTime opensAt;
        private LocalDateTime closesAt;
        private Integer advanceCount = 0;

        public Long getSeasonId() { return seasonId; }
        public void setSeasonId(Long seasonId) { this.seasonId = seasonId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getSequence() { return sequence; }
        public void setSequence(Integer sequence) { this.sequence = sequence; }
        public LocalDateTime getOpensAt() { return opensAt; }
        public void setOpensAt(LocalDateTime opensAt) { this.opensAt = opensAt; }
        public LocalDateTime getClosesAt() { return closesAt; }
        public void setClosesAt(LocalDateTime closesAt) { this.closesAt = closesAt; }
        public Integer getAdvanceCount() { return advanceCount; }
        public void setAdvanceCount(Integer advanceCount) { this.advanceCount = advanceCount; }
    }

    public static class AssignLineUpRequest {
        @NotNull(message = "Contestant IDs required")
        private List<Long> contestantIds;

        public List<Long> getContestantIds() { return contestantIds; }
        public void setContestantIds(List<Long> contestantIds) { this.contestantIds = contestantIds; }
    }

    public static class ContestantDto {
        private Long id;
        private Long seasonId;
        private String seasonName;
        private String fullName;
        private String stageName;
        private String displayName;
        private Integer age;
        private String district;
        private String bio;
        private String photoUrl;
        private ContestantStatus status;
        private int totalVotes;

        public static ContestantDto from(Contestant c) {
            if (c == null) return null;
            ContestantDto dto = new ContestantDto();
            dto.id = c.getId();
            dto.seasonId = c.getSeason() != null ? c.getSeason().getId() : null;
            dto.seasonName = c.getSeason() != null ? c.getSeason().getName() : null;
            dto.fullName = c.getFullName();
            dto.stageName = c.getStageName();
            dto.displayName = c.getDisplayName();
            dto.age = c.getAge();
            dto.district = c.getDistrict();
            dto.bio = c.getBio();
            dto.photoUrl = c.getPhotoUrl();
            dto.status = c.getStatus();
            dto.totalVotes = c.getTotalVotes();
            return dto;
        }

        public Long getId() { return id; }
        public Long getSeasonId() { return seasonId; }
        public String getSeasonName() { return seasonName; }
        public String getFullName() { return fullName; }
        public String getStageName() { return stageName; }
        public String getDisplayName() { return displayName; }
        public Integer getAge() { return age; }
        public String getDistrict() { return district; }
        public String getBio() { return bio; }
        public String getPhotoUrl() { return photoUrl; }
        public ContestantStatus getStatus() { return status; }
        public int getTotalVotes() { return totalVotes; }
    }

    public static class RoundDto {
        private Long id;
        private Long seasonId;
        private String seasonName;
        private String name;
        private int sequence;
        private RoundStatus status;
        private LocalDateTime opensAt;
        private LocalDateTime closesAt;
        private int advanceCount;
        private boolean votingOpen;

        public static RoundDto from(Round r) {
            if (r == null) return null;
            RoundDto dto = new RoundDto();
            dto.id = r.getId();
            dto.seasonId = r.getSeason() != null ? r.getSeason().getId() : null;
            dto.seasonName = r.getSeason() != null ? r.getSeason().getName() : null;
            dto.name = r.getName();
            dto.sequence = r.getSequence();
            dto.status = r.getStatus();
            dto.opensAt = r.getOpensAt();
            dto.closesAt = r.getClosesAt();
            dto.advanceCount = r.getAdvanceCount();
            dto.votingOpen = r.isVotingOpen();
            return dto;
        }

        public Long getId() { return id; }
        public Long getSeasonId() { return seasonId; }
        public String getSeasonName() { return seasonName; }
        public String getName() { return name; }
        public int getSequence() { return sequence; }
        public RoundStatus getStatus() { return status; }
        public LocalDateTime getOpensAt() { return opensAt; }
        public LocalDateTime getClosesAt() { return closesAt; }
        public int getAdvanceCount() { return advanceCount; }
        public boolean isVotingOpen() { return votingOpen; }
    }

    public static class LeaderboardStandingDto {
        private int rank;
        private Long contestantId;
        private String name;
        private String district;
        private String photoUrl;
        private int votes;
        private double share;
        private RoundOutcome outcome;

        public LeaderboardStandingDto(int rank, Long contestantId, String name, String district, String photoUrl, int votes, double share, RoundOutcome outcome) {
            this.rank = rank;
            this.contestantId = contestantId;
            this.name = name;
            this.district = district;
            this.photoUrl = photoUrl;
            this.votes = votes;
            this.share = share;
            this.outcome = outcome;
        }

        public int getRank() { return rank; }
        public Long getContestantId() { return contestantId; }
        public String getName() { return name; }
        public String getDistrict() { return district; }
        public String getPhotoUrl() { return photoUrl; }
        public int getVotes() { return votes; }
        public double getShare() { return share; }
        public RoundOutcome getOutcome() { return outcome; }
    }

    public static class LeaderboardDto {
        private RoundDto round;
        private int totalVotes;
        private List<LeaderboardStandingDto> standings;

        public LeaderboardDto(RoundDto round, int totalVotes, List<LeaderboardStandingDto> standings) {
            this.round = round;
            this.totalVotes = totalVotes;
            this.standings = standings;
        }

        public RoundDto getRound() { return round; }
        public int getTotalVotes() { return totalVotes; }
        public List<LeaderboardStandingDto> getStandings() { return standings; }
    }

    public static class RoundEntryDto {
        private Long id;
        private Long roundId;
        private String roundName;
        private ContestantDto contestant;
        private int votesInRound;
        private Integer position;
        private RoundOutcome outcome;
        private String performanceTitle;
        private String mediaUrl;
        private String mediaMime;
        private Long mediaSizeBytes;
        private String mediaOriginalName;
        private LocalDateTime mediaUploadedAt;

        public static RoundEntryDto from(RoundEntry e) {
            if (e == null) return null;
            RoundEntryDto dto = new RoundEntryDto();
            dto.id = e.getId();
            dto.roundId = e.getRound() != null ? e.getRound().getId() : null;
            dto.roundName = e.getRound() != null ? e.getRound().getName() : null;
            dto.contestant = ContestantDto.from(e.getContestant());
            dto.votesInRound = e.getVotesInRound();
            dto.position = e.getPosition();
            dto.outcome = e.getOutcome();
            dto.performanceTitle = e.getPerformanceTitle();
            dto.mediaUrl = e.getMediaUrl();
            dto.mediaMime = e.getMediaMime();
            dto.mediaSizeBytes = e.getMediaSizeBytes();
            dto.mediaOriginalName = e.getMediaOriginalName();
            dto.mediaUploadedAt = e.getMediaUploadedAt();
            return dto;
        }

        public Long getId() { return id; }
        public Long getRoundId() { return roundId; }
        public String getRoundName() { return roundName; }
        public ContestantDto getContestant() { return contestant; }
        public int getVotesInRound() { return votesInRound; }
        public Integer getPosition() { return position; }
        public RoundOutcome getOutcome() { return outcome; }
        public String getPerformanceTitle() { return performanceTitle; }
        public String getMediaUrl() { return mediaUrl; }
        public String getMediaMime() { return mediaMime; }
        public Long getMediaSizeBytes() { return mediaSizeBytes; }
        public String getMediaOriginalName() { return mediaOriginalName; }
        public LocalDateTime getMediaUploadedAt() { return mediaUploadedAt; }
    }

    public static class RoundLineUpDto {
        private RoundDto round;
        private List<RoundEntryDto> entries;

        public RoundLineUpDto() {}
        public RoundLineUpDto(RoundDto round, List<RoundEntryDto> entries) {
            this.round = round;
            this.entries = entries;
        }

        public RoundDto getRound() { return round; }
        public void setRound(RoundDto round) { this.round = round; }
        public List<RoundEntryDto> getEntries() { return entries; }
        public void setEntries(List<RoundEntryDto> entries) { this.entries = entries; }
    }
}
