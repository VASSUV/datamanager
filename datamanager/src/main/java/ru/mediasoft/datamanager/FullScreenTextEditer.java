package ru.mediasoft.datamanager;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

public class FullScreenTextEditer {

    public static void open(final FrameLayout frameLayout, String text, final int cursorPosition, final FullScreenEditerListener listener) {
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP;


        final View view = LayoutInflater.from(frameLayout.getContext()).inflate(R.layout.full_screen_text_editer, frameLayout, false);
        final EditText editText = view.findViewById(R.id.edit_text);

        editText.setText(text);
        editText.setSelection(cursorPosition);
        editText.requestFocus();

        view.findViewById(R.id.apply).setOnClickListener(v -> {
            listener.onApply(editText.getText().toString(), editText.getSelectionStart());
            destroy(frameLayout, view);
        });
        view.findViewById(R.id.close).setOnClickListener(v -> {
            listener.onClose();
            destroy(frameLayout, view);
        });
        view.findViewById(R.id.button_exit_full_screen).setOnClickListener(v -> {
            listener.onExitFullScreen(editText.getText().toString(), editText.getSelectionStart());
            destroy(frameLayout, view);
        });

        frameLayout.addView(view, params);
    }

    private static void destroy(FrameLayout frameLayout, View textEditer) {
        if (frameLayout != null) {
            frameLayout.removeView(textEditer);
        }
    }

    public interface FullScreenEditerListener {
        void onApply(String text, int cursorPosition);

        void onExitFullScreen(String text, int cursorPosition);

        void onClose();
    }
}
