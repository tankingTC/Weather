package com.example.weather.api;

import com.example.weather.model.BaiduReverseGeocodeResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface BaiduMapApiService {

    @GET("reverse_geocoding/v3/")
    Call<BaiduReverseGeocodeResponse> reverseGeocode(
            @Query("location") String location,
            @Query("coordtype") String coordType,
            @Query("output") String output,
            @Query("extensions_poi") int extensionsPoi,
            @Query("ak") String apiKey
    );
}
