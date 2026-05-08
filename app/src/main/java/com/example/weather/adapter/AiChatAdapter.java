package com.example.weather.adapter;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weather.R;
import com.example.weather.databinding.ItemAiMessageBinding;
import com.example.weather.model.AiChatMessage;

import java.util.ArrayList;
import java.util.List;

public class AiChatAdapter extends RecyclerView.Adapter<AiChatAdapter.ChatViewHolder> {

    private final List<AiChatMessage> items = new ArrayList<>();

    public void submitList(List<AiChatMessage> messages) {
        items.clear();
        if (messages != null) {
            items.addAll(messages);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAiMessageBinding binding = ItemAiMessageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ChatViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        AiChatMessage message = items.get(position);
        holder.binding.messageText.setText(message.getContent());
        holder.binding.messageRoleText.setText(message.isUser()
                ? R.string.ai_user_label
                : R.string.ai_assistant_short_name);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) holder.binding.bubbleCard.getLayoutParams();
        if (message.isUser()) {
            params.gravity = Gravity.END;
            holder.binding.bubbleCard.setCardBackgroundColor(
                    ContextCompat.getColor(holder.binding.getRoot().getContext(), R.color.ai_user_bubble)
            );
            holder.binding.bubbleCard.setStrokeWidth(0);
            holder.binding.messageText.setTextColor(
                    ContextCompat.getColor(holder.binding.getRoot().getContext(), R.color.ai_user_text)
            );
            holder.binding.messageRoleText.setTextColor(
                    ContextCompat.getColor(holder.binding.getRoot().getContext(), R.color.ai_user_text_secondary)
            );
        } else {
            params.gravity = Gravity.START;
            holder.binding.bubbleCard.setCardBackgroundColor(
                    ContextCompat.getColor(holder.binding.getRoot().getContext(),
                            message.isLoading() ? R.color.ai_assistant_loading_bubble : R.color.ai_assistant_bubble)
            );
            holder.binding.bubbleCard.setStrokeWidth(1);
            holder.binding.messageText.setTextColor(
                    ContextCompat.getColor(holder.binding.getRoot().getContext(), R.color.text_primary)
            );
            holder.binding.messageRoleText.setTextColor(
                    ContextCompat.getColor(holder.binding.getRoot().getContext(), R.color.ai_assistant_role)
            );
        }
        holder.binding.bubbleCard.setLayoutParams(params);
        holder.binding.messageText.setAlpha(message.isLoading() ? 0.74f : 1f);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        private final ItemAiMessageBinding binding;

        ChatViewHolder(ItemAiMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
