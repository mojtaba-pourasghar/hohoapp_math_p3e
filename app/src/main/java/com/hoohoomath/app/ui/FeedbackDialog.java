package com.hoohoomath.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.hoohoomath.app.R;

/**
 * The "آفرین! / اشکالی ندارد!" sheet that slides up after every answer, and the plain
 * notice used for locked content and hints. A bottom sheet rather than a stock dialog,
 * matching the design — big friendly icon, one clear button, nothing to misread.
 */
public final class FeedbackDialog {
    private FeedbackDialog() {}

    public static void show(Context ctx, boolean ok, String message, String buttonLabel, Runnable onDismiss) {
        BottomSheetDialog dialog = new BottomSheetDialog(ctx);
        View content = LayoutInflater.from(ctx).inflate(R.layout.dialog_feedback, null);

        TextView icon = content.findViewById(R.id.fb_icon);
        icon.setText(ok ? "🌟" : "🤔");
        icon.setBackground(UiKit.roundedBg(
            ContextCompat.getColor(ctx, ok ? R.color.orange_bg : R.color.pink_bg), 0, 999f, ctx));

        TextView title = content.findViewById(R.id.fb_title);
        title.setText(ok ? "آفرین!" : "اشکالی ندارد!");
        title.setTextColor(ContextCompat.getColor(ctx, ok ? R.color.orange_text : R.color.pink_dark));

        ((TextView) content.findViewById(R.id.fb_message)).setText(message);

        TextView button = content.findViewById(R.id.fb_button);
        button.setText(buttonLabel);
        button.setOnClickListener(v -> {
            dialog.dismiss();
            if (onDismiss != null) onDismiss.run();
        });

        dialog.setContentView(content);
        dialog.setCancelable(false);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setNavigationBarColor(Color.WHITE);
        }
        dialog.show();
    }

    public static void info(Context ctx, String message) {
        BottomSheetDialog dialog = new BottomSheetDialog(ctx);
        View content = LayoutInflater.from(ctx).inflate(R.layout.dialog_feedback, null);

        TextView icon = content.findViewById(R.id.fb_icon);
        icon.setText("🦉");
        icon.setBackground(UiKit.roundedBg(ContextCompat.getColor(ctx, R.color.orange_bg), 0, 999f, ctx));

        TextView title = content.findViewById(R.id.fb_title);
        title.setText("هوهو می‌گوید");
        title.setTextColor(ContextCompat.getColor(ctx, R.color.orange_text));

        ((TextView) content.findViewById(R.id.fb_message)).setText(message);

        TextView button = content.findViewById(R.id.fb_button);
        button.setText("باشه");
        button.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(content);
        dialog.show();
    }
}
