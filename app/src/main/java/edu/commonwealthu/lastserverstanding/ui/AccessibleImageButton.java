package edu.commonwealthu.lastserverstanding.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;

/**
 * Custom ImageButton that properly overrides performClick for accessibility.
 * This is required when using setOnTouchListener for drag-and-drop functionality.
 */
public class AccessibleImageButton extends AppCompatImageButton {

    public AccessibleImageButton(@NonNull Context context) {
        super(context);
    }

    public AccessibleImageButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AccessibleImageButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean performClick() {
        // Call super to handle accessibility
        super.performClick();
        return true;
    }
}
