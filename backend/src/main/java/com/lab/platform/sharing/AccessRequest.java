package com.lab.platform.sharing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lab.platform.auth.User;
import com.lab.platform.institution.Institution;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "access_requests")
public class AccessRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "listing_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private SharingListing listing;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requester_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "passwordHash", "resetPasswordToken"})
    private User requester;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requester_institution_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Institution requesterInstitution;

    @Column(nullable = false, length = 1000)
    private String purpose;

    @Column(nullable = false)
    private Integer estimatedUsageHours = 10;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccessRequestStatus status = AccessRequestStatus.PENDING_HOME_APPROVAL;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "home_admin_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "passwordHash", "resetPasswordToken"})
    private User homeAdmin;

    @Column(length = 1000)
    private String homeDecisionNotes;

    private LocalDateTime homeDecisionAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "host_admin_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "passwordHash", "resetPasswordToken"})
    private User hostAdmin;

    @Column(length = 1000)
    private String hostDecisionNotes;

    private LocalDateTime hostDecisionAt;

    private LocalDate grantedValidFrom;

    private LocalDate grantedValidTo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "generated_grant_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private AccessGrant generatedGrant;

    private LocalDateTime requestedAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    public AccessRequest() {}

    public AccessRequest(SharingListing listing, User requester, Institution requesterInstitution, String purpose, Integer estimatedUsageHours) {
        this.listing = listing;
        this.requester = requester;
        this.requesterInstitution = requesterInstitution;
        this.purpose = purpose;
        this.estimatedUsageHours = estimatedUsageHours != null ? estimatedUsageHours : 10;
        this.status = AccessRequestStatus.PENDING_HOME_APPROVAL;
        this.requestedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (requestedAt == null) requestedAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SharingListing getListing() {
        return listing;
    }

    public void setListing(SharingListing listing) {
        this.listing = listing;
    }

    public User getRequester() {
        return requester;
    }

    public void setRequester(User requester) {
        this.requester = requester;
    }

    public Institution getRequesterInstitution() {
        return requesterInstitution;
    }

    public void setRequesterInstitution(Institution requesterInstitution) {
        this.requesterInstitution = requesterInstitution;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public Integer getEstimatedUsageHours() {
        return estimatedUsageHours;
    }

    public void setEstimatedUsageHours(Integer estimatedUsageHours) {
        this.estimatedUsageHours = estimatedUsageHours;
    }

    public AccessRequestStatus getStatus() {
        return status;
    }

    public void setStatus(AccessRequestStatus status) {
        this.status = status;
    }

    public User getHomeAdmin() {
        return homeAdmin;
    }

    public void setHomeAdmin(User homeAdmin) {
        this.homeAdmin = homeAdmin;
    }

    public String getHomeDecisionNotes() {
        return homeDecisionNotes;
    }

    public void setHomeDecisionNotes(String homeDecisionNotes) {
        this.homeDecisionNotes = homeDecisionNotes;
    }

    public LocalDateTime getHomeDecisionAt() {
        return homeDecisionAt;
    }

    public void setHomeDecisionAt(LocalDateTime homeDecisionAt) {
        this.homeDecisionAt = homeDecisionAt;
    }

    public User getHostAdmin() {
        return hostAdmin;
    }

    public void setHostAdmin(User hostAdmin) {
        this.hostAdmin = hostAdmin;
    }

    public String getHostDecisionNotes() {
        return hostDecisionNotes;
    }

    public void setHostDecisionNotes(String hostDecisionNotes) {
        this.hostDecisionNotes = hostDecisionNotes;
    }

    public LocalDateTime getHostDecisionAt() {
        return hostDecisionAt;
    }

    public void setHostDecisionAt(LocalDateTime hostDecisionAt) {
        this.hostDecisionAt = hostDecisionAt;
    }

    public LocalDate getGrantedValidFrom() {
        return grantedValidFrom;
    }

    public void setGrantedValidFrom(LocalDate grantedValidFrom) {
        this.grantedValidFrom = grantedValidFrom;
    }

    public LocalDate getGrantedValidTo() {
        return grantedValidTo;
    }

    public void setGrantedValidTo(LocalDate grantedValidTo) {
        this.grantedValidTo = grantedValidTo;
    }

    public AccessGrant getGeneratedGrant() {
        return generatedGrant;
    }

    public void setGeneratedGrant(AccessGrant generatedGrant) {
        this.generatedGrant = generatedGrant;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
