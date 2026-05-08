package com.example.weather.model;

import java.util.List;

public class ForecastResponse {

    private String code;
    private String updateTime;
    private List<Daily> daily;

    public String getCode() {
        return code;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public List<Daily> getDaily() {
        return daily;
    }

    public static class Daily {
        private String fxDate;
        private String tempMax;
        private String tempMin;
        private String iconDay;
        private String textDay;

        public String getFxDate() {
            return fxDate;
        }

        public String getTempMax() {
            return tempMax;
        }

        public String getTempMin() {
            return tempMin;
        }

        public String getIconDay() {
            return iconDay;
        }

        public String getTextDay() {
            return textDay;
        }
    }
}
