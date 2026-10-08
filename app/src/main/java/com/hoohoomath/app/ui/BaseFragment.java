package com.hoohoomath.app.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.hoohoomath.app.data.AppState;
import com.hoohoomath.app.ui.mascot.MascotController;

public abstract class BaseFragment extends Fragment {
    protected AppState state() {
        return AppState.get();
    }

    protected Navigator nav() {
        return (Navigator) requireActivity();
    }

    protected MascotController mascot() {
        return nav().mascot();
    }

    /** Whether everything is open — the free part plus what the subscription adds. */
    protected boolean paid() {
        return com.hoohoomath.app.billing.Entitlement.unlocked(requireContext());
    }

    /**
     * Sends the child to the paywall, telling it what they were reaching for.
     *
     * `what` becomes the first line of that screen («فصل ۳ در نسخه‌ی کامل است»), so the answer to
     * «why can I not open this?» is on the screen that asks for money, not a step away from it.
     */
    protected void goPaywall(String what) {
        android.os.Bundle args = new android.os.Bundle();
        args.putString("wanted", what == null ? "" : what);
        nav().go(Screen.PAYWALL, args);
    }

    /** Shown once when the screen first appears; screens override to give هوهو a contextual tip. */
    protected String entryTip() {
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        String tip = entryTip();
        if (tip != null) mascot().say(tip);
    }
}
