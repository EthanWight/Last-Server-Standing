package edu.commonwealthu.lastserverstanding.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.List;

import edu.commonwealthu.lastserverstanding.R;
import edu.commonwealthu.lastserverstanding.data.firebase.FirebaseManager;
import edu.commonwealthu.lastserverstanding.util.ToastHelper;

/**
 * Stats Fragment - Displays leaderboard ranked by highest wave reached.
 *
 * @author Ethan Wight
 */
public class StatsFragment extends Fragment {

    private static final String TAG = "StatsFragment";
    private static final long LOADING_TIMEOUT_MS = 5000; // 5 seconds timeout

    private RecyclerView leaderboardRecyclerView;
    private CircularProgressIndicator loadingIndicator;
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
        MaterialButton backButton = view.findViewById(R.id.btn_back);

        // Set up RecyclerView with adapter
        adapter = new LeaderboardAdapter();
        leaderboardRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        leaderboardRecyclerView.setAdapter(adapter);

        // Set up back button
        backButton.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

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

        // Set a timeout in case Firebase is slow or unreachable
        final boolean[] responseReceived = {false};
        new android.os.Handler().postDelayed(() -> {
            if (!responseReceived[0] && isAdded()) {
                Log.w(TAG, "Leaderboard loading timeout");
                responseReceived[0] = true;
                showError("Connection timeout");
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
                        leaderboardRecyclerView.setVisibility(View.VISIBLE);
                        adapter.setEntries(entries);
                        Log.d(TAG, "Leaderboard displayed with " + entries.size() + " entries");
                    });
                }

                @Override
                public void onError(String message) {
                    if (getActivity() == null || !isAdded() || responseReceived[0]) return;
                    responseReceived[0] = true;

                    Log.e(TAG, "Failed to load leaderboard: " + message);

                    getActivity().runOnUiThread(() -> {
                        showError("Failed to connect: " + message);
                        if (getView() != null) {
                            ToastHelper.showLong(getView(),
                                    "Could not connect to leaderboard: " + message);
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception loading leaderboard", e);
            responseReceived[0] = true;
            if (isAdded()) {
                showError("Error: " + e.getMessage());
                if (getView() != null) {
                    ToastHelper.showLong(getView(),
                            "Leaderboard error: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Show error message and hide loading indicator
     */
    private void showError(String message) {
        if (!isAdded()) return;

        requireActivity().runOnUiThread(() -> {
            loadingIndicator.setVisibility(View.GONE);
            leaderboardRecyclerView.setVisibility(View.VISIBLE);
            adapter.setEntries(List.of()); // Show empty list
            Log.d(TAG, "Error shown: " + message);
        });
    }
}
