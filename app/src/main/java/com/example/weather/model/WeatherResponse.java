package com.example.weather.model;

public class WeatherResponse {

    private String code;
    private String updateTime;
    private Now now;

    public String getCode() {
        return code;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public Now getNow() {
        return now;
    }

    public static class Now {
        private String temp;
        private String feelsLike;
        private String icon;
        private String text;
        private String windDir;
        private String windScale;
        private String windSpeed;
        private String humidity;
        private String precip;
        private String pressure;
        private String vis;
        private String cloud;
        private String dew;

        public String getTemp() {
            return temp;
        }

        public String getFeelsLike() {
            return feelsLike;
        }

        public String getIcon() {
            return icon;
        }

        public String getText() {
            return text;
        }

        public String getWindDir() {
            return windDir;
        }

        public String getWindScale() {
            return windScale;
        }

        public String getWindSpeed() {
            return windSpeed;
        }

        public String getHumidity() {
            return humidity;
        }

        public String getPrecip() {
            return precip;
        }

        public String getPressure() {
            return pressure;
        }

        public String getVis() {
            return vis;
        }

        public String getCloud() {
            return cloud;
        }

        public String getDew() {
            return dew;
        }
    }
}
