package com.tatoalu.hotpotato;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private List<LeaderboardManager.Score> scores = new ArrayList<>();

    public void setScores(List<LeaderboardManager.Score> newScores) {
        scores = newScores;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeaderboardManager.Score score = scores.get(position);
        holder.playerName.setText(score.playerName);
        holder.scoreText.setText(score.score + " pts");

        switch (position) {
            case 0:
                holder.rankIcon.setImageResource(R.drawable.ic_gold_medal);
                break;
            case 1:
                holder.rankIcon.setImageResource(R.drawable.ic_silver_medal);
                break;
            case 2:
                holder.rankIcon.setImageResource(R.drawable.ic_bronze_medal);
                break;
            default:
                holder.rankIcon.setImageResource(R.drawable.ic_rank_default);
        }

        holder.rankText.setText(String.valueOf(position + 1));
    }

    @Override
    public int getItemCount() {
        return scores.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView rankIcon;
        TextView playerName, scoreText, rankText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            rankIcon = itemView.findViewById(R.id.rankIcon);
            playerName = itemView.findViewById(R.id.playerName);
            scoreText = itemView.findViewById(R.id.scoreText);
            rankText = itemView.findViewById(R.id.rankText);
        }
    }
}