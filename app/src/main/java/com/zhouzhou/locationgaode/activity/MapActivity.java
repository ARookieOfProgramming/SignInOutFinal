package com.zhouzhou.locationgaode.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.content.PermissionChecker;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.Circle;
import com.amap.api.maps2d.model.CircleOptions;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.MyLocationStyle;
import com.zhouzhou.locationgaode.DBHelper;
import com.zhouzhou.locationgaode.R;
import com.zhouzhou.locationgaode.bean.Constant;
import com.zhouzhou.locationgaode.bean.SignStatusInfo;
import com.zhouzhou.locationgaode.bean.SignStatusInfoFull;
import com.zhouzhou.locationgaode.bean.SignTableInfo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MapActivity extends AppCompatActivity {


    @BindView(R.id.map)
    MapView map;

    //定义接收广播的action字符串
    public static final String GEOFENCE_BROADCAST_ACTION = "com.location.apis.geofencedemo.broadcast";
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.design_navigation_view)
    NavigationView designNavigationView;
    @BindView(R.id.design_drawer_view)
    DrawerLayout designDrawerView;
    @BindView(R.id.btn_sign)
    Button btnSign;

    private static final String MYSERVICENAME = "com.zhouzhou.locationgaode.service.MyService";
    @BindView(R.id.btn_map_location)
    Button btnMapLocation;
    private AMapLocationClient mLocationClient = null;//声明AMapLocationClient类对象
    // private AMapLocationListener mLocationListener = new myAMapLocationListener();//声明定位回调监听器
    private Boolean isPassed = false;//权限通过
    private AMapLocationClientOption mLocationOption = null;//定位参数设置
    private AMap aMap = null;
    private MyLocationStyle myLocationStyle = null;//蓝点style设置
    private Circle circle = null;//签到圆
    private DBHelper dbHelper;
    private SQLiteDatabase db;
    private SignStatusInfoFull statusInfoFull = null;
    private SignStatusInfo statusInfo = null;
    private ContentValues values = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        ButterKnife.bind(this);
        //初始化数据库相关
        dbHelper = new DBHelper(this);
        db = dbHelper.getReadableDatabase();

        //获取权限
        getPerssions();

        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        map.onCreate(savedInstanceState);

        //初始化标题栏
        initToolBar();
        try {
            statusInfo = dbHelper.getStatusInfo(db, dbHelper.dateToString(new Date(), Constant.timeSimple)).getStatusInfo();
        } catch (Exception e) {
        }
    }

    /*
     *@Author: zhouzhoou
     *@Date: 2019/12/9
     *@Deecribe：地图相关参数设置
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    private void init() {
        if (aMap == null) {
            aMap = map.getMap();
            aMap.moveCamera(CameraUpdateFactory.zoomTo(16));
        }
        aMap.getUiSettings().setScaleControlsEnabled(true);//显示比例尺
        aMap.getUiSettings().setZoomControlsEnabled(false);
        aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false
        mLocationClient = new AMapLocationClient(getApplicationContext());//初始化定位
        // mLocationClient.setLocationListener(mLocationListener);//设置定位回调监听
        mLocationClient.setLocationOption(setOption());//设置option
        aMap.setMyLocationStyle(setMyLocationType());//设置定位蓝点的Style
    }

    /*
     *@Author: zhouzhou
     *@Date: 2019/12/9
     *@Deecribe：初始化标题栏
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    private void initToolBar() {
        toolbar.setLogo(R.drawable.sign_in);
        toolbar.setNavigationIcon(R.drawable.menu);
        toolbar.setTitle(" 考勤打卡");
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        setSupportActionBar(toolbar);
        designNavigationView.setNavigationItemSelectedListener(new mYOnNavigationItemSelectedListener());

        Boolean isRunning = dbHelper.isServiceRunning(this, MYSERVICENAME);
        String sign = "";
        if (!isRunning) {
            btnSign.setText("开启提醒");
        } else if (isRunning) {
            btnSign.setText("关闭提醒");
        }

    }

    /*
     *@Author: zhouzhou
     *@Date: 19-11-27
     *@Deecribe：在地图上添加一个圆
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    private void addCircle(LatLng latLng) {
        circle = aMap.addCircle(new CircleOptions().center(latLng)//中心点
                .radius((float) dbHelper.queryInfo(db, Constant.name).getRadius())//半径
                .strokeColor(Color.argb(100, 0, 221, 255))//边框颜色
                .fillColor(Color.argb(40, 0, 221, 255)).strokeWidth(5f));//填充颜色
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-11-27
     *@Deecribe：定位蓝点样式设置
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    private MyLocationStyle setMyLocationType() {
        if (myLocationStyle == null) {
            myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
            //myLocationStyle
            myLocationStyle.radiusFillColor(Color.argb(0, 0, 0, 0))//精度圈填充颜色,argb(透明度,red,green,blue)(透明度ff完全不透明，0完全透明)
                    .strokeColor(Color.argb(150, 12, 32, 56))//精度圈边框颜色
                    .showMyLocation(true)//设置是否显示定位小蓝点，用于满足只想使用定位，不想使用定位小蓝点的场景，设置false以后图面上不再有定位蓝点的概念，但是会持续回调位置信息。
                    .interval(1000)//设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
                    .myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW);//蓝点定位模式
            //.myLocationIcon();//蓝点图标
            if (null != mLocationClient) {
                mLocationClient.setLocationOption(setOption());
                //设置场景模式后最好调用一次stop，再调用start以保证场景模式生效
                mLocationClient.stopLocation();
                mLocationClient.startLocation();
            }
        }
        return myLocationStyle;
    }

    @OnClick(R.id.btn_sign)
    public void onViewClicked() {
        boolean serviceRunning = dbHelper.isServiceRunning(this, MYSERVICENAME);
        //开启提醒
        Log.d("yazhou", "serviceRunning:  " + serviceRunning);
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.zhouzhou.locationgaode", MYSERVICENAME));
        if (!serviceRunning) {
            btnSign.setText("关闭提醒");
            startService(intent);
        }
        //取消提醒
        else {
            btnSign.setText("开启提醒");
            //stopService(intent);
            sendBroadcast(new Intent(Constant.STOP_SERVICE).putExtra("stop",2));
        }
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-12-13
     *@Deecribe：自定义按钮定位到当前位置
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    @OnClick(R.id.btn_map_location)
    public void onViewBtnMapLocationClicked() {

        Location myLocation = aMap.getMyLocation();
        aMap.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(myLocation.getLatitude(),myLocation.getLongitude())));
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-11-27
     *@Deecribe：地图初始化相关属性
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    private AMapLocationClientOption setOption() {
        if (mLocationOption == null) {
            mLocationOption = new AMapLocationClientOption();
            // 设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
            mLocationOption.setOnceLocation(true);
            mLocationOption.setSensorEnable(true);
            mLocationOption.setInterval(1000);
        }
        return mLocationOption;
    }

    private void updateInfo(String type) {
        ContentValues values = new ContentValues();
        switch (type) {
            case "review":
                statusInfo.setSignInSend(0);
                statusInfo.setsignOutSend(0);
                break;
        }
        String json = dbHelper.toJson(statusInfo);
        values.put("info", json);
        dbHelper.upSignStatus(db, values, dbHelper.dateToString(new Date(), Constant.timeSimple));
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-11-27
     *@Deecribe：动态获取权限
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    private void getPerssions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> perssionList = new ArrayList<>();
            if (PermissionChecker.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                perssionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (PermissionChecker.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                perssionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            if (perssionList.size() > 0) {
                requestPermissions(perssionList.toArray(new String[perssionList.size()]), 1);
            }
        }
    }

    //    /*
    //     *@Author: zhouzhou
    //     *@Date: 19-11-27
    //     *@Deecribe：权限获取回调
    //     *@Params:
    //     *@Return:
    //     *@Email：zhou.zhou@sim.com
    //     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //权限通过
                    //                    //初始化数据
                    //                    initData();
                } else {
                    finish();
                }
        }
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-12-5
     *@Deecribe：初始化设置参数数据
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    private void initData() {
        if (dbHelper.queryStatusName(db, dbHelper.originName).equals("false")) {
            ContentValues values = new ContentValues();
            values.put("Time", "00:02");
            values.put("Radius", "100");
            values.put("Lat", "39.913385");
            values.put("Lng", "116.405706");
            values.put("TimeStart", "08:00");
            values.put("TimeStop", "18:00");
            values.put("TimeFrequency", "00:01");
            values.put("UserName", Constant.name);
            if (dbHelper.addStatusData(db, values, Constant.name)) {
                Log.d("CurrentName", "succeed");
            }
            values.put("UserName", "OriginName");
            if (dbHelper.addStatusData(db, values, dbHelper.originName)) {
                Toast.makeText(this, "初始化数据成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "初始化数据失败", Toast.LENGTH_SHORT).show();
            }
            SharedPreferences spf = getSharedPreferences("sign", MODE_MULTI_PROCESS);
            SharedPreferences.Editor editor = spf.edit();
            editor.putInt("signIdentity", 0);
            editor.apply();
        }
        isAddNowadays();
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-12-5
     *@Deecribe：添加当日状态表
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    private void isAddNowadays() {
        if (!dbHelper.querySignStatus(db, dbHelper.dateToString(new Date(), Constant.timeSimple))) {
            statusInfo = new SignStatusInfo();
            ContentValues values = new ContentValues();
            values.put("UserName", Constant.name);
            values.put("nowadays", dbHelper.dateToString(new Date(), Constant.timeSimple));
            values.put("info", dbHelper.toJson(statusInfo));
            if (dbHelper.addSignStatus(db, values, dbHelper.dateToString(new Date(), Constant.timeSimple))) {
                Log.d("isAddNowadays", "succeed");
            } else {
                Log.d("isAddNowadays", "failure");
            }
        }
        statusInfoFull = dbHelper.getStatusInfo(db, dbHelper.dateToString(new Date(), Constant.timeSimple));
        statusInfo = statusInfoFull.getStatusInfo();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
                        case R.id.action_location:
                            updateInfo("review");//还原状态表,方便测试时使用
                            break;
            case android.R.id.home:
                //打开左边侧滑栏
                designDrawerView.openDrawer(GravityCompat.START);
                break;
            case R.id.toolbar_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }

        return true;
    }


    private class mYOnNavigationItemSelectedListener implements NavigationView.OnNavigationItemSelectedListener {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.nav_view_settings:
                    startActivity(new Intent(MapActivity.this, SettingsActivity.class));
                    break;
                case R.id.nav_view_about:
                    startActivity(new Intent(MapActivity.this, AboutActivity.class));
                    break;
                case R.id.nav_view_login_out:
                    finish();
                    break;
            }
            return true;
        }
    }

    @Override
    protected void onDestroy() {
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        if (null != map) {
            map.onDestroy();
        }
        if (mLocationClient != null) {
            mLocationClient.onDestroy();
        }
        try {
            //  unregisterReceiver(mGeoFenceReceiver);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        sendBroadcast(new Intent(Constant.STOP_SERVICE).putExtra("stop",1));
        super.onDestroy();
    }


    @Override
    protected void onResume() {
        super.onResume();
        init();
        SignTableInfo info = dbHelper.queryInfo(db, Constant.name);
        double v1 = info.getLatitude();
        double v2 = info.getLongitude();
        if (circle != null) {
            circle.remove();
        }
        addCircle(new LatLng(v1, v2));//画圆
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        map.onResume();
        initData();
        statusInfoFull = dbHelper.getStatusInfo(db, dbHelper.dateToString(new Date(), Constant.timeSimple));
        statusInfo = statusInfoFull.getStatusInfo();

    }

    @Override
    protected void onPause() {

        //设置进入后台后蓝点不定位模式
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_SHOW);
        aMap.setMyLocationStyle(myLocationStyle);;
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        map.onPause();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        map.onSaveInstanceState(outState);
    }
}
