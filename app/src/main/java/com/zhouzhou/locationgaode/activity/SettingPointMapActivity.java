package com.zhouzhou.locationgaode.activity;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.AMapUtils;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.Circle;
import com.amap.api.maps2d.model.CircleOptions;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.MyLocationStyle;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.zhouzhou.locationgaode.DBHelper;
import com.zhouzhou.locationgaode.R;
import com.zhouzhou.locationgaode.bean.Constant;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SettingPointMapActivity extends AppCompatActivity {

    @BindView(R.id.settings_map)
    MapView settingsMap;
    @BindView(R.id.btn_point_sure)
    Button btnPointSure;
    @BindView(R.id.activity_et_name)
    EditText activityEtName;
    @BindView(R.id.activity_et_search)
    EditText activityEtSearch;
    @BindView(R.id.activity_et_location)
    EditText activityEtLocation;
    @BindView(R.id.activity_et_down)
    EditText activityEtDown;
    @BindView(R.id.ll_map_search)
    LinearLayout llMapSearch;

    private AMap aMap = null;
    private Circle circle = null;
    private float radius = 0f;
    private DBHelper dbHelper = null;
    private SQLiteDatabase db = null;
    private LatLng point = null;

    private PoiSearch.OnPoiSearchListener mYOnPoiSearchListener = new MYOnPoiSearchListener();
    private View view = null;
    private ListView listView = null;
    private Marker marker = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_point_map);
        ButterKnife.bind(this);

        //数据库初始化
        dbHelper = new DBHelper(this);
        db = dbHelper.getReadableDatabase();
        //获得从settings活动带来的半径
        Intent intent = getIntent();
        String ra = intent.getStringExtra("radius");
        radius = Float.parseFloat(ra);

        //EditText文本框监听
        setEditTextWhater();
        init();
        settingsMap.onCreate(savedInstanceState);
    }
    /*
     *@Author: zhouzhou
     *@Date: 19-12-12
     *@Deecribe：初始化地图和弹窗布局
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */

    private void init() {

        if (aMap == null) {
            aMap = settingsMap.getMap();
        }
        setMystyle();//小蓝点样式
        aMap.moveCamera(CameraUpdateFactory.zoomTo(14.0f));//初始放大级别14
        aMap.setOnMapClickListener(mapClickListener);//注册地图单击事件
        //加载popuwindow布局
        view = getLayoutInflater().inflate(R.layout.activity_setting_point_map_popuwindow, null);
        //加载listview
        listView = (ListView) view.findViewById(R.id.activity_setting_map_lv);
        // 绑定 Marker 被点击事件
        aMap.setOnMarkerClickListener(markerClickListener);
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-12-12
     *@Deecribe：设置定位蓝点样式
     *@Params:[]
     *@Return:void
     *@Email：zhou.zhou@sim.com
     */
    private void setMystyle() {

        MyLocationStyle myLocationStyle;
        myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE);//定位一次，且将视角移动到地图中心点。
        //myLocationSty|le.interval(2000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
        //myLocationStyle.showMyLocation(true);
        myLocationStyle.radiusFillColor(Color.argb(0, 0, 0, 0))//精度圈填充颜色,argb(透明度,red,green,blue)(透明度ff完全不透明，0完全透明)
                .strokeColor(Color.argb(150, 12, 32, 56));//精度圈边框颜色
        aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
        aMap.getUiSettings().setMyLocationButtonEnabled(true);//设置默认定位按钮是否显示，非必需设置。
        aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-12-12
     *@Deecribe：高德地图单击事件回调
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    private AMap.OnMapClickListener mapClickListener = new AMap.OnMapClickListener() {
        @Override
        public void onMapClick(LatLng latLng) {
            //地图上有圆存在，则删除它，保证同一时间地图上只有一个圆
            if (circle != null) {
                circle.remove();
            }
            //地图上画圆
            drawCircle(latLng);
        }
    };

    /*
     *@Author: zhouzhou
     *@Date: 19-12-12
     *@Deecribe：在地图上画一个圆形
     *@Params:[latLng 坐标]
     *@Return:void
     *@Email：zhou.zhou@sim.com
     */
    private void drawCircle(LatLng latLng) {
        circle = aMap.addCircle(new CircleOptions().center(latLng)//中心点
                .radius(radius)//半径
                .strokeColor(Color.argb(100, 0, 221, 255))//边框颜色
                .fillColor(Color.argb(40, 0, 221, 255)).strokeWidth(5f));//填充颜色
        point = latLng;//保存当前圆心
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-12-12
     *@Deecribe：文本编辑框注册监听
     *@Params:[]
     *@Return:void
     *@Email：zhou.zhou@sim.com
     */
    private void setEditTextWhater() {
        myTextWatcher watcher = new myTextWatcher();
        activityEtSearch.addTextChangedListener(watcher);
        activityEtDown.addTextChangedListener(watcher);
    }

    @OnClick(R.id.btn_point_sure)
    public void onViewClicked() {
        if (point == null) {
            Toast.makeText(this, "您还未标点", Toast.LENGTH_SHORT).show();
        } else {
            //将保存的中心点坐标传回settingsActivity
            setResult(Constant.resultCode, new Intent().putExtra("latlng", new double[]{point.latitude, point.longitude}));
            finish();
        }
    }


    @OnClick(R.id.activity_et_search)
    public void onActivityEtSearchClicked() {
        if (activityEtName.getText().length() == 0) {
            Toast.makeText(this, "请输入搜索的名称", Toast.LENGTH_SHORT).show();
        } else {
            PoiSearch.Query query = new PoiSearch.Query(activityEtName.getText().toString(), "", activityEtLocation.getText().toString());
            //keyWord表示搜索字符串，
            //第二个参数表示POI搜索类型，二者选填其一，选用POI搜索类型时建议填写类型代码，码表可以参考下方（而非文字）
            //cityCode表示POI搜索区域，可以是城市编码也可以是城市名称，也可以传空字符串，空字符串代表全国在全国范围内进行搜索
            query.setPageSize(20);// 设置每页最多返回多少条poiitem
            query.setPageNum(1);//设置查询页码
            //构造 PoiSearch 对象，并设置监听。
            PoiSearch poiSearch = new PoiSearch(this, query);
            poiSearch.setOnPoiSearchListener(mYOnPoiSearchListener);

            poiSearch.searchPOIAsyn();//调用 PoiSearch 的 searchPOIAsyn() 方法发送请求。
        }
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-12-12
     *@Deecribe：弹出列表框
     *@Params:[poiResult 搜索回调的poi信息]
     *@Return:void
     *@Email：zhou.zhou@sim.com
     */
    private void showPopuwindow(final PoiResult poiResult) {
        listView.setAdapter(new MyAdapter(poiResult));//listview设置适配器
        final PopupWindow popupWindow = new PopupWindow(view);//初始化popupwindow
        popupWindow.setHeight(settingsMap.getHeight() / 2);//设置窗口高度为地图的一半
        popupWindow.setWidth(settingsMap.getWidth());//设置宽度为地图宽度
        popupWindow.setAnimationStyle(R.style.myPopuwindowStyle);//设置弹窗出现消失的动画
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#F8F8F8")));//设置弹窗背景颜色
        popupWindow.setFocusable(true);//设置弹窗允许获得焦点
        popupWindow.setOutsideTouchable(true);//设置触摸弹窗外消失
        //popupWindow.update();
        popupWindow.showAsDropDown(llMapSearch);//设置弹窗位于llmapsearch的正下方

        //listview单击事件
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //获得当前选择的poi对象
                PoiItem poiItem = poiResult.getPois().get(position);
                //搜索框显示poi名称
                activityEtName.setText(poiItem.getTitle());
                //获得选择的poi的经纬度
                LatLng latLngNow = new LatLng(poiItem.getLatLonPoint().getLatitude(), poiItem.getLatLonPoint().getLongitude());
                Location myLocation = aMap.getMyLocation();//获取用户自己的位置
                LatLng latLngTarget = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());//用户当前的位置
                float distance = AMapUtils.calculateLineDistance(latLngTarget, latLngNow);//用户与poi位置的距离
                int scaleMap = scaleMap(distance, (float) llMapSearch.getWidth());//获取缩放级别
                //获取用户与poi点的中间位置
                LatLng latLngMiddle = new LatLng((latLngNow.latitude + latLngTarget.latitude) / 2, (latLngNow.longitude + latLngTarget.longitude) / 2);
                aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngMiddle, scaleMap));//移动地图中心至两点中间，刚好呈现当前位置和选择的poi位置
                //保证当前地图只有一个marker点，有就删除重新添加，没有就直接添加
                if (null != marker) {
                    marker.remove();
                }
                marker = aMap.addMarker(new MarkerOptions()
                        .position(latLngNow)//设置坐标
                        //设置图标
                        .icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.location_point)))
                        .anchor((float)latLngNow.latitude,(float)latLngNow.longitude));//设置锚点

                //保证当前地图只有一个圆形范围，有就删除重新添加，没有就直接添加
                if (null != circle) {
                    circle.remove();
                }
                drawCircle(latLngNow);
                //给poi点添加marker点
                popupWindow.dismiss();//取消弹窗
            }
        });
    }

    /*
    *@Author: zhouzhou
    *@Date: 19-12-11
    *@Deecribe：50/100   return 18 、100/110 return 17、200/110 return 16；这些公式的代表意思就是：
        地图缩放比例是18时，实际地面距离50米 = 实际屏幕110像素距离，那比例就是  50/110 = 0.45M：1px
        地图缩放比例是17时，实际地面距离100米 = 实际屏幕110像素距离，那比例就是  100/110 =0.9M： 1px
        地图缩放比例是16时，实际地面距离200米 = 实际屏幕110像素距离，那比例就是  200/110 = 1.8M：1px
        地图缩放比例是15时，实际地面距离200米 = 实际屏幕50像素距离 .，那比例就是  200/50 = 4M：1px
        .......以此类推
        distance是所有坐标点中，两点距离最远的值
        curHeight是地图的总高度
    *@Params:[float distance 当前与poi点的距离, Float curHeight 当前屏幕宽度]
    *@Return:
    *@Email：zhou.zhou@sim.com
    */
    private int scaleMap(float distance, Float curHeight) {

        if (distance / curHeight <= 50 / 110)
            return 18;
        else if (distance / curHeight <= 100 / 110)
            return 17;
        else if (distance / curHeight <= 200 / 110)
            return 16;
        else if (distance / curHeight <= 200 / 50)
            return 15;
        else if (distance / curHeight <= 300 / 60)
            return 14;
        else if (distance / curHeight <= 1000 / 60)
            return 13;
        else if (distance / curHeight <= 2000 / 60)
            return 12;
        else if (distance / curHeight <= 5000 / 80)
            return 11;
        else if (distance / curHeight <= 10000 / 80)
            return 10;
        else if (distance / curHeight <= 20000 / 80)
            return 9;
        else if (distance / curHeight <= 50000 / 110)
            return 8;
        else if (distance / curHeight <= 100000 / 110)
            return 7;
        else if (distance / curHeight <= 100000 / 50)
            return 6;
        else if (distance / curHeight <= 200000 / 50)
            return 5;
        else if (distance / curHeight <= 500000 / 80)
            return 4;
        else if (distance / curHeight <= 1000000 / 60)
            return 3;
        return 3;
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-12-12
     *@Deecribe： 定义 Marker 点击事件监听
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */ AMap.OnMarkerClickListener markerClickListener = new AMap.OnMarkerClickListener() {
        // marker 对象被点击时回调的接口
        // 返回 true 则表示接口已响应事件，否则返回false
        @Override
        public boolean onMarkerClick(Marker marker) {
            if (aMap.getScalePerPixel() != 17f) {
                aMap.moveCamera(CameraUpdateFactory.zoomTo(17f));
            }
            return false;
        }
    };


    @OnClick(R.id.activity_et_down)
    public void onActivityEtDownClicked() {
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-12-12
     *@Deecribe：poi搜索结果回调监听
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    private class MYOnPoiSearchListener implements PoiSearch.OnPoiSearchListener {
        @Override
        public void onPoiSearched(PoiResult poiResult, int i) {
            Log.d("onPoiSearched", poiResult.getPois().toString());
            //搜索结果不为空则弹窗，为空则提示为搜索结果
            if (poiResult.getPois().size() > 0) {
                showPopuwindow(poiResult);
            } else {
                Toast.makeText(SettingPointMapActivity.this, "无搜索结果", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onPoiItemSearched(PoiItem poiItem, int i) {

        }
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-12-12
     *@Deecribe：listview的适配器类
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    class MyAdapter extends BaseAdapter {
        private PoiResult poiResult = null;

        public MyAdapter(PoiResult poiResult) {
            this.poiResult = poiResult;
        }

        @Override
        public int getCount() {
            return poiResult.getPois().size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (null == convertView) {
                convertView = getLayoutInflater().inflate(R.layout.activity_setting_point_map_popuwindow_listview_item, null);
                viewHolder = new ViewHolder();
                viewHolder.tv_listview_item_name = convertView.findViewById(R.id.tv_listview_item_name);
                viewHolder.tv_listview_item_adress = convertView.findViewById(R.id.tv_listview_item_adress);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            final PoiItem poiItem = poiResult.getPois().get(position);
            viewHolder.tv_listview_item_name.setText(poiItem.getTitle());
            if (poiItem.getBusinessArea().length() > 0) {
                String area = "-" + poiItem.getBusinessArea();
            }
            String area = poiItem.getBusinessArea();
            viewHolder.tv_listview_item_adress.setText(poiItem.getTypeDes().substring(poiItem.getTypeDes().lastIndexOf(";") + 1, poiItem.getTypeDes().length()) + ":" + poiItem.getCityName() + "-" + poiItem.getAdName() + area);
            return convertView;
        }

        class ViewHolder {
            TextView tv_listview_item_name;
            TextView tv_listview_item_adress;
        }
    }

    private class myTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            Log.d("TextWatcher", s.toString() + "beforeTextChanged");

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            Log.d("TextWatcher", s.toString() + "onTextChanged");

        }

        @Override
        public void afterTextChanged(Editable s) {
            Log.d("TextWatcher", s.toString() + "afterTextChanged");
            Toast.makeText(SettingPointMapActivity.this, s.toString() + "afterTextChanged", Toast.LENGTH_SHORT).show();
            activityEtSearch.clearFocus();
            activityEtName.clearFocus();
            if (activityEtLocation.getText().length() == 0) {
                activityEtName.requestFocus();
            }
            if (activityEtName.getText().length() == 0) {
                activityEtLocation.requestFocus();
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行settingsMap.onDestroy()，销毁地图
        settingsMap.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行settingsMap.onResume ()，重新绘制加载地图
        settingsMap.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行settingsMap.onPause ()，暂停地图的绘制
        settingsMap.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行settingsMap.onSaveInstanceState (outState)，保存地图当前的状态
        settingsMap.onSaveInstanceState(outState);
    }

}
