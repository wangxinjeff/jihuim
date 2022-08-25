package com.hyphenate.easeim.common.model;

public class GroupApplyBean {

    private String userName;
    private String userNickName;
    private String groupId;
    private String groupName;
    private String state;
    private String inviterName;
    private String inviterNickName;
    private String role;
    private boolean isOperated;
    private String operatedResult;

    public String getUserNickName() {
        return userNickName;
    }

    public void setUserNickName(String userNickName) {
        this.userNickName = userNickName;
    }

    public String getInviterNickName() {
        return inviterNickName;
    }

    public void setInviterNickName(String inviterNickName) {
        this.inviterNickName = inviterNickName;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

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
