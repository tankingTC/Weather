package com.example.weather;

import android.app.Application;
import android.util.Log;

import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;

public class WeatherApplication extends Application {

    private static final String TAG = "WeatherApplication";
    private static volatile Throwable mapInitError;

    public static Throwable getMapInitError() {
        return mapInitError;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            SDKInitializer.setAgreePrivacy(this, true);
            SDKInitializer.initialize(this);
            SDKInitializer.setCoordType(CoordType.BD09LL);
            mapInitError = null;
        } catch (Throwable throwable) {
            mapInitError = throwable;
            Log.e(TAG, "Failed to initialize Baidu SDK", throwable);
        }
    }
}
