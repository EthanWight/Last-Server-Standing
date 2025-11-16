package edu.commonwealthu.lastserverstanding.data.firebase;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import java.util.ArrayList;
import java.util.List;

/**
 * Manager for Firebase Realtime Database operations
 * Optimized to prevent ANR and crashes
 */
public class FirebaseManager {

    private static final String TAG = "FirebaseManager";
    private static FirebaseManager instance;
    private DatabaseReference leaderboardRef;
    private boolean initialized;

    private FirebaseManager() {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();

            // Enable persistence to allow offline writes that sync when online
            // This is safe because we're not blocking the main thread
            try {
                database.setPersistenceEnabled(true);
                Log.d(TAG, "Firebase persistence enabled for offline support");
            } catch (DatabaseException e) {
                // Persistence can only be enabled once per app session
                // If it's already enabled, this will throw an exception - that's fine
                Log.d(TAG, "Firebase persistence already configured");
            }

            DatabaseReference databaseRef = database.getReference();
            leaderboardRef = databaseRef.child("leaderboard");

            // Enable keepSynced to prioritize loading leaderboard data
            // But keep it lightweight to prevent ANR
            leaderboardRef.keepSynced(true);

            initialized = true;
            Log.d(TAG, "Firebase initialized successfully - leaderboard ready");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase: " + e.getMessage(), e);
            initialized = false;
        }
    }

    public static FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    /**
     * Submit highest wave to leaderboard
     */
    public void submitHighScore(String playerName, int wave, LeaderboardCallback callback) {
        if (!initialized || leaderboardRef == null) {
            Log.w(TAG, "Firebase not initialized, skipping score submission");
            if (callback != null) {
                callback.onError("Firebase not initialized");
            }
            return;
        }

        Log.d(TAG, "Submitting score to leaderboard: Player=" + playerName + ", Wave=" + wave);

        try {
            String entryId = leaderboardRef.push().getKey();
            if (entryId == null) {
                Log.e(TAG, "Failed to generate entry ID for score submission");
                if (callback != null) {
                    callback.onError("Failed to generate entry ID");
                }
                return;
            }

            LeaderboardEntry entry = new LeaderboardEntry(playerName, wave, System.currentTimeMillis());

            Log.d(TAG, "Writing score to Firebase with ID: " + entryId);
            leaderboardRef.child(entryId).setValue(entry)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ Score successfully written to Firebase: " + playerName + " - Wave " + wave);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Failed to write score to Firebase: " + e.getMessage(), e);
                    Log.e(TAG, "  Error details - Code: " + e.getClass().getSimpleName());
                    Log.e(TAG, "  Make sure Firebase Database Rules allow writes to /leaderboard/");
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Exception submitting score: " + e.getMessage(), e);
            if (callback != null) {
                callback.onError(e.getMessage());
            }
        }
    }

    /**
     * Populate Firebase with sample test data
     * This is for testing/debugging purposes
     */
    public void populateTestData(LeaderboardCallback callback) {
        // Create sample entries with different timestamps
        long now = System.currentTimeMillis();
        LeaderboardEntry[] testEntries = {
            new LeaderboardEntry("CyberDefender", 25, now - 86400000),      // 1 day ago
            new LeaderboardEntry("FirewallPro", 20, now - 172800000),       // 2 days ago
            new LeaderboardEntry("HackerHunter", 18, now - 259200000),      // 3 days ago
            new LeaderboardEntry("SecurityExpert", 15, now - 345600000),    // 4 days ago
            new LeaderboardEntry("TowerMaster", 12, now - 432000000),       // 5 days ago
            new LeaderboardEntry("CodeWarrior", 10, now - 518400000),       // 6 days ago
            new LeaderboardEntry("PixelDefender", 8, now - 604800000),      // 7 days ago
            new LeaderboardEntry("GameHero", 7, now - 691200000),           // 8 days ago
            new LeaderboardEntry("WaveRider", 5, now - 777600000),          // 9 days ago
            new LeaderboardEntry("Beginner", 3, now - 864000000)            // 10 days ago
        };

        // Submit all test entries
        final int[] successCount = {0};
        final int[] errorCount = {0};

        for (LeaderboardEntry entry : testEntries) {
            String entryId = leaderboardRef.push().getKey();
            if (entryId != null) {
                leaderboardRef.child(entryId).setValue(entry)
                    .addOnSuccessListener(aVoid -> {
                        successCount[0]++;
                        if (successCount[0] + errorCount[0] == testEntries.length) {
                            if (errorCount[0] == 0) {
                                callback.onSuccess();
                            } else {
                                callback.onError("Some entries failed: " + errorCount[0] + " errors");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        errorCount[0]++;
                        if (successCount[0] + errorCount[0] == testEntries.length) {
                            callback.onError("Failed to populate test data: " + errorCount[0] + " errors");
                        }
                    });
            } else {
                errorCount[0]++;
            }
        }
    }

    /**
     * Clear all leaderboard data (for testing)
     */
    public void clearLeaderboard(LeaderboardCallback callback) {
        leaderboardRef.removeValue()
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Get top players by highest wave reached
     */
    public void getTopScores(int limit, LeaderboardFetchCallback callback) {
        if (!initialized || leaderboardRef == null) {
            Log.w(TAG, "Firebase not initialized, returning empty leaderboard");
            if (callback != null) {
                callback.onSuccess(new ArrayList<>());
            }
            return;
        }

        try {
            leaderboardRef.orderByChild("wave").limitToLast(limit)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<LeaderboardEntry> entries = new ArrayList<>();
                        try {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                LeaderboardEntry entry = snapshot.getValue(LeaderboardEntry.class);
                                if (entry != null) {
                                    entries.add(entry);
                                }
                            }
                            // Sort by wave descending (Firebase sorts ascending)
                            entries.sort((a, b) -> Integer.compare(b.wave, a.wave));
                            if (callback != null) {
                                callback.onSuccess(entries);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing leaderboard data: " + e.getMessage());
                            if (callback != null) {
                                callback.onError(e.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Leaderboard query cancelled: " + databaseError.getMessage());
                        if (callback != null) {
                            callback.onError(databaseError.getMessage());
                        }
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Exception fetching leaderboard: " + e.getMessage());
            if (callback != null) {
                callback.onError(e.getMessage());
            }
        }
    }

    /**
     * Leaderboard entry model
     */
    public static class LeaderboardEntry {
        public String playerName;
        public int wave;
        public long timestamp;

        public LeaderboardEntry(String playerName, int wave, long timestamp) {
            this.playerName = playerName;
            this.wave = wave;
            this.timestamp = timestamp;
        }
    }

    // Callback interfaces
    public interface LeaderboardCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface LeaderboardFetchCallback {
        void onSuccess(List<LeaderboardEntry> entries);
        void onError(String message);
    }
}
