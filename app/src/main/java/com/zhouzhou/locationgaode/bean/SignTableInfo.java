package com.zhouzhou.locationgaode.bean;

/**
 * author : ZhouZhou
 * e-mail : zhou.zhou@sim.com
 * date   : 19-11-28下午3:02
 * desc   :设置项实体
 * version: 1.0
 */
public class SignTableInfo {
    private double latitude;//纬度
    private double longitude;//经度
    private double radius;//打卡距离
    private String timeQuantum;//时间段
    private String timeStart;//开始时间
    private String timeStop;//结束时间
    private String timeFrequency;//结束时间
    public String getTimeFrequency() {
        return timeFrequency;
    }

    public void setTimeFrequency(String timeFrequency) {
        this.timeFrequency = timeFrequency;
    }
    public SignTableInfo() {
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public String getTimeQuantum() {
        return timeQuantum;
    }

    public void setTimeQuantum(String timeQuantum) {
        this.timeQuantum = timeQuantum;
    }

    public String getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(String timeStart) {
        this.timeStart = timeStart;
    }

    public String getTimeStop() {
        return timeStop;
    }

    public void setTimeStop(String timeStop) {
        this.timeStop = timeStop;
    }
    @Override
    public String toString() {
        return "SignTableInfo{" + "latitude=" + latitude + ", longitude=" + longitude + ", radius=" + radius + ", timeQuantum='" + timeQuantum + '\'' + ", timeStart='" + timeStart + '\'' + ", timeStop='" + timeStop + '\'' + '}';
    }
}
