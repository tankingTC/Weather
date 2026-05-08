package com.example.weather.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weather.databinding.ItemSavedCityBinding;
import com.example.weather.model.City;

import java.util.ArrayList;
import java.util.List;

public class CityManageAdapter extends RecyclerView.Adapter<CityManageAdapter.CityViewHolder> {

    public interface CityActionListener {
        void onCitySelected(City city);

        void onCityDelete(City city);
    }

    private final List<City> items = new ArrayList<>();
    private final CityActionListener listener;
    private String selectedCityId;

    public CityManageAdapter(CityActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<City> cities, String selectedCityId) {
        this.selectedCityId = selectedCityId;
        items.clear();
        if (cities != null) {
            items.addAll(cities);
        }
        notifyDataSetChanged();
    }

    public City getItem(int position) {
        return items.get(position);
    }

    @NonNull
    @Override
    public CityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSavedCityBinding binding = ItemSavedCityBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new CityViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CityViewHolder holder, int position) {
        City city = items.get(position);
        holder.binding.cityNameText.setText(city.getName());
        holder.binding.cityMetaText.setText(buildMeta(city));
        holder.binding.cityTempText.setText(buildTemp(city));
        holder.binding.selectedBadge.setVisibility(city.getId().equals(selectedCityId)
                ? View.VISIBLE : View.GONE);
        holder.binding.getRoot().setOnClickListener(v -> listener.onCitySelected(city));
        holder.binding.deleteButton.setOnClickListener(v -> listener.onCityDelete(city));
        holder.binding.getRoot().setOnLongClickListener(v -> {
            listener.onCityDelete(city);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String buildMeta(City city) {
        String text = city.getWeatherText();
        if (text == null || text.isEmpty()) {
            return "暂无天气摘要";
        }
        return text;
    }

    private String buildTemp(City city) {
        String temp = city.getCurrentTemp();
        if (temp == null || temp.isEmpty()) {
            return "--°";
        }
        return temp + "°";
    }

    static class CityViewHolder extends RecyclerView.ViewHolder {
        private final ItemSavedCityBinding binding;

        CityViewHolder(ItemSavedCityBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
