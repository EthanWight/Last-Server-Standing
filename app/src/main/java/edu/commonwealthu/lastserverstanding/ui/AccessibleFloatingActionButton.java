package edu.commonwealthu.lastserverstanding.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * Custom FloatingActionButton that properly overrides performClick for accessibility.
 * This is required when using setOnTouchListener for drag-and-drop functionality.
 */
public class AccessibleFloatingActionButton extends FloatingActionButton {

    public AccessibleFloatingActionButton(@NonNull Context context) {
        super(context);
    }

    public AccessibleFloatingActionButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AccessibleFloatingActionButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean performClick() {
        // Call the superclass implementation which triggers any OnClickListener
        return super.performClick();
    }
}
