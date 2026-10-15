package com.hoohoomath.app.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.hoohoomath.app.data.Notices;
import com.hoohoomath.app.ui.Announcements;

import java.util.concurrent.TimeUnit;

/**
 * Looking for news while the app is closed.
 *
 * This is what makes a notice arrive on a phone sitting in a bag, which is the whole thing a
 * push service would have been for — and it costs no SDK, no account and no third party: the
 * system wakes the app now and then, it fetches the same small signed file, and anything new
 * goes to the notification tray.
 *
 * Android will not run this more often than every fifteen minutes however it is asked, and
 * batches it with whatever else the phone is doing anyway. Half an hour is the ask here: news
 * about an exam this evening is in good time, and a child's phone is not kept awake for it.
 *
 * It only runs with a connection — there is nothing to do without one — and the same «one a
 * day» and «late news is no news» rules apply, because they live in {@link Notices} rather than
 * in whoever happens to be asking.
 */
public class NoticeWork extends Worker {

    private static final String NAME = "hoohoo-notices";
    private static final long EVERY_MINUTES = 30;

    public NoticeWork(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    /** Asks the system to keep checking. Safe to call on every launch; it replaces nothing. */
    public static void schedule(Context context) {
        try {
            PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(
                NoticeWork.class, EVERY_MINUTES, TimeUnit.MINUTES)
                .setConstraints(new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build())
                .build();
            WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, work);
        } catch (Exception notAvailable) {
            // WorkManager could not start; the in-app checks still run
        }
    }

    /** Stops looking — what «خبرها خاموش» means once the app is closed. */
    public static void cancel(Context context) {
        try {
            WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(NAME);
        } catch (Exception ignored) {
            // nothing was scheduled
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        if (!Notices.allowed(context)) return Result.success();
        // this is the worker's own thread, so the fetch happens on it rather than being handed
        // to a pool and waited on
        Notices.refreshNow(context);
        Announcements.post(context);
        return Result.success();
    }
}
