package com.hoohoomath.app.tts;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

/**
 * Plays a narration line. Every line has a key; if a recorded voice file with that name sits in
 * res/raw it is played, otherwise the device's Persian text-to-speech reads the same words. That
 * way the lessons talk on any device today, and swapping in real recordings later is just a
 * matter of dropping files into res/raw (see res/raw/audio_manifest.txt for the full list).
 */
public final class LessonAudio {

    public interface PlaybackListener {
        /** Called once playback starts, with how long the line is expected to take. */
        void onStarted(long durationMs);
        void onFinished();
    }

    private static MediaPlayer player;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private LessonAudio() {}

    public static void play(Context context, String audioKey, String text, PlaybackListener listener) {
        stop();

        int resId = resolveRaw(context, audioKey);
        if (resId != 0) {
            try {
                player = MediaPlayer.create(context.getApplicationContext(), resId);
                if (player != null) {
                    long duration = Math.max(1200, player.getDuration());
                    player.setOnCompletionListener(mp -> {
                        release();
                        if (listener != null) listener.onFinished();
                    });
                    player.start();
                    if (listener != null) listener.onStarted(duration);
                    return;
                }
            } catch (Exception ignored) {
                release();
            }
        }

        long estimate = estimateSpokenMs(text);
        TtsManager tts = TtsManager.get();
        if (listener != null) listener.onStarted(estimate);

        // The engine stays silent when the device has no Persian voice installed, and then it
        // never reports back — so a watchdog always releases the lesson after the expected time.
        final boolean[] done = {false};
        Runnable finishOnce = () -> {
            if (done[0]) return;
            done[0] = true;
            if (listener != null) listener.onFinished();
        };
        MAIN.postDelayed(finishOnce, estimate + 1200);

        if (tts == null) return;
        tts.speak(text, new TtsManager.Callback() {
            @Override public void onDone() { MAIN.post(finishOnce); }
            @Override public void onError() { MAIN.post(finishOnce); }
        });
    }

    public static void stop() {
        release();
        TtsManager tts = TtsManager.get();
        if (tts != null) tts.stop();
    }

    private static void release() {
        if (player != null) {
            try {
                player.release();
            } catch (Exception ignored) {
            }
            player = null;
        }
    }

    /** res/raw/<key>.(mp3|ogg|wav) — returns 0 when no recording has been added yet. */
    private static int resolveRaw(Context context, String audioKey) {
        if (audioKey == null || audioKey.isEmpty()) return 0;
        try {
            return context.getResources().getIdentifier(audioKey, "raw", context.getPackageName());
        } catch (Exception e) {
            return 0;
        }
    }

    /** Rough length of the spoken line, used to pace the stage animation against the voice. */
    public static long estimateSpokenMs(String text) {
        if (text == null || text.isEmpty()) return 1500;
        long ms = 700 + (long) (text.length() * 72);
        return Math.max(1800, Math.min(ms, 16000));
    }
}
