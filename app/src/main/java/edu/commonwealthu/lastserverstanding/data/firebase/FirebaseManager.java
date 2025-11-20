package edu.commonwealthu.lastserverstanding.data.firebase;

import android.util.Log;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manager for Firebase Firestore leaderboard operations.
 * Optimized to prevent ANR and crashes.
 *
 * @author Ethan Wight
 */
public class FirebaseManager {

    private static final String TAG = "FirebaseManager";
    private static final String COLLECTION_LEADERBOARD = "leaderboard";
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_PLAYER_NAME = "playerName";
    private static final String FIELD_WAVE = "wave";
    private static final String FIELD_TIMESTAMP = "timestamp";

    private static FirebaseManager instance;
    private FirebaseFirestore firestore;
    private boolean initialized;

    private FirebaseManager() {
        try {
            firestore = FirebaseFirestore.getInstance();
            initialized = true;
            Log.d(TAG, "Firebase Firestore initialized successfully - leaderboard ready");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase: " + e.getMessage(), e);
            initialized = false;
        }
    }

    /**
     * Get the singleton instance.
     *
     * @return The FirebaseManager instance.
     */
    public static FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    /**
     * Submit highest wave to leaderboard.
     * Only updates if this wave is higher than the player's current best.
     * Uses userId as the document ID to prevent duplicates when player changes name.
     *
     * @param userId The user ID.
     * @param playerName The player name.
     * @param wave The wave to submit.
     * @param callback Callback for the operation.
     */
    public void submitHighScore(String userId, String playerName, int wave,
                                LeaderboardCallback callback) {
        if (!initialized || firestore == null) {
            Log.w(TAG, "Firebase not initialized, skipping score submission");
            if (callback != null) {
                callback.onError("Firebase not initialized");
            }
            return;
        }

        Log.d(TAG, "Submitting score to leaderboard: UserId=" + userId + ", Player=" + playerName + ", Wave=" + wave);

        try {
            // Use userId as the document ID (no need to sanitize - it's already a valid ID)
            // This ensures one entry per device, even if player changes their name

            // Check if player already has a score
            firestore.collection(COLLECTION_LEADERBOARD)
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    LeaderboardEntry existingEntry = snapshot.exists() ?
                        snapshot.toObject(LeaderboardEntry.class) : null;

                    // Only update if new wave is higher than existing, or no entry exists
                    if (existingEntry == null || wave > existingEntry.wave) {
                        Map<String, Object> entryData = new HashMap<>();
                        entryData.put(FIELD_USER_ID, userId);
                        entryData.put(FIELD_PLAYER_NAME, playerName);
                        entryData.put(FIELD_WAVE, wave);
                        entryData.put(FIELD_TIMESTAMP, System.currentTimeMillis());

                        Log.d(TAG, "Writing score to Firestore for userId: " + userId
                                + (existingEntry != null ? " (updating from wave "
                                + existingEntry.wave + " as '" + existingEntry.playerName + "')"
                                : " (new entry)"));

                        firestore.collection(COLLECTION_LEADERBOARD)
                            .document(userId)
                            .set(entryData)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "✓ Score successfully written to Firestore: " + playerName + " - Wave " + wave);
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "✗ Failed to write score to Firestore: " + e.getMessage(), e);
                                if (callback != null) {
                                    callback.onError(e.getMessage());
                                }
                            });
                    } else {
                        Log.d(TAG, "Not updating - existing wave " + existingEntry.wave + " is higher than " + wave);
                        if (callback != null) {
                            callback.onSuccess(); // Still success, just didn't update
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check existing score: " + e.getMessage(), e);
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
     * Get top players by highest wave reached.
     * Shows only each player's highest wave (one entry per player).
     *
     * @param limit The maximum number of entries.
     * @param callback Callback for the operation.
     */
    public void getTopScores(int limit, LeaderboardFetchCallback callback) {
        if (!initialized || firestore == null) {
            Log.w(TAG, "Firebase not initialized, returning empty leaderboard");
            if (callback != null) {
                callback.onSuccess(new ArrayList<>());
            }
            return;
        }

        Log.d(TAG, "Fetching top " + limit + " scores from leaderboard");

        try {
            // Fetch top scores ordered by wave descending
            firestore.collection(COLLECTION_LEADERBOARD)
                .orderBy(FIELD_WAVE, Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<LeaderboardEntry> entries = new ArrayList<>();
                    try {
                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            LeaderboardEntry entry = document.toObject(LeaderboardEntry.class);
                            if (entry != null) {
                                entries.add(entry);
                                Log.d(TAG, "  Entry: " + entry.playerName + " - Wave " + entry.wave);
                            }
                        }

                        // Already sorted by wave descending from query
                        // Add secondary sort by timestamp if needed
                        entries.sort((a, b) -> {
                            // Primary sort: wave (descending)
                            int waveCompare = Integer.compare(b.wave, a.wave);
                            if (waveCompare != 0) return waveCompare;

                            // Secondary sort: timestamp (most recent first)
                            return Long.compare(b.timestamp, a.timestamp);
                        });

                        Log.d(TAG, "✓ Fetched " + entries.size() + " leaderboard entries, sorted by highest wave");

                        if (callback != null) {
                            callback.onSuccess(entries);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing leaderboard data: " + e.getMessage(), e);
                        if (callback != null) {
                            callback.onError(e.getMessage());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Leaderboard query failed: " + e.getMessage(), e);
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Exception fetching leaderboard: " + e.getMessage(), e);
            if (callback != null) {
                callback.onError(e.getMessage());
            }
        }
    }

    /**
     * Leaderboard entry model.
     *
     * @author Ethan Wight
     */
    public static class LeaderboardEntry {
        public String userId;       // Firebase anonymous auth user ID (used as document ID)
        public String playerName;   // Display name for the player
        public int wave;
        public long timestamp;

        /**
         * Default constructor required for Firebase deserialization.
         */
        @SuppressWarnings("unused")
        public LeaderboardEntry() {
        }

        /**
         * Constructor with all fields.
         *
         * @param userId The user ID.
         * @param playerName The player name.
         * @param wave The wave number.
         * @param timestamp The timestamp of the entry.
         */
        @SuppressWarnings("unused")
        public LeaderboardEntry(String userId, String playerName, int wave, long timestamp) {
            this.userId = userId;
            this.playerName = playerName;
            this.wave = wave;
            this.timestamp = timestamp;
        }
    }

    /**
     * Callback for leaderboard submission operations.
     */
    public interface LeaderboardCallback {
        /**
         * Called on successful operation.
         */
        void onSuccess();

        /**
         * Called on error.
         *
         * @param message The error message.
         */
        void onError(String message);
    }

    /**
     * Callback for leaderboard fetch operations.
     */
    public interface LeaderboardFetchCallback {
        /**
         * Called on successful fetch.
         *
         * @param entries The fetched leaderboard entries.
         */
        void onSuccess(List<LeaderboardEntry> entries);

        /**
         * Called on error.
         *
         * @param message The error message.
         */
        void onError(String message);
    }
}
