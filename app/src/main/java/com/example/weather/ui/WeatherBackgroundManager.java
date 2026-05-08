package com.example.weather.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.weather.R;

public class WeatherBackgroundManager {

    public void applyWeatherTheme(Context context, View container, WeatherEffectOverlayView overlayView,
                                  String iconCode, String weatherText) {
        ThemeConfig config = resolveTheme(iconCode, weatherText);
        GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        ContextCompat.getColor(context, config.topColorRes),
                        ContextCompat.getColor(context, config.bottomColorRes)
                }
        );
        container.setBackground(gradientDrawable);
        overlayView.setMode(config.mode);
    }

    private ThemeConfig resolveTheme(String iconCode, String weatherText) {
        String normalized = weatherText == null ? "" : weatherText.toLowerCase();
        int code = parseCode(iconCode);
        WeatherIconManager.WeatherKind kind = WeatherIconManager.resolveKind(iconCode);

        if (code >= 150 && code < 200) {
            return new ThemeConfig(R.color.bg_night_top, R.color.bg_night_bottom, WeatherEffectOverlayView.Mode.NIGHT);
        }
        if (kind == WeatherIconManager.WeatherKind.RAIN
                && (normalized.contains("雷") || normalized.contains("暴")
                || normalized.contains("storm") || normalized.contains("thunder"))) {
            return new ThemeConfig(R.color.bg_storm_top, R.color.bg_storm_bottom, WeatherEffectOverlayView.Mode.STORM);
        }
        if (kind == WeatherIconManager.WeatherKind.SNOW
                || normalized.contains("snow")
                || normalized.contains("sleet")) {
            return new ThemeConfig(R.color.bg_snow_top, R.color.bg_snow_bottom, WeatherEffectOverlayView.Mode.SNOW);
        }
        if (kind == WeatherIconManager.WeatherKind.RAIN
                || normalized.contains("rain")
                || normalized.contains("drizzle")) {
            return new ThemeConfig(R.color.bg_rain_top, R.color.bg_rain_bottom, WeatherEffectOverlayView.Mode.RAIN);
        }
        if (kind == WeatherIconManager.WeatherKind.OVERCAST
                || normalized.contains("overcast")) {
            return new ThemeConfig(R.color.bg_overcast_top, R.color.bg_overcast_bottom,
                    WeatherEffectOverlayView.Mode.OVERCAST);
        }
        if (kind == WeatherIconManager.WeatherKind.CLOUDY
                || normalized.contains("cloud")) {
            return new ThemeConfig(R.color.bg_cloudy_top, R.color.bg_cloudy_bottom, WeatherEffectOverlayView.Mode.CLOUDY);
        }
        if (kind == WeatherIconManager.WeatherKind.WIND || kind == WeatherIconManager.WeatherKind.DEFAULT) {
            return new ThemeConfig(R.color.bg_night_top, R.color.bg_night_bottom, WeatherEffectOverlayView.Mode.NIGHT);
        }
        return new ThemeConfig(R.color.bg_clear_top, R.color.bg_clear_bottom, WeatherEffectOverlayView.Mode.CLEAR);
    }

    private int parseCode(String iconCode) {
        try {
            return Integer.parseInt(iconCode);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static class ThemeConfig {
        final int topColorRes;
        final int bottomColorRes;
        final WeatherEffectOverlayView.Mode mode;

        ThemeConfig(int topColorRes, int bottomColorRes, WeatherEffectOverlayView.Mode mode) {
            this.topColorRes = topColorRes;
            this.bottomColorRes = bottomColorRes;
            this.mode = mode;
        }
    }
}
