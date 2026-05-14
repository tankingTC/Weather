package com.example.weather.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baidu.mapapi.model.LatLng;
import com.example.weather.api.BaiduMapApiService;
import com.example.weather.api.RetrofitClient;
import com.example.weather.model.BaiduReverseGeocodeResponse;
import com.example.weather.model.City;
import com.example.weather.service.WeatherRepository;

public class MapPickViewModel extends ViewModel {
    
    private final MutableLiveData<City> pickedCityLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>();
    private final MutableLiveData<Throwable> errorLiveData = new MutableLiveData<>();

    // Keep it simple for now, can be expanded.
}