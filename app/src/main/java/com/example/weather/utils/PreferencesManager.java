package com.example.weather.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.example.weather.model.City;

public class PreferencesManager {

    private final SharedPreferences sharedPreferences;

    public PreferencesManager(Context context) {
        sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveSelectedCity(City city, boolean fromLocation) {
        sharedPreferences.edit()
                .putString(Constants.PREF_SELECTED_CITY_ID, city.getId())
                .putString(Constants.PREF_SELECTED_CITY_NAME, city.getName())
                .putString(Constants.PREF_SELECTED_CITY_LAT, String.valueOf(city.getLat()))
                .putString(Constants.PREF_SELECTED_CITY_LON, String.valueOf(city.getLon()))
                .putBoolean(Constants.PREF_SELECTED_CITY_IS_LOCATION, fromLocation)
                .apply();
    }

    public City getSelectedCity() {
        String id = sharedPreferences.getString(Constants.PREF_SELECTED_CITY_ID, "");
        String name = sharedPreferences.getString(Constants.PREF_SELECTED_CITY_NAME, "");
        if (TextUtils.isEmpty(id) || TextUtils.isEmpty(name)) {
            return null;
        }
        City city = new City();
        city.setId(id);
        city.setName(name);
        city.setLat(parseDouble(sharedPreferences.getString(Constants.PREF_SELECTED_CITY_LAT, "0")));
        city.setLon(parseDouble(sharedPreferences.getString(Constants.PREF_SELECTED_CITY_LON, "0")));
        return city;
    }

    public boolean isSelectedCityFromLocation() {
        return sharedPreferences.getBoolean(Constants.PREF_SELECTED_CITY_IS_LOCATION, false);
    }

    public void saveLocationCity(City city) {
        sharedPreferences.edit()
                .putString(Constants.PREF_LAST_LOCATION_ID, city.getId())
                .putString(Constants.PREF_LAST_LOCATION_NAME, city.getName())
                .putString(Constants.PREF_LAST_LOCATION_LAT, String.valueOf(city.getLat()))
                .putString(Constants.PREF_LAST_LOCATION_LON, String.valueOf(city.getLon()))
                .apply();
    }

    public City getLastLocationCity() {
        String id = sharedPreferences.getString(Constants.PREF_LAST_LOCATION_ID, "");
        String name = sharedPreferences.getString(Constants.PREF_LAST_LOCATION_NAME, "");
        if (TextUtils.isEmpty(id) || TextUtils.isEmpty(name)) {
            return null;
        }
        City city = new City();
        city.setId(id);
        city.setName(name);
        city.setLat(parseDouble(sharedPreferences.getString(Constants.PREF_LAST_LOCATION_LAT, "0")));
        city.setLon(parseDouble(sharedPreferences.getString(Constants.PREF_LAST_LOCATION_LON, "0")));
        return city;
    }

    public void saveWeatherCache(String cacheKey, String json) {
        sharedPreferences.edit().putString(Constants.PREF_CACHE_PREFIX + cacheKey, json).apply();
    }

    public String getWeatherCache(String cacheKey) {
        return sharedPreferences.getString(Constants.PREF_CACHE_PREFIX + cacheKey, "");
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return 0d;
        }
    }
}
