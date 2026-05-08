package com.example.weather.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.example.weather.utils.PermissionUtils;

public class LocationHelper {

    public interface LocationCallback {
        void onLocationReceived(double latitude, double longitude);

        void onLocationFailed(String message);
    }

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public LocationHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    @SuppressLint("MissingPermission")
    public void requestSingleLocation(LocationCallback callback) {
        if (!PermissionUtils.hasLocationPermission(context)) {
            callback.onLocationFailed("缺少定位权限");
            return;
        }

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            callback.onLocationFailed("定位服务不可用");
            return;
        }

        String provider = null;
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            provider = LocationManager.NETWORK_PROVIDER;
        } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
        }

        if (provider == null) {
            callback.onLocationFailed("请开启定位服务");
            return;
        }

        Location lastKnownLocation = locationManager.getLastKnownLocation(provider);
        if (lastKnownLocation != null) {
            callback.onLocationReceived(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            return;
        }

        String finalProvider = provider;
        final boolean[] completed = {false};
        final LocationListener[] listenerHolder = new LocationListener[1];
        Runnable timeoutRunnable = () -> {
            if (completed[0]) {
                return;
            }
            completed[0] = true;
            locationManager.removeUpdates(listenerHolder[0]);
            callback.onLocationFailed("定位超时，已切换默认城市");
        };

        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (completed[0]) {
                    return;
                }
                completed[0] = true;
                handler.removeCallbacks(timeoutRunnable);
                locationManager.removeUpdates(this);
                callback.onLocationReceived(location.getLatitude(), location.getLongitude());
            }

            @Override
            public void onProviderDisabled(@NonNull String providerName) {
                if (completed[0]) {
                    return;
                }
                completed[0] = true;
                handler.removeCallbacks(timeoutRunnable);
                locationManager.removeUpdates(this);
                callback.onLocationFailed("定位提供者不可用");
            }
        };
        listenerHolder[0] = listener;

        locationManager.requestLocationUpdates(finalProvider, 0L, 0f, listener, Looper.getMainLooper());
        handler.postDelayed(timeoutRunnable, 8000L);
    }
}
