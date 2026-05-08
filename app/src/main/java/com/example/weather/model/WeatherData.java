package com.example.weather.model;

public class WeatherData {

    private City city;
    private WeatherResponse weatherResponse;
    private ForecastResponse forecastResponse;
    private AirQualityResponse airQualityResponse;
    private transient boolean fromCache;

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }

    public WeatherResponse getWeatherResponse() {
        return weatherResponse;
    }

    public void setWeatherResponse(WeatherResponse weatherResponse) {
        this.weatherResponse = weatherResponse;
    }

    public ForecastResponse getForecastResponse() {
        return forecastResponse;
    }

    public void setForecastResponse(ForecastResponse forecastResponse) {
        this.forecastResponse = forecastResponse;
    }

    public AirQualityResponse getAirQualityResponse() {
        return airQualityResponse;
    }

    public void setAirQualityResponse(AirQualityResponse airQualityResponse) {
        this.airQualityResponse = airQualityResponse;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    public void setFromCache(boolean fromCache) {
        this.fromCache = fromCache;
    }
}
