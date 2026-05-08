package com.example.weather;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weather.adapter.AiChatAdapter;
import com.example.weather.adapter.WeatherForecastAdapter;
import com.example.weather.databinding.ActivityMainBinding;
import com.example.weather.databinding.LayoutAiAssistantBinding;
import com.example.weather.db.AppDatabase;
import com.example.weather.location.LocationHelper;
import com.example.weather.model.AiChatMessage;
import com.example.weather.model.City;
import com.example.weather.model.ClothingSuggestion;
import com.example.weather.model.ForecastResponse;
import com.example.weather.model.WeatherData;
import com.example.weather.model.WeatherResponse;
import com.example.weather.service.AiAssistantRepository;
import com.example.weather.service.ClothingSuggestionService;
import com.example.weather.service.WeatherRepository;
import com.example.weather.ui.WeatherBackgroundManager;
import com.example.weather.ui.HolographicImageView;
import com.example.weather.ui.WeatherIconManager;
import com.example.weather.utils.Constants;
import com.example.weather.utils.PermissionUtils;
import com.example.weather.utils.PreferencesManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private WeatherForecastAdapter forecastAdapter;
    private WeatherRepository weatherRepository;
    private ClothingSuggestionService clothingSuggestionService;
    private WeatherBackgroundManager backgroundManager;
    private PreferencesManager preferencesManager;
    private LocationHelper locationHelper;
    private AiAssistantRepository aiAssistantRepository;
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private ExecutorService executorService;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private ActivityResultLauncher<Intent> cityManageLauncher;
    private final List<AiChatMessage> assistantMessages = new ArrayList<>();
    private AiChatAdapter aiChatAdapter;
    private BottomSheetDialog aiAssistantDialog;
    private LayoutAiAssistantBinding aiAssistantBinding;
    private City selectedCity;
    private WeatherData currentWeatherData;
    private ClothingSuggestion currentClothingSuggestion;
    private boolean selectedCityFromLocation;
    private String assistantContextCityId = "";
    private boolean assistantRequestInFlight;
    private boolean weatherContentAnimated;
    private String lastRenderedTemp = "";
    private String lastAiSuggestionKey = "";
    private boolean aiSuggestionRequestInFlight;
    private ValueAnimator weatherIconAnimator;
    private float aiFabTouchDownRawX;
    private float aiFabTouchDownRawY;
    private float aiFabStartTranslationX;
    private float aiFabStartTranslationY;
    private boolean aiFabDragging;
    private final SensorEventListener parallaxSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float x = clamp(-event.values[0] * 2.8f, -18f, 18f);
            float y = clamp(event.values[1] * 2.1f, -12f, 12f);
            binding.weatherEffectView.setDepthOffset(x, y);
            binding.weatherContentLayout.setTranslationX(x * 0.22f);
            binding.weatherContentLayout.setTranslationY(y * 0.15f);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        configureImmersiveWindow();

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            binding.mainScrollView.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        weatherRepository = new WeatherRepository(this);
        clothingSuggestionService = new ClothingSuggestionService();
        backgroundManager = new WeatherBackgroundManager();
        preferencesManager = new PreferencesManager(this);
        locationHelper = new LocationHelper(this);
        aiAssistantRepository = new AiAssistantRepository();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        executorService = Executors.newSingleThreadExecutor();

        registerLaunchers();
        setupViews();
        restoreLastSelection();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (weatherIconAnimator != null) {
            weatherIconAnimator.cancel();
        }
        executorService.shutdown();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometerSensor != null) {
            sensorManager.registerListener(parallaxSensorListener, accelerometerSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(parallaxSensorListener);
        }
    }

    private void registerLaunchers() {
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            boolean granted = Boolean.TRUE.equals(result.get(android.Manifest.permission.ACCESS_FINE_LOCATION))
                    || Boolean.TRUE.equals(result.get(android.Manifest.permission.ACCESS_COARSE_LOCATION));
            if (granted) {
                requestLocationAndWeather();
            } else {
                showToast(getString(R.string.location_permission_denied));
                restoreSavedCityOrFallback();
            }
        });

        cityManageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                return;
            }
            Intent data = result.getData();
            String cityId = data.getStringExtra(Constants.EXTRA_SELECTED_CITY_ID);
            if (cityId != null) {
                City city = new City();
                city.setId(cityId);
                city.setName(data.getStringExtra(Constants.EXTRA_SELECTED_CITY_NAME));
                city.setLat(data.getDoubleExtra(Constants.EXTRA_SELECTED_CITY_LAT, 0d));
                city.setLon(data.getDoubleExtra(Constants.EXTRA_SELECTED_CITY_LON, 0d));
                applySelectedCity(city, data.getBooleanExtra(Constants.EXTRA_SELECTED_CITY_IS_LOCATION, false));
                fetchWeather(false);
                return;
            }

            boolean listChanged = data.getBooleanExtra(Constants.EXTRA_CITY_LIST_CHANGED, false);
            if (listChanged) {
                reconcileSelectionAfterListChange();
            }
        });
    }

    private void setupViews() {
        forecastAdapter = new WeatherForecastAdapter();
        binding.forecastRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        binding.forecastRecyclerView.setAdapter(forecastAdapter);
        binding.forecastRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                applyForecastDepthEffect();
            }
        });
        binding.forecastRecyclerView.post(this::applyForecastDepthEffect);
        binding.swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(this, R.color.text_neon_primary),
                ContextCompat.getColor(this, R.color.ai_fab_bg)
        );

        binding.manageCityButton.setOnClickListener(v -> openCityManager());
        binding.cityHeaderGroup.setOnClickListener(v -> openCityManager());
        binding.retryButton.setOnClickListener(v -> refreshContent());
        binding.swipeRefreshLayout.setOnRefreshListener(this::refreshContent);
        binding.aiAssistantFab.setOnClickListener(v -> openAiAssistantDialog(binding.aiAssistantFab));
        binding.aiAssistantFab.setOnTouchListener(this::handleAiFabDrag);
        binding.clothingContainer.setOnClickListener(v -> openAiAssistantDialog(binding.clothingContainer));
    }

    private void restoreLastSelection() {
        requestLocationOrPermission();
    }

    private void requestLocationOrPermission() {
        if (PermissionUtils.hasLocationPermission(this)) {
            requestLocationAndWeather();
        } else {
            permissionLauncher.launch(PermissionUtils.getLocationPermissions());
        }
    }

    private void refreshContent() {
        if (selectedCityFromLocation) {
            requestLocationOrPermission();
        } else if (selectedCity != null) {
            fetchWeather(true);
        } else {
            requestLocationOrPermission();
        }
    }

    private void requestLocationAndWeather() {
        showStateCard(true, getString(R.string.locating_message), false);
        setRefreshing(true);
        locationHelper.requestSingleLocation(new LocationHelper.LocationCallback() {
            @Override
            public void onLocationReceived(double latitude, double longitude) {
                weatherRepository.lookupCityByCoordinates(latitude, longitude, new WeatherRepository.CityLookupCallback() {
                    @Override
                    public void onSuccess(City city) {
                        city.setLat(latitude);
                        city.setLon(longitude);
                        preferencesManager.saveLocationCity(city);
                        ensureInitialLocationCitySaved(city);
                        applySelectedCity(city, true);
                        fetchWeather(false);
                    }

                    @Override
                    public void onFailure(String message) {
                        City lastLocationCity = preferencesManager.getLastLocationCity();
                        if (lastLocationCity != null) {
                            applySelectedCity(lastLocationCity, true);
                            fetchWeather(false);
                        } else {
                            showToast(message);
                            switchToFallbackCity();
                        }
                    }
                });
            }

            @Override
            public void onLocationFailed(String message) {
                showToast(message);
                switchToFallbackCity();
            }
        });
    }

    private void switchToFallbackCity() {
        City city = buildDefaultCity();
        applySelectedCity(city, false);
        fetchWeather(false);
    }

    private void restoreSavedCityOrFallback() {
        City savedCity = preferencesManager.getSelectedCity();
        if (savedCity != null) {
            applySelectedCity(savedCity, preferencesManager.isSelectedCityFromLocation());
            fetchWeather(false);
            return;
        }
        City lastLocationCity = preferencesManager.getLastLocationCity();
        if (lastLocationCity != null) {
            applySelectedCity(lastLocationCity, true);
            fetchWeather(false);
            return;
        }
        switchToFallbackCity();
    }

    private void ensureInitialLocationCitySaved(City city) {
        executorService.execute(() -> {
            AppDatabase database = AppDatabase.getInstance(this);
            List<City> savedCities = database.cityDao().getAllCities();
            if (!savedCities.isEmpty()) {
                return;
            }
            City cityToSave = new City();
            cityToSave.setId(city.getId());
            cityToSave.setName(city.getName());
            cityToSave.setLat(city.getLat());
            cityToSave.setLon(city.getLon());
            cityToSave.setAddTime(System.currentTimeMillis());
            database.cityDao().insertCity(cityToSave);
        });
    }

    private City buildDefaultCity() {
        City city = new City();
        city.setId(Constants.DEFAULT_CITY_ID);
        city.setName(Constants.DEFAULT_CITY_NAME);
        city.setLat(Constants.DEFAULT_CITY_LAT);
        city.setLon(Constants.DEFAULT_CITY_LON);
        city.setAddTime(System.currentTimeMillis());
        return city;
    }

    private void applySelectedCity(City city, boolean fromLocation) {
        if (selectedCity == null || !selectedCity.getId().equals(city.getId())) {
            clearAssistantConversation();
        }
        selectedCity = city;
        selectedCityFromLocation = fromLocation;
        preferencesManager.saveSelectedCity(city, fromLocation);
        binding.cityNameText.setText(city.getName());
        binding.cityMetaText.setText(fromLocation ? R.string.current_location_label : R.string.saved_city_label);
    }

    private void fetchWeather(boolean isManualRefresh) {
        if (selectedCity == null) {
            setRefreshing(false);
            return;
        }
        if (!isManualRefresh) {
            showStateCard(true, getString(R.string.loading_weather), false);
        }
        setRefreshing(true);
        weatherRepository.fetchWeather(selectedCity, new WeatherRepository.WeatherCallback() {
            @Override
            public void onSuccess(WeatherData weatherData, boolean fromCache) {
                weatherData.setFromCache(fromCache);
                renderWeather(weatherData, fromCache);
                showStateCard(false, "", false);
                setRefreshing(false);
            }

            @Override
            public void onFailure(String message, @Nullable WeatherData cachedData) {
                if (cachedData != null) {
                    cachedData.setFromCache(true);
                    renderWeather(cachedData, true);
                    showStateCard(true, message, true);
                } else {
                    showStateCard(true, message, true);
                }
                setRefreshing(false);
            }
        });
    }

    private void renderWeather(WeatherData weatherData, boolean fromCache) {
        WeatherResponse weatherResponse = weatherData.getWeatherResponse();
        ForecastResponse forecastResponse = weatherData.getForecastResponse();
        if (weatherResponse == null || weatherResponse.getNow() == null) {
            showStateCard(true, getString(R.string.no_weather_data), true);
            return;
        }

        currentWeatherData = weatherData;
        WeatherResponse.Now now = weatherResponse.getNow();
        if (binding.weatherContentLayout.getVisibility() != View.VISIBLE) {
            binding.weatherContentLayout.setVisibility(View.VISIBLE);
        }
        if (!weatherContentAnimated) {
            binding.weatherContentLayout.setAlpha(0f);
            binding.weatherContentLayout.setTranslationY(36f);
            binding.weatherContentLayout.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(520L)
                    .start();
            weatherContentAnimated = true;
        }
        animateTemperatureText(now.getTemp());
        binding.weatherConditionText.setText(now.getText());
        binding.feelsLikeValueText.setText(getString(R.string.temp_value, now.getFeelsLike()));
        binding.humidityValueText.setText(getString(R.string.percent_value, now.getHumidity()));
        binding.windValueText.setText(getString(R.string.wind_value, now.getWindDir(), now.getWindScale()));
        binding.updateTimeText.setText(buildUpdateText(weatherResponse.getUpdateTime(), fromCache));
        WeatherIconManager.loadWeatherIcon(this, binding.weatherIcon, now.getIcon());
        applyWeatherIconMotion(now.getIcon());
        updateDayNightSticker();
        bindCurrentSummary(now);
        bindExtraIndices(now);

        List<ForecastResponse.Daily> dailyList = forecastResponse == null ? null : forecastResponse.getDaily();
        forecastAdapter.submitList(dailyList);
        binding.forecastRecyclerView.scrollToPosition(0);
        binding.forecastRecyclerView.post(this::applyForecastDepthEffect);

        ForecastResponse.Daily today = dailyList != null && !dailyList.isEmpty() ? dailyList.get(0) : null;
        ClothingSuggestion suggestion = clothingSuggestionService.buildSuggestion(today, now);
        currentClothingSuggestion = suggestion;
        bindSuggestion(suggestion);
        requestAiClothingSuggestion(now, today, suggestion);
        updateAssistantHeader();

        backgroundManager.applyWeatherTheme(this, binding.mainBackground, binding.weatherEffectView,
                now.getIcon(), now.getText());
    }

    private void bindSuggestion(ClothingSuggestion suggestion) {
        binding.clothingIcon.setImageResource(suggestion.getIconResId());
        binding.clothingSuggestionText.setText(suggestion.getSuggestion());
        if (suggestion.getAccessoryTip() == null || suggestion.getAccessoryTip().isEmpty()) {
            binding.clothingAccessoryText.setText(R.string.no_accessory_tip);
        } else {
            binding.clothingAccessoryText.setText(suggestion.getAccessoryTip());
        }

        int backgroundColor;
        switch (suggestion.getLevel()) {
            case "hot":
                backgroundColor = R.color.clothing_hot;
                break;
            case "warm":
                backgroundColor = R.color.clothing_warm;
                break;
            case "cold":
                backgroundColor = R.color.clothing_cold;
                break;
            case "freezing":
                backgroundColor = R.color.clothing_freezing;
                break;
            default:
                backgroundColor = R.color.clothing_cool;
                break;
        }
        int tintedColor = ColorUtils.setAlphaComponent(
                ContextCompat.getColor(this, backgroundColor),
                126
        );
        binding.clothingContainer.setBackgroundTintList(ColorStateList.valueOf(tintedColor));
        if (binding.clothingIcon instanceof HolographicImageView) {
            int startColor;
            int endColor;
            switch (suggestion.getLevel()) {
                case "hot":
                    startColor = Color.argb(188, 255, 178, 112);
                    endColor = Color.argb(158, 255, 104, 170);
                    break;
                case "cold":
                case "freezing":
                    startColor = Color.argb(176, 202, 234, 255);
                    endColor = Color.argb(152, 112, 194, 255);
                    break;
                default:
                    startColor = Color.argb(182, 136, 232, 255);
                    endColor = Color.argb(158, 110, 255, 205);
                    break;
            }
            ((HolographicImageView) binding.clothingIcon).setGlowPalette(startColor, endColor);
        }
    }

    private void applyWeatherIconMotion(String iconCode) {
        if (weatherIconAnimator != null) {
            weatherIconAnimator.cancel();
        }
        binding.weatherIcon.setRotation(0f);
        binding.weatherIcon.setRotationY(0f);
        binding.weatherIcon.setTranslationX(0f);
        binding.weatherIcon.setTranslationY(0f);
        binding.weatherIcon.setScaleX(1f);
        binding.weatherIcon.setScaleY(1f);

        WeatherIconManager.WeatherKind kind = WeatherIconManager.resolveKind(iconCode);
        weatherIconAnimator = ValueAnimator.ofFloat(0f, 1f);
        weatherIconAnimator.setDuration(4200L);
        weatherIconAnimator.setRepeatCount(ValueAnimator.INFINITE);
        weatherIconAnimator.addUpdateListener(animation -> {
            float phase = (float) animation.getAnimatedValue();
            float wave = (float) Math.sin(phase * Math.PI * 2);
            float waveSlow = (float) Math.sin((phase * Math.PI * 2) * 0.5f);
            switch (kind) {
                case SUNNY:
                    binding.weatherIcon.setRotation(phase * 8f);
                    binding.weatherIcon.setScaleX(1f + waveSlow * 0.04f);
                    binding.weatherIcon.setScaleY(1f + waveSlow * 0.04f);
                    binding.weatherIcon.setTranslationY(wave * 2.2f);
                    break;
                case CLOUDY:
                    binding.weatherIcon.setTranslationX(wave * 3.6f);
                    binding.weatherIcon.setTranslationY(waveSlow * 2.4f);
                    binding.weatherIcon.setRotation(wave * 2.2f);
                    break;
                case OVERCAST:
                    binding.weatherIcon.setTranslationX(waveSlow * 2f);
                    binding.weatherIcon.setTranslationY(wave * 1.6f);
                    binding.weatherIcon.setScaleX(1f + wave * 0.015f);
                    binding.weatherIcon.setScaleY(1f + wave * 0.015f);
                    break;
                case RAIN:
                    binding.weatherIcon.setTranslationY(Math.abs(wave) * 2.4f);
                    binding.weatherIcon.setRotation(wave * 1.6f);
                    break;
                case SNOW:
                    binding.weatherIcon.setRotation(wave * 5.5f);
                    binding.weatherIcon.setTranslationY(waveSlow * 2f);
                    binding.weatherIcon.setScaleX(1f + wave * 0.02f);
                    binding.weatherIcon.setScaleY(1f + wave * 0.02f);
                    break;
                default:
                    binding.weatherIcon.setTranslationY(wave * 1.2f);
                    break;
            }
        });
        weatherIconAnimator.start();
    }

    private void updateDayNightSticker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        boolean isDaytime = hour >= 6 && hour < 18;
        binding.dayNightStickerImage.setImageResource(isDaytime
                ? R.drawable.sticker_sun
                : R.drawable.sticker_moon);
        binding.dayNightStickerImage.setRotation(isDaytime ? -4f : 4f);
        binding.dayNightStickerImage.animate()
                .cancel();
        binding.dayNightStickerImage.setScaleX(1f);
        binding.dayNightStickerImage.setScaleY(1f);
        binding.dayNightStickerImage.setTranslationY(0f);
        binding.dayNightStickerImage.animate()
                .translationY(isDaytime ? -3f : 3f)
                .scaleX(1.02f)
                .scaleY(1.02f)
                .setDuration(1800L)
                .withEndAction(() -> {
                    if (binding == null) {
                        return;
                    }
                    binding.dayNightStickerImage.animate()
                            .translationY(0f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(1800L)
                            .start();
                })
                .start();
    }

    private void applyForecastDepthEffect() {
        RecyclerView recyclerView = binding.forecastRecyclerView;
        int childCount = recyclerView.getChildCount();
        if (childCount == 0) {
            return;
        }
        float center = recyclerView.getPaddingLeft() + (recyclerView.getWidth() - recyclerView.getPaddingLeft()
                - recyclerView.getPaddingRight()) * 0.5f;
        for (int i = 0; i < childCount; i++) {
            View child = recyclerView.getChildAt(i);
            float childCenter = child.getX() + child.getWidth() * 0.5f;
            float distance = Math.abs(center - childCenter);
            float normalized = Math.min(1f, distance / Math.max(1f, recyclerView.getWidth() * 0.6f));
            float scale = 1f - normalized * 0.16f;
            float alpha = 1f - normalized * 0.32f;
            float rotation = (childCenter - center) / Math.max(1f, recyclerView.getWidth()) * 12f;
            child.setPivotX(child.getWidth() * 0.5f);
            child.setPivotY(child.getHeight() * 0.5f);
            child.setScaleX(scale);
            child.setScaleY(scale);
            child.setAlpha(alpha);
            child.setRotationY(rotation);
            child.setTranslationY(normalized * 14f);
        }
    }

    private void requestAiClothingSuggestion(WeatherResponse.Now now,
                                             @Nullable ForecastResponse.Daily today,
                                             ClothingSuggestion fallbackSuggestion) {
        if (selectedCity == null || now == null || aiSuggestionRequestInFlight) {
            return;
        }
        String requestKey = buildAiSuggestionKey(now, today, fallbackSuggestion);
        if (requestKey.equals(lastAiSuggestionKey)) {
            return;
        }
        lastAiSuggestionKey = requestKey;
        aiSuggestionRequestInFlight = true;
        List<AiChatMessage> promptMessages = new ArrayList<>();
        promptMessages.add(new AiChatMessage(
                AiChatMessage.ROLE_USER,
                buildAiSuggestionUserPrompt(now, today, fallbackSuggestion)
        ));
        aiAssistantRepository.chat(promptMessages, buildAiSuggestionSystemPrompt(), new AiAssistantRepository.ChatCallback() {
            @Override
            public void onSuccess(String responseText) {
                runOnUiThread(() -> {
                    aiSuggestionRequestInFlight = false;
                    if (!requestKey.equals(lastAiSuggestionKey)) {
                        return;
                    }
                    applyAiSuggestionToCard(responseText, fallbackSuggestion);
                });
            }

            @Override
            public void onFailure(String message) {
                runOnUiThread(() -> {
                    aiSuggestionRequestInFlight = false;
                    if (!requestKey.equals(lastAiSuggestionKey)) {
                        return;
                    }
                    bindSuggestion(fallbackSuggestion);
                });
            }
        });
    }

    private String buildAiSuggestionKey(WeatherResponse.Now now,
                                        @Nullable ForecastResponse.Daily today,
                                        ClothingSuggestion fallbackSuggestion) {
        return nonEmpty(selectedCity == null ? "" : selectedCity.getId(), "")
                + "|" + nonEmpty(now.getTemp(), "")
                + "|" + nonEmpty(now.getText(), "")
                + "|" + nonEmpty(now.getWindScale(), "")
                + "|" + (today == null ? "" : nonEmpty(today.getTempMin(), "") + "-" + nonEmpty(today.getTempMax(), ""))
                + "|" + nonEmpty(fallbackSuggestion.getSuggestion(), "");
    }

    private String buildAiSuggestionSystemPrompt() {
        return "你是天气穿衣助手。请只输出两行简短中文建议。"
                + "第一行不超过18个字，直接回答今天怎么穿。"
                + "第二行不超过20个字，补充随身物品或出行提醒。"
                + "不要使用序号、标题、表情、引号。";
    }

    private String buildAiSuggestionUserPrompt(WeatherResponse.Now now,
                                               @Nullable ForecastResponse.Daily today,
                                               ClothingSuggestion fallbackSuggestion) {
        StringBuilder builder = new StringBuilder();
        builder.append("城市：").append(selectedCity == null ? "当前城市" : selectedCity.getName()).append("。");
        builder.append("当前天气：").append(nonEmpty(now.getText(), "天气平稳"))
                .append("，温度 ").append(nonEmpty(now.getTemp(), "--")).append("°")
                .append("，体感 ").append(nonEmpty(now.getFeelsLike(), nonEmpty(now.getTemp(), "--"))).append("°")
                .append("，湿度 ").append(nonEmpty(now.getHumidity(), "--")).append("%")
                .append("，风力 ").append(nonEmpty(now.getWindDir(), "风")).append(nonEmpty(now.getWindScale(), "--")).append("级。");
        if (today != null) {
            builder.append("今日预报：")
                    .append(nonEmpty(today.getTextDay(), nonEmpty(now.getText(), "天气平稳")))
                    .append("，最低 ").append(nonEmpty(today.getTempMin(), "--")).append("°")
                    .append("，最高 ").append(nonEmpty(today.getTempMax(), "--")).append("°。");
        }
        builder.append("现有建议：").append(nonEmpty(fallbackSuggestion.getSuggestion(), "按舒适层次穿搭。")).append("。");
        if (!isEmpty(fallbackSuggestion.getAccessoryTip())) {
            builder.append("补充提醒：").append(fallbackSuggestion.getAccessoryTip()).append("。");
        }
        return builder.toString();
    }

    private void applyAiSuggestionToCard(String responseText, ClothingSuggestion fallbackSuggestion) {
        String cleaned = responseText == null ? "" : responseText.replace("\r", "").trim();
        if (cleaned.isEmpty()) {
            bindSuggestion(fallbackSuggestion);
            return;
        }
        String[] lines = cleaned.split("\n+");
        String primary = lines.length > 0 ? lines[0].trim() : "";
        String secondary = lines.length > 1 ? lines[1].trim() : "";
        if (primary.isEmpty()) {
            primary = fallbackSuggestion.getSuggestion();
        }
        if (secondary.isEmpty()) {
            secondary = fallbackSuggestion.getAccessoryTip();
        }
        if (secondary == null || secondary.trim().isEmpty()) {
            secondary = getString(R.string.no_accessory_tip);
        }
        binding.clothingSuggestionText.setText(primary);
        binding.clothingAccessoryText.setText(secondary);
    }

    private void bindCurrentSummary(WeatherResponse.Now now) {
        StringBuilder summary = new StringBuilder();
        summary.append("\u4eca\u5929").append(nonEmpty(now.getText(), "\u5929\u6c14\u5e73\u7a33"));
        String feelsLike = nonEmpty(now.getFeelsLike(), now.getTemp());
        if (!feelsLike.isEmpty()) {
            summary.append("\uff0c\u4f53\u611f\u7ea6 ").append(feelsLike).append("\u00b0");
        }
        if (!isEmpty(now.getVis())) {
            summary.append("\uff0c\u80fd\u89c1\u5ea6 ").append(now.getVis()).append(" km");
        }
        summary.append("\u3002");
        binding.currentSummaryText.setText(summary.toString());
    }

    private void bindExtraIndices(WeatherResponse.Now now) {
        binding.indexPrecipValueText.setText(formatMetric(now.getPrecip(), "mm"));
        binding.indexPressureValueText.setText(formatMetric(now.getPressure(), "hPa"));
        binding.indexVisibilityValueText.setText(formatMetric(now.getVis(), "km"));
        binding.indexCloudValueText.setText(formatMetric(now.getCloud(), "%"));
    }

    private void animateTemperatureText(String tempValue) {
        if (tempValue == null || tempValue.isEmpty()) {
            binding.currentTempText.setText("--\u00B0");
            return;
        }
        if (tempValue.equals(lastRenderedTemp)) {
            binding.currentTempText.setText(getString(R.string.temp_value, tempValue));
            return;
        }
        int startValue = safeParseInt(lastRenderedTemp, 0);
        int targetValue = safeParseInt(tempValue, startValue);
        ValueAnimator animator = ValueAnimator.ofInt(startValue, targetValue);
        animator.setDuration(760L);
        animator.addUpdateListener(animation ->
                binding.currentTempText.setText(getString(
                        R.string.temp_value,
                        String.valueOf(animation.getAnimatedValue())
                )));
        animator.start();
        lastRenderedTemp = tempValue;
    }

    private int safeParseInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String buildUpdateText(String updateTime, boolean fromCache) {
        String prefix = fromCache ? getString(R.string.cache_hint_prefix) : getString(R.string.update_prefix);
        if (updateTime == null || updateTime.isEmpty()) {
            return prefix;
        }
        return prefix + formatTime(updateTime);
    }

    private String formatTime(String rawValue) {
        try {
            String dateTime = rawValue.replace("T", " ").replace("+08:00", "");
            Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).parse(dateTime.substring(0, 16));
            return new SimpleDateFormat("HH:mm", Locale.CHINA).format(date);
        } catch (Exception ignored) {
            return rawValue;
        }
    }

    private void showStateCard(boolean visible, String message, boolean showRetry) {
        binding.stateCard.setVisibility(visible ? View.VISIBLE : View.GONE);
        binding.stateText.setText(message);
        binding.loadingProgress.setVisibility(showRetry ? View.GONE : View.VISIBLE);
        binding.retryButton.setVisibility(showRetry ? View.VISIBLE : View.GONE);
    }

    private void setRefreshing(boolean refreshing) {
        binding.swipeRefreshLayout.setRefreshing(refreshing);
    }

    private void openCityManager() {
        Intent intent = new Intent(this, CityManageActivity.class);
        if (selectedCity != null) {
            intent.putExtra(Constants.EXTRA_SELECTED_CITY_ID, selectedCity.getId());
        }
        cityManageLauncher.launch(intent);
    }

    private void reconcileSelectionAfterListChange() {
        executorService.execute(() -> {
            List<City> cities = AppDatabase.getInstance(this).cityDao().getAllCities();
            City matchedCity = null;
            if (selectedCity != null) {
                for (City city : cities) {
                    if (city.getId().equals(selectedCity.getId())) {
                        matchedCity = city;
                        break;
                    }
                }
            }
            City finalMatchedCity = matchedCity;
            runOnUiThread(() -> {
                if (finalMatchedCity != null) {
                    applySelectedCity(finalMatchedCity, false);
                    fetchWeather(false);
                } else if (!cities.isEmpty()) {
                    applySelectedCity(cities.get(0), false);
                    fetchWeather(false);
                } else if (selectedCityFromLocation) {
                    requestLocationOrPermission();
                } else {
                    switchToFallbackCity();
                }
            });
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private String formatMetric(String value, String unit) {
        if (isEmpty(value)) {
            return "--";
        }
        return value + " " + unit;
    }

    private String nonEmpty(String primary, String fallback) {
        return isEmpty(primary) ? Objects.requireNonNullElse(fallback, "") : primary;
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void openAiAssistantDialog(View sourceView) {
        ensureAiAssistantDialog();
        ensureAssistantConversationSeeded();
        updateAssistantHeader();
        aiAssistantAdapterRefresh();
        animateAssistantTrigger(sourceView);
        aiAssistantDialog.show();
        Window window = aiAssistantDialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        View bottomSheet = aiAssistantDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.setBackgroundColor(Color.TRANSPARENT);
            ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
            layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
            bottomSheet.setLayoutParams(layoutParams);
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setSkipCollapsed(true);
            behavior.setFitToContents(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
        aiAssistantBinding.aiInputEditText.requestFocus();
        playAssistantPanelEnterAnimation();
        aiAssistantBinding.aiInputEditText.post(this::showAssistantInputMethod);
        aiAssistantBinding.aiChatRecyclerView.post(this::scrollAiAssistantToBottom);
    }

    private void ensureAiAssistantDialog() {
        if (aiAssistantDialog != null) {
            return;
        }
        aiAssistantDialog = new BottomSheetDialog(this);
        aiAssistantBinding = LayoutAiAssistantBinding.inflate(getLayoutInflater());
        aiAssistantDialog.setContentView(aiAssistantBinding.getRoot());
        int rootPaddingTop = aiAssistantBinding.getRoot().getPaddingTop();
        int rootPaddingBottom = aiAssistantBinding.getRoot().getPaddingBottom();
        int composerPaddingBottom = aiAssistantBinding.assistantComposerContainer.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(aiAssistantBinding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int bottomInset = Math.max(systemBars.bottom, imeBottom);
            v.setPadding(v.getPaddingLeft(), rootPaddingTop + systemBars.top, v.getPaddingRight(), rootPaddingBottom);
            aiAssistantBinding.assistantComposerContainer.setPadding(
                    aiAssistantBinding.assistantComposerContainer.getPaddingLeft(),
                    aiAssistantBinding.assistantComposerContainer.getPaddingTop(),
                    aiAssistantBinding.assistantComposerContainer.getPaddingRight(),
                    composerPaddingBottom + bottomInset
            );
            aiAssistantBinding.aiChatRecyclerView.setPadding(
                    aiAssistantBinding.aiChatRecyclerView.getPaddingLeft(),
                    aiAssistantBinding.aiChatRecyclerView.getPaddingTop(),
                    aiAssistantBinding.aiChatRecyclerView.getPaddingRight(),
                    dpToPx(12)
            );
            if (imeBottom > 0) {
                aiAssistantBinding.aiChatRecyclerView.post(this::scrollAiAssistantToBottom);
            }
            return insets;
        });

        aiChatAdapter = new AiChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(false);
        aiAssistantBinding.aiChatRecyclerView.setLayoutManager(layoutManager);
        aiAssistantBinding.aiChatRecyclerView.setAdapter(aiChatAdapter);
        aiAssistantBinding.aiInputEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                syncAssistantSendState();
                aiAssistantBinding.aiChatRecyclerView.post(MainActivity.this::scrollAiAssistantToBottom);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        aiAssistantBinding.aiInputEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                aiAssistantBinding.aiChatRecyclerView.post(this::scrollAiAssistantToBottom);
            }
        });
        aiAssistantBinding.aiInputEditText.setOnClickListener(v ->
                aiAssistantBinding.aiChatRecyclerView.post(this::scrollAiAssistantToBottom));

        aiAssistantBinding.closeAiButton.setOnClickListener(v -> aiAssistantDialog.dismiss());
        aiAssistantBinding.sendButton.setOnClickListener(v -> sendAssistantMessage());
        aiAssistantBinding.aiInputEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN)) {
                sendAssistantMessage();
                return true;
            }
            return false;
        });
        aiAssistantBinding.promptClothingChip.setOnClickListener(v ->
                sendAssistantMessage(getString(R.string.ai_prompt_clothing)));
        aiAssistantBinding.promptUmbrellaChip.setOnClickListener(v ->
                sendAssistantMessage(getString(R.string.ai_prompt_umbrella)));
        aiAssistantBinding.promptPlanChip.setOnClickListener(v ->
                sendAssistantMessage(getString(R.string.ai_prompt_plan)));
        bindAssistantActionButton(aiAssistantBinding.voiceInputButton);
        syncAssistantSendState();
    }

    private void ensureAssistantConversationSeeded() {
        String cityId = selectedCity == null ? "" : selectedCity.getId();
        if (!assistantMessages.isEmpty() && cityId.equals(assistantContextCityId)) {
            return;
        }
        assistantContextCityId = cityId;
        assistantMessages.clear();
        assistantMessages.add(new AiChatMessage(
                AiChatMessage.ROLE_ASSISTANT,
                buildAssistantWelcomeMessage()
        ));
    }

    private void clearAssistantConversation() {
        assistantContextCityId = "";
        assistantMessages.clear();
        if (aiAssistantBinding != null) {
            aiAssistantAdapterRefresh();
            updateAssistantHeader();
        }
    }

    private String buildAssistantWelcomeMessage() {
        if (selectedCity == null || currentWeatherData == null || currentWeatherData.getWeatherResponse() == null
                || currentWeatherData.getWeatherResponse().getNow() == null) {
            return getString(R.string.ai_default_welcome);
        }
        WeatherResponse.Now now = currentWeatherData.getWeatherResponse().getNow();
        String accessory = currentClothingSuggestion == null || currentClothingSuggestion.getAccessoryTip() == null
                || currentClothingSuggestion.getAccessoryTip().isEmpty()
                ? getString(R.string.no_accessory_tip)
                : currentClothingSuggestion.getAccessoryTip();
        return getString(
                R.string.ai_weather_welcome,
                selectedCity.getName(),
                now.getTemp(),
                now.getText(),
                currentClothingSuggestion == null ? getString(R.string.ai_fallback_suggestion)
                        : currentClothingSuggestion.getSuggestion(),
                accessory
        );
    }

    private void updateAssistantHeader() {
        if (aiAssistantBinding == null) {
            return;
        }
        if (selectedCity == null || currentWeatherData == null || currentWeatherData.getWeatherResponse() == null
                || currentWeatherData.getWeatherResponse().getNow() == null) {
            aiAssistantBinding.aiSubtitleText.setText(R.string.ai_subtitle_placeholder);
            return;
        }
        WeatherResponse.Now now = currentWeatherData.getWeatherResponse().getNow();
        aiAssistantBinding.aiSubtitleText.setText(getString(
                R.string.ai_subtitle_weather,
                selectedCity.getName(),
                now.getTemp(),
                now.getText()
        ));
    }

    private void sendAssistantMessage() {
        String input = aiAssistantBinding.aiInputEditText.getText() == null
                ? ""
                : aiAssistantBinding.aiInputEditText.getText().toString().trim();
        sendAssistantMessage(input);
    }

    private void sendAssistantMessage(String input) {
        if (input == null || input.trim().isEmpty()) {
            return;
        }
        if (assistantRequestInFlight) {
            showToast(getString(R.string.ai_waiting_response));
            return;
        }
        ensureAssistantConversationSeeded();
        assistantMessages.add(new AiChatMessage(AiChatMessage.ROLE_USER, input.trim()));
        assistantMessages.add(new AiChatMessage(AiChatMessage.ROLE_ASSISTANT,
                getString(R.string.ai_thinking), true));
        assistantRequestInFlight = true;
        if (aiAssistantBinding != null) {
            aiAssistantBinding.aiInputEditText.setText("");
            aiAssistantBinding.aiInputEditText.setEnabled(false);
            syncAssistantSendState();
        }
        aiAssistantAdapterRefresh();

        aiAssistantRepository.chat(new ArrayList<>(assistantMessages), buildAssistantSystemPrompt(),
                new AiAssistantRepository.ChatCallback() {
                    @Override
                    public void onSuccess(String responseText) {
                        runOnUiThread(() -> completeAssistantRequest(responseText, null));
                    }

                    @Override
                    public void onFailure(String message) {
                        runOnUiThread(() -> completeAssistantRequest(null, message));
                    }
                });
    }

    private void completeAssistantRequest(@Nullable String responseText, @Nullable String errorMessage) {
        assistantRequestInFlight = false;
        if (!assistantMessages.isEmpty() && assistantMessages.get(assistantMessages.size() - 1).isLoading()) {
            assistantMessages.remove(assistantMessages.size() - 1);
        }
        if (responseText != null && !responseText.isEmpty()) {
            assistantMessages.add(new AiChatMessage(AiChatMessage.ROLE_ASSISTANT, responseText));
        } else {
            assistantMessages.add(new AiChatMessage(
                    AiChatMessage.ROLE_ASSISTANT,
                    errorMessage == null ? getString(R.string.ai_request_failed_generic) : errorMessage
            ));
        }
        if (aiAssistantBinding != null) {
            aiAssistantBinding.aiInputEditText.setEnabled(true);
            syncAssistantSendState();
        }
        aiAssistantAdapterRefresh();
    }

    private void aiAssistantAdapterRefresh() {
        if (aiChatAdapter == null) {
            return;
        }
        aiChatAdapter.submitList(new ArrayList<>(assistantMessages));
        if (aiAssistantBinding != null && !assistantMessages.isEmpty()) {
            aiAssistantBinding.aiChatRecyclerView.post(this::scrollAiAssistantToBottom);
        }
    }

    private void scrollAiAssistantToBottom() {
        if (aiAssistantBinding == null || assistantMessages.isEmpty()) {
            return;
        }
        aiAssistantBinding.aiChatRecyclerView.smoothScrollToPosition(assistantMessages.size() - 1);
    }

    private void syncAssistantSendState() {
        if (aiAssistantBinding == null) {
            return;
        }
        boolean hasInput = aiAssistantBinding.aiInputEditText.getText() != null
                && !aiAssistantBinding.aiInputEditText.getText().toString().trim().isEmpty();
        boolean enabled = hasInput && !assistantRequestInFlight && aiAssistantBinding.aiInputEditText.isEnabled();
        aiAssistantBinding.sendButton.setEnabled(enabled);
        aiAssistantBinding.sendButton.setAlpha(enabled ? 1f : 0.56f);
    }

    private void bindAssistantActionButton(View actionView) {
        actionView.setOnClickListener(v -> showToast(getString(R.string.ai_feature_coming_soon)));
    }

    private void showAssistantInputMethod() {
        if (aiAssistantBinding == null) {
            return;
        }
        WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(aiAssistantBinding.aiInputEditText);
        if (controller != null) {
            controller.show(WindowInsetsCompat.Type.ime());
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void animateAssistantTrigger(@Nullable View sourceView) {
        if (sourceView == null) {
            return;
        }
        sourceView.animate()
                .scaleX(0.96f)
                .scaleY(0.96f)
                .setDuration(90L)
                .withEndAction(() -> sourceView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(180L)
                        .setInterpolator(new OvershootInterpolator(1.05f))
                        .start())
                .start();
    }

    private void playAssistantPanelEnterAnimation() {
        if (aiAssistantBinding == null) {
            return;
        }
        View panel = aiAssistantBinding.getRoot();
        panel.setAlpha(0f);
        panel.setTranslationY(42f);
        panel.setScaleX(0.98f);
        panel.setScaleY(0.98f);
        panel.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(280L)
                .setInterpolator(new OvershootInterpolator(0.92f))
                .start();
    }

    private void configureImmersiveWindow() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
        }
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, binding.getRoot());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(false);
        }
    }

    private boolean handleAiFabDrag(View view, MotionEvent event) {
        View parent = (View) view.getParent();
        if (parent == null) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                aiFabTouchDownRawX = event.getRawX();
                aiFabTouchDownRawY = event.getRawY();
                aiFabStartTranslationX = view.getTranslationX();
                aiFabStartTranslationY = view.getTranslationY();
                aiFabDragging = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float deltaX = event.getRawX() - aiFabTouchDownRawX;
                float deltaY = event.getRawY() - aiFabTouchDownRawY;
                if (!aiFabDragging && (Math.abs(deltaX) > 8f || Math.abs(deltaY) > 8f)) {
                    aiFabDragging = true;
                }
                if (!aiFabDragging) {
                    return true;
                }
                float minTranslationX = -view.getLeft();
                float maxTranslationX = parent.getWidth() - view.getRight();
                float minTranslationY = -view.getTop();
                float maxTranslationY = parent.getHeight() - view.getBottom();
                view.setTranslationX(clamp(aiFabStartTranslationX + deltaX, minTranslationX, maxTranslationX));
                view.setTranslationY(clamp(aiFabStartTranslationY + deltaY, minTranslationY, maxTranslationY));
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!aiFabDragging) {
                    view.performClick();
                    return true;
                }
                snapAiFabToEdge(view, parent);
                return true;
            default:
                return false;
        }
    }

    private void snapAiFabToEdge(View view, View parent) {
        float centerX = view.getX() + (view.getWidth() / 2f);
        float targetTranslationX = centerX < (parent.getWidth() / 2f)
                ? -view.getLeft()
                : parent.getWidth() - view.getRight();
        float minTranslationY = -view.getTop();
        float maxTranslationY = parent.getHeight() - view.getBottom();
        view.animate()
                .translationX(targetTranslationX)
                .translationY(clamp(view.getTranslationY(), minTranslationY, maxTranslationY))
                .setDuration(220L)
                .start();
    }

    private String buildAssistantSystemPrompt() {
        StringBuilder prompt = new StringBuilder();
        prompt.append("\u4f60\u662f Weather \u5e94\u7528\u5185\u7684 AI \u5929\u6c14\u52a9\u624b\u3002");
        prompt.append("\u8bf7\u53ea\u7528\u7b80\u4f53\u4e2d\u6587\u56de\u7b54\uff0c\u8bed\u6c14\u81ea\u7136\u3001\u5b9e\u7528\u3001\u7b80\u6d01\uff0c\u4f18\u5148\u7ed9\u51fa\u53ef\u6267\u884c\u5efa\u8bae\u3002");
        prompt.append("\u56de\u7b54\u65f6\u7ed3\u5408\u5f53\u524d\u5929\u6c14\u3001\u7a7f\u8863\u5efa\u8bae\u3001\u51fa\u884c\u98ce\u9669\u548c\u7528\u6237\u95ee\u9898\uff0c\u4e0d\u8981\u7f16\u9020\u4e0d\u5b58\u5728\u7684\u6570\u636e\u3002");
        prompt.append("\u5982\u679c\u7528\u6237\u8be2\u95ee\u548c\u5929\u6c14\u65e0\u5173\u7684\u8bdd\u9898\uff0c\u53ef\u4ee5\u793c\u8c8c\u56de\u5e94\uff0c\u4f46\u5c3d\u91cf\u62c9\u56de\u5929\u6c14\u4e0e\u51fa\u884c\u573a\u666f\u3002");

        if (selectedCity != null) {
            prompt.append("\u5f53\u524d\u57ce\u5e02\uff1a").append(selectedCity.getName()).append("\u3002");
        }
        if (currentWeatherData != null && currentWeatherData.getWeatherResponse() != null
                && currentWeatherData.getWeatherResponse().getNow() != null) {
            WeatherResponse.Now now = currentWeatherData.getWeatherResponse().getNow();
            prompt.append("\u5f53\u524d\u5929\u6c14\uff1a")
                    .append(now.getText())
                    .append("\uff0c\u6e29\u5ea6 ").append(now.getTemp()).append("\u00b0\uff0c\u4f53\u611f ")
                    .append(now.getFeelsLike()).append("\u00b0\uff0c\u6e7f\u5ea6 ")
                    .append(now.getHumidity()).append("%\uff0c\u98ce\u5411\u98ce\u529b ")
                    .append(now.getWindDir()).append(now.getWindScale()).append(" \u7ea7\u3002");
        } else {
            prompt.append("\u5f53\u524d\u5929\u6c14\u6570\u636e\u6682\u65f6\u4e0d\u53ef\u7528\u3002");
        }
        if (currentWeatherData != null && currentWeatherData.getForecastResponse() != null
                && currentWeatherData.getForecastResponse().getDaily() != null
                && !currentWeatherData.getForecastResponse().getDaily().isEmpty()) {
            ForecastResponse.Daily today = currentWeatherData.getForecastResponse().getDaily().get(0);
            prompt.append("\u4eca\u65e5\u9884\u62a5\uff1a")
                    .append(today.getTextDay()).append("\uff0c\u6700\u4f4e ")
                    .append(today.getTempMin()).append("\u00b0\uff0c\u6700\u9ad8 ")
                    .append(today.getTempMax()).append("\u00b0\u3002");
        }
        if (currentClothingSuggestion != null) {
            prompt.append("\u5e94\u7528\u5df2\u6709\u7a7f\u8863\u5efa\u8bae\uff1a").append(currentClothingSuggestion.getSuggestion()).append("\u3002");
            if (currentClothingSuggestion.getAccessoryTip() != null
                    && !currentClothingSuggestion.getAccessoryTip().isEmpty()) {
                prompt.append("\u914d\u4ef6\u63d0\u9192\uff1a").append(currentClothingSuggestion.getAccessoryTip()).append("\u3002");
            }
        }
        if (currentWeatherData != null && currentWeatherData.isFromCache()) {
            prompt.append("\u6ce8\u610f\uff1a\u5f53\u524d\u5929\u6c14\u6765\u81ea\u79bb\u7ebf\u7f13\u5b58\uff0c\u8bf7\u63d0\u9192\u7528\u6237\u6570\u636e\u53ef\u80fd\u4e0d\u662f\u6700\u65b0\u3002");
        }
        return prompt.toString();
    }
}
