package com.example.weather;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.example.weather.api.BaiduMapApiService;
import com.example.weather.api.RetrofitClient;
import com.example.weather.databinding.ActivityMapPickBinding;
import com.example.weather.location.LocationHelper;
import com.example.weather.model.BaiduReverseGeocodeResponse;
import com.example.weather.model.City;
import com.example.weather.service.WeatherRepository;
import com.example.weather.utils.Constants;
import com.example.weather.utils.PermissionUtils;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapPickActivity extends AppCompatActivity {

    private static final String TAG = "MapPickActivity";
    private static final LatLng DEFAULT_CENTER = new LatLng(35.8617, 104.1954);
    private static final float DEFAULT_ZOOM = 5.0f;
    private static final float CITY_ZOOM = 12.8f;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ActivityMapPickBinding binding;
    private WeatherRepository weatherRepository;
    private BaiduMapApiService baiduMapApiService;
    private LocationHelper locationHelper;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private MapView mapView;
    private BaiduMap baiduMap;
    private City pickedCity;
    private boolean finishingSelection;
    private int resolveRequestToken;

    private final Runnable resolveCenterRunnable = () -> {
        if (baiduMap == null) return;
        MapStatus mapStatus = baiduMap.getMapStatus();
        if (mapStatus != null && mapStatus.target != null) {
            resolveMapCenter(mapStatus.target);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMapPickBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        weatherRepository = new WeatherRepository(this);
        baiduMapApiService = RetrofitClient.getBaiduMapApiService();
        locationHelper = new LocationHelper(this);

        registerLaunchers();
        setupToolbar();
        setupActions();

        Throwable mapInitError = WeatherApplication.getMapInitError();
        if (mapInitError != null) {
            showMapInitFailure(mapInitError);
            return;
        }

        try {
            setupMap();
        } catch (Throwable throwable) {
            showMapInitFailure(throwable);
        }
    }

    private void registerLaunchers() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean granted = Boolean.TRUE.equals(result.get(android.Manifest.permission.ACCESS_FINE_LOCATION))
                            || Boolean.TRUE.equals(result.get(android.Manifest.permission.ACCESS_COARSE_LOCATION));
                    if (granted) {
                        moveToCurrentLocation();
                    } else {
                        Toast.makeText(this, R.string.map_pick_permission_denied, Toast.LENGTH_SHORT).show();
                        moveMapTo(DEFAULT_CENTER, DEFAULT_ZOOM);
                    }
                });
    }

    private void setupToolbar() {
        binding.topAppBar.setNavigationOnClickListener(v -> finish());
    }

    private void setupActions() {
        binding.recenterButton.setOnClickListener(v -> requestCurrentLocationOrFallback());
        binding.addAndViewButton.setOnClickListener(v -> confirmSelection());
    }

    /**
     * 初始化地图控件及核心配置
     * 作用：把地图展示到界面上，并监听地图的滑动与状态变化，实现拖动地图选址功能
     */
    private void setupMap() {
        binding.mapContainer.removeAllViews();
        mapView = new MapView(this);
        mapView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        binding.mapContainer.addView(mapView);

        mapView.showZoomControls(false);
        mapView.showScaleControl(false);

        baiduMap = mapView.getMap();
        baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        baiduMap.setMyLocationEnabled(true);
        baiduMap.setOnMapLoadedCallback(() -> {
            binding.mapLoading.setVisibility(View.GONE);
            requestCurrentLocationOrFallback();
        });
        baiduMap.setOnMapStatusChangeListener(new BaiduMap.OnMapStatusChangeListener() {
            @Override
            public void onMapStatusChangeStart(MapStatus status) {
                liftCenterPin(true);
            }

            @Override
            public void onMapStatusChangeStart(MapStatus status, int reason) {
                liftCenterPin(true);
            }

            @Override
            public void onMapStatusChange(MapStatus status) {
            }

            @Override
            public void onMapStatusChangeFinish(MapStatus status) {
                liftCenterPin(false);
                if (status == null || status.target == null) {
                    return;
                }
                scheduleCenterResolve();
            }
        });
        moveMapTo(DEFAULT_CENTER, DEFAULT_ZOOM);
    }

    private void requestCurrentLocationOrFallback() {
        if (!PermissionUtils.hasLocationPermission(this)) {
            permissionLauncher.launch(PermissionUtils.getLocationPermissions());
            return;
        }
        moveToCurrentLocation();
    }

    private void moveToCurrentLocation() {
        showResolvingState(getString(R.string.map_pick_resolving_city), getString(R.string.map_pick_no_coord));
        locationHelper.requestSingleLocation(new LocationHelper.LocationCallback() {
            @Override
            public void onLocationReceived(double latitude, double longitude) {
                LatLng current = new LatLng(latitude, longitude);
                updateMyLocation(current);
                moveMapTo(current, CITY_ZOOM);
            }

            @Override
            public void onLocationFailed(String message) {
                Toast.makeText(MapPickActivity.this, R.string.map_pick_using_default, Toast.LENGTH_SHORT).show();
                moveMapTo(DEFAULT_CENTER, DEFAULT_ZOOM);
            }
        });
    }

    private void updateMyLocation(LatLng latLng) {
        if (baiduMap == null || latLng == null) {
            return;
        }
        MyLocationData locationData = new MyLocationData.Builder()
                .latitude(latLng.latitude)
                .longitude(latLng.longitude)
                .accuracy(25f)
                .build();
        baiduMap.setMyLocationData(locationData);
    }

    private void moveMapTo(LatLng target, float zoom) {
        if (baiduMap == null || target == null) {
            return;
        }
        MapStatusUpdate update = MapStatusUpdateFactory.newLatLngZoom(target, zoom);
        baiduMap.animateMapStatus(update);
    }

    private void scheduleCenterResolve() {
        mainHandler.removeCallbacks(resolveCenterRunnable);
        mainHandler.postDelayed(resolveCenterRunnable, 280L);
    }

    /**
     * 根据当前地图中心点，逆向解析地理位置信息
     * 作用：当用户滑动完地图后，用坐标去请求“这是哪个城市、哪条街道”
     */
    private void resolveMapCenter(LatLng target) {
        if (target == null) {
            return;
        }
        int requestToken = ++resolveRequestToken;
        showResolvingState(getString(R.string.map_pick_resolving_city), formatCoords(target.latitude, target.longitude));

        weatherRepository.lookupCityByCoordinates(target.latitude, target.longitude,
                new WeatherRepository.CityLookupCallback() {
                    @Override
                    public void onSuccess(City city) {
                        runOnUiThread(() -> {
                            if (requestToken != resolveRequestToken) {
                                return;
                            }
                            City resolvedCity = city == null ? new City() : city;
                            resolvedCity.setLat(target.latitude);
                            resolvedCity.setLon(target.longitude);
                            resolvedCity.setAddTime(System.currentTimeMillis());
                            if (isBlank(resolvedCity.getId())) {
                                resolvedCity.setId(buildFallbackCityId(target));
                            }
                            if (isBlank(resolvedCity.getName())) {
                                resolvedCity.setName(getString(R.string.map_pick_location_fallback));
                            }
                            pickedCity = resolvedCity;
                            binding.mapLoading.setVisibility(View.GONE);
                            binding.selectedCityText.setText(resolvedCity.getName());
                            binding.selectedCoordText.setText(R.string.map_pick_address_pending);
                            binding.addAndViewButton.setEnabled(true);
                            binding.addAndViewButton.setText(R.string.map_pick_confirm);
                            requestReverseAddress(target, requestToken);
                        });
                    }

                    @Override
                    public void onFailure(String message) {
                        if (TextUtils.isEmpty(BuildConfig.BAIDU_MAP_AK)) {
                            runOnUiThread(() -> {
                                if (requestToken != resolveRequestToken) {
                                    return;
                                }
                                pickedCity = buildFallbackCity(target);
                                renderFallbackSelection(target);
                            });
                            return;
                        }
                        requestReverseAddress(target, requestToken);
                    }
                });
    }

    private void requestReverseAddress(LatLng target, int requestToken) {
        if (TextUtils.isEmpty(BuildConfig.BAIDU_MAP_AK)) {
            if (requestToken == resolveRequestToken) {
                binding.selectedCoordText.setText(formatCoords(target.latitude, target.longitude));
                if (pickedCity == null) {
                    pickedCity = buildFallbackCity(target);
                    renderFallbackSelection(target);
                }
            }
            return;
        }

        baiduMapApiService.reverseGeocode(
                        formatReverseGeocodeLocation(target),
                        "bd09ll",
                        "json",
                        0,
                        BuildConfig.BAIDU_MAP_AK
                )
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(Call<BaiduReverseGeocodeResponse> call,
                                           Response<BaiduReverseGeocodeResponse> response) {
                        runOnUiThread(() -> {
                            if (requestToken != resolveRequestToken) {
                                return;
                            }
                            if (!response.isSuccessful() || response.body() == null
                                    || response.body().getStatus() != 0
                                    || response.body().getResult() == null) {
                                handleReverseAddressFailure(target);
                                return;
                            }
                            renderPickedAddress(target, response.body().getResult());
                        });
                    }

                    @Override
                    public void onFailure(Call<BaiduReverseGeocodeResponse> call, Throwable throwable) {
                        runOnUiThread(() -> {
                            if (requestToken != resolveRequestToken) {
                                return;
                            }
                            Log.e(TAG, "Reverse geocode failed", throwable);
                            handleReverseAddressFailure(target);
                        });
                    }
                });
    }

    private void renderPickedAddress(LatLng target, BaiduReverseGeocodeResponse.Result result) {
        if (pickedCity == null) {
            pickedCity = buildPickedCity(target, result);
        } else if (isBlank(pickedCity.getName())) {
            pickedCity.setName(extractCityName(result));
        }
        binding.mapLoading.setVisibility(View.GONE);
        binding.selectedCityText.setText(pickedCity.getName());
        binding.selectedCoordText.setText(buildAddressLine(result, target));
        binding.addAndViewButton.setEnabled(true);
        binding.addAndViewButton.setText(R.string.map_pick_confirm);
    }

    private void handleReverseAddressFailure(LatLng target) {
        binding.mapLoading.setVisibility(View.GONE);
        if (pickedCity != null) {
            binding.selectedCityText.setText(pickedCity.getName());
            binding.selectedCoordText.setText(formatCoords(target.latitude, target.longitude));
            binding.addAndViewButton.setEnabled(true);
            binding.addAndViewButton.setText(R.string.map_pick_confirm);
            return;
        }
        pickedCity = buildFallbackCity(target);
        renderFallbackSelection(target);
    }

    private City buildPickedCity(LatLng target, BaiduReverseGeocodeResponse.Result result) {
        City city = new City();
        String cityName = extractCityName(result);
        city.setId(buildFallbackCityId(target));
        city.setName(isBlank(cityName) ? getString(R.string.map_pick_location_fallback) : cityName);
        city.setLat(target.latitude);
        city.setLon(target.longitude);
        city.setAddTime(System.currentTimeMillis());
        return city;
    }

    private String extractCityName(BaiduReverseGeocodeResponse.Result result) {
        if (result != null && result.getAddressComponent() != null) {
            BaiduReverseGeocodeResponse.AddressComponent component = result.getAddressComponent();
            if (!isBlank(component.getCity())) return trimAdministrativeSuffix(component.getCity());
            if (!isBlank(component.getDistrict())) return trimAdministrativeSuffix(component.getDistrict());
            if (!isBlank(component.getProvince())) return trimAdministrativeSuffix(component.getProvince());
        }
        if (result != null && !isBlank(result.getFormattedAddress())) return result.getFormattedAddress();
        return getString(R.string.map_pick_location_fallback);
    }

    private String buildAddressLine(BaiduReverseGeocodeResponse.Result result, LatLng target) {
        if (result == null) {
            return formatCoords(target.latitude, target.longitude);
        }
        if (result.getAddressComponent() != null) {
            String province = trimAdministrativeSuffix(result.getAddressComponent().getProvince());
            String city = trimAdministrativeSuffix(result.getAddressComponent().getCity());
            if (!isBlank(province) && !isBlank(city)) {
                return province + " · " + city;
            }
            if (!isBlank(city)) {
                return city;
            }
            if (!isBlank(province)) {
                return province;
            }
        }
        if (!isBlank(result.getFormattedAddress())) {
            return result.getFormattedAddress();
        }
        return formatCoords(target.latitude, target.longitude);
    }

    private void renderFallbackSelection(LatLng target) {
        binding.mapLoading.setVisibility(View.GONE);
        binding.selectedCityText.setText(pickedCity == null
                ? getString(R.string.map_pick_location_fallback)
                : pickedCity.getName());
        binding.selectedCoordText.setText(formatCoords(target.latitude, target.longitude));
        binding.addAndViewButton.setEnabled(true);
        binding.addAndViewButton.setText(R.string.map_pick_confirm);
    }

    private City buildFallbackCity(LatLng target) {
        City city = new City();
        city.setId(buildFallbackCityId(target));
        city.setName(getString(R.string.map_pick_location_fallback));
        city.setLat(target.latitude);
        city.setLon(target.longitude);
        city.setAddTime(System.currentTimeMillis());
        return city;
    }

    private void confirmSelection() {
        if (pickedCity == null || finishingSelection) {
            Toast.makeText(this, R.string.map_pick_need_select, Toast.LENGTH_SHORT).show();
            return;
        }

        finishingSelection = true;
        binding.addAndViewButton.setEnabled(false);
        binding.addAndViewButton.setText(R.string.map_pick_confirming);

        weatherRepository.lookupCityByCoordinates(pickedCity.getLat(), pickedCity.getLon(),
                new WeatherRepository.CityLookupCallback() {
                    @Override
                    public void onSuccess(City city) {
                        runOnUiThread(() -> finishWithSelectedCity(buildFinalCity(city)));
                    }

                    @Override
                    public void onFailure(String message) {
                        runOnUiThread(() -> finishWithSelectedCity(buildFinalCity(null)));
                    }
                });
    }

    private City buildFinalCity(City repoCity) {
        City finalCity = repoCity == null ? new City() : repoCity;
        finalCity.setLat(pickedCity.getLat());
        finalCity.setLon(pickedCity.getLon());
        finalCity.setAddTime(System.currentTimeMillis());
        if (isBlank(finalCity.getId())) finalCity.setId(pickedCity.getId());
        if (isBlank(finalCity.getName())) finalCity.setName(pickedCity.getName());
        return finalCity;
    }

    private void finishWithSelectedCity(City city) {
        if (city == null || isBlank(city.getName())) {
            finishingSelection = false;
            binding.addAndViewButton.setEnabled(true);
            binding.addAndViewButton.setText(R.string.map_pick_confirm);
            Toast.makeText(this, R.string.map_pick_add_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent();
        intent.putExtra(Constants.EXTRA_SELECTED_CITY_ID, city.getId());
        intent.putExtra(Constants.EXTRA_SELECTED_CITY_NAME, city.getName());
        intent.putExtra(Constants.EXTRA_SELECTED_CITY_LAT, city.getLat());
        intent.putExtra(Constants.EXTRA_SELECTED_CITY_LON, city.getLon());
        intent.putExtra(Constants.EXTRA_SELECTED_CITY_IS_LOCATION, false);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void showResolvingState(String title, String subtitle) {
        binding.mapLoading.setVisibility(View.VISIBLE);
        binding.selectedCityText.setText(title);
        binding.selectedCoordText.setText(subtitle);
        binding.addAndViewButton.setEnabled(false);
    }

    private void showMapInitFailure(Throwable throwable) {
        Log.e(TAG, "Failed to initialize Baidu map", throwable);
        binding.mapLoading.setVisibility(View.GONE);
        binding.selectedCityText.setText(R.string.map_pick_init_failed);
        binding.selectedCoordText.setText(throwable != null && throwable.getMessage() != null
                ? throwable.getMessage()
                : getString(R.string.map_pick_no_coord));
        binding.addAndViewButton.setEnabled(false);
        binding.recenterButton.setEnabled(false);
        Toast.makeText(this, R.string.map_pick_init_failed, Toast.LENGTH_LONG).show();
    }

    private void liftCenterPin(boolean lifted) {
        binding.centerPinView.animate()
                .translationY(lifted ? -14f : 0f)
                .setDuration(lifted ? 110L : 170L)
                .start();
    }

    private String buildFallbackCityId(LatLng target) {
        long lat = Math.round(target.latitude * 10000d);
        long lon = Math.round(target.longitude * 10000d);
        return "bd_" + lat + "_" + lon;
    }

    private String formatReverseGeocodeLocation(LatLng target) {
        return String.format(Locale.US, "%.6f,%.6f", target.latitude, target.longitude);
    }

    private String formatCoords(double latitude, double longitude) {
        return String.format(Locale.US, "%.4f, %.4f", latitude, longitude);
    }

    private String trimAdministrativeSuffix(String value) {
        if (isBlank(value)) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.endsWith("市") || trimmed.endsWith("区") || trimmed.endsWith("县") || trimmed.endsWith("省")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        mainHandler.removeCallbacks(resolveCenterRunnable);
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacks(resolveCenterRunnable);
        if (baiduMap != null) {
            baiduMap.setOnMapLoadedCallback(null);
            baiduMap.setOnMapStatusChangeListener(null);
            baiduMap.setMyLocationEnabled(false);
        }
        if (mapView != null) {
            mapView.onDestroy();
            mapView = null;
        }
        super.onDestroy();
    }
}
