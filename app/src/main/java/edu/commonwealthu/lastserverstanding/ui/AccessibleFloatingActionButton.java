package edu.commonwealthu.lastserverstanding.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * Custom FloatingActionButton that properly overrides performClick for accessibility.
 * This is required when using setOnTouchListener for drag-and-drop functionality.
 *
 * @author Ethan Wight
 */
public class AccessibleFloatingActionButton extends FloatingActionButton {

    /**
     * Constructor for programmatic creation.
     *
     * @param context The application context.
     */
    public AccessibleFloatingActionButton(@NonNull Context context) {
        super(context);
    }

    /**
     * Constructor for XML inflation.
     *
     * @param context The application context.
     * @param attrs The attribute set.
     */
    public AccessibleFloatingActionButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Constructor for XML inflation with style.
     *
     * @param context The application context.
     * @param attrs The attribute set.
     * @param defStyleAttr The default style attribute.
     */
    public AccessibleFloatingActionButton(@NonNull Context context, @Nullable AttributeSet attrs,
                                         int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

}
