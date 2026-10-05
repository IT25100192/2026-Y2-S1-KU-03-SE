package com.starvoicelanka.contestant.entity;

import com.starvoicelanka.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "contestants", indexes = {
        @Index(name = "idx_contestant_season", columnList = "season_id"),
        @Index(name = "idx_contestant_status", columnList = "status")
})
public class Contestant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "stage_name", length = 120)
    private String stageName;

    private Integer age;

    @Column(length = 60)
    private String district;

    @Column(length = 1000)
    private String bio;

    @Column(name = "photo_url", length = 255)
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ContestantStatus status = ContestantStatus.REGISTERED;

    @Column(name = "total_votes", nullable = false)
    private int totalVotes = 0;

    public Contestant() {}

    public Contestant(Season season, String fullName, String stageName) {
        this(season, fullName, stageName, null, null, null, null);
    }

    public Contestant(Season season, String fullName, String stageName, Integer age, String district, String bio, String photoUrl) {
        this.season = season;
        this.fullName = fullName;
        this.stageName = stageName;
        this.age = age;
        this.district = district;
        this.bio = bio;
        this.photoUrl = photoUrl;
        this.status = ContestantStatus.REGISTERED;
        this.totalVotes = 0;
    }

    public String getDisplayName() {
        return (stageName != null && !stageName.isBlank()) ? stageName : fullName;
    }

    public Season getSeason() { return season; }
    public void setSeason(Season season) { this.season = season; }

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

    public ContestantStatus getStatus() { return status; }
    public void setStatus(ContestantStatus status) { this.status = status; }

    public int getTotalVotes() { return totalVotes; }
    public void setTotalVotes(int totalVotes) { this.totalVotes = totalVotes; }
}
