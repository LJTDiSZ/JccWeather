package com.jcc.jccweather.util;

/**
 * Created by juyuan on 12/30/2015.
 */
public interface HttpCallbackListener {
    void onFinish(String response);
    void onError(Exception e);
}
