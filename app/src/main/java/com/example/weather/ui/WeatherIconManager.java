package com.example.weather.ui;

import android.content.Context;
import android.graphics.Color;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;

import com.example.weather.R;
public final class WeatherIconManager {

    public enum WeatherKind {
        SUNNY,
        CLOUDY,
        OVERCAST,
        RAIN,
        SNOW,
        WIND,
        DEFAULT
    }

    private WeatherIconManager() {
    }

    public static void loadWeatherIcon(Context context, ImageView imageView, String iconCode) {
        int fallbackResId = getFallbackIcon(iconCode);
        if (imageView instanceof HolographicImageView) {
            int[] palette = resolvePalette(iconCode);
            ((HolographicImageView) imageView).setGlowPalette(palette[0], palette[1]);
        }
        imageView.setImageResource(fallbackResId);
    }

    @DrawableRes
    public static int getFallbackIcon(String iconCode) {
        WeatherKind kind = resolveKind(iconCode);
        if (kind == WeatherKind.SUNNY) {
            return R.drawable.ic_weather_fallback_clear;
        }
        if (kind == WeatherKind.CLOUDY) {
            return R.drawable.ic_weather_fallback_cloudy;
        }
        if (kind == WeatherKind.OVERCAST) {
            return R.drawable.ic_weather_fallback_overcast;
        }
        if (kind == WeatherKind.RAIN) {
            return R.drawable.ic_weather_fallback_rain;
        }
        if (kind == WeatherKind.SNOW) {
            return R.drawable.ic_weather_fallback_snow;
        }
        if (kind == WeatherKind.WIND) {
            return R.drawable.ic_weather_fallback_wind;
        }
        return R.drawable.ic_weather_fallback_default;
    }

    public static WeatherKind resolveKind(String iconCode) {
        int code = parseCode(iconCode);
        if (code == 100 || code == 150) {
            return WeatherKind.SUNNY;
        }
        if ((code >= 101 && code <= 103) || (code >= 151 && code <= 153)) {
            return WeatherKind.CLOUDY;
        }
        if (code == 104 || code == 154 || (code >= 200 && code < 300)) {
            return WeatherKind.OVERCAST;
        }
        if (code >= 300 && code < 400) {
            return WeatherKind.RAIN;
        }
        if (code >= 400 && code < 500) {
            return WeatherKind.SNOW;
        }
        if (code >= 500 && code < 600) {
            return WeatherKind.WIND;
        }
        return WeatherKind.DEFAULT;
    }

    private static int parseCode(String iconCode) {
        try {
            return Integer.parseInt(iconCode);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static int[] resolvePalette(String iconCode) {
        WeatherKind kind = resolveKind(iconCode);
        if (kind == WeatherKind.SUNNY) {
            return new int[]{
                    Color.argb(192, 255, 205, 108),
                    Color.argb(166, 255, 122, 180)
            };
        }
        if (kind == WeatherKind.CLOUDY) {
            return new int[]{
                    Color.argb(172, 180, 236, 255),
                    Color.argb(156, 129, 187, 255)
            };
        }
        if (kind == WeatherKind.OVERCAST) {
            return new int[]{
                    Color.argb(172, 210, 224, 242),
                    Color.argb(148, 118, 146, 196)
            };
        }
        if (kind == WeatherKind.RAIN) {
            return new int[]{
                    Color.argb(184, 102, 206, 255),
                    Color.argb(164, 64, 138, 232)
            };
        }
        if (kind == WeatherKind.SNOW) {
            return new int[]{
                    Color.argb(176, 220, 248, 255),
                    Color.argb(148, 116, 207, 255)
            };
        }
        return new int[]{
                Color.argb(176, 142, 231, 255),
                Color.argb(156, 90, 195, 255)
        };
    }
}
