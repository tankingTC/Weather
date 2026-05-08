package com.example.weather.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weather.databinding.ItemCitySearchResultBinding;
import com.example.weather.model.CityResponse;

import java.util.ArrayList;
import java.util.List;

public class CitySearchAdapter extends RecyclerView.Adapter<CitySearchAdapter.SearchViewHolder> {

    public interface SearchActionListener {
        void onAddCity(CityResponse.Location location);
    }

    private final List<CityResponse.Location> items = new ArrayList<>();
    private final SearchActionListener listener;

    public CitySearchAdapter(SearchActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<CityResponse.Location> locations) {
        items.clear();
        if (locations != null) {
            items.addAll(locations);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCitySearchResultBinding binding = ItemCitySearchResultBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new SearchViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
        CityResponse.Location location = items.get(position);
        holder.binding.searchCityNameText.setText(location.getName());
        holder.binding.searchCityMetaText.setText(location.getDisplayName());
        holder.binding.addButton.setOnClickListener(v -> listener.onAddCity(location));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class SearchViewHolder extends RecyclerView.ViewHolder {
        private final ItemCitySearchResultBinding binding;

        SearchViewHolder(ItemCitySearchResultBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
