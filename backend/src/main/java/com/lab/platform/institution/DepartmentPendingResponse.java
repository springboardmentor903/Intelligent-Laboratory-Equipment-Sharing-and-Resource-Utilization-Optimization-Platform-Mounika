package com.lab.platform.institution;

import java.time.LocalDateTime;

public class DepartmentPendingResponse {
    private Long departmentId;
    private String departmentName;
    private String departmentCode;
    private Long institutionId;
    private String institutionName;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private String userPhoneNumber;
    private LocalDateTime requestedAt;

    public DepartmentPendingResponse() {}

    public DepartmentPendingResponse(Long departmentId, String departmentName, String departmentCode,
                                     Long institutionId, String institutionName,
                                     Long userId, String userFullName, String userEmail, String userPhoneNumber,
                                     LocalDateTime requestedAt) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.departmentCode = departmentCode;
        this.institutionId = institutionId;
        this.institutionName = institutionName;
        this.userId = userId;
        this.userFullName = userFullName;
        this.userEmail = userEmail;
        this.userPhoneNumber = userPhoneNumber;
        this.requestedAt = requestedAt;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserPhoneNumber() {
        return userPhoneNumber;
    }

    public void setUserPhoneNumber(String userPhoneNumber) {
        this.userPhoneNumber = userPhoneNumber;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }
}
