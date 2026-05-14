package com.example.weather.model;

public class BaiduReverseGeocodeResponse {

    private int status;
    private Result result;

    public int getStatus() {
        return status;
    }

    public Result getResult() {
        return result;
    }

    public static class Result {
        private String formatted_address;
        private String sematic_description;
        private AddressComponent addressComponent;

        public String getFormattedAddress() {
            return formatted_address;
        }

        public String getSematicDescription() {
            return sematic_description;
        }

        public AddressComponent getAddressComponent() {
            return addressComponent;
        }
    }

    public static class AddressComponent {
        private String country;
        private String province;
        private String city;
        private String district;
        private String town;
        private String street;

        public String getCountry() {
            return country;
        }

        public String getProvince() {
            return province;
        }

        public String getCity() {
            return city;
        }

        public String getDistrict() {
            return district;
        }

        public String getTown() {
            return town;
        }

        public String getStreet() {
            return street;
        }
    }
}
