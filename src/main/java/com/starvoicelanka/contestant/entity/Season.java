package com.starvoicelanka.contestant.entity;

import com.starvoicelanka.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "seasons")
public class Season extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "season_year", nullable = false)
    private int year;

    @Column(name = "is_current", nullable = false)
    private boolean isCurrent = false;

    public Season() {}

    public Season(String name, int year) {
        this(name, year, false);
    }

    public Season(String name, int year, boolean isCurrent) {
        this.name = name;
        this.year = year;
        this.isCurrent = isCurrent;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public boolean isCurrent() { return isCurrent; }
    public void setCurrent(boolean current) { isCurrent = current; }
}
