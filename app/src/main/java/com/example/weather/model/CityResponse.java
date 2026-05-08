package com.example.weather.model;

import java.util.List;

public class CityResponse {

    private String code;
    private List<Location> location;

    public String getCode() {
        return code;
    }

    public List<Location> getLocation() {
        return location;
    }

    public static class Location {
        private String id;
        private String name;
        private String lat;
        private String lon;
        private String adm1;
        private String adm2;
        private String country;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getLat() {
            return lat;
        }

        public String getLon() {
            return lon;
        }

        public String getAdm1() {
            return adm1;
        }

        public String getAdm2() {
            return adm2;
        }

        public String getCountry() {
            return country;
        }

        public City toCity() {
            City city = new City();
            city.setId(id);
            city.setName(name);
            city.setLat(parseDouble(lat));
            city.setLon(parseDouble(lon));
            city.setAddTime(System.currentTimeMillis());
            return city;
        }

        public String getDisplayName() {
            if (adm2 == null || adm2.isEmpty() || adm2.equals(name)) {
                return name + " · " + adm1;
            }
            return name + " · " + adm2 + ", " + adm1;
        }

        private double parseDouble(String value) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException ignored) {
                return 0d;
            }
        }
    }
}
