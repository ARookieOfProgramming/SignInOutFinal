package com.zhouzhou.locationgaode.bean;

/**
 * author : ZhouZhou
 * e-mail : zhou.zhou@sim.com
 * date   : 19-12-3下午1:38
 * desc   :
 * version: 1.0
 */
public class SignStatusInfoFull {

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNowadays() {
        return nowadays;
    }

    public void setNowadays(String nowadays) {
        this.nowadays = nowadays;
    }

    public SignStatusInfo getStatusInfo() {
        return statusInfo;
    }

    public void setStatusInfo(SignStatusInfo statusInfo) {
        this.statusInfo = statusInfo;
    }

    private String userName;
    private String nowadays;
    private SignStatusInfo statusInfo;
}
