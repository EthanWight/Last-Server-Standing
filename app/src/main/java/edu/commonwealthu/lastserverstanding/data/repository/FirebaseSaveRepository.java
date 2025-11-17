package edu.commonwealthu.lastserverstanding.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

import edu.commonwealthu.lastserverstanding.LastServerStandingApplication;
import edu.commonwealthu.lastserverstanding.data.models.GameState;

/**
 * Repository for saving/loading game state to/from Firebase Firestore
 * Uses anonymous authentication combined with player name for device identification
 */
public class FirebaseSaveRepository {

    private static final String TAG = "FirebaseSaveRepository";
    private static final String COLLECTION_SAVES = "game_saves";
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_PLAYER_NAME = "playerName"; // Changed from deviceId
    private static final String FIELD_GAME_STATE_JSON = "gameStateJson";
    private static final String FIELD_TIMESTAMP = "timestamp";
    private static final String FIELD_IS_AUTO_SAVE = "isAutoSave";
    private static final String FIELD_WAVE = "wave";
    private static final String FIELD_SCORE = "score";

    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private String deviceId; // This now stores the player name

    public FirebaseSaveRepository() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        
        // Get player name from SharedPreferences as device identifier
        try {
            Context context = LastServerStandingApplication.getInstance();
            SharedPreferences prefs = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE);
            deviceId = prefs.getString("player_name", "");
            
            // If no player name set, use device model as fallback
            if (deviceId.isEmpty()) {
                deviceId = android.os.Build.MODEL;
            }
            
            // Sanitize the device ID for Firebase (remove special characters)
            deviceId = deviceId.replaceAll("[^a-zA-Z0-9]", "_");
            
            Log.d(TAG, "Device ID (player name) initialized: " + deviceId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get player name", e);
            deviceId = "unknown_player_" + System.currentTimeMillis();
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
            
            // Sanitize the player name for Firebase
            newPlayerName = newPlayerName.replaceAll("[^a-zA-Z0-9]", "_");
            
            if (!newPlayerName.equals(deviceId)) {
                Log.d(TAG, "Player name updated from " + deviceId + " to " + newPlayerName);
                deviceId = newPlayerName;
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
            String json = gameState.toJson();
            long timestamp = System.currentTimeMillis();

            Map<String, Object> saveData = new HashMap<>();
            saveData.put(FIELD_USER_ID, userId);
            saveData.put(FIELD_PLAYER_NAME, deviceId); // Use player name as fallback
            saveData.put(FIELD_GAME_STATE_JSON, json);
            saveData.put(FIELD_TIMESTAMP, timestamp);
            saveData.put(FIELD_IS_AUTO_SAVE, isAutoSave);
            saveData.put(FIELD_WAVE, gameState.currentWave);
            saveData.put(FIELD_SCORE, gameState.score);

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
     * Save to Firestore with immediate persistence
     */
    private void saveToPrimaryFirestore(Map<String, Object> saveData, SaveCallback callback) {
        firestore.collection(COLLECTION_SAVES)
            .add(saveData)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "Game saved successfully to Firestore: " + documentReference.getId());
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
        ensureAuthenticated(new AuthCallback() {
            @Override
            public void onSuccess(String userId) {
                performLoad(userId, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError("Authentication failed: " + error);
            }
        });
    }

    /**
     * Perform the actual load operation
     */
    private void performLoad(String userId, LoadCallback callback) {
        // First try to load with current user ID
        loadSavesForUser(userId, new LoadCallback() {
            @Override
            public void onSuccess(GameState gameState) {
                callback.onSuccess(gameState);
            }

            @Override
            public void onError(String error) {
                // If no saves found for current user, try player name fallback
                Log.d(TAG, "No saves found for current user, trying player name fallback");
                loadSavesForPlayerName(callback);
            }
        });
    }

    /**
     * Load saves for a specific user ID
     */
    private void loadSavesForUser(String userId, LoadCallback callback) {
        firestore.collection(COLLECTION_SAVES)
            .whereEqualTo(FIELD_USER_ID, userId)
            .whereEqualTo(FIELD_IS_AUTO_SAVE, true)
            .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot != null && !snapshot.isEmpty()) {
                        DocumentSnapshot document = snapshot.getDocuments().get(0);
                        String json = document.getString(FIELD_GAME_STATE_JSON);

                        if (json != null) {
                            try {
                                GameState gameState = GameState.fromJson(json);
                                Log.d(TAG, "Loaded autosave for user " + userId + " - Wave: " + gameState.currentWave);
                                callback.onSuccess(gameState);
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to deserialize game state", e);
                                callback.onError("Failed to parse save data: " + e.getMessage());
                            }
                        } else {
                            callback.onError("Save data is corrupted");
                        }
                    } else {
                        callback.onError("No autosave found for user");
                    }
                } else {
                    String error = task.getException() != null ?
                        task.getException().getMessage() : "Unknown error";
                    Log.e(TAG, "Failed to load autosave", task.getException());
                    callback.onError("Failed to load: " + error);
                }
            });
    }

    /**
     * Load saves for player name (fallback when user authentication changes)
     */
    private void loadSavesForPlayerName(LoadCallback callback) {
        firestore.collection(COLLECTION_SAVES)
            .whereEqualTo(FIELD_PLAYER_NAME, deviceId)
            .whereEqualTo(FIELD_IS_AUTO_SAVE, true)
            .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot != null && !snapshot.isEmpty()) {
                        DocumentSnapshot document = snapshot.getDocuments().get(0);
                        String json = document.getString(FIELD_GAME_STATE_JSON);

                        if (json != null) {
                            try {
                                GameState gameState = GameState.fromJson(json);
                                Log.d(TAG, "Loaded autosave for player " + deviceId + " - Wave: " + gameState.currentWave);
                                callback.onSuccess(gameState);
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to deserialize game state", e);
                                callback.onError("Failed to parse save data: " + e.getMessage());
                            }
                        } else {
                            callback.onError("Save data is corrupted");
                        }
                    } else {
                        Log.d(TAG, "No autosave found for player name either");
                        callback.onError("No autosave found");
                    }
                } else {
                    String error = task.getException() != null ?
                        task.getException().getMessage() : "Unknown error";
                    Log.e(TAG, "Failed to load autosave from player name fallback", task.getException());
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
