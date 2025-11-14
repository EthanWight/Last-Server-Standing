package edu.commonwealthu.lastserverstanding.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.commonwealthu.lastserverstanding.R;
import edu.commonwealthu.lastserverstanding.data.firebase.FirebaseManager;

/**
 * Adapter for displaying leaderboard entries
 */
public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder> {

    private List<FirebaseManager.LeaderboardEntry> entries;

    public LeaderboardAdapter() {
        this.entries = new ArrayList<>();
    }

    public void setEntries(List<FirebaseManager.LeaderboardEntry> entries) {
        this.entries = entries;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LeaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard_entry, parent, false);
        return new LeaderboardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaderboardViewHolder holder, int position) {
        FirebaseManager.LeaderboardEntry entry = entries.get(position);
        holder.bind(entry, position + 1);
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class LeaderboardViewHolder extends RecyclerView.ViewHolder {
        private final TextView rankText;
        private final TextView playerNameText;
        private final TextView waveText;
        private final TextView timestampText;

        public LeaderboardViewHolder(@NonNull View itemView) {
            super(itemView);
            rankText = itemView.findViewById(R.id.text_rank);
            playerNameText = itemView.findViewById(R.id.text_player_name);
            waveText = itemView.findViewById(R.id.text_wave);
            timestampText = itemView.findViewById(R.id.text_timestamp);
        }

        public void bind(FirebaseManager.LeaderboardEntry entry, int rank) {
            rankText.setText(String.valueOf(rank));
            playerNameText.setText(entry.playerName != null ? entry.playerName : "Anonymous");
            waveText.setText(String.valueOf(entry.wave));
            timestampText.setText(formatTimestamp(entry.timestamp));
        }

        private String formatTimestamp(long timestamp) {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;

            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (days > 0) {
                return days + (days == 1 ? " day ago" : " days ago");
            } else if (hours > 0) {
                return hours + (hours == 1 ? " hour ago" : " hours ago");
            } else if (minutes > 0) {
                return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
            } else {
                return "Just now";
            }
        }
    }
}
