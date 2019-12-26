package com.zhouzhou.locationgaode.bean;

/**
 * author : ZhouZhou
 * e-mail : zhou.zhou@sim.com
 * date   : 19-11-27下午4:39
 * desc   :
 * version: 1.0
 */
public class Constant {

    //进入或离开标识标识
    public static final int IN = 1;
    public static final int OUT = 2;

    //时间选择器的区分标识
    public static final int TYPE1 = 4;
    public static final int TYPE2 = 5;
    public static final int TYPE3 = 6;
    public static final int TYPE4 = 7;

    //通知的channel
    public static final String ID = "T";

    //当前的登录名
    public static String name = "CurrentName";

    //message的标识
    public static final int WHAT = 8;

    //activity的回调结果码
    public static final int resultCode = 9;

    //时间格式的区分码
    public static final int timeSimple = 10;
    public static final int timeFull = 11;
    public static final int timeHour = 12;

    //闹钟时间间隔模式标识
    public static final int NORMAL = 13;
    public static final int LOCATION = 14;
    public static int setResult = 15;

    //广播action
    public static String STOP_SERVICE = "STOP_SERVICE_BROADCASTSTOP_SERVICE_BROADCAST";
}
