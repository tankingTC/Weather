package com.example.weather.api;

import com.example.weather.model.CityResponse;
import com.example.weather.model.ForecastResponse;
import com.example.weather.model.AirQualityResponse;
import com.example.weather.model.WeatherResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface WeatherApiService {

    @GET("v7/weather/now")
    Call<WeatherResponse> getCurrentWeather(
            @Query("location") String location,
            @Query("lang") String language,
            @Query("key") String apiKey
    );

    @GET("v7/weather/7d")
    Call<ForecastResponse> getForecast(
            @Query("location") String location,
            @Query("lang") String language,
            @Query("key") String apiKey
    );

    @GET("airquality/v1/current/{latitude}/{longitude}")
    Call<AirQualityResponse> getAirQuality(
            @Path("latitude") String latitude,
            @Path("longitude") String longitude,
            @Query("lang") String language,
            @Header("Authorization") String authorization,
            @Query("key") String apiKey
    );

    @GET("geo/v2/city/lookup")
    Call<CityResponse> searchCity(
            @Query("location") String query,
            @Query("lang") String language,
            @Query("number") int number,
            @Query("key") String apiKey
    );
}
