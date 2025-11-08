package edu.commonwealthu.lastserverstanding.data.firebase;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import java.util.ArrayList;
import java.util.List;

/**
 * Manager for Firebase Realtime Database operations
 */
public class FirebaseManager {

    private static FirebaseManager instance;
    private DatabaseReference databaseRef;
    private DatabaseReference leaderboardRef;

    private FirebaseManager() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseRef = database.getReference();
        leaderboardRef = databaseRef.child("leaderboard");
    }

    public static FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    /**
     * Submit score to leaderboard
     */
    public void submitScore(String playerName, long score, int wave, LeaderboardCallback callback) {
        String entryId = leaderboardRef.push().getKey();
        if (entryId == null) {
            callback.onError("Failed to generate entry ID");
            return;
        }

        LeaderboardEntry entry = new LeaderboardEntry(playerName, score, wave, System.currentTimeMillis());

        leaderboardRef.child(entryId).setValue(entry)
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Get top scores from leaderboard
     */
    public void getTopScores(int limit, LeaderboardFetchCallback callback) {
        leaderboardRef.orderByChild("score").limitToLast(limit)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    List<LeaderboardEntry> entries = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        LeaderboardEntry entry = snapshot.getValue(LeaderboardEntry.class);
                        if (entry != null) {
                            entries.add(entry);
                        }
                    }
                    // Sort by score descending (Firebase sorts ascending)
                    entries.sort((a, b) -> Long.compare(b.score, a.score));
                    callback.onSuccess(entries);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    callback.onError(databaseError.getMessage());
                }
            });
    }

    /**
     * Leaderboard entry model
     */
    public static class LeaderboardEntry {
        public String playerName;
        public long score;
        public int wave;
        public long timestamp;

        public LeaderboardEntry() {
            // Required for Firebase
        }

        public LeaderboardEntry(String playerName, long score, int wave, long timestamp) {
            this.playerName = playerName;
            this.score = score;
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
