package com.example.weather.service;

import android.text.TextUtils;

import com.example.weather.R;
import com.example.weather.model.ClothingSuggestion;
import com.example.weather.model.ForecastResponse;
import com.example.weather.model.WeatherResponse;

public class ClothingSuggestionService {

    public ClothingSuggestion buildSuggestion(ForecastResponse.Daily today, WeatherResponse.Now now) {
        int referenceTemp = parseTemp(today != null ? today.getTempMax() : null);
        if (referenceTemp == Integer.MIN_VALUE && now != null) {
            referenceTemp = parseTemp(now.getTemp());
        }

        String weatherText = today != null ? today.getTextDay() : (now != null ? now.getText() : "");
        String windScale = now != null ? now.getWindScale() : "";

        String suggestion;
        String level;
        int iconResId;

        if (referenceTemp >= 30) {
            suggestion = "酷热，建议穿短袖短裤，注意防晒";
            level = "hot";
            iconResId = R.drawable.ic_clothing_hot;
        } else if (referenceTemp >= 25) {
            suggestion = "温暖，短袖搭配薄裤会比较舒服";
            level = "warm";
            iconResId = R.drawable.ic_clothing_warm;
        } else if (referenceTemp >= 15) {
            suggestion = "体感舒适，长袖或薄外套都合适";
            level = "cool";
            iconResId = R.drawable.ic_clothing_cool;
        } else if (referenceTemp >= 5) {
            suggestion = "稍凉，建议加一件毛衣或夹克";
            level = "cold";
            iconResId = R.drawable.ic_clothing_cold;
        } else {
            suggestion = "天气偏冷，羽绒服和保暖配件更稳妥";
            level = "freezing";
            iconResId = R.drawable.ic_clothing_freezing;
        }

        StringBuilder accessoryTip = new StringBuilder();
        if (!TextUtils.isEmpty(weatherText) && weatherText.contains("雨")) {
            accessoryTip.append("建议携带雨伞");
        }
        if (!TextUtils.isEmpty(windScale) && parseTemp(windScale) >= 5) {
            if (accessoryTip.length() > 0) {
                accessoryTip.append(" · ");
            }
            accessoryTip.append("注意防风");
        }
        if (!TextUtils.isEmpty(weatherText) && weatherText.contains("雪")) {
            if (accessoryTip.length() > 0) {
                accessoryTip.append(" · ");
            }
            accessoryTip.append("注意保暖防滑");
        }

        return new ClothingSuggestion(suggestion, iconResId, level, accessoryTip.toString());
    }

    private int parseTemp(String tempValue) {
        if (TextUtils.isEmpty(tempValue)) {
            return Integer.MIN_VALUE;
        }
        try {
            String normalized = tempValue.trim();
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("-?\\d+").matcher(normalized);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group());
            }
            return Integer.MIN_VALUE;
        } catch (NumberFormatException ignored) {
            return Integer.MIN_VALUE;
        }
    }
}
