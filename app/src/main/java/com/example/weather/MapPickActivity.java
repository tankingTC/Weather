package com.example.weather;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.weather.databinding.ActivityMapPickBinding;
import com.example.weather.model.City;
import com.example.weather.service.WeatherRepository;
import com.example.weather.utils.Constants;

import java.util.Locale;

public class MapPickActivity extends AppCompatActivity {

    private ActivityMapPickBinding binding;
    private WeatherRepository weatherRepository;
    private City pickedCity;
    private boolean lookupInFlight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMapPickBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        weatherRepository = new WeatherRepository(this);
        setupToolbar();
        setupMap();
        binding.addAndViewButton.setOnClickListener(v -> finishWithSelectedCity());
    }

    private void setupToolbar() {
        binding.topAppBar.setNavigationOnClickListener(v -> finish());
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    private void setupMap() {
        WebSettings settings = binding.mapWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        binding.mapWebView.setWebChromeClient(new WebChromeClient());
        binding.mapWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                binding.mapLoading.setVisibility(android.view.View.GONE);
            }
        });
        binding.mapWebView.addJavascriptInterface(new MapBridge(), "AndroidMapPicker");
        binding.mapWebView.loadUrl("file:///android_asset/map_picker.html");
    }

    private void resolveCity(double latitude, double longitude) {
        if (lookupInFlight) {
            return;
        }
        lookupInFlight = true;
        binding.selectedCityText.setText("正在解析城市...");
        binding.selectedCoordText.setText(String.format(Locale.US, "%.4f, %.4f", latitude, longitude));
        binding.addAndViewButton.setEnabled(false);
        weatherRepository.lookupCityByCoordinates(latitude, longitude, new WeatherRepository.CityLookupCallback() {
            @Override
            public void onSuccess(City city) {
                runOnUiThread(() -> {
                    lookupInFlight = false;
                    city.setLat(latitude);
                    city.setLon(longitude);
                    pickedCity = city;
                    binding.selectedCityText.setText(city.getName());
                    binding.selectedCoordText.setText(String.format(Locale.US,
                            "%.4f, %.4f", latitude, longitude));
                    binding.addAndViewButton.setEnabled(true);
                });
            }

            @Override
            public void onFailure(String message) {
                runOnUiThread(() -> {
                    lookupInFlight = false;
                    pickedCity = null;
                    binding.selectedCityText.setText("未能识别这个位置");
                    binding.selectedCoordText.setText(message);
                    binding.addAndViewButton.setEnabled(false);
                });
            }
        });
    }

    private void finishWithSelectedCity() {
        if (pickedCity == null) {
            Toast.makeText(this, "请先在地图上选择一个城市位置", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent();
        intent.putExtra(Constants.EXTRA_SELECTED_CITY_ID, pickedCity.getId());
        intent.putExtra(Constants.EXTRA_SELECTED_CITY_NAME, pickedCity.getName());
        intent.putExtra(Constants.EXTRA_SELECTED_CITY_LAT, pickedCity.getLat());
        intent.putExtra(Constants.EXTRA_SELECTED_CITY_LON, pickedCity.getLon());
        intent.putExtra(Constants.EXTRA_SELECTED_CITY_IS_LOCATION, false);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        binding.mapWebView.removeJavascriptInterface("AndroidMapPicker");
        binding.mapWebView.destroy();
        super.onDestroy();
    }

    private final class MapBridge {
        @JavascriptInterface
        public void onLocationPicked(String latitudeValue, String longitudeValue) {
            try {
                double latitude = Double.parseDouble(latitudeValue);
                double longitude = Double.parseDouble(longitudeValue);
                runOnUiThread(() -> resolveCity(latitude, longitude));
            } catch (NumberFormatException ignored) {
            }
        }
    }
}
