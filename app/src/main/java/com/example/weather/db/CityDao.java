package com.example.weather.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.weather.model.City;

import java.util.List;

@Dao
public interface CityDao {

    // 获取所有已收藏的城市（通过 LiveData 包装，只要数据库改变，UI会自动刷新列表）
    @Query("SELECT * FROM cities ORDER BY addTime DESC")
    LiveData<List<City>> getAllCities();

    // 同步获取所有城市（给后台任务一次性读取使用的普通方法）
    @Query("SELECT * FROM cities ORDER BY addTime DESC")
    List<City> getAllCitiesSync();

    // 根据城市ID查询特定的城市信息
    @Query("SELECT * FROM cities WHERE id = :cityId LIMIT 1")
    City getCityById(String cityId);

    // 插入一个新城市（如果城市已存在，就替换更新它，防止重复）
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCity(City city);

    // 从数据库中删除一个指定的城市
    @Delete
    void deleteCity(City city);
}
