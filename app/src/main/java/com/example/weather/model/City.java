package com.example.weather.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "cities")
public class City {

    @PrimaryKey
    @NonNull
    private String id = "";
    private String name;
    private double lat;
    private double lon;
    private long addTime;

    @Ignore
    private String currentTemp;
    @Ignore
    private String weatherText;

    public City() {
    }

    @Ignore
    public City(@NonNull String id, String name, double lat, double lon, long addTime) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.addTime = addTime;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public long getAddTime() {
        return addTime;
    }

    public void setAddTime(long addTime) {
        this.addTime = addTime;
    }

    public String getCurrentTemp() {
        return currentTemp;
    }

    public void setCurrentTemp(String currentTemp) {
        this.currentTemp = currentTemp;
    }

    public String getWeatherText() {
        return weatherText;
    }

    public void setWeatherText(String weatherText) {
        this.weatherText = weatherText;
    }
}
