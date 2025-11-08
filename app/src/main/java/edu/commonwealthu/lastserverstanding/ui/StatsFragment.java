package edu.commonwealthu.lastserverstanding.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;

import edu.commonwealthu.lastserverstanding.R;
import edu.commonwealthu.lastserverstanding.data.firebase.FirebaseManager;

/**
 * Stats Fragment - Displays leaderboard and player statistics
 */
public class StatsFragment extends Fragment {

    private RecyclerView leaderboardRecyclerView;
    private CircularProgressIndicator loadingIndicator;
    private View emptyStateView;
    private MaterialButton backButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        leaderboardRecyclerView = view.findViewById(R.id.leaderboard_recycler);
        loadingIndicator = view.findViewById(R.id.loading_indicator);
        emptyStateView = view.findViewById(R.id.empty_state);
        backButton = view.findViewById(R.id.btn_back);

        // Set up RecyclerView
        leaderboardRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Set up back button
        backButton.setOnClickListener(v -> {
            Navigation.findNavController(v).navigateUp();
        });

        // Load leaderboard data
        loadLeaderboard();
    }

    /**
     * Load leaderboard from Firebase
     */
    private void loadLeaderboard() {
        loadingIndicator.setVisibility(View.VISIBLE);
        leaderboardRecyclerView.setVisibility(View.GONE);
        emptyStateView.setVisibility(View.GONE);

        FirebaseManager.getInstance().getTopScores(10, new FirebaseManager.LeaderboardFetchCallback() {
            @Override
            public void onSuccess(List<FirebaseManager.LeaderboardEntry> entries) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    loadingIndicator.setVisibility(View.GONE);

                    if (entries.isEmpty()) {
                        emptyStateView.setVisibility(View.VISIBLE);
                    } else {
                        leaderboardRecyclerView.setVisibility(View.VISIBLE);
                        // TODO: Set adapter with entries
                        // For now, show placeholder
                        showPlaceholderData();
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    loadingIndicator.setVisibility(View.GONE);
                    emptyStateView.setVisibility(View.VISIBLE);
                    Toast.makeText(requireContext(),
                            "Failed to load leaderboard: " + message,
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Show placeholder data while RecyclerView adapter is not implemented
     */
    private void showPlaceholderData() {
        // TODO: Implement LeaderboardAdapter
        // For now, just show the RecyclerView
        Toast.makeText(requireContext(),
                "Leaderboard loaded (adapter not yet implemented)",
                Toast.LENGTH_SHORT).show();
    }
}
