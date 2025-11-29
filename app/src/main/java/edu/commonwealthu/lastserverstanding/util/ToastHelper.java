package edu.commonwealthu.lastserverstanding.util;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

/**
 * Helper class for displaying messages with proper text wrapping.
 * Uses Snackbar instead of Toast to ensure full text is always visible.
 *
 * @author Ethan Wight
 */
public class ToastHelper {

    /**
     * Show a short message with text wrapping.
     *
     * @param view The view to anchor the message to (typically the root view)
     * @param message The message to display
     */
    public static void showShort(View view, String message) {
        show(view, message, Snackbar.LENGTH_SHORT);
    }

    /**
     * Show a long message with text wrapping.
     *
     * @param view The view to anchor the message to (typically the root view)
     * @param message The message to display
     */
    public static void showLong(View view, String message) {
        show(view, message, Snackbar.LENGTH_LONG);
    }

    /**
     * Internal method to show message.
     *
     * @param view The view to anchor the message to
     * @param message The message to display
     * @param duration Snackbar.LENGTH_SHORT or Snackbar.LENGTH_LONG
     */
    private static void show(View view, String message, int duration) {
        Snackbar snackbar = Snackbar.make(view, message, duration);

        // Get the snackbar view and center it
        View snackbarView = snackbar.getView();

        // Center the snackbar itself
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbarView.getLayoutParams();
        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        snackbarView.setLayoutParams(params);

        // Get the TextView and set max lines to allow wrapping and center alignment
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (textView != null) {
            textView.setMaxLines(5); // Allow up to 5 lines of text
            textView.setGravity(Gravity.CENTER); // Center the text within the TextView
            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER); // Additional centering
        }

        snackbar.show();
    }
}
