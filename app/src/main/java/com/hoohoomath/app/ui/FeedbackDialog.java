package com.hoohoomath.app.ui;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

/** The small "آفرین! / اشکالی ندارد!" pop-up shown after every answer, and for locked-content notices. */
public final class FeedbackDialog {
    private FeedbackDialog() {}

    public static void show(Context ctx, boolean ok, String message, String buttonLabel, Runnable onDismiss) {
        new AlertDialog.Builder(ctx)
            .setTitle(ok ? "🌟 آفرین!" : "🤔 اشکالی ندارد!")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(buttonLabel, (d, w) -> {
                d.dismiss();
                if (onDismiss != null) onDismiss.run();
            })
            .show();
    }

    public static void info(Context ctx, String message) {
        new AlertDialog.Builder(ctx)
            .setTitle("🦉 هوهو می‌گوید")
            .setMessage(message)
            .setPositiveButton("باشه", (d, w) -> d.dismiss())
            .show();
    }
}
