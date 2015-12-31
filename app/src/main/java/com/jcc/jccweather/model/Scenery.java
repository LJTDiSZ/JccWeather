package com.jcc.jccweather.model;

/**
 * Created by juyuan on 12/31/2015.
 */
public class Scenery {
    private int id;
    private String sceneryName;
    private String sceneryCode;
    private int cityId;
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getSceneryName() {
        return sceneryName;
    }
    public void setSceneryName(String sceneryName) {
        this.sceneryName = sceneryName;
    }
    public String getSceneryCode() {
        return sceneryCode;
    }
    public void setSceneryCode(String sceneryCode) {
        this.sceneryCode = sceneryCode;
    }
    public int getCityId() {
        return cityId;
    }
    public void setCityId(int cityId) {
        this.cityId = cityId;
    }
}
