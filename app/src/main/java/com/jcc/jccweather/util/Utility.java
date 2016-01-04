package com.jcc.jccweather.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.jcc.jccweather.db.JccWeatherDB;
import com.jcc.jccweather.model.City;
import com.jcc.jccweather.model.County;
import com.jcc.jccweather.model.Province;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by juyuan on 12/31/2015.
 */
public class Utility {
    /**
     * 解析和处理服务器返回的省级数据
     */
    public synchronized static boolean handleProvincesResponse(JccWeatherDB coolWeatherDB, String response) {
        if (!TextUtils.isEmpty(response)) {
            String[] allProvinces = response.split(",");
            if (allProvinces != null && allProvinces.length > 0) {
                for (String p : allProvinces) {
                    String[] array = p.split("\\|");
                    Province province = new Province();
                    province.setProvinceCode(array[0]);
                    province.setProvinceName(array[1]);
// 将解析出来的数据存储到Province表
                    coolWeatherDB.saveProvince(province);
                }
                return true;
            }
        }
        return false;
    }
    /**
     * 解析和处理服务器返回的市级数据
     */
    public static boolean handleCitiesResponse(JccWeatherDB coolWeatherDB,
                                               String response, int provinceId) {
        if (!TextUtils.isEmpty(response)) {
            String[] allCities = response.split(",");
            if (allCities != null && allCities.length > 0) {
                for (String c : allCities) {
                    String[] array = c.split("\\|");
                    City city = new City();
                    city.setCityCode(array[0]);
                    city.setCityName(array[1]);
                    city.setProvinceId(provinceId);
// 将解析出来的数据存储到City表
                    coolWeatherDB.saveCity(city);
                }
                return true;
            }
        }
        return false;
    }
    /**
     * 解析和处理服务器返回的县级数据
     */
    public static boolean handleCountiesResponse(JccWeatherDB coolWeatherDB,
                                                 String response, int cityId) {
        if (!TextUtils.isEmpty(response)) {
            String[] allCounties = response.split(",");
            if (allCounties != null && allCounties.length > 0) {
                for (String c : allCounties) {
                    String[] array = c.split("\\|");
                    County county = new County();
                    county.setCountyCode(array[0]);
                    county.setCountyName(array[1]);
                    county.setCityId(cityId);
// 将解析出来的数据存储到County表
                    coolWeatherDB.saveCounty(county);
                }
                return true;
            }
        }
        return false;
    }


    public static String loadProvincesFromAsset(Context context, final String assetFile){
        try {
            InputStreamReader is = new InputStreamReader(context.getAssets().open(assetFile));

            BufferedReader reader = new BufferedReader(is);
            String line;
            line = reader.readLine();//Skip the first header line
            StringBuilder sb = new StringBuilder();
            List<String> pros = new ArrayList<String>();
            while ((line = reader.readLine()) != null) {
                String[] elements = line.split(",");
                if (elements != null && elements.length > 0 && elements[0].trim().length() > 0){
                    String one = elements[0].substring(0, 7) + "|" + elements[4];
                    if (!pros.contains(one)){
                        pros.add(one);
                    }
                }
            }
            for(String c : pros){
                sb.append(c).append(",");
            }
            return sb.toString();
        }catch (Exception e){
            Toast.makeText(context, "加载错误：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    public static String loadCitiesFromAsset(Context context, final String assetFile, final String provinceCode){
        try {
            InputStreamReader is = new InputStreamReader(context.getAssets().open(assetFile));

            BufferedReader reader = new BufferedReader(is);
            String line;
            line = reader.readLine();//Skip the first header line
            StringBuilder sb = new StringBuilder();
            List<String> pros = new ArrayList<String>();
            while ((line = reader.readLine()) != null) {
                String[] elements = line.split(",");
                if (elements != null && elements.length > 0 && elements[0].startsWith(provinceCode)){
                    String one = elements[0].substring(0, 9) + "|" + elements[3];
                    if (elements[0].endsWith("00")){
                        one = elements[0].substring(0, 7) + "|" + elements[3];
                    }
                    if (!pros.contains(one)){
                        pros.add(one);
                    }
                }
            }
            for(String c : pros){
                sb.append(c).append(",");
            }
            return sb.toString();
        }catch (Exception e){
            Toast.makeText(context, "加载错误：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    public static String loadCountiesFromAsset(Context context, final String assetFile, final String cityCode){
        try {
            InputStreamReader is = new InputStreamReader(context.getAssets().open(assetFile));

            BufferedReader reader = new BufferedReader(is);
            String line;
            line = reader.readLine();//Skip the first header line
            StringBuilder sb = new StringBuilder();
            List<String> pros = new ArrayList<String>();
            while ((line = reader.readLine()) != null) {
                String[] elements = line.split(",");
                if (elements != null && elements.length > 0 && elements[0].startsWith(cityCode)){
                    String one = elements[0] + "|" + elements[2];
                    if (!pros.contains(one)){
                        pros.add(one);
                    }
                }
            }
            for(String c : pros){
                sb.append(c).append(",");
            }
            return sb.toString();
        }catch (Exception e){
            Toast.makeText(context, "加载错误：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    public static void handleWeatherResponse(Context context, String response){
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONObject firstObj = jsonObject.getJSONArray("HeWeather data service 3.0").getJSONObject(0);
            JSONObject nowObj = firstObj.getJSONObject("now");
            JSONObject basicObj = firstObj.getJSONObject("basic");

            String cityName = basicObj.getString("city");
            String weatherCode = basicObj.getString("id");
            String temp1 = nowObj.getString("tmp");
            String temp2 = nowObj.getString("fl");
            String publishTime = basicObj.getJSONObject("update").getString("loc");
            String weatherDesp = nowObj.getJSONObject("cond").getString("txt") + ", "
                    + nowObj.getJSONObject("wind").getString("dir");
            saveWeatherInfo(context, cityName, weatherCode, temp1, temp2,
                    weatherDesp, publishTime);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void saveWeatherInfo(Context context, String cityName, String weatherCode,
                                       String temp1, String temp2, String weatherDesp, String publishTime){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日", Locale.CHINA);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean("city_selected", true);
        editor.putString("city_name", cityName);
        editor.putString("weather_code", weatherCode);
        editor.putString("temp1", temp1);
        editor.putString("temp2", temp2);
        editor.putString("weather_desp", weatherDesp);
        editor.putString("publish_time", publishTime);
        editor.putString("current_date", sdf.format(new Date()));
        editor.commit();
    }
}
