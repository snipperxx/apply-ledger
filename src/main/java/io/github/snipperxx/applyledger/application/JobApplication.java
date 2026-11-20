package io.github.snipperxx.applyledger.application;

import io.github.snipperxx.applyledger.company.Company;
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
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "job_application")
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "role_title", nullable = false, length = 200)
    private String roleTitle;

    @Column(name = "job_url", length = 1000)
    private String jobUrl;

    @Column(length = 200)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode", length = 20)
    private WorkMode workMode;

    @Column(length = 100)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status;

    @Column(name = "applied_on")
    private LocalDate appliedOn;

    @Column(name = "next_action", length = 300)
    private String nextAction;

    @Column(name = "next_action_on")
    private LocalDate nextActionOn;

    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected JobApplication() {
    }

    JobApplication(Company company, ApplicationStatus status, Instant now) {
        this.company = company;
        this.status = status;
        this.createdAt = now;
        this.updatedAt = now;
    }

    void edit(Company company, String roleTitle, String jobUrl, String location, WorkMode workMode, String source,
              LocalDate appliedOn, String nextAction, LocalDate nextActionOn, String notes, Instant now) {
        this.company = company;
        this.roleTitle = roleTitle;
        this.jobUrl = jobUrl;
        this.location = location;
        this.workMode = workMode;
        this.source = source;
        this.appliedOn = appliedOn;
        this.nextAction = nextAction;
        this.nextActionOn = nextActionOn;
        this.notes = notes;
        this.updatedAt = now;
    }

    /** Callers check {@link ApplicationStatus#canBecome} first; this only records the result. */
    void moveTo(ApplicationStatus next, LocalDate on, Instant now) {
        if (next == ApplicationStatus.APPLIED && appliedOn == null) {
            appliedOn = on;
        }
        if (!next.isOpen()) {
            // A closed application has nothing left to follow up on.
            nextAction = null;
            nextActionOn = null;
        }
        this.status = next;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public String getRoleTitle() {
        return roleTitle;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public String getLocation() {
        return location;
    }

    public WorkMode getWorkMode() {
        return workMode;
    }

    public String getSource() {
        return source;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public LocalDate getAppliedOn() {
        return appliedOn;
    }

    public String getNextAction() {
        return nextAction;
    }

    public LocalDate getNextActionOn() {
        return nextActionOn;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
