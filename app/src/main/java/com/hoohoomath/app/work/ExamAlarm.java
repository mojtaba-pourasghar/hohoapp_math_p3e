package com.hoohoomath.app.work;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.hoohoomath.app.data.AppState;
import com.hoohoomath.app.data.Notices;
import com.hoohoomath.app.data.OnlineExam;
import com.hoohoomath.app.ui.Announcements;

/**
 * One of the five countdown moments arriving.
 *
 * Everything is checked again here rather than trusted from when the alarm was set: a week can
 * pass between the two, and in that week the exam may have moved, the child may have sat it
 * early, or a parent may have turned announcements off. An alarm that fired is a question, not
 * an instruction.
 */
public class ExamAlarm extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Context app = context.getApplicationContext();
        int minutes = intent.getIntExtra("minutes", 0);
        String forExam = intent.getStringExtra("examKey");

        if (!Notices.allowed(app)) return;

        OnlineExam exam = OnlineExam.cached(app);
        if (!exam.announced() || exam.startsAt <= 0) return;
        if (forExam != null && !forExam.equals(exam.key())) return;   // a different exam since

        AppState state = AppState.get();
        if (!state.getBoolean(exam.key() + "_registered", false)) return;
        if (state.getBoolean(exam.key() + "_done", false)) return;

        // the alarm is inexact, so it may be early or late; if the paper is already over there
        // is nothing to hurry towards
        long now = System.currentTimeMillis();
        if (now >= exam.endsAt()) return;

        Announcements.examCountdown(app, exam, minutes);
    }
}
