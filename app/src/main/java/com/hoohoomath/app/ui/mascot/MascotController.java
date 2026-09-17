package com.hoohoomath.app.ui.mascot;

import android.animation.ObjectAnimator;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import com.hoohoomath.app.data.AppState;
import com.hoohoomath.app.tts.HootSoundPlayer;
import com.hoohoomath.app.tts.SoundManager;
import com.hoohoomath.app.tts.TtsManager;

import java.util.Random;

/**
 * Ties the owl view + its speech bubble + TTS together into the handful of things the rest of the
 * app asks هوهو to do: say a line, celebrate a right answer, comfort a wrong one, or fly a little
 * on screen and loading transitions. هوهو does NOT fly nonstop — only at those moments.
 *
 * The bubble can be closed with its × and switched off entirely from the parent panel, because a
 * character who talks over everything gets annoying fast.
 */
public class MascotController {
    private final HooHooView owl;
    private final View overlay;      // the draggable container holding bubble + owl
    private final View bubbleWrap;
    private final TextView bubbleText;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private Runnable pendingRevert;
    private boolean awayFromHome = false;
    private float homeTranslationX, homeTranslationY;

    public MascotController(HooHooView owl, View overlay, View bubbleWrap, TextView bubbleText) {
        this.owl = owl;
        this.overlay = overlay;
        this.bubbleWrap = bubbleWrap;
        this.bubbleText = bubbleText;
    }

    private boolean bubblesAllowed() {
        try {
            return AppState.get().tipsEnabled();
        } catch (Exception e) {
            return true;
        }
    }

    private void setBubble(String text) {
        if (bubbleWrap == null || bubbleText == null) return;
        if (!bubblesAllowed() || text == null || text.isEmpty()) {
            bubbleWrap.setVisibility(View.GONE);
            return;
        }
        bubbleText.setText(text);
        bubbleWrap.setVisibility(View.VISIBLE);
    }

    /** The × on the bubble — closes whatever هوهو is currently showing. */
    public void hideBubble() {
        if (bubbleWrap != null) bubbleWrap.setVisibility(View.GONE);
    }

    /**
     * Puts a line in the bubble without speaking it — for screens that drive their own audio
     * (the lesson plays recorded narration) and just want هوهو's mouth to move along.
     */
    public void showBubble(String text, boolean speaking) {
        setBubble(text);
        owl.setSpeaking(speaking);
    }

    /** هوهو speaks a line and its beak animates while the audio plays. */
    public void say(String text) {
        setBubble(text);
        owl.setSpeaking(true);
        TtsManager tts = TtsManager.get();
        if (tts != null) {
            tts.speak(text, new TtsManager.Callback() {
                @Override public void onDone() { owl.post(() -> owl.setSpeaking(false)); }
                @Override public void onError() { owl.post(() -> owl.setSpeaking(false)); }
            });
        }
    }

    /** Correct answer: happy bounce, a sparkle, sometimes a hoot, then back to idle. */
    public void celebrate(String praiseText) {
        cancelPendingRevert();
        owl.setMood(HooHooView.Mood.HAPPY);
        owl.setFlying(true);
        bounce();
        SoundManager sound = SoundManager.get();
        if (sound != null) sound.star();
        if (random.nextFloat() < 0.3f) HootSoundPlayer.playHoot();
        if (praiseText != null) say(praiseText);
        pendingRevert = () -> {
            owl.setMood(HooHooView.Mood.IDLE);
            owl.setFlying(false);
        };
        handler.postDelayed(pendingRevert, 2600);
    }

    /** Wrong answer: thoughtful, not flying — no need to make the kid feel worse. */
    public void comfort(String encouragementText) {
        cancelPendingRevert();
        owl.setMood(HooHooView.Mood.THINKING);
        owl.setFlying(false);
        if (encouragementText != null) say(encouragementText);
        pendingRevert = () -> owl.setMood(HooHooView.Mood.IDLE);
        handler.postDelayed(pendingRevert, 2600);
    }

    /**
     * Flies over to whatever is being explained and hovers beside it, so the child's eye follows
     * the owl to the right part of the screen. Call returnHome() when the explanation is over.
     */
    public void flyTo(View target) {
        if (overlay == null || target == null || target.getWidth() == 0) return;

        int[] targetPos = new int[2];
        int[] ownPos = new int[2];
        target.getLocationInWindow(targetPos);
        overlay.getLocationInWindow(ownPos);

        if (!awayFromHome) {
            homeTranslationX = overlay.getTranslationX();
            homeTranslationY = overlay.getTranslationY();
            awayFromHome = true;
        }

        // sit near the lower start-side corner of the target, leaving room for the bubble above
        float wantedCx = targetPos[0] + target.getWidth() * 0.22f;
        float wantedCy = targetPos[1] + target.getHeight() * 0.68f;
        float currentCx = ownPos[0] + overlay.getWidth() / 2f;
        float currentCy = ownPos[1] + overlay.getHeight() / 2f;

        glide(overlay.getTranslationX() + (wantedCx - currentCx),
              overlay.getTranslationY() + (wantedCy - currentCy));
    }

    /** Settles back wherever the owl was before it flew off to explain something. */
    public void returnHome() {
        if (overlay == null || !awayFromHome) return;
        awayFromHome = false;
        glide(homeTranslationX, homeTranslationY);
    }

    private void glide(float toX, float toY) {
        cancelPendingRevert();
        owl.setFlying(true);
        ObjectAnimator x = ObjectAnimator.ofFloat(overlay, View.TRANSLATION_X, toX);
        ObjectAnimator y = ObjectAnimator.ofFloat(overlay, View.TRANSLATION_Y, toY);
        x.setDuration(750);
        y.setDuration(750);
        x.setInterpolator(new AccelerateDecelerateInterpolator());
        y.setInterpolator(new AccelerateDecelerateInterpolator());
        x.start();
        y.start();
        pendingRevert = () -> owl.setFlying(false);
        handler.postDelayed(pendingRevert, 800);
    }

    /** Brief flutter when navigating between screens. */
    public void onScreenTransition() {
        cancelPendingRevert();
        owl.setFlying(true);
        bounce();
        pendingRevert = () -> owl.setFlying(false);
        handler.postDelayed(pendingRevert, 1400);
    }

    /** Continuous gentle flight, used on the splash/loading screen only. */
    public void setLoading(boolean loading) {
        owl.setFlying(loading);
        owl.setMood(HooHooView.Mood.IDLE);
    }

    private void bounce() {
        ObjectAnimator anim = ObjectAnimator.ofFloat(owl, View.SCALE_X, 1f, 1.12f, 1f);
        ObjectAnimator anim2 = ObjectAnimator.ofFloat(owl, View.SCALE_Y, 1f, 1.12f, 1f);
        anim.setDuration(420);
        anim2.setDuration(420);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim2.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.start();
        anim2.start();
    }

    private void cancelPendingRevert() {
        if (pendingRevert != null) handler.removeCallbacks(pendingRevert);
    }
}
