package com.zhouzhou.locationgaode.bean;

import com.amap.api.maps2d.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * author : ZhouZhou
 * e-mail : zhou.zhou@sim.com
 * date   : 19-12-2下午4:45
 * desc   :签到状态实体
 * version: 1.0
 */
public class SignStatusInfo {
    private int signInSend = 0;//签到通知标识
    private int signOutSend = 0;//离开通知标识
    private String signInDate = "2000:00:00 00:00";//签到时间
    private String signOutDate = "2000:00:00 00:00";//签退时间
    private String isOpen = "no";//是否开启提醒
    public int getSignOutSend() {
        return signOutSend;
    }

    public void setSignOutSend(int signOutSend) {
        this.signOutSend = signOutSend;
    }



    public String getisOpen() {
        return isOpen;
    }

    public void setisOpen(String isOpen) {
        this.isOpen = isOpen;
    }
    public int getSignInSend() {
        return signInSend;
    }

    public void setSignInSend(int signInSend) {
        this.signInSend = signInSend;
    }

    public int signOutSend() {
        return signOutSend;
    }

    public void setsignOutSend(int signOutSend) {
        this.signOutSend = signOutSend;
    }

    public String getSignInDate() {
        return signInDate;
    }

    public void setSignInDate(String signInDate) {
        this.signInDate = signInDate;
    }

    public String getSignOutDate() {
        return signOutDate;
    }

    public void setSignOutDate(String signOutDate) {
        this.signOutDate = signOutDate;
    }

    public SignStatusInfo() {
    }

}
