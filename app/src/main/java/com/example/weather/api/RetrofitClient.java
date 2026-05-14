package com.example.weather.api;

import com.example.weather.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitClient {

    private static volatile WeatherApiService weatherApiService;
    private static volatile BaiduMapApiService baiduMapApiService;
    private static volatile OkHttpClient sharedOkHttpClient;

    private RetrofitClient() {
    }

    public static WeatherApiService getWeatherApiService() {
        if (weatherApiService == null) {
            synchronized (RetrofitClient.class) {
                if (weatherApiService == null) {
                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(normalizeBaseUrl(BuildConfig.QWEATHER_API_HOST))
                            .client(getSharedOkHttpClient())
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();

                    weatherApiService = retrofit.create(WeatherApiService.class);
                }
            }
        }
        return weatherApiService;
    }

    public static BaiduMapApiService getBaiduMapApiService() {
        if (baiduMapApiService == null) {
            synchronized (RetrofitClient.class) {
                if (baiduMapApiService == null) {
                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl("https://api.map.baidu.com/")
                            .client(getSharedOkHttpClient())
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                    baiduMapApiService = retrofit.create(BaiduMapApiService.class);
                }
            }
        }
        return baiduMapApiService;
    }

    private static OkHttpClient getSharedOkHttpClient() {
        if (sharedOkHttpClient == null) {
            synchronized (RetrofitClient.class) {
                if (sharedOkHttpClient == null) {
                    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
                    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

                    sharedOkHttpClient = new OkHttpClient.Builder()
                            .addInterceptor(loggingInterceptor)
                            .build();
                }
            }
        }
        return sharedOkHttpClient;
    }

    private static String normalizeBaseUrl(String rawHost) {
        if (rawHost == null || rawHost.trim().isEmpty()) {
            return "https://devapi.qweather.com/";
        }

        String normalized = rawHost.trim();
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://" + normalized;
        }
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        return normalized;
    }
}
