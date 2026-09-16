package com.hoohoomath.app.tts;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wraps Android's built-in text-to-speech engine for هوهو's narration: Persian locale,
 * a male-sounding voice when the device offers one, and a slightly lower/slower delivery
 * so it reads a bit like a warm owl-teacher instead of a generic phone voice.
 */
public class TtsManager {
    private static TtsManager instance;

    private final TextToSpeech tts;
    private boolean ready = false;
    private final AtomicInteger idCounter = new AtomicInteger();

    public interface Callback {
        default void onStart() {}
        default void onDone() {}
        default void onError() {}
    }

    private TtsManager(Context appContext) {
        tts = new TextToSpeech(appContext, status -> {
            if (status == TextToSpeech.SUCCESS) {
                ready = true;
                configureVoice();
            }
        });
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) { dispatch(utteranceId, Event.START); }
            @Override public void onDone(String utteranceId) { dispatch(utteranceId, Event.DONE); }
            @Override public void onError(String utteranceId) { dispatch(utteranceId, Event.ERROR); }
        });
    }

    public static synchronized void init(Context appContext) {
        if (instance == null) instance = new TtsManager(appContext.getApplicationContext());
    }

    public static TtsManager get() {
        return instance;
    }

    private void configureVoice() {
        Locale fa = new Locale("fa", "IR");
        int result = tts.setLanguage(fa);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(new Locale("fa"));
        }

        Voice best = null;
        try {
            Set<Voice> voices = tts.getVoices();
            if (voices != null) {
                for (Voice v : voices) {
                    if (v.getLocale() == null) continue;
                    String lang = v.getLocale().getLanguage();
                    if (!"fa".equals(lang)) continue;
                    String name = v.getName() == null ? "" : v.getName().toLowerCase(Locale.US);
                    boolean male = name.contains("male") && !name.contains("female");
                    if (male) { best = v; break; }
                    if (best == null) best = v; // fall back to any Persian voice
                }
            }
        } catch (Exception ignored) {}
        if (best != null) tts.setVoice(best);

        tts.setPitch(0.86f);   // a touch lower = warmer, more owl-like
        tts.setSpeechRate(0.94f);
    }

    private final java.util.Map<String, Callback> callbacks = new java.util.concurrent.ConcurrentHashMap<>();

    private enum Event { START, DONE, ERROR }

    private void dispatch(String id, Event e) {
        Callback cb = callbacks.get(id);
        if (cb == null) return;
        switch (e) {
            case START: cb.onStart(); break;
            case DONE: callbacks.remove(id); cb.onDone(); break;
            case ERROR: callbacks.remove(id); cb.onError(); break;
        }
    }

    /** Speaks a line, replacing anything currently queued. */
    public void speak(String text) {
        speak(text, null);
    }

    public void speak(String text, Callback callback) {
        if (!ready || text == null || text.isEmpty()) return;
        String id = "u" + idCounter.incrementAndGet();
        if (callback != null) callbacks.put(id, callback);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id);
    }

    public void stop() {
        if (ready) tts.stop();
    }

    public boolean isSpeaking() {
        return ready && tts.isSpeaking();
    }

    public void shutdown() {
        if (tts != null) tts.shutdown();
    }
}
