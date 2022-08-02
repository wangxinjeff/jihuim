package com.hyphenate.easeim.common.model;

public class GroupApplyBean {

    private String userName;
    private String groupId;
    private String state;
    private String inviterName;
    private String role;
    private boolean isOperated;
    private String operatedResult;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getInviterName() {
        return inviterName;
    }

    public void setInviterName(String inviterName) {
        this.inviterName = inviterName;
    }

    public boolean isOperated() {
        return isOperated;
    }

    public void setOperated(boolean operated) {
        isOperated = operated;
    }

    public String getOperatedResult() {
        return operatedResult;
    }

    public void setOperatedResult(String operatedResult) {
        this.operatedResult = operatedResult;
    }
}
