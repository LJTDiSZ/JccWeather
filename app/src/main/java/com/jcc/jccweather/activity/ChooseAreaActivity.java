package com.jcc.jccweather.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.jcc.jccweather.R;
import com.jcc.jccweather.db.JccWeatherDB;
import com.jcc.jccweather.model.City;
import com.jcc.jccweather.model.County;
import com.jcc.jccweather.model.Province;
import com.jcc.jccweather.util.HttpCallbackListener;
import com.jcc.jccweather.util.HttpUtil;
import com.jcc.jccweather.util.Utility;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChooseAreaActivity extends AppCompatActivity {
    private static final String TAG = "ChooseArea";
    private static final int SHOW_TEMP = 1;
    private static final int SHOW_ERROR = -1;

    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;
    private TextView titleText;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private JccWeatherDB jccWeatherDB;
    private List<String> dataList = new ArrayList<String>();

    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;
    private Province selectedProvince;
    private City selectedCity;
    private County selectedCounty;
    private int currentLevel;

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case SHOW_ERROR:
                    break;
                case SHOW_TEMP:
                    String sInfo = (String)msg.obj;
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(ChooseAreaActivity.this);
                    dialogBuilder.setTitle("Warning");
                    dialogBuilder.setMessage(sInfo);
                    dialogBuilder.setCancelable(false);
                    dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    });
                    AlertDialog alertDialog = dialogBuilder.create();
                    alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                    alertDialog.show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_area);

        listView = (ListView)findViewById(R.id.list_view);
        titleText = (TextView)findViewById(R.id.title_text);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        jccWeatherDB = JccWeatherDB.getInstance(this);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (currentLevel == LEVEL_PROVINCE){
                    selectedProvince = provinceList.get(i);
                    queryCities();
                } else if (currentLevel == LEVEL_CITY){
                    selectedCity = cityList.get(i);
                    queryCounties();
                } else if (currentLevel == LEVEL_COUNTY) {
                    selectedCounty = countyList.get(i);
                    queryWeatherFromServer(selectedCounty.getCountyCode());
                }
            }
        });
        queryProvinces();
    }

    /**
     * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询。
     */
    private void queryProvinces() {
        provinceList = jccWeatherDB.loadProvinces();
        if (provinceList.size() > 0) {
            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName() + " | " + province.getProvinceCode());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText("中国");
            currentLevel = LEVEL_PROVINCE;
        } else {
            queryFromCSV(null, "province");
        }
    }
    /**
     * 查询选中省内所有的市，优先从数据库查询，如果没有查询到再去服务器上查询。
     */
    private void queryCities() {
        cityList = jccWeatherDB.loadCities(selectedProvince.getId());
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName() + " | " + city.getCityCode());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedProvince.getProvinceName());
            currentLevel = LEVEL_CITY;
        } else {
            queryFromCSV(selectedProvince.getProvinceCode(), "city");
        }
    }
    /**
     * 查询选中市内所有的县，优先从数据库查询，如果没有查询到再去服务器上查询。
     */
    private void queryCounties() {
        countyList = jccWeatherDB.loadCounties(selectedCity.getId());
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName() + " | " + county.getCountyCode());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedCity.getCityName());
            currentLevel = LEVEL_COUNTY;
        } else {
            queryFromCSV(selectedCity.getCityCode(), "county");
        }
    }
    /**
     * 根据传入的代号和类型从服务器上查询省市县数据。
     */
    private void queryFromServer(final String code, final String type) {
        String address;
        if (!TextUtils.isEmpty(code)) {
            address = "http://www.weather.com.cn/data/list3/city" + code + ".xml";
        } else {
            address = "http://www.weather.com.cn/data/list3/city.xml";
        }
        showProgressDialog();
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                boolean result = false;
                if ("province".equals(type)) {
                    result = Utility.handleProvincesResponse(jccWeatherDB,
                            response);
                } else if ("city".equals(type)) {
                    result = Utility.handleCitiesResponse(jccWeatherDB,
                            response, selectedProvince.getId());
                } else if ("county".equals(type)) {
                    result = Utility.handleCountiesResponse(jccWeatherDB,
                            response, selectedCity.getId());
                }
                if (result) {
// 通过runOnUiThread()方法回到主线程处理逻辑
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)) {
                                queryProvinces();
                            } else if ("city".equals(type)) {
                                queryCities();
                            } else if ("county".equals(type)) {
                                queryCounties();
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
// 通过runOnUiThread()方法回到主线程处理逻辑
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(ChooseAreaActivity.this,
                                "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    /**
     * //JUN: 从assets/.csv文件中读取
     */
    private void queryFromCSV(final String code, final String type){
        Log.d(TAG, "query " + type + " from CSV with code " + code);

        String data = "";
        showProgressDialog();
        try {
            boolean result = false;
            if ("province".equals(type)) {
                data = Utility.loadProvincesFromAsset(this, "citylist.csv");
                result = Utility.handleProvincesResponse(jccWeatherDB, data);
            } else if ("city".equals(type)) {
                data = Utility.loadCitiesFromAsset(this, "citylist.csv", selectedProvince.getProvinceCode());
                result = Utility.handleCitiesResponse(jccWeatherDB, data, selectedProvince.getId());
            } else if ("county".equals(type)) {
                data = Utility.loadCountiesFromAsset(this, "citylist.csv", selectedCity.getCityCode());
                result = Utility.handleCountiesResponse(jccWeatherDB, data, selectedCity.getId());
            }

//            String[] ttt = data.split(",");
//            dataList.clear();
//            for (String c : ttt) {
//                dataList.add(c);
//            }
//            adapter.notifyDataSetChanged();

            closeProgressDialog();
            if (result) {
                if ("province".equals(type)) {
                    queryProvinces();
                } else if ("city".equals(type)) {
                    queryCities();
                } else if ("county".equals(type)) {
                    queryCounties();
                }
            }
        }catch (Exception e) {
            closeProgressDialog();
            Toast.makeText(ChooseAreaActivity.this,
                    "加载失败" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * 显示进度对话框
     */
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }
    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }
    /**
     * 捕获Back按键，根据当前的级别来判断，此时应该返回市列表、省列表、还是直接退出。
     */
    @Override
    public void onBackPressed() {
        if (currentLevel == LEVEL_COUNTY) {
            queryCities();
        } else if (currentLevel == LEVEL_CITY) {
            queryProvinces();
        } else {
            finish();
        }
    }

    private void queryWeatherFromServer(final String cityCode){
        String httpUrl = "https://api.heweather.com/x3/weather?cityid=" + cityCode + "&key=a8a9f3f25a01453d8908e0b755570e9b";
        HttpUtil.sendHttpRequest(httpUrl, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                try {
                    JSONObject theRes = new JSONObject(response);
                    JSONObject nowObj = theRes.getJSONArray("HeWeather data service 3.0").getJSONObject(0).getJSONObject("now");
                    JSONObject conObj = nowObj.getJSONObject("cond");

                    Message msg = new Message();
                    msg.what = SHOW_TEMP;
                    msg.obj = conObj.getString("txt") + " : " + nowObj.getInt("tmp");
                    handler.sendMessage(msg);
                }catch (Exception e){
                    Log.d(TAG, e.getMessage());
                }
            }

            @Override
            public void onError(Exception e) {
                final String errMsg = e.getMessage();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ChooseAreaActivity.this,
                                "失败:" + errMsg, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
