package com.schedule.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shift_requirements")
public class ShiftRequirement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;

    @Column(name = "natural_input", columnDefinition = "TEXT")
    private String naturalInput;

    @Column(name = "parsed_unavailable", columnDefinition = "TEXT")
    private String parsedUnavailable;

    @Column(name = "parsed_preferences", columnDefinition = "TEXT")
    private String parsedPreferences;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public ShiftRequirement() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getYearMonth() { return yearMonth; }
    public void setYearMonth(String yearMonth) { this.yearMonth = yearMonth; }
    public String getNaturalInput() { return naturalInput; }
    public void setNaturalInput(String naturalInput) { this.naturalInput = naturalInput; }
    public String getParsedUnavailable() { return parsedUnavailable; }
    public void setParsedUnavailable(String parsedUnavailable) { this.parsedUnavailable = parsedUnavailable; }
    public String getParsedPreferences() { return parsedPreferences; }
    public void setParsedPreferences(String parsedPreferences) { this.parsedPreferences = parsedPreferences; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
