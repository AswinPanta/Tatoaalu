package com.tatoalu.hotpotato;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

/**
 * RecyclerView Adapter for displaying discovered LAN rooms
 */
public class RoomListAdapter extends RecyclerView.Adapter<RoomListAdapter.RoomViewHolder> {

    private List<EnhancedLanDiscovery.DiscoveredRoom> rooms;
    private OnRoomClickListener onRoomClickListener;

    public interface OnRoomClickListener {
        void onRoomClick(EnhancedLanDiscovery.DiscoveredRoom room);
    }

    public RoomListAdapter(List<EnhancedLanDiscovery.DiscoveredRoom> rooms, OnRoomClickListener listener) {
        this.rooms = rooms;
        this.onRoomClickListener = listener;
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_room, parent, false);
        return new RoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        EnhancedLanDiscovery.DiscoveredRoom room = rooms.get(position);
        holder.bind(room, onRoomClickListener);
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    public void updateRooms(List<EnhancedLanDiscovery.DiscoveredRoom> newRooms) {
        this.rooms.clear();
        this.rooms.addAll(newRooms);
        notifyDataSetChanged();
    }

    static class RoomViewHolder extends RecyclerView.ViewHolder {
        private TextView roomCodeText;
        private TextView hostNameText;
        private TextView playerCountText;
        private TextView statusText;
        private ImageView signalIcon;
        private MaterialButton joinButton;
        private MaterialCardView playerCountCard;
        private View roomFullOverlay;
        private TextView roomFullText;

        public RoomViewHolder(@NonNull View itemView) {
            super(itemView);

            roomCodeText = itemView.findViewById(R.id.roomCodeText);
            hostNameText = itemView.findViewById(R.id.hostNameText);
            playerCountText = itemView.findViewById(R.id.playerCountText);
            statusText = itemView.findViewById(R.id.statusText);
            signalIcon = itemView.findViewById(R.id.signalIcon);
            joinButton = itemView.findViewById(R.id.joinButton);
            playerCountCard = itemView.findViewById(R.id.playerCountCard);
            roomFullOverlay = itemView.findViewById(R.id.roomFullOverlay);
            roomFullText = itemView.findViewById(R.id.roomFullText);
        }

        public void bind(EnhancedLanDiscovery.DiscoveredRoom room, OnRoomClickListener listener) {
            // Set room code
            roomCodeText.setText("Room " + room.roomCode);

            // Set host name
            hostNameText.setText("Hosted by " + room.hostName);

            // Set player count
            playerCountText.setText(room.playerCount + "/" + room.maxPlayers);

            // Determine room status and appearance
            boolean isJoinable = room.isJoinable && room.playerCount < room.maxPlayers;

            if (isJoinable) {
                // Room is available
                statusText.setText("Available");
                statusText.setTextColor(itemView.getContext().getColor(R.color.flame_green));
                signalIcon.setImageResource(android.R.drawable.presence_online);
                signalIcon.setColorFilter(itemView.getContext().getColor(R.color.flame_green));

                joinButton.setEnabled(true);
                joinButton.setText("Join");
                joinButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onRoomClick(room);
                    }
                });

                roomFullOverlay.setVisibility(View.GONE);
                roomFullText.setVisibility(View.GONE);

                // Set player count card color based on occupancy
                if (room.playerCount >= room.maxPlayers * 0.75) {
                    playerCountCard.setCardBackgroundColor(itemView.getContext().getColor(R.color.flame_orange));
                } else {
                    playerCountCard.setCardBackgroundColor(itemView.getContext().getColor(R.color.flame_green));
                }

            } else {
                // Room is full or not joinable
                statusText.setText("Full");
                statusText.setTextColor(itemView.getContext().getColor(R.color.flame_red));
                signalIcon.setImageResource(android.R.drawable.presence_busy);
                signalIcon.setColorFilter(itemView.getContext().getColor(R.color.flame_red));

                joinButton.setEnabled(false);
                joinButton.setText("Full");
                joinButton.setOnClickListener(null);

                roomFullOverlay.setVisibility(View.VISIBLE);
                roomFullText.setVisibility(View.VISIBLE);

                playerCountCard.setCardBackgroundColor(itemView.getContext().getColor(R.color.flame_red));
            }

            // Calculate connection quality based on last seen time
            long timeSinceLastSeen = System.currentTimeMillis() - room.lastSeen;
            if (timeSinceLastSeen < 5000) {
                // Very recent - excellent connection
                signalIcon.setAlpha(1.0f);
            } else if (timeSinceLastSeen < 10000) {
                // Somewhat recent - good connection
                signalIcon.setAlpha(0.8f);
            } else {
                // Old - weak connection
                signalIcon.setAlpha(0.5f);
            }

            // Set up click listener for the entire card
            itemView.setOnClickListener(v -> {
                if (isJoinable && listener != null) {
                    listener.onRoomClick(room);
                }
            });

            // Apply visual feedback for clickable state
            itemView.setClickable(isJoinable);
            itemView.setAlpha(isJoinable ? 1.0f : 0.7f);
        }
    }
}
