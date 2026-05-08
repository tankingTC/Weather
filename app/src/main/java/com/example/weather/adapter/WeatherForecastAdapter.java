package com.example.weather.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weather.databinding.ItemForecastBinding;
import com.example.weather.model.ForecastResponse;
import com.example.weather.ui.WeatherIconManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WeatherForecastAdapter extends RecyclerView.Adapter<WeatherForecastAdapter.ForecastViewHolder> {

    private final List<ForecastResponse.Daily> items = new ArrayList<>();
    private final SimpleDateFormat sourceFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("M\u6708d\u65e5", Locale.CHINA);
    private final SimpleDateFormat weekFormat = new SimpleDateFormat("E", Locale.CHINA);

    public void submitList(List<ForecastResponse.Daily> forecastItems) {
        items.clear();
        if (forecastItems != null) {
            items.addAll(forecastItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ForecastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemForecastBinding binding = ItemForecastBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ForecastViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ForecastViewHolder holder, int position) {
        ForecastResponse.Daily daily = items.get(position);
        holder.binding.forecastDateText.setText(formatDay(daily.getFxDate()));
        holder.binding.forecastWeekText.setText(formatWeek(daily.getFxDate()));
        holder.binding.forecastConditionText.setText(daily.getTextDay());
        holder.binding.forecastTempText.setText(daily.getTempMin() + "\u00B0 / " + daily.getTempMax() + "\u00B0");
        WeatherIconManager.loadWeatherIcon(holder.binding.getRoot().getContext(),
                holder.binding.forecastIcon, daily.getIconDay());
        holder.binding.getRoot().setScaleX(1f);
        holder.binding.getRoot().setScaleY(1f);
        holder.binding.getRoot().setAlpha(1f);
        holder.binding.getRoot().setRotationY(0f);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatDay(String value) {
        Date date = parseDate(value);
        return date == null ? value : dayFormat.format(date);
    }

    private String formatWeek(String value) {
        Date date = parseDate(value);
        return date == null ? "" : weekFormat.format(date);
    }

    private Date parseDate(String value) {
        try {
            return sourceFormat.parse(value);
        } catch (ParseException ignored) {
            return null;
        }
    }

    static class ForecastViewHolder extends RecyclerView.ViewHolder {
        private final ItemForecastBinding binding;

        ForecastViewHolder(ItemForecastBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
