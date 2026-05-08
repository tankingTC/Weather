package com.example.weather.model;

import java.util.List;

public class AirQualityResponse {

    private List<Index> indexes;

    public List<Index> getIndexes() {
        return indexes;
    }

    public static class Index {
        private String code;
        private String name;
        private String aqiDisplay;
        private String category;
        private PrimaryPollutant primaryPollutant;
        private Health health;

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getAqiDisplay() {
            return aqiDisplay;
        }

        public String getCategory() {
            return category;
        }

        public PrimaryPollutant getPrimaryPollutant() {
            return primaryPollutant;
        }

        public Health getHealth() {
            return health;
        }
    }

    public static class PrimaryPollutant {
        private String code;
        private String name;
        private String fullName;

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getFullName() {
            return fullName;
        }
    }

    public static class Health {
        private String effect;
        private Advice advice;

        public String getEffect() {
            return effect;
        }

        public Advice getAdvice() {
            return advice;
        }
    }

    public static class Advice {
        private String generalPopulation;
        private String sensitivePopulation;

        public String getGeneralPopulation() {
            return generalPopulation;
        }

        public String getSensitivePopulation() {
            return sensitivePopulation;
        }
    }
}
