package com.example.weather.api;

import com.example.weather.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 统一的网络请求客户端工具类 (基于 Retrofit)
 * 作用：负责创建和管理所有的网络请求服务，并确保全局只有一个实例（单例模式）
 */
public final class RetrofitClient {

    // 缓存“和风天气API”服务实例
    private static volatile WeatherApiService weatherApiService;
    // 缓存“百度地图API”服务实例
    private static volatile BaiduMapApiService baiduMapApiService;
    // 全局共享的 HTTP 客户端实例（提供超时、日志等统一配置）
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
        loggingInterceptor.setLevel(com.example.weather.BuildConfig.DEBUG ? 
                HttpLoggingInterceptor.Level.BASIC : 
                HttpLoggingInterceptor.Level.NONE);
                    sharedOkHttpClient = new OkHttpClient.Builder()
                            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                            .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                            .writeTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
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
