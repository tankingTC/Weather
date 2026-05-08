package com.example.weather.model;

public class ClothingSuggestion {

    private String suggestion;
    private int iconResId;
    private String level;
    private String accessoryTip;

    public ClothingSuggestion(String suggestion, int iconResId, String level, String accessoryTip) {
        this.suggestion = suggestion;
        this.iconResId = iconResId;
        this.level = level;
        this.accessoryTip = accessoryTip;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getLevel() {
        return level;
    }

    public String getAccessoryTip() {
        return accessoryTip;
    }
}
