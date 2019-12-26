package com.zhouzhou.locationgaode.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.AMapUtils;
import com.amap.api.maps2d.model.LatLng;
import com.zhouzhou.locationgaode.DBHelper;
import com.zhouzhou.locationgaode.R;
import com.zhouzhou.locationgaode.activity.MapActivity;
import com.zhouzhou.locationgaode.bean.Constant;
import com.zhouzhou.locationgaode.bean.SignStatusInfo;
import com.zhouzhou.locationgaode.bean.SignStatusInfoFull;
import com.zhouzhou.locationgaode.bean.SignTableInfo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

/**
 * author : ZhouZhou
 * e-mail : zhou.zhou@sim.com
 * date   : 19-12-4下午1:14
 * desc   :
 * version: 1.0
 */
public class MyService extends Service {

    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    //声明定位回调监听器
    public AMapLocationListener mLocationListener = new MyAMapLocationListener();
    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;
    private DBHelper dbHelper;
    private SQLiteDatabase db;
    private float before = 0;//最新的一次定位距离
    private float after = 0;//上一次定位距离
    public static AlarmManager alarmManager = null;
    private SignStatusInfo statusInfo = null;
    private Boolean isIn = false;//是否能签到
    private Boolean isOut = false;//是否能签退
    private Intent alarmIntent = null;
    private PendingIntent pendingIntent = null;
    private SignStatusInfoFull statusInfoFull = null;

    private IntentFilter intentFilter = null;
    private BroadcastReceiver broadcastReceiver = null;

    public MyService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //注册动态广播
        broadcastReceiver = new MtyBroadcastReceiver();
        intentFilter = new IntentFilter();
        intentFilter.addAction(Constant.STOP_SERVICE);
        registerReceiver(broadcastReceiver, intentFilter);
        dbHelper = new DBHelper(this);
        db = dbHelper.getReadableDatabase();
        //时间在8:00/18:10点且未打卡就开启定位，直到打卡完成停止定位
        statusInfoFull = dbHelper.getStatusInfo(db, dbHelper.dateToString(new Date(), Constant.timeSimple));
        statusInfo = statusInfoFull.getStatusInfo();
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmIntent = new Intent(this, MyService.class);
        pendingIntent = PendingIntent.getService(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        ContentValues values = new ContentValues();
        //更新标识
        updateInfo(statusInfo, "isOpenYes");
        Log.d("yazhou", "onCreate");

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SignStatusInfo info = dbHelper.getStatusInfo(db, dbHelper.dateToString(new Date(), Constant.timeSimple)).getStatusInfo();
        Log.d("yazhou", "onStartCommand");
        if (info.getisOpen().equals("yes")) {
            Constant.setResult = Constant.NORMAL;
        } else {
            alarmManager.cancel(pendingIntent);
            stopService(alarmIntent);
        }
        Log.d("yazhou", "" + "isOpen?: " + info.getisOpen());
        if (determineTime().equals("start")) {
            if (this.statusInfo.getSignInSend() == 0) {
                Log.d("yazhou", "" + "getSignInSend?: " + info.getSignOutSend());
                isIn = true;
            }
        }
        if (determineTime().equals("stop")) {
            if (this.statusInfo.getSignOutSend() == 0) {
                isOut = true;
            }
        }
        if (isIn || isOut) {
            Constant.setResult = Constant.LOCATION;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //初始化定位
                    mLocationClient = new AMapLocationClient(getApplicationContext());
                    //设置定位回调监听
                    mLocationClient.setLocationListener(mLocationListener);
                    //给定位客户端对象设置定位参数
                    setOption();
                    //启动定位，
                    mLocationClient.startLocation();
                }
            }).start();
        }
        setAlarmClock(Constant.setResult);

        return super.onStartCommand(intent, flags, startId);
    }

    private class MyAMapLocationListener implements AMapLocationListener {
        @Override
        public void onLocationChanged(AMapLocation aMapLocation) {
            SignTableInfo tableInfo = dbHelper.queryInfo(db, Constant.name);
            statusInfo = dbHelper.getStatusInfo(db, dbHelper.dateToString(new Date(), Constant.timeSimple)).getStatusInfo();
            double radius = tableInfo.getRadius();
            float distance = AMapUtils.calculateLineDistance(new LatLng(tableInfo.getLatitude(), tableInfo.getLongitude()), new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude()));
//            if (distance > 100) {
//                if (statusInfo.getSignOutSend() == 0) {
//                    //间隔指定时间启动钉钉
//                    //timeCountNotification(statusInfo);
//                }
//            }
            if (isOut) {
                //到下班时间点直接签退，不管距离
                showNotification(Constant.OUT);
            } else if (isIn && distance < radius) {
                //进入公司
                showNotification(Constant.IN);
            }
            mLocationClient.stopLocation();
        }
    }

    /*
     *@Author: 18122
     *@Date: 2019/12/9
     *@Deecribe：设置闹钟
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    private void setAlarmClock(int type) {
        long time = 0;
        long timeNew = 0;
        int wakeup = -1000;
        long triggerAtMillis = 0;
        Intent alarmIntent = new Intent(this, MyService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pendingIntent);

        if (type == Constant.NORMAL) {
            time = 4 * 60 * 60 * 1000;
            timeNew = fixTime(time);
            wakeup = AlarmManager.ELAPSED_REALTIME;
            triggerAtMillis = SystemClock.elapsedRealtime() + timeNew;
            Log.d("yazhou", "setAlarmClock NORMAL");
        } else if (type == Constant.LOCATION) {
            Date toDate = dbHelper.stringToDate(dbHelper.queryInfo(db, Constant.name).getTimeFrequency(), Constant.timeHour);
            //获得设置的时间长度
            time = (toDate.getHours() > 0)?((toDate.getMinutes() > 0)?((toDate.getHours() * 60 + toDate.getMinutes()) * 60 * 1000):(toDate.getHours() * 60 * 60 *1000 )):(toDate.getMinutes() * 60 * 1000);
            Log.d("yazhou", "setAlarmClock LOCATION" + "  " + time + " ms");
            wakeup = AlarmManager.ELAPSED_REALTIME_WAKEUP;
            triggerAtMillis = SystemClock.elapsedRealtime() + time;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {// 6.0
            alarmManager.setExactAndAllowWhileIdle(wakeup, triggerAtMillis, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {//  4.4
            alarmManager.setExact(wakeup, triggerAtMillis, pendingIntent);
        } else {
            alarmManager.set(wakeup, triggerAtMillis, pendingIntent);
        }
        //        if (Constant.setResult == Constant.NORMAL){
        //            stopSelf();
        //        }
//        //定时启动代码
//        //获取当前毫秒值
//        long systemTime = System.currentTimeMillis();
//        //        Toast.makeText(this, "kk", Toast.LENGTH_SHORT).show();
//        Log.d("alaerzhou","w");
//        Calendar calendar = Calendar.getInstance();
//        //当前时区
//        calendar.setTimeZone(TimeZone.getTimeZone("GMT+8"));
//        //设置提醒时
//        calendar.set(Calendar.HOUR_OF_DAY,8);
//        //设置提醒分
//        calendar.set(Calendar.MINUTE,0);
//        //设置提醒秒
//        calendar.set(Calendar.SECOND,0);
//        //设置提醒微秒
//        calendar.set(Calendar.MILLISECOND,0);
//        //当前绝对时间
//        long setTime = calendar.getTimeInMillis();
//        // 如果当前时间大于设置的时间，那么就从第二天的设定时间开始
//        if(systemTime > setTime) {
//            calendar.add(Calendar.DAY_OF_MONTH, 1);
//        }
//        AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
//        Intent intent1 = new Intent(getApplicationContext(),MyService.class);
//        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(),0,intent1,0);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
//            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC,setTime,pendingIntent);
//        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
//            alarmManager.setExact(AlarmManager.RTC,setTime,pendingIntent);
//        }else{
//            alarmManager.set(AlarmManager.RTC,setTime,pendingIntent);
//        }
    }

    /*
    *@Author: zhouzhou
    *@Date: 19-12-25
    *@Deecribe：改进非定位时间段重启后，时间点落在离打卡点较远地方;
    *@Params:
    *@Return:
    *@Email：zhou.zhou@sim.com
    */

    private long fixTime(long time) {
        Date date = new Date();
        long timeNew = time;

        SignTableInfo tableInfo = dbHelper.queryInfo(db, Constant.name);
        Date toDateStart = dbHelper.stringToDate(tableInfo.getTimeStart(), Constant.timeHour);
        Date toDateStop = dbHelper.stringToDate(tableInfo.getTimeStop(), Constant.timeHour);
        Log.d("yazhou",toDateStart+"###1");

        Log.d("yazhou",toDateStop+"###1");
        Log.d("yazhou",date+"###1");

        long toDateStartTime = (toDateStart.getHours()-date.getHours()) * 60 * 60 * 1000 + (toDateStart.getMinutes() - date.getMinutes()) * 60 * 1000;
        if (date.getHours() < toDateStart.getHours()){
            if (toDateStartTime  < time){
                timeNew = toDateStartTime + 10 * 1000;//加１０秒是为了启动后时间肯定落在设定的打卡时间之后，直接进入location模式;
                Log.d("yazhou",timeNew+"###1");
                return timeNew;

            }
        }else if(date.getHours() == toDateStart.getHours()&& date.getMinutes() <= toDateStart.getMinutes()){
            if (toDateStartTime  < time){

                timeNew = toDateStartTime + 10 * 1000;//加１０秒是为了启动后时间肯定落在设定的打卡时间之后，直接进入location模式;
                Log.d("yazhou",timeNew+"###2");
                return timeNew;
            }
        }


        long toDateStopTime =  (toDateStop.getHours()-date.getHours()) * 60 * 60 * 1000 + (toDateStop.getMinutes() - date.getMinutes()) * 60 * 1000;
        if (date.getHours() < toDateStop.getHours() ){
            if (toDateStopTime  < time){

                timeNew = toDateStopTime + 10 * 1000;//加１０秒是为了启动后，时间肯定落在设定的打卡时间之后，直接进入location模式;
                Log.d("yazhou",timeNew+"###3");
                return timeNew;
            }
        }else if (date.getHours() == toDateStop.getHours() && date.getMinutes() <= toDateStop.getMinutes()){
            if (toDateStopTime  < time){

                timeNew = toDateStopTime + 10 * 1000;//加１０秒是为了启动后，时间肯定落在设定的打卡时间之后，直接进入location模式;
                Log.d("yazhou",timeNew+"###4");
                return timeNew;
            }
        }
        Log.d("yazhou",timeNew+"###0");
        return timeNew;
    }

    /*
     *@Author: 18122
     *@Date: 2019/12/9
     *@Deecribe：定位相关参数设置
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    private void setOption() {
        if (null != mLocationOption) {
            //初始化AMapLocationClientOption对象
            mLocationOption = new AMapLocationClientOption();
            mLocationOption.setOnceLocation(true);
            //设置定位间隔,单位毫秒,默认为2000ms，最低1000ms。
            //mLocationOption.setInterval(5000);
            //设置定位模式为AMapLocationMode.Hight_Accuracy，低功耗模式。
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Battery_Saving);
            //设置是否允许模拟位置,默认为true，允许模拟位置
            mLocationOption.setMockEnable(true);
            //单位是毫秒，默认30000毫秒，建议超时时间不要低于8000毫秒。
            mLocationOption.setHttpTimeOut(20000);
            //关闭缓存机制
            mLocationOption.setLocationCacheEnable(false);
        }
        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-11-28
     *@Deecribe：判断当前时间是否在时间段内
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    private String determineTime() {
        //LocalDateTime now = LocalDateTime.now();
        String result = "";
        GregorianCalendar gre = new GregorianCalendar();
        //Date date = new Date(now.getYear() - 1900, now.getMonthValue() - 1, now.getDayOfMonth()); //年要减去1900，月份是0-11
        Date date = new Date();//当前时间
        Date date1 = new Date();//开始时间
        Date date2 = new Date();//结束时间
        gre.setTime(date);
        int weekday = gre.get(Calendar.DAY_OF_WEEK) - 1; //0是星期天
        if (weekday > 5) {
            result = "dayFalse";
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        SignTableInfo info = dbHelper.queryInfo(db, Constant.name);
        //开始时间
        try {
            date1 = simpleDateFormat.parse(info.getTimeStart());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        //结束时间
        try {
            date2 = simpleDateFormat.parse(info.getTimeStop());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (date.getHours() >= date2.getHours() && date.getMinutes() >= date2.getMinutes()) {
            result = result + "stop";
        } else if (date.getHours() >= date1.getHours() && date.getMinutes() >= date1.getMinutes()) {
            result = result + "start";
        }
        return result;
    }

    private void updateInfo(SignStatusInfo statusInfo, String type) {
        ContentValues values = new ContentValues();
        switch (type) {
            case "signInSend":
                statusInfo.setSignInSend(1);
                break;
            case "signOutSend":
                statusInfo.setsignOutSend(1);
                isAddNowadays();
                break;
            case "isOpenYes":
                statusInfo.setisOpen("yes");
                break;
            case "isOpenNo":
                statusInfo.setisOpen("no");
                break;
        }
        String json = dbHelper.toJson(statusInfo);
        values.put("info", json);
        dbHelper.upSignStatus(db, values, dbHelper.dateToString(new Date(), Constant.timeSimple));
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-12-6
     *@Deecribe：打开钉钉
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    public void openDing(String packageName, Context context, String type, SignStatusInfo statusInfo) {

        isIn = false;
        isOut = false;
        PackageManager packageManager = context.getPackageManager();
        PackageInfo pi = null;
        try {
            pi = packageManager.getPackageInfo("com.alibaba.android.rimet", 0);
        } catch (PackageManager.NameNotFoundException e) {
        }
        Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
        resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        resolveIntent.setPackage(pi.packageName);
        List<ResolveInfo> apps = packageManager.queryIntentActivities(resolveIntent, 0);
        ResolveInfo resolveInfo = apps.iterator().next();
        if (resolveInfo != null) {
            String className = resolveInfo.activityInfo.name;
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ComponentName cn = new ComponentName(packageName, className);
            intent.setComponent(cn);
            context.startActivity(intent);
        }
        //已打卡保存
        updateInfo(statusInfo, type);
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-12-2
     *@Deecribe：计时器
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    private Timer timer = new Timer();
    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            if (statusInfo.getSignOutSend() == 1) {
                timer.cancel();
            } else {
                showNotification(Constant.OUT);
                timer.cancel();
            }
        }
    };

    private void timeCountNotification(final SignStatusInfo statusInfo) {
        String timeQuantum = dbHelper.queryInfo(db, Constant.name).getTimeQuantum();
        Date date = dbHelper.stringToDate(timeQuantum, 0);
        long longtime = ((date.getHours() * 60) + date.getMinutes()) * 60 * 1000;
        timer.schedule(task, longtime);

    }

    private Vibrator vibrator = null;//震动相关
    //通知栏相关
    private NotificationManager manager = null;
    private NotificationCompat.Builder builder = null;
    private Notification notification = null;

    /*
     *@Author: 18122
     *@Date: 2019/12/9
     *@Deecribe：通知栏提醒
     *@Params:type ：签到还是签退
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    private void showNotification(int type) {
        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(Constant.ID, "b", NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(notificationChannel);
        }
        Intent intent = new Intent(this, MapActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder = new NotificationCompat.Builder(this).setChannelId(Constant.ID).setWhen(System.currentTimeMillis())//通知栏显示时间
                .setSmallIcon(R.mipmap.ic_launcher)//通知栏小图标
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))//通知栏下拉是图标
                .setContentIntent(pendingIntent)//关联点击通知栏跳转页面
                .setPriority(NotificationCompat.PRIORITY_MAX)//设置通知消息优先级
                .setAutoCancel(true)//设置点击通知栏消息后，通知消息自动消失
                //                .setSound(Uri.fromFile(new File("/system/MP3/music.mp3"))) //通知栏消息提示音
                .setVibrate(new long[]{0, 1000, 1000, 1000}) //通知栏消息震动
                .setLights(Color.GREEN, 1000, 2000); //通知栏消息闪灯(亮一秒间隔两秒再亮)
        //.setDefaults(NotificationCompat.DEFAULT_ALL); //通知栏提示音、震动、闪灯等都设置为默认
        vibrator = (Vibrator) getApplicationContext().getSystemService(Service.VIBRATOR_SERVICE);//震动
        vibrator.vibrate(1000);//震动1秒
        if (type == Constant.IN) {
            builder.setContentText("上班了签到喽");
            notification = builder.build();
            manager.notify(Constant.IN, notification);
            try {
                openDing("com.alibaba.android.rimet", getApplicationContext(), "signInSend", statusInfo);//吊起钉钉
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else if (type == Constant.OUT) {
            builder.setContentText("下班了签退喽");
            notification = builder.build();
            manager.notify(Constant.IN, notification);
            try {
                openDing("com.alibaba.android.rimet", getApplicationContext(), "signOutSend", statusInfo);//吊起钉钉
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-12-5
     *@Deecribe：添加当日或下一日状态表
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    private void isAddNowadays() {
        statusInfo = new SignStatusInfo();
        ContentValues values = new ContentValues();
        values.put("UserName", Constant.name);
        values.put("info", dbHelper.toJson(statusInfo));
        if (!dbHelper.querySignStatus(db, dbHelper.dateToString(new Date(), Constant.timeSimple))) {
            values.put("nowadays", dbHelper.dateToString(new Date(), Constant.timeSimple));
            if (dbHelper.addSignStatus(db, values, dbHelper.dateToString(new Date(), Constant.timeSimple))) {
                Log.d("初始化", "成功");
            } else {
                Log.d("初始化", "失败");
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.d("yazhou", "onDestroy");
        if (mLocationClient != null) {
            mLocationClient.onDestroy();
        }
        try {
            //  unregisterReceiver(mGeoFenceReceiver);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        mLocationClient.unRegisterLocationListener(mLocationListener);
        //应用结束
        mLocationClient.stopLocation();
        mLocationClient.onDestroy();
        updateInfo(statusInfo, "isOpenNo");
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    private class MtyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int stop = intent.getIntExtra("stop", 3);
            stopSelf();
            if (stop == 2) {
                alarmManager.cancel(pendingIntent);
            }


            Log.d("yazhou", "MtyBroadcastReceiver");

        }
    }
}