package com.example.weather.service;

import android.content.Context;
import android.text.TextUtils;

import com.example.weather.BuildConfig;
import com.example.weather.R;
import com.example.weather.api.RetrofitClient;
import com.example.weather.api.WeatherApiService;
import com.example.weather.model.AirQualityResponse;
import com.example.weather.model.City;
import com.example.weather.model.CityResponse;
import com.example.weather.model.ForecastResponse;
import com.example.weather.model.WeatherData;
import com.example.weather.model.WeatherResponse;
import com.example.weather.utils.NetworkUtils;
import com.example.weather.utils.PreferencesManager;

import com.google.gson.Gson;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherRepository {

    public interface WeatherCallback {
        void onSuccess(WeatherData weatherData, boolean fromCache);

        void onFailure(String message, WeatherData cachedData);
    }

    public interface CitySearchCallback {
        void onSuccess(List<CityResponse.Location> results);

        void onFailure(String message);
    }

    public interface CityLookupCallback {
        void onSuccess(City city);

        void onFailure(String message);
    }

    public interface CitySummaryCallback {
        void onSuccess(WeatherResponse.Now now, boolean fromCache);

        void onFailure(String message);
    }

    private final Context context;
    private final WeatherApiService apiService;
    private final PreferencesManager preferencesManager;
    private final Gson gson;

    public WeatherRepository(Context context) {
        this.context = context.getApplicationContext();
        apiService = RetrofitClient.getWeatherApiService();
        preferencesManager = new PreferencesManager(context);
        gson = new Gson();
    }

    public void fetchWeather(City city, WeatherCallback callback) {
        WeatherData cachedData = loadCachedWeather(city);
        if (TextUtils.isEmpty(BuildConfig.QWEATHER_API_KEY)) {
            if (cachedData != null) {
                callback.onSuccess(cachedData, true);
            } else {
                callback.onFailure("请在 local.properties 中配置 QWEATHER_API_KEY", null);
            }
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(context)) {
            if (cachedData != null) {
                callback.onSuccess(cachedData, true);
            } else {
                callback.onFailure("网络异常，暂无可用缓存", null);
            }
            return;
        }

        String locationQuery = buildLocationQuery(city);
        apiService.getCurrentWeather(locationQuery, "zh", BuildConfig.QWEATHER_API_KEY)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onFailure(buildHttpErrorMessage(response.code()), cachedData);
                            return;
                        }
                        WeatherResponse weatherResponse = response.body();
                        if (!"200".equals(weatherResponse.getCode())) {
                            callback.onFailure(buildBusinessErrorMessage(weatherResponse.getCode()), cachedData);
                            return;
                        }
        fetchForecast(city, weatherResponse, callback, cachedData);
                    }

                    @Override
                    public void onFailure(Call<WeatherResponse> call, Throwable throwable) {
                        callback.onFailure("天气请求失败：" + throwable.getMessage(), cachedData);
                    }
                });
    }

    public void fetchCitySummary(City city, CitySummaryCallback callback) {
        WeatherData cachedData = loadCachedWeather(city);
        if (cachedData != null && cachedData.getWeatherResponse() != null && cachedData.getWeatherResponse().getNow() != null) {
            callback.onSuccess(cachedData.getWeatherResponse().getNow(), true);
        }
        if (TextUtils.isEmpty(BuildConfig.QWEATHER_API_KEY) || !NetworkUtils.isNetworkAvailable(context)) {
            if (cachedData == null || cachedData.getWeatherResponse() == null || cachedData.getWeatherResponse().getNow() == null) {
                callback.onFailure("暂无城市天气摘要");
            }
            return;
        }
        apiService.getCurrentWeather(buildLocationQuery(city), "zh", BuildConfig.QWEATHER_API_KEY)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                        if (!response.isSuccessful() || response.body() == null || !"200".equals(response.body().getCode())) {
                            callback.onFailure(response.isSuccessful() && response.body() != null
                                    ? buildBusinessErrorMessage(response.body().getCode())
                                    : buildHttpErrorMessage(response.code()));
                            return;
                        }
                        WeatherResponse weatherResponse = response.body();
                        WeatherData dataToCache = cachedData != null ? cachedData : new WeatherData();
                        dataToCache.setCity(city);
                            dataToCache.setWeatherResponse(weatherResponse);
                            if (cachedData != null) {
                                dataToCache.setForecastResponse(cachedData.getForecastResponse());
                                dataToCache.setAirQualityResponse(cachedData.getAirQualityResponse());
                            }
                            saveCache(dataToCache);
                            callback.onSuccess(weatherResponse.getNow(), false);
                    }

                    @Override
                    public void onFailure(Call<WeatherResponse> call, Throwable throwable) {
                        callback.onFailure("城市天气获取失败");
                    }
                });
    }

    public void searchCity(String query, CitySearchCallback callback) {
        if (TextUtils.isEmpty(BuildConfig.QWEATHER_API_KEY)) {
            callback.onFailure("请先配置 QWEATHER_API_KEY");
            return;
        }
        apiService.searchCity(query, "zh", 10, BuildConfig.QWEATHER_API_KEY)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(Call<CityResponse> call, Response<CityResponse> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onFailure(buildHttpErrorMessage(response.code()));
                            return;
                        }
                        CityResponse cityResponse = response.body();
                        if (!"200".equals(cityResponse.getCode())) {
                            callback.onFailure(buildBusinessErrorMessage(cityResponse.getCode()));
                            return;
                        }
                        callback.onSuccess(cityResponse.getLocation() == null
                                ? Collections.emptyList()
                                : cityResponse.getLocation());
                    }

                    @Override
                    public void onFailure(Call<CityResponse> call, Throwable throwable) {
                        callback.onFailure("城市搜索失败：" + throwable.getMessage());
                    }
                });
    }

    public void lookupCityByCoordinates(double latitude, double longitude, CityLookupCallback callback) {
        if (TextUtils.isEmpty(BuildConfig.QWEATHER_API_KEY)) {
            callback.onFailure("请先配置 QWEATHER_API_KEY");
            return;
        }
        String location = longitude + "," + latitude;
        apiService.searchCity(location, "zh", 1, BuildConfig.QWEATHER_API_KEY)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(Call<CityResponse> call, Response<CityResponse> response) {
                        if (!response.isSuccessful() || response.body() == null || response.body().getLocation() == null
                                || response.body().getLocation().isEmpty()) {
                            callback.onFailure(buildLookupFailureMessage(response.code()));
                            return;
                        }
                        City city = response.body().getLocation().get(0).toCity();
                        city.setLat(latitude);
                        city.setLon(longitude);
                        callback.onSuccess(city);
                    }

                    @Override
                    public void onFailure(Call<CityResponse> call, Throwable throwable) {
                        callback.onFailure("定位城市解析失败：" + throwable.getMessage());
                    }
                });
    }

    public WeatherData loadCachedWeather(City city) {
        if (city == null || TextUtils.isEmpty(city.getId())) {
            return null;
        }
        String cacheJson = preferencesManager.getWeatherCache(city.getId());
        if (TextUtils.isEmpty(cacheJson)) {
            return null;
        }
        try {
            return gson.fromJson(cacheJson, WeatherData.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void fetchForecast(City city, WeatherResponse weatherResponse, WeatherCallback callback, WeatherData cachedData) {
        apiService.getForecast(buildLocationQuery(city), "zh", BuildConfig.QWEATHER_API_KEY)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(Call<ForecastResponse> call, Response<ForecastResponse> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onFailure(buildHttpErrorMessage(response.code()), cachedData);
                            return;
                        }
                        ForecastResponse forecastResponse = response.body();
                        if (!"200".equals(forecastResponse.getCode())) {
                            callback.onFailure(buildBusinessErrorMessage(forecastResponse.getCode()), cachedData);
                            return;
                        }
                        WeatherData weatherData = new WeatherData();
                        weatherData.setCity(city);
                        weatherData.setWeatherResponse(weatherResponse);
                        weatherData.setForecastResponse(forecastResponse);
                        if (cachedData != null) {
                            weatherData.setAirQualityResponse(cachedData.getAirQualityResponse());
                        }
                        fetchAirQuality(city, weatherData, callback);
                    }

                    @Override
                    public void onFailure(Call<ForecastResponse> call, Throwable throwable) {
                        callback.onFailure("天气预报请求失败：" + throwable.getMessage(), cachedData);
                    }
                });
    }

    private void saveCache(WeatherData weatherData) {
        if (weatherData == null || weatherData.getCity() == null || TextUtils.isEmpty(weatherData.getCity().getId())) {
            return;
        }
        preferencesManager.saveWeatherCache(weatherData.getCity().getId(), gson.toJson(weatherData));
    }

    private void fetchAirQuality(City city, WeatherData weatherData, WeatherCallback callback) {
        if (city == null || (city.getLat() == 0d && city.getLon() == 0d)) {
            saveCache(weatherData);
            callback.onSuccess(weatherData, false);
            return;
        }
        String latitude = formatCoordinate(city.getLat());
        String longitude = formatCoordinate(city.getLon());
        apiService.getAirQuality(
                        latitude,
                        longitude,
                        "zh",
                        buildAuthorizationHeader(),
                        BuildConfig.QWEATHER_API_KEY
                )
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(Call<AirQualityResponse> call, Response<AirQualityResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            weatherData.setAirQualityResponse(response.body());
                        }
                        saveCache(weatherData);
                        callback.onSuccess(weatherData, false);
                    }

                    @Override
                    public void onFailure(Call<AirQualityResponse> call, Throwable throwable) {
                        saveCache(weatherData);
                        callback.onSuccess(weatherData, false);
                    }
                });
    }

    private String buildLocationQuery(City city) {
        if (city.getLat() != 0d && city.getLon() != 0d) {
            return city.getLon() + "," + city.getLat();
        }
        return city.getId();
    }

    private String buildAuthorizationHeader() {
        if (TextUtils.isEmpty(BuildConfig.QWEATHER_API_KEY)) {
            return "";
        }
        return "Bearer " + BuildConfig.QWEATHER_API_KEY;
    }

    private String formatCoordinate(double value) {
        return String.format(Locale.US, "%.4f", value);
    }

    private String buildLookupFailureMessage(int httpCode) {
        if (httpCode == 403) {
            return "定位城市解析失败：请配置和风天气专属 API Host";
        }
        if (httpCode > 0) {
            return "定位城市解析失败：HTTP " + httpCode;
        }
        return "定位城市解析失败";
    }

    private String buildHttpErrorMessage(int httpCode) {
        if (httpCode == 403) {
            return "和风天气鉴权失败：请在 local.properties 中配置 QWEATHER_API_HOST";
        }
        if (httpCode == 404) {
            return "天气接口地址不可用，请检查 QWEATHER_API_HOST 配置";
        }
        if (httpCode > 0) {
            return "天气请求失败：HTTP " + httpCode;
        }
        return "天气请求失败";
    }

    private String buildBusinessErrorMessage(String code) {
        if ("401".equals(code)) {
            return "天气接口鉴权失败，请检查 API Key";
        }
        if ("403".equals(code)) {
            return "天气接口无权限访问，请检查凭据权限";
        }
        return "天气服务返回异常：" + code;
    }
}
