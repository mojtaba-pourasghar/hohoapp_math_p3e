package com.hoohoomath.app;

import android.app.Application;

import com.hoohoomath.app.data.AppState;
import com.hoohoomath.app.tts.SoundManager;
import com.hoohoomath.app.tts.TtsManager;

public class HooHooApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppState.init(this);
        TtsManager.init(this);
        SoundManager.init(this);
    }
}
