package io.github.snipperxx.applyledger.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/** One step in an application's history. Rows are only ever inserted, never edited. */
@Entity
@Table(name = "status_change")
public class StatusChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id")
    private JobApplication application;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private ApplicationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private ApplicationStatus toStatus;

    /** The day it happened (may be in the past when I log something late). */
    @Column(name = "changed_on", nullable = false)
    private LocalDate changedOn;

    @Column(length = 1000)
    private String note;

    /** When it was written down. */
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected StatusChange() {
    }

    StatusChange(JobApplication application, ApplicationStatus fromStatus, ApplicationStatus toStatus,
                 LocalDate changedOn, String note, Instant recordedAt) {
        this.application = application;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedOn = changedOn;
        this.note = note;
        this.recordedAt = recordedAt;
    }

    public Long getId() {
        return id;
    }

    public ApplicationStatus getFromStatus() {
        return fromStatus;
    }

    public ApplicationStatus getToStatus() {
        return toStatus;
    }

    public LocalDate getChangedOn() {
        return changedOn;
    }

    public String getNote() {
        return note;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
