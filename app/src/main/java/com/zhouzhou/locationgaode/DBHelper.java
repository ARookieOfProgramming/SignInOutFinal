package com.zhouzhou.locationgaode;

import android.app.ActivityManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.zhouzhou.locationgaode.bean.Constant;
import com.zhouzhou.locationgaode.bean.SignStatusInfo;
import com.zhouzhou.locationgaode.bean.SignStatusInfoFull;
import com.zhouzhou.locationgaode.bean.SignTableInfo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * author : ZhouZhou
 * e-mail : zhou.zhou@sim.com
 * date   : 19-11-25下午4:03
 * desc   :
 * version: 1.0
 */
public class DBHelper extends SQLiteOpenHelper {

    private Context mContext;
    private static final String patternSimple = "yyyy:MM:dd";
    private static final String patternFull = "yyyy:MM:dd HH:mm";
    private static final String patternHour = "HH:mm";

    public static final String originName = "OriginName";
    private static final String SQName = "SignIn";
    private static final int VERSION = 1;
    private static final String CREATE_STATUS = "create table Status(" + " id Integer primary key autoincrement," + " UserName text," + " Time text," + " Radius text," + " Lat text," + " Lng text," + " TimeStart text," + "TimeStop text," + " TimeFrequency text)";
    private static final String CREATE_SIGNSTATUS = "create table SignStatus(" + " id Integer primary key autoincrement," + "  UserName text," + "  nowadays text," + "info text)";

    public DBHelper(@Nullable Context context) {
        super(context, SQName, null, VERSION);
        this.mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_STATUS);
        db.execSQL(CREATE_SIGNSTATUS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists Status");
        db.execSQL("drop table if exists SignStatus");
    }


    /*
     *@Author: zhouzhou
     *@Date: 19-11-29
     *@Deecribe：是否有自定义的设置数据（根据用户名查询）
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    public String queryStatusName(SQLiteDatabase db, String type) {

        db = this.getReadableDatabase();
        String result = "";
        Cursor cursor = null;
        try {
            if (db != null) {
                cursor = db.query("Status", null, "UserName = ?", new String[]{type}, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToNext()) {
                        result = "true";
                    } else {
                        result = "false";
                    }
                }
            }
        } catch (Exception e) {
        } finally {
            db.close();
        }
        return result;
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-11-29
     *@Deecribe：添加设置数据
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    public Boolean addStatusData(SQLiteDatabase db, ContentValues values, String type) {
        db = this.getReadableDatabase();
        Boolean isSuccess = false;
        try {
            db.insert("Status", null, values);
        } catch (Exception e) {
        } finally {
            db.close();
        }
        if (queryStatusName(db, type).equals("true")) {
            isSuccess = true;
        }
        return isSuccess;
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-11-28
     *@Deecribe：更新定位设置数据
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    public void updateSettings(SQLiteDatabase db, SignTableInfo info) {
        db = this.getReadableDatabase();
        Boolean isExist = false;
        Cursor cursor = null;
        //UserName text, Time text, Radius text, Issign text, Lat real, Lon real, TimeStart text,TimeStop text
        ContentValues values = new ContentValues();
        values.put("UserName", Constant.name);
        values.put("Time", info.getTimeQuantum());
        values.put("TimeStart", info.getTimeStart());
        values.put("TimeStop", info.getTimeStop());
        values.put("Radius", info.getRadius());
        values.put("Lat", info.getLatitude());
        values.put("Lng", info.getLongitude());
        values.put("TimeFrequency", info.getTimeFrequency());
        try {
            cursor = db.query("Status", null, "UserName = ?", new String[]{Constant.name}, null, null, null);
            if (cursor.moveToNext()) {
                isExist = true;
            }
        } catch (Exception e) {

        } finally {
            db.close();
        }
        if (isExist) {
            updateSet(db, values);
        } else {
            addStatusData(db, values, Constant.name);
        }

    }

    public void updateSet(SQLiteDatabase db, ContentValues values) {
        db = getReadableDatabase();
        try {
            db.update("Status", values, "UserName = ?", new String[]{Constant.name});
        } catch (Exception e) {

        } finally {
            db.close();
        }
    }

    /*
     *@Author: zhouzhou
     *@Date: 2019/11/28
     *@Deecribe：取signTableinfo数据
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    public SignTableInfo queryInfo(SQLiteDatabase db, String userName) {
        SignTableInfo info = new SignTableInfo();
        db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            //new String[]{"Time", "Issign", "Lat", "Lng", "TimeStart", "TimeStop"}
            cursor = db.query("Status", null, "UserName = ?", new String[]{userName}, null, null, null);
            if (cursor.moveToNext()) {
                info.setLatitude(Double.parseDouble(getString(cursor, "Lat")));
                info.setLongitude(Double.parseDouble(cursor.getString(cursor.getColumnIndex("Lng"))));
                info.setRadius(Double.parseDouble(getString(cursor, "Radius")));
                info.setTimeQuantum(getString(cursor, "Time"));
                info.setTimeStart(getString(cursor, "TimeStart"));
                info.setTimeStop(getString(cursor, "TimeStop"));
                info.setTimeFrequency(getString(cursor, "TimeFrequency"));
            }
        } catch (Exception e) {

        } finally {
            db.close();
        }
        return info;
    }

    /*
     *@Author: zhouzhou
     *@Date: 2019/11/28
     *@Deecribe：简化代码量
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    private String getString(Cursor cursor, String str) {
        return cursor.getString(cursor.getColumnIndex(str));
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-12-2
     *@Deecribe：查询signStatus表
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    public Boolean querySignStatus(SQLiteDatabase db, String nowadays) {
        db = getReadableDatabase();
        Boolean isTrue = false;
        Cursor cursor = null;
        try {
            cursor = db.query("SignStatus", null, "UserName = ? and nowadays = ?", new String[]{Constant.name, nowadays}, null, null, null);
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    isTrue = true;
                }
            }
        } catch (Exception e) {
            String w = e.getMessage();
        } finally {
            db.close();
        }
        return isTrue;
    }
    /*
     *@Author: zhouzhou
     *@Date: 19-12-3
     *@Deecribe：返回statusInfo
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */

    public SignStatusInfoFull getStatusInfo(SQLiteDatabase db, String nowadays) {
        db = getReadableDatabase();
        SignStatusInfoFull statusInfo = new SignStatusInfoFull();
        Cursor cursor = null;
        try {
            cursor = db.query("SignStatus", null, "UserName = ? and nowadays = ?", new String[]{Constant.name, nowadays}, null, null, null);
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    statusInfo.setNowadays(nowadays);
                    statusInfo.setUserName(Constant.name);
                    statusInfo.setStatusInfo(fromJson(cursor.getString(cursor.getColumnIndex("info"))));
                }
            }
        } catch (Exception e) {
            String w = e.getMessage();
        } finally {
            db.close();
        }
        return statusInfo;
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-12-2
     *@Deecribe：增加数据
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    public boolean addSignStatus(SQLiteDatabase db, ContentValues values, String nowadays) {
        db = getReadableDatabase();
        boolean isTrue = false;
        Cursor cursor = null;
        try {
            db.insert("SignStatus", null, values);
        } catch (Exception e) {

        } finally {
            db.close();
        }
        if (querySignStatus(db, nowadays)) {
            isTrue = true;
        }
        return isTrue;
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-12-2
     *@Deecribe：更新当天数据
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    public void upSignStatus(SQLiteDatabase db, ContentValues values, String nowadays) {
        db = getReadableDatabase();
        try {
            db.update("SignStatus", values, "UserName = ? and nowadays = ?", new String[]{Constant.name, nowadays});
        } catch (Exception e) {
            String mess = e.getMessage();
        } finally {
            db.close();
        }
    }
    /*
     *@Author: zhouzhou
     *@Date: 19-12-2
     *@Deecribe：更新成下一天数据
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    public void upSignStatusNext(SQLiteDatabase db, ContentValues values) {
        db = getReadableDatabase();
        try {
            db.update("SignStatus", values, "UserName = ?", new String[]{Constant.name});
        } catch (Exception e) {
            String mess = e.getMessage();
        } finally {
            db.close();
        }
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-12-3
     *@Deecribe：日期转化为字符串
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    public String dateToString(Date date, int type) {
        SimpleDateFormat format = null;
        if (Constant.timeFull == type) {
            format = new SimpleDateFormat(patternFull);
        } else {
            format = new SimpleDateFormat(patternSimple);
        }

        String dateString = "";
        if (null != date) {
            dateString = format.format(date);
        }
        return dateString;
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-12-3
     *@Deecri字符串转化为日期
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    public Date stringToDate(String dateString, int type) {
        SimpleDateFormat format = null;
        if (Constant.timeFull == type) {
            format = new SimpleDateFormat(patternFull);
        } else if(Constant.timeSimple == type){
            format = new SimpleDateFormat(patternSimple);
        }else{
            format = new SimpleDateFormat(patternHour);
        }
        Date date = null;
        try {
            date = format.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-12-3
     *@Deecribe：转换成json
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    public String toJson(SignStatusInfo info) {
        Gson gson = new Gson();
        String toJson = gson.toJson(info);
        return toJson;
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-12-3
     *@Deecribe：json转换回来
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    public SignStatusInfo fromJson(String json) {
        Gson gson = new Gson();
        SignStatusInfo statusInfo = gson.fromJson(json, SignStatusInfo.class);
        return statusInfo;
    }
    /*
     *@Author: zhouzhou
     *@Date: 19-12-5
     *@Deecribe：判断服务是否正在运行
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    public   boolean isServiceRunning(Context context, String ServiceName) {
        if (TextUtils.isEmpty(ServiceName)) {
            return false;
        }
        ActivityManager myManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningServiceInfo> runningService = (ArrayList<ActivityManager.RunningServiceInfo>) myManager
                .getRunningServices(30);
        for (int i = 0; i < runningService.size(); i++) {
            if (runningService.get(i).service.getClassName().toString()
                    .equals(ServiceName)) {
                return true;
            }
        }
        return false;
    }
}
