package com.example.weather.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.weather.model.City;

import java.util.List;

@Dao
public interface CityDao {

    @Query("SELECT * FROM cities ORDER BY addTime DESC")
    List<City> getAllCities();

    @Query("SELECT * FROM cities WHERE id = :cityId LIMIT 1")
    City getCityById(String cityId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCity(City city);

    @Delete
    void deleteCity(City city);
}
