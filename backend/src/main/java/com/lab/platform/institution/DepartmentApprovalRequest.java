package com.lab.platform.institution;

public class DepartmentApprovalRequest {
    private boolean approved;
    private String reason;

    public DepartmentApprovalRequest() {}

    public DepartmentApprovalRequest(boolean approved, String reason) {
        this.approved = approved;
        this.reason = reason;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
