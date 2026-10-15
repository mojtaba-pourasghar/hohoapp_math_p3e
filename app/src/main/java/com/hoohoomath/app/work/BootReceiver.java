package com.hoohoomath.app.work;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Putting the exam reminders back after the phone restarts.
 *
 * AlarmManager forgets everything on a reboot — unlike WorkManager, which keeps its queue in a
 * database and reschedules itself, so the quarter-hourly check needs nothing here. The exam
 * countdown does: without this, a phone restarted the morning of the exam would simply never
 * ring.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
            && !"android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            return;
        }
        ExamReminders.refresh(context);
        NoticeWork.schedule(context);     // belt and braces; WorkManager usually has this already
    }
}
