package edu.commonwealthu.lastserverstanding.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

import edu.commonwealthu.lastserverstanding.LastServerStandingApplication;
import edu.commonwealthu.lastserverstanding.data.firebase.FirebaseManager;
import edu.commonwealthu.lastserverstanding.data.models.GameState;

/**
 * Repository for saving/loading game state to/from Firebase Firestore
 * Uses anonymous authentication combined with player name for device identification
 */
public class FirebaseSaveRepository {

    private static final String TAG = "FirebaseSaveRepository";
    private static final String COLLECTION_SAVES = "game_saves";
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_PLAYER_NAME = "playerName"; // Sanitized for queries
    private static final String FIELD_DISPLAY_NAME = "displayName"; // Original name with punctuation
    private static final String FIELD_GAME_STATE_JSON = "gameStateJson";
    private static final String FIELD_TIMESTAMP = "timestamp";
    private static final String FIELD_IS_AUTO_SAVE = "isAutoSave";
    private static final String FIELD_WAVE = "wave";
    private static final String FIELD_SCORE = "score";

    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private String deviceId; // Sanitized version for queries/document IDs
    private String originalPlayerName; // Original name with punctuation preserved

    public FirebaseSaveRepository() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Get player name from SharedPreferences as device identifier
        try {
            Context context = LastServerStandingApplication.getInstance();
            SharedPreferences prefs = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE);
            originalPlayerName = prefs.getString("player_name", "");

            // If no player name set, use device model as fallback
            if (originalPlayerName.isEmpty()) {
                originalPlayerName = android.os.Build.MODEL;
            }

            // Create sanitized version for document IDs (only remove problematic chars)
            // Keep alphanumeric, spaces, and most punctuation - only remove Firestore-invalid chars
            deviceId = originalPlayerName.replaceAll("/", "_");

            Log.d(TAG, "Player name initialized: '" + originalPlayerName + "' (sanitized: '" + deviceId + "')");
        } catch (Exception e) {
            Log.e(TAG, "Failed to get player name", e);
            originalPlayerName = "unknown_player_" + System.currentTimeMillis();
            deviceId = originalPlayerName;
        }

        // Note: Offline persistence is enabled by default in Firestore
        // No need to explicitly configure it
    }

    /**
     * Refresh the device ID (player name) from SharedPreferences
     * Call this when the player changes their name in settings
     */
    public void refreshPlayerName() {
        try {
            Context context = LastServerStandingApplication.getInstance();
            SharedPreferences prefs = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE);
            String newPlayerName = prefs.getString("player_name", "");

            // If no player name set, use device model as fallback
            if (newPlayerName.isEmpty()) {
                newPlayerName = android.os.Build.MODEL;
            }

            if (!newPlayerName.equals(originalPlayerName)) {
                Log.d(TAG, "Player name updated from '" + originalPlayerName + "' to '" + newPlayerName + "'");
                originalPlayerName = newPlayerName;
                // Create sanitized version for document IDs
                deviceId = originalPlayerName.replaceAll("/", "_");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to refresh player name", e);
        }
    }

    /**
     * Ensure user is authenticated (anonymously)
     */
    public void ensureAuthenticated(AuthCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            Log.d(TAG, "User already authenticated: " + currentUser.getUid());
            callback.onSuccess(currentUser.getUid());
            return;
        }

        // Sign in anonymously
        Log.d(TAG, "Signing in anonymously...");
        auth.signInAnonymously()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null) {
                        Log.d(TAG, "Anonymous sign-in successful: " + user.getUid());
                        callback.onSuccess(user.getUid());
                    } else {
                        callback.onError("Authentication succeeded but user is null");
                    }
                } else {
                    String error = task.getException() != null ?
                        task.getException().getMessage() : "Unknown error";
                    Log.e(TAG, "Anonymous sign-in failed: " + error);
                    callback.onError("Authentication failed: " + error);
                }
            });
    }

    /**
     * Save game state to Firestore
     */
    public void saveGame(GameState gameState, boolean isAutoSave, SaveCallback callback) {
        ensureAuthenticated(new AuthCallback() {
            @Override
            public void onSuccess(String userId) {
                performSave(userId, gameState, isAutoSave, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError("Authentication failed: " + error);
            }
        });
    }

    /**
     * Perform the actual save operation
     */
    private void performSave(String userId, GameState gameState, boolean isAutoSave, SaveCallback callback) {
        try {
            Map<String, Object> saveData = createSaveData(userId, gameState, isAutoSave);

            if (isAutoSave) {
                // For autosaves, first delete old autosaves, then save new one
                deleteOldAutoSaves(userId, new DeleteCallback() {
                    @Override
                    public void onSuccess() {
                        // Now save the new autosave
                        saveToPrimaryFirestore(saveData, callback);
                    }

                    @Override
                    public void onError(String error) {
                        // Even if deletion fails, still try to save
                        Log.w(TAG, "Failed to delete old autosaves, proceeding with save: " + error);
                        saveToPrimaryFirestore(saveData, callback);
                    }
                });
            } else {
                // For manual saves, just save directly
                saveToPrimaryFirestore(saveData, callback);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error preparing save data", e);
            callback.onError("Failed to serialize game state: " + e.getMessage());
        }
    }

    /**
     * Create save data map from game state
     */
    private Map<String, Object> createSaveData(String userId, GameState gameState, boolean isAutoSave) {
        String json = gameState.toJson();
        long timestamp = System.currentTimeMillis();

        Map<String, Object> saveData = new HashMap<>();
        saveData.put(FIELD_USER_ID, userId);
        saveData.put(FIELD_PLAYER_NAME, deviceId); // Sanitized for queries
        saveData.put(FIELD_DISPLAY_NAME, originalPlayerName); // Original name with punctuation
        saveData.put(FIELD_GAME_STATE_JSON, json);
        saveData.put(FIELD_TIMESTAMP, timestamp);
        saveData.put(FIELD_IS_AUTO_SAVE, isAutoSave);
        saveData.put(FIELD_WAVE, gameState.currentWave);
        saveData.put(FIELD_SCORE, gameState.score);

        return saveData;
    }

    /**
     * Save to Firestore with immediate persistence
     */
    private void saveToPrimaryFirestore(Map<String, Object> saveData, SaveCallback callback) {
        firestore.collection(COLLECTION_SAVES)
            .add(saveData)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "Game saved successfully to Firestore: " + documentReference.getId());

                // Submit high score to leaderboard (only updates if wave is higher)
                Object userIdObj = saveData.get(FIELD_USER_ID);
                Object waveObj = saveData.get(FIELD_WAVE);
                Object displayNameObj = saveData.get(FIELD_DISPLAY_NAME); // Use display name with punctuation

                if (userIdObj instanceof String userId && waveObj instanceof Integer wave && displayNameObj instanceof String displayName) {
                    Log.d(TAG, "Submitting wave " + wave + " to leaderboard for userId: " + userId + ", player: " + displayName);

                    FirebaseManager.getInstance().submitHighScore(userId, displayName, wave, new FirebaseManager.LeaderboardCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Leaderboard updated successfully");
                        }

                        @Override
                        public void onError(String message) {
                            // Don't fail the save if leaderboard update fails
                            Log.w(TAG, "Failed to update leaderboard: " + message);
                        }
                    });
                }

                callback.onSuccess(documentReference.getId());
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to save game to Firestore", e);
                callback.onError("Failed to save: " + e.getMessage());
            });
    }

    /**
     * Load most recent autosave for current user
     */
    public void loadLatestAutoSave(LoadCallback callback) {
        Log.d(TAG, "=== loadLatestAutoSave called ===");
        Log.d(TAG, "Current deviceId (playerName): " + deviceId);

        ensureAuthenticated(new AuthCallback() {
            @Override
            public void onSuccess(String userId) {
                Log.d(TAG, "Authentication successful, userId: " + userId);
                performLoad(userId, callback);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Authentication failed: " + error);
                callback.onError("Authentication failed: " + error);
            }
        });
    }

    /**
     * Perform the actual load operation
     */
    private void performLoad(String userId, LoadCallback callback) {
        Log.d(TAG, "performLoad: Starting load sequence");
        Log.d(TAG, "performLoad: Will try playerName first, then userId");

        // First try to load by player name (what users identify with)
        loadSavesForPlayerName(new LoadCallback() {
            @Override
            public void onSuccess(GameState gameState) {
                Log.d(TAG, "performLoad: SUCCESS via playerName");
                callback.onSuccess(gameState);
            }

            @Override
            public void onError(String error) {
                // If no saves found for player name, try userId fallback
                Log.d(TAG, "performLoad: playerName failed, trying userId fallback");
                Log.d(TAG, "performLoad: playerName error was: " + error);
                loadSavesForUser(userId, callback);
            }
        });
    }

    /**
     * Load saves for a specific user ID (fallback method)
     */
    private void loadSavesForUser(String userId, LoadCallback callback) {
        Log.d(TAG, "loadSavesForUser: Querying for userId = '" + userId + "'");

        // Note: orderBy removed to avoid composite index requirement
        // We'll get all docs for this userId and sort in-app
        firestore.collection(COLLECTION_SAVES)
            .whereEqualTo(FIELD_USER_ID, userId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    QuerySnapshot snapshot = task.getResult();
                    Log.d(TAG, "loadSavesForUser: Query successful");
                    Log.d(TAG, "loadSavesForUser: Found " + (snapshot != null ? snapshot.size() : 0) + " document(s)");

                    if (snapshot != null && !snapshot.isEmpty()) {
                        // Sort documents by timestamp in descending order (newest first)
                        java.util.List<DocumentSnapshot> documents = snapshot.getDocuments();
                        documents.sort((a, b) -> {
                            Long timeA = a.getLong(FIELD_TIMESTAMP);
                            Long timeB = b.getLong(FIELD_TIMESTAMP);
                            if (timeA == null) timeA = 0L;
                            if (timeB == null) timeB = 0L;
                            return timeB.compareTo(timeA); // DESC order
                        });

                        // Get the most recent document
                        DocumentSnapshot document = documents.get(0);
                        String json = document.getString(FIELD_GAME_STATE_JSON);

                        if (json != null) {
                            try {
                                GameState gameState = GameState.fromJson(json);
                                Log.d(TAG, "loadSavesForUser: ✓ SUCCESS - Wave: " + gameState.currentWave);
                                callback.onSuccess(gameState);
                            } catch (Exception e) {
                                Log.e(TAG, "loadSavesForUser: Failed to deserialize", e);
                                callback.onError("Failed to parse save data: " + e.getMessage());
                            }
                        } else {
                            Log.e(TAG, "loadSavesForUser: JSON field is null");
                            callback.onError("Save data is corrupted");
                        }
                    } else {
                        Log.d(TAG, "loadSavesForUser: ✗ No documents found");
                        callback.onError("No autosave found for user");
                    }
                } else {
                    String error = task.getException() != null ?
                        task.getException().getMessage() : "Unknown error";
                    Log.e(TAG, "loadSavesForUser: Query FAILED: " + error, task.getException());
                    callback.onError("Failed to load: " + error);
                }
            });
    }

    /**
     * Load saves for player name (primary method - players identify by name)
     */
    private void loadSavesForPlayerName(LoadCallback callback) {
        Log.d(TAG, "loadSavesForPlayerName: Querying for playerName = '" + deviceId + "'");
        Log.d(TAG, "loadSavesForPlayerName: Collection = " + COLLECTION_SAVES);

        // Note: orderBy removed to avoid composite index requirement
        // We'll get all docs for this playerName and sort in-app
        firestore.collection(COLLECTION_SAVES)
            .whereEqualTo(FIELD_PLAYER_NAME, deviceId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    QuerySnapshot snapshot = task.getResult();
                    Log.d(TAG, "loadSavesForPlayerName: Query successful");
                    Log.d(TAG, "loadSavesForPlayerName: Found " + (snapshot != null ? snapshot.size() : 0) + " document(s)");

                    if (snapshot != null && !snapshot.isEmpty()) {
                        // Sort documents by timestamp in descending order (newest first)
                        java.util.List<DocumentSnapshot> documents = snapshot.getDocuments();

                        // Log all documents before sorting
                        Log.d(TAG, "loadSavesForPlayerName: === BEFORE SORTING ===");
                        for (int i = 0; i < documents.size(); i++) {
                            DocumentSnapshot doc = documents.get(i);
                            Long ts = doc.getLong(FIELD_TIMESTAMP);
                            Long wave = doc.getLong("wave");
                            Log.d(TAG, "  [" + i + "] ts=" + ts + " wave=" + wave + " date=" + new java.util.Date(ts != null ? ts : 0));
                        }

                        documents.sort((a, b) -> {
                            Long timeA = a.getLong(FIELD_TIMESTAMP);
                            Long timeB = b.getLong(FIELD_TIMESTAMP);
                            if (timeA == null) timeA = 0L;
                            if (timeB == null) timeB = 0L;
                            return timeB.compareTo(timeA); // DESC order (newest first)
                        });

                        // Log after sorting
                        Log.d(TAG, "loadSavesForPlayerName: === AFTER SORTING ===");
                        for (int i = 0; i < documents.size(); i++) {
                            DocumentSnapshot doc = documents.get(i);
                            Long ts = doc.getLong(FIELD_TIMESTAMP);
                            Long wave = doc.getLong("wave");
                            Log.d(TAG, "  [" + i + "] ts=" + ts + " wave=" + wave + " date=" + new java.util.Date(ts != null ? ts : 0));
                        }

                        // Get the most recent document (should be first after sorting DESC)
                        DocumentSnapshot document = documents.get(0);
                        Log.d(TAG, "loadSavesForPlayerName: >>> SELECTED document[0] with ts=" + document.getLong(FIELD_TIMESTAMP));
                        String json = document.getString(FIELD_GAME_STATE_JSON);

                        if (json != null) {
                            try {
                                GameState gameState = GameState.fromJson(json);
                                Log.d(TAG, "loadSavesForPlayerName: ✓ SUCCESS - Wave: " + gameState.currentWave);
                                callback.onSuccess(gameState);
                            } catch (Exception e) {
                                Log.e(TAG, "loadSavesForPlayerName: Failed to deserialize", e);
                                callback.onError("Failed to parse save data: " + e.getMessage());
                            }
                        } else {
                            Log.e(TAG, "loadSavesForPlayerName: JSON field is null");
                            callback.onError("Save data is corrupted");
                        }
                    } else {
                        Log.d(TAG, "loadSavesForPlayerName: ✗ No documents found");
                        callback.onError("No autosave found");
                    }
                } else {
                    String error = task.getException() != null ?
                        task.getException().getMessage() : "Unknown error";
                    Log.e(TAG, "loadSavesForPlayerName: Query FAILED: " + error, task.getException());
                    callback.onError("Failed to load: " + error);
                }
            });
    }

    /**
     * Delete old autosaves for current user (keep only the most recent)
     */
    private void deleteOldAutoSaves(String userId, DeleteCallback callback) {
        firestore.collection(COLLECTION_SAVES)
            .whereEqualTo(FIELD_USER_ID, userId)
            .whereEqualTo(FIELD_IS_AUTO_SAVE, true)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot != null && !snapshot.isEmpty()) {
                        // Delete all existing autosaves
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            document.getReference().delete()
                                .addOnSuccessListener(aVoid ->
                                    Log.d(TAG, "Deleted old autosave: " + document.getId()))
                                .addOnFailureListener(e ->
                                    Log.w(TAG, "Failed to delete old autosave: " + document.getId(), e));
                        }
                    }
                    // Also clean up old player name saves to prevent orphaned data
                    cleanupOldPlayerSaves(callback);
                } else {
                    String error = task.getException() != null ?
                        task.getException().getMessage() : "Unknown error";
                    callback.onError(error);
                }
            });
    }

    /**
     * Clean up old player name saves (orphaned data from previous auth sessions)
     */
    private void cleanupOldPlayerSaves(DeleteCallback callback) {
        firestore.collection(COLLECTION_SAVES)
            .whereEqualTo(FIELD_PLAYER_NAME, deviceId)
            .whereEqualTo(FIELD_IS_AUTO_SAVE, true)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot != null && !snapshot.isEmpty()) {
                        // Delete orphaned player saves
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            document.getReference().delete()
                                .addOnSuccessListener(aVoid ->
                                    Log.d(TAG, "Deleted orphaned player save: " + document.getId()))
                                .addOnFailureListener(e ->
                                    Log.w(TAG, "Failed to delete orphaned player save: " + document.getId(), e));
                        }
                    }
                }
                // Always call success, as cleanup is not critical
                callback.onSuccess();
            });
    }

    /**
     * Delete autosave for current user
     */
    public void deleteAutoSave(DeleteCallback callback) {
        ensureAuthenticated(new AuthCallback() {
            @Override
            public void onSuccess(String userId) {
                deleteOldAutoSaves(userId, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError("Authentication failed: " + error);
            }
        });
    }

    // Callback interfaces
    public interface AuthCallback {
        void onSuccess(String userId);
        void onError(String error);
    }

    public interface SaveCallback {
        void onSuccess(String saveId);
        void onError(String error);
    }

    public interface LoadCallback {
        void onSuccess(GameState gameState);
        void onError(String error);
    }

    public interface DeleteCallback {
        void onSuccess();
        void onError(String error);
    }
}
