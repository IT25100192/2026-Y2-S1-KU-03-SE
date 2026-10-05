package com.starvoicelanka.contestant.entity;

import com.starvoicelanka.common.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "round_entries", indexes = {
        @Index(name = "idx_entry_round_contestant", columnList = "round_id, contestant_id", unique = true),
        @Index(name = "idx_entry_round_votes", columnList = "round_id, votes_in_round")
})
public class RoundEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contestant_id", nullable = false)
    private Contestant contestant;

    @Column(name = "votes_in_round", nullable = false)
    private int votesInRound = 0;

    private Integer position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoundOutcome outcome = RoundOutcome.PENDING;

    @Column(name = "performance_title", length = 200)
    private String performanceTitle;

    // CM06 - media fields
    @Column(name = "media_url", length = 255)
    private String mediaUrl;

    @Column(name = "media_mime", length = 60)
    private String mediaMime;

    @Column(name = "media_size_bytes")
    private Long mediaSizeBytes;

    @Column(name = "media_original_name", length = 255)
    private String mediaOriginalName;

    @Column(name = "media_uploaded_at")
    private LocalDateTime mediaUploadedAt;

    public RoundEntry() {}

    public RoundEntry(Round round, Contestant contestant) {
        this.round = round;
        this.contestant = contestant;
        this.votesInRound = 0;
        this.outcome = RoundOutcome.PENDING;
    }

    public Round getRound() { return round; }
    public void setRound(Round round) { this.round = round; }

    public Contestant getContestant() { return contestant; }
    public void setContestant(Contestant contestant) { this.contestant = contestant; }

    public int getVotesInRound() { return votesInRound; }
    public void setVotesInRound(int votesInRound) { this.votesInRound = votesInRound; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

    public RoundOutcome getOutcome() { return outcome; }
    public void setOutcome(RoundOutcome outcome) { this.outcome = outcome; }

    public String getPerformanceTitle() { return performanceTitle; }
    public void setPerformanceTitle(String performanceTitle) { this.performanceTitle = performanceTitle; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    public String getMediaMime() { return mediaMime; }
    public void setMediaMime(String mediaMime) { this.mediaMime = mediaMime; }

    public Long getMediaSizeBytes() { return mediaSizeBytes; }
    public void setMediaSizeBytes(Long mediaSizeBytes) { this.mediaSizeBytes = mediaSizeBytes; }

    public String getMediaOriginalName() { return mediaOriginalName; }
    public void setMediaOriginalName(String mediaOriginalName) { this.mediaOriginalName = mediaOriginalName; }

    public LocalDateTime getMediaUploadedAt() { return mediaUploadedAt; }
    public void setMediaUploadedAt(LocalDateTime mediaUploadedAt) { this.mediaUploadedAt = mediaUploadedAt; }
}
