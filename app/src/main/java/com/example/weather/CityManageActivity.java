package com.example.weather;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weather.adapter.CityManageAdapter;
import com.example.weather.adapter.CitySearchAdapter;
import com.example.weather.databinding.ActivityCityManageBinding;
import com.example.weather.db.AppDatabase;
import com.example.weather.model.City;
import com.example.weather.model.CityResponse;
import com.example.weather.service.WeatherRepository;
import com.example.weather.utils.Constants;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CityManageActivity extends AppCompatActivity {

    private ActivityCityManageBinding binding;
    private CityManageAdapter cityManageAdapter;
    private CitySearchAdapter citySearchAdapter;
    private ExecutorService executorService;
    private AppDatabase appDatabase;
    private WeatherRepository weatherRepository;
    private String selectedCityId;
    private boolean cityListChanged;
    private ActivityResultLauncher<Intent> mapPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityCityManageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        selectedCityId = getIntent().getStringExtra(Constants.EXTRA_SELECTED_CITY_ID);
        executorService = Executors.newSingleThreadExecutor();
        appDatabase = AppDatabase.getInstance(this);
        weatherRepository = new WeatherRepository(this);

        registerLaunchers();
        setupToolbar();
        setupRecyclerViews();
        setupSearchAction();
        loadSavedCities();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    private void registerLaunchers() {
        mapPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                        return;
                    }
                    Intent data = result.getData();
                    City city = new City();
                    city.setId(data.getStringExtra(Constants.EXTRA_SELECTED_CITY_ID));
                    city.setName(data.getStringExtra(Constants.EXTRA_SELECTED_CITY_NAME));
                    city.setLat(data.getDoubleExtra(Constants.EXTRA_SELECTED_CITY_LAT, 0d));
                    city.setLon(data.getDoubleExtra(Constants.EXTRA_SELECTED_CITY_LON, 0d));
                    addCityAndOpen(city);
                });
    }

    private void setupToolbar() {
        binding.topAppBar.setNavigationOnClickListener(v -> finishWithResult(null));
        binding.topAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_pick_on_map) {
                openMapPicker();
                return true;
            }
            return false;
        });
    }

    private void setupRecyclerViews() {
        cityManageAdapter = new CityManageAdapter(new CityManageAdapter.CityActionListener() {
            @Override
            public void onCitySelected(City city) {
                finishWithResult(city);
            }

            @Override
            public void onCityDelete(City city) {
                deleteCity(city);
            }
        });
        citySearchAdapter = new CitySearchAdapter(this::addCity);

        binding.savedCitiesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.savedCitiesRecyclerView.setAdapter(cityManageAdapter);

        binding.searchResultRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.searchResultRecyclerView.setAdapter(citySearchAdapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                City city = cityManageAdapter.getItem(position);
                deleteCity(city);
            }
        });
        itemTouchHelper.attachToRecyclerView(binding.savedCitiesRecyclerView);
    }

    private void setupSearchAction() {
        binding.searchButton.setOnClickListener(v -> searchCity());
        binding.searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchCity();
                return true;
            }
            return false;
        });
    }

    private void loadSavedCities() {
        executorService.execute(() -> {
            List<City> cities = appDatabase.cityDao().getAllCities();
            runOnUiThread(() -> {
                cityManageAdapter.submitList(cities, selectedCityId);
                binding.savedEmptyText.setVisibility(cities.isEmpty() ? View.VISIBLE : View.GONE);
                refreshCitySummaries(cities);
            });
        });
    }

    private void refreshCitySummaries(List<City> cities) {
        for (City city : cities) {
            weatherRepository.fetchCitySummary(city, new WeatherRepository.CitySummaryCallback() {
                @Override
                public void onSuccess(com.example.weather.model.WeatherResponse.Now now, boolean fromCache) {
                    city.setCurrentTemp(now.getTemp());
                    city.setWeatherText(now.getText());
                    cityManageAdapter.notifyDataSetChanged();
                }

                @Override
                public void onFailure(String message) {
                }
            });
        }
    }

    private void searchCity() {
        String query = binding.searchEditText.getText() == null
                ? ""
                : binding.searchEditText.getText().toString().trim();
        if (query.isEmpty()) {
            binding.searchEditText.setError("请输入要搜索的城市");
            return;
        }
        binding.searchProgress.setVisibility(View.VISIBLE);
        weatherRepository.searchCity(query, new WeatherRepository.CitySearchCallback() {
            @Override
            public void onSuccess(List<CityResponse.Location> results) {
                binding.searchProgress.setVisibility(View.GONE);
                citySearchAdapter.submitList(results);
                binding.searchResultTitle.setVisibility(View.VISIBLE);
                binding.searchResultRecyclerView.setVisibility(View.VISIBLE);
                binding.searchEmptyText.setVisibility(results.isEmpty() ? View.VISIBLE : View.GONE);
                hideKeyboard();
                scrollToSearchResults();
            }

            @Override
            public void onFailure(String message) {
                binding.searchProgress.setVisibility(View.GONE);
                binding.searchResultTitle.setVisibility(View.VISIBLE);
                binding.searchResultRecyclerView.setVisibility(View.GONE);
                binding.searchEmptyText.setVisibility(View.VISIBLE);
                binding.searchEmptyText.setText(message);
                hideKeyboard();
                scrollToSearchResults();
            }
        });
    }

    private void scrollToSearchResults() {
        binding.cityManageScrollView.post(() ->
                binding.cityManageScrollView.smoothScrollTo(0, binding.searchResultTitle.getTop()));
    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager = getSystemService(InputMethodManager.class);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(binding.searchEditText.getWindowToken(), 0);
        }
        binding.searchEditText.clearFocus();
    }

    private void addCity(CityResponse.Location location) {
        City city = location.toCity();
        executorService.execute(() -> {
            appDatabase.cityDao().insertCity(city);
            cityListChanged = true;
            runOnUiThread(() -> {
                Toast.makeText(this, "已添加 " + city.getName(), Toast.LENGTH_SHORT).show();
                loadSavedCities();
            });
        });
    }

    private void addCityAndOpen(City city) {
        executorService.execute(() -> {
            city.setAddTime(System.currentTimeMillis());
            appDatabase.cityDao().insertCity(city);
            cityListChanged = true;
            runOnUiThread(() -> {
                Toast.makeText(this, "已添加 " + city.getName(), Toast.LENGTH_SHORT).show();
                finishWithResult(city);
            });
        });
    }

    private void deleteCity(City city) {
        executorService.execute(() -> {
            appDatabase.cityDao().deleteCity(city);
            cityListChanged = true;
            runOnUiThread(() -> {
                Toast.makeText(this, "已删除 " + city.getName(), Toast.LENGTH_SHORT).show();
                loadSavedCities();
            });
        });
    }

    private void finishWithResult(City city) {
        Intent intent = new Intent();
        intent.putExtra(Constants.EXTRA_CITY_LIST_CHANGED, cityListChanged);
        if (city != null) {
            intent.putExtra(Constants.EXTRA_SELECTED_CITY_ID, city.getId());
            intent.putExtra(Constants.EXTRA_SELECTED_CITY_NAME, city.getName());
            intent.putExtra(Constants.EXTRA_SELECTED_CITY_LAT, city.getLat());
            intent.putExtra(Constants.EXTRA_SELECTED_CITY_LON, city.getLon());
            intent.putExtra(Constants.EXTRA_SELECTED_CITY_IS_LOCATION, false);
            setResult(RESULT_OK, intent);
        } else {
            setResult(cityListChanged ? RESULT_OK : RESULT_CANCELED, intent);
        }
        finish();
    }

    private void openMapPicker() {
        mapPickerLauncher.launch(new Intent(this, MapPickActivity.class));
    }
}
