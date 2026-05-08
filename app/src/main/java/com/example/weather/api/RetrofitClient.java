package com.example.weather.api;

import com.example.weather.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitClient {

    private static volatile WeatherApiService weatherApiService;

    private RetrofitClient() {
    }

    public static WeatherApiService getWeatherApiService() {
        if (weatherApiService == null) {
            synchronized (RetrofitClient.class) {
                if (weatherApiService == null) {
                    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
                    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

                    OkHttpClient okHttpClient = new OkHttpClient.Builder()
                            .addInterceptor(loggingInterceptor)
                            .build();

                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(normalizeBaseUrl(BuildConfig.QWEATHER_API_HOST))
                            .client(okHttpClient)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();

                    weatherApiService = retrofit.create(WeatherApiService.class);
                }
            }
        }
        return weatherApiService;
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
