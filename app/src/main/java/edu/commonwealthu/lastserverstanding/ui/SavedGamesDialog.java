package edu.commonwealthu.lastserverstanding.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import edu.commonwealthu.lastserverstanding.R;
import edu.commonwealthu.lastserverstanding.data.entities.SaveGameEntity;
import edu.commonwealthu.lastserverstanding.data.repository.GameRepository;
import edu.commonwealthu.lastserverstanding.viewmodel.GameViewModel;

/**
 * Dialog that displays a list of saved games and allows loading/deleting them
 */
public class SavedGamesDialog extends DialogFragment implements SavedGameAdapter.OnSaveGameClickListener {

    private GameViewModel viewModel;
    private SavedGameAdapter adapter;
    private RecyclerView recyclerView;
    private TextView emptyStateText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_saved_games, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(GameViewModel.class);

        // Initialize views
        recyclerView = view.findViewById(R.id.saved_games_list);
        emptyStateText = view.findViewById(R.id.empty_state);
        MaterialButton closeButton = view.findViewById(R.id.btn_close);

        // Set up RecyclerView
        adapter = new SavedGameAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // Observe saved games
        viewModel.getAllSaves().observe(getViewLifecycleOwner(), this::updateSavedGamesList);

        // Close button
        closeButton.setOnClickListener(v -> dismiss());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Create a dialog with rounded corners
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Set dialog width to 90% of screen width
        if (getDialog() != null && getDialog().getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void updateSavedGamesList(List<SaveGameEntity> saves) {
        if (saves == null || saves.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
            adapter.setSavedGames(saves);
        }
    }

    @Override
    public void onLoadSave(SaveGameEntity save) {
        // Dismiss dialog
        dismiss();

        // Navigate to game with the save ID
        Bundle args = new Bundle();
        args.putBoolean("continue_game", true);
        args.putInt("save_id", save.getId());

        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                .navigate(R.id.action_menu_to_game, args);
    }

    @Override
    public void onDeleteSave(SaveGameEntity save) {
        // Show confirmation dialog
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_save)
                .setMessage(R.string.confirm_delete_save)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    viewModel.deleteSave(save, new GameRepository.DeleteCallback() {
                        @Override
                        public void onSuccess() {
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() ->
                                        Toast.makeText(requireContext(),
                                                R.string.save_deleted,
                                                Toast.LENGTH_SHORT).show()
                                );
                            }
                        }

                        @Override
                        public void onError(String message) {
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() ->
                                        Toast.makeText(requireContext(),
                                                "Error: " + message,
                                                Toast.LENGTH_SHORT).show()
                                );
                            }
                        }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
