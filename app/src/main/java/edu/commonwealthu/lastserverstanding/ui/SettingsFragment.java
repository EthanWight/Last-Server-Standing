package edu.commonwealthu.lastserverstanding.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

import edu.commonwealthu.lastserverstanding.R;
import edu.commonwealthu.lastserverstanding.data.entities.SettingsEntity;
import edu.commonwealthu.lastserverstanding.viewmodel.GameViewModel;

/**
 * Settings Fragment - User preferences and configuration
 */
public class SettingsFragment extends Fragment {

    private GameViewModel viewModel;

    // UI Elements
    private SwitchMaterial soundSwitch;
    private SwitchMaterial vibrationSwitch;
    private Slider sensitivitySlider;
    private MaterialButton saveButton;
    private MaterialButton backButton;

    private SettingsEntity currentSettings;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(GameViewModel.class);

        // Initialize views
        soundSwitch = view.findViewById(R.id.switch_sound);
        vibrationSwitch = view.findViewById(R.id.switch_vibration);
        sensitivitySlider = view.findViewById(R.id.slider_sensitivity);
        saveButton = view.findViewById(R.id.btn_save);
        backButton = view.findViewById(R.id.btn_back);

        // Load current settings
        loadSettings();

        // Set up listeners
        saveButton.setOnClickListener(v -> saveSettings());
        backButton.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        // Set up slider
        sensitivitySlider.setLabelFormatter(value -> {
            if (value < 2.0f) return "Very Sensitive";
            else if (value < 3.0f) return "Sensitive";
            else if (value < 4.0f) return "Normal";
            else return "Less Sensitive";
        });
    }

    /**
     * Load settings from ViewModel
     */
    private void loadSettings() {
        viewModel.getSettings().observe(getViewLifecycleOwner(), settings -> {
            if (settings != null) {
                currentSettings = settings;
                soundSwitch.setChecked(settings.isSoundEnabled());
                vibrationSwitch.setChecked(settings.isVibrationEnabled());
                sensitivitySlider.setValue(settings.getAccelerometerSensitivity());
            } else {
                // Use defaults if no settings exist
                currentSettings = new SettingsEntity();
                soundSwitch.setChecked(true);
                vibrationSwitch.setChecked(true);
                sensitivitySlider.setValue(2.5f);
            }
        });
    }

    /**
     * Save settings to database
     */
    private void saveSettings() {
        if (currentSettings == null) {
            currentSettings = new SettingsEntity();
        }

        // Update settings from UI
        currentSettings.setSoundEnabled(soundSwitch.isChecked());
        currentSettings.setVibrationEnabled(vibrationSwitch.isChecked());
        currentSettings.setAccelerometerSensitivity(sensitivitySlider.getValue());

        // Save via ViewModel
        viewModel.updateSettings(currentSettings);

        // Show confirmation
        Toast.makeText(requireContext(), "Settings saved", Toast.LENGTH_SHORT).show();

        // Navigate back
        Navigation.findNavController(requireView()).navigateUp();
    }
}
