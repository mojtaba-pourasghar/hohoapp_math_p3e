package com.hoohoomath.app.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.hoohoomath.app.R;
import com.hoohoomath.app.data.Book;
import com.hoohoomath.app.data.VoiceCatalog;
import com.hoohoomath.app.tts.VoiceStore;

import java.util.List;

import static com.hoohoomath.app.data.PersianDigits.fa;

public final class ChapterVoiceDownloadDialog {
    private ChapterVoiceDownloadDialog() {}

    public static void showIfNeeded(Context context, int chapter, Runnable onDone) {
        if (context == null) return;
        List<String> keys = VoiceCatalog.keysOfChapter(context, chapter);
        if (keys == null || keys.isEmpty()) {
            if (onDone != null) onDone.run();
            return;
        }

        int have = VoiceStore.countHave(context, keys);
        if (have >= keys.size()) {
            if (onDone != null) onDone.run();
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_voice_download, null);

        Book.Chapter ch = Book.chapter(chapter);
        TextView title = view.findViewById(R.id.dl_title);
        TextView subtitle = view.findViewById(R.id.dl_subtitle);
        TextView status = view.findViewById(R.id.dl_status);
        ProgressBar progress = view.findViewById(R.id.dl_progress);
        TextView bgBtn = view.findViewById(R.id.dl_btn_bg);
        TextView cancelBtn = view.findViewById(R.id.dl_btn_cancel);

        if (title != null) title.setText("دانلود صداهای فصل " + ch.numberFa + " (" + ch.title + ")");
        if (subtitle != null) subtitle.setText("برای شنیدن توضیحات هوهو با صدای باکیفیت، فایل‌های صوتی دانلود می‌شوند.");

        progress.setMax(keys.size());
        progress.setProgress(have);
        if (status != null) status.setText(fa(have) + " از " + fa(keys.size()) + " فایل صوتی دانلود شده است (" + fa((have * 100) / keys.size()) + "٪)");

        Handler handler = new Handler(Looper.getMainLooper());

        VoiceStore.pause(false);
        VoiceStore.downloadChapter(context, chapter, new VoiceStore.Progress() {
            @Override
            public void onProgress(int ch, int done, int total) {
                handler.post(() -> {
                    progress.setMax(total);
                    progress.setProgress(done);
                    if (status != null) {
                        int pct = total > 0 ? (done * 100) / total : 0;
                        status.setText(fa(done) + " از " + fa(total) + " فایل صوتی دانلود شده است (" + fa(pct) + "٪)");
                    }
                    if (done >= total) {
                        handler.postDelayed(() -> {
                            try { dialog.dismiss(); } catch (Exception e) {}
                            if (onDone != null) onDone.run();
                        }, 500);
                    }
                });
            }
        });

        if (bgBtn != null) {
            bgBtn.setOnClickListener(v -> {
                try { dialog.dismiss(); } catch (Exception e) {}
                if (onDone != null) onDone.run();
            });
        }

        if (cancelBtn != null) {
            cancelBtn.setOnClickListener(v -> {
                VoiceStore.pause(true);
                try { dialog.dismiss(); } catch (Exception e) {}
                if (onDone != null) onDone.run();
            });
        }

        dialog.setContentView(view);
        dialog.setCancelable(true);
        try { dialog.show(); } catch (Exception e) {}
    }
}