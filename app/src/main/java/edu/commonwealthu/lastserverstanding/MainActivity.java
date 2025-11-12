package edu.commonwealthu.lastserverstanding;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

/**
 * Main Activity for Last Server Standing
 * Uses Navigation Component for fragment-based architecture
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "=== Last Server Standing ===");

        // Use the fragment-based layout
        setContentView(R.layout.activity_main_updated);

        // Set up Navigation Component
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            Log.d(TAG, "Navigation initialized - starting at Main Menu");
        } else {
            Log.e(TAG, "NavHostFragment not found!");
        }

        Log.d(TAG, "MainActivity created");
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MainActivity destroyed");
    }
}
