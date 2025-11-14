package edu.commonwealthu.lastserverstanding.ui;

import android.os.Bundle;
import android.util.Log;
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
 * Stats Fragment - Displays leaderboard ranked by highest wave reached
 */
public class StatsFragment extends Fragment {

    private static final String TAG = "StatsFragment";
    private static final long LOADING_TIMEOUT_MS = 5000; // 5 seconds timeout

    private RecyclerView leaderboardRecyclerView;
    private CircularProgressIndicator loadingIndicator;
    private View emptyStateView;
    private MaterialButton backButton;
    private MaterialButton populateTestDataButton;
    private MaterialButton clearDataButton;
    private MaterialButton reloadButton;
    private LeaderboardAdapter adapter;

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
        populateTestDataButton = view.findViewById(R.id.btn_populate_test_data);
        clearDataButton = view.findViewById(R.id.btn_clear_data);
        reloadButton = view.findViewById(R.id.btn_reload);

        // Set up RecyclerView with adapter
        adapter = new LeaderboardAdapter();
        leaderboardRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        leaderboardRecyclerView.setAdapter(adapter);

        // Set up back button
        backButton.setOnClickListener(v -> {
            Navigation.findNavController(v).navigateUp();
        });

        // Set up debug buttons
        populateTestDataButton.setOnClickListener(v -> populateTestData());
        clearDataButton.setOnClickListener(v -> clearLeaderboard());
        reloadButton.setOnClickListener(v -> loadLeaderboard());

        // Load leaderboard data
        loadLeaderboard();
    }

    /**
     * Load leaderboard from Firebase
     */
    private void loadLeaderboard() {
        Log.d(TAG, "Loading leaderboard...");
        loadingIndicator.setVisibility(View.VISIBLE);
        leaderboardRecyclerView.setVisibility(View.GONE);
        emptyStateView.setVisibility(View.GONE);

        // Set a timeout in case Firebase is slow or unreachable
        final boolean[] responseReceived = {false};
        new android.os.Handler().postDelayed(() -> {
            if (!responseReceived[0] && isAdded()) {
                Log.w(TAG, "Leaderboard loading timeout");
                responseReceived[0] = true;
                showEmptyState("Connection timeout. Try reloading or add test data.");
            }
        }, LOADING_TIMEOUT_MS);

        try {
            FirebaseManager.getInstance().getTopScores(10, new FirebaseManager.LeaderboardFetchCallback() {
                @Override
                public void onSuccess(List<FirebaseManager.LeaderboardEntry> entries) {
                    if (getActivity() == null || !isAdded() || responseReceived[0]) return;
                    responseReceived[0] = true;

                    Log.d(TAG, "Leaderboard loaded: " + entries.size() + " entries");

                    getActivity().runOnUiThread(() -> {
                        loadingIndicator.setVisibility(View.GONE);

                        if (entries.isEmpty()) {
                            Log.d(TAG, "No entries found");
                            showEmptyState("No leaderboard entries yet. Add test data to get started!");
                        } else {
                            leaderboardRecyclerView.setVisibility(View.VISIBLE);
                            adapter.setEntries(entries);
                            Log.d(TAG, "Leaderboard displayed with " + entries.size() + " entries");
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    if (getActivity() == null || !isAdded() || responseReceived[0]) return;
                    responseReceived[0] = true;

                    Log.e(TAG, "Failed to load leaderboard: " + message);

                    getActivity().runOnUiThread(() -> {
                        loadingIndicator.setVisibility(View.GONE);
                        showEmptyState("Failed to connect: " + message);
                        Toast.makeText(requireContext(),
                                "Could not connect to leaderboard: " + message,
                                Toast.LENGTH_LONG).show();
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception loading leaderboard", e);
            responseReceived[0] = true;
            if (isAdded()) {
                showEmptyState("Error: " + e.getMessage());
                Toast.makeText(requireContext(),
                        "Leaderboard error: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Show empty state with custom message
     */
    private void showEmptyState(String message) {
        if (!isAdded()) return;

        requireActivity().runOnUiThread(() -> {
            loadingIndicator.setVisibility(View.GONE);
            leaderboardRecyclerView.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.VISIBLE);
            Log.d(TAG, "Empty state shown: " + message);
        });
    }

    /**
     * Populate Firebase with test data
     */
    private void populateTestData() {
        Log.d(TAG, "Populating test data...");
        populateTestDataButton.setEnabled(false);
        loadingIndicator.setVisibility(View.VISIBLE);

        FirebaseManager.getInstance().populateTestData(new FirebaseManager.LeaderboardCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;

                Log.d(TAG, "Test data populated successfully");
                requireActivity().runOnUiThread(() -> {
                    populateTestDataButton.setEnabled(true);
                    Toast.makeText(requireContext(),
                            "Test data added successfully!",
                            Toast.LENGTH_SHORT).show();
                    // Reload to show new data
                    loadLeaderboard();
                });
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;

                Log.e(TAG, "Failed to populate test data: " + message);
                requireActivity().runOnUiThread(() -> {
                    populateTestDataButton.setEnabled(true);
                    loadingIndicator.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),
                            "Failed to add test data: " + message,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Clear all leaderboard data
     */
    private void clearLeaderboard() {
        Log.d(TAG, "Clearing leaderboard...");
        clearDataButton.setEnabled(false);
        loadingIndicator.setVisibility(View.VISIBLE);

        FirebaseManager.getInstance().clearLeaderboard(new FirebaseManager.LeaderboardCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;

                Log.d(TAG, "Leaderboard cleared successfully");
                requireActivity().runOnUiThread(() -> {
                    clearDataButton.setEnabled(true);
                    Toast.makeText(requireContext(),
                            "Leaderboard cleared!",
                            Toast.LENGTH_SHORT).show();
                    // Reload to show empty state
                    loadLeaderboard();
                });
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;

                Log.e(TAG, "Failed to clear leaderboard: " + message);
                requireActivity().runOnUiThread(() -> {
                    clearDataButton.setEnabled(true);
                    loadingIndicator.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),
                            "Failed to clear data: " + message,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
