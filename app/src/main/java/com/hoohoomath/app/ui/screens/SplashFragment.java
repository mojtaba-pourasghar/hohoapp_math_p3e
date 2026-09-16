package com.hoohoomath.app.ui.screens;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hoohoomath.app.R;
import com.hoohoomath.app.tts.TtsManager;
import com.hoohoomath.app.ui.BaseFragment;
import com.hoohoomath.app.ui.Screen;
import com.hoohoomath.app.ui.mascot.HooHooView;

public class SplashFragment extends BaseFragment {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable autoAdvance;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_splash, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        HooHooView owl = view.findViewById(R.id.splash_owl);
        owl.setFlying(true);

        ProgressBar progress = view.findViewById(R.id.splash_progress);
        ObjectAnimator anim = ObjectAnimator.ofInt(progress, "progress", 8, 100);
        anim.setDuration(1600);
        anim.start();

        view.findViewById(R.id.splash_start).setOnClickListener(v -> goToMap());

        TtsManager tts = TtsManager.get();
        if (tts != null) tts.speak("سلام! من هوهو هستم، معلم ریاضی تو. بیا با هم ریاضی سوم دبستان را یاد بگیریم.");

        autoAdvance = this::goToMap;
        handler.postDelayed(autoAdvance, 2600);
    }

    private void goToMap() {
        if (!isAdded()) return;
        handler.removeCallbacks(autoAdvance);
        nav().go(Screen.MAP);
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(autoAdvance);
        super.onDestroyView();
    }

    @Override
    protected String entryTip() {
        return null; // splash speaks its own greeting above; skip the generic entry-tip
    }
}
