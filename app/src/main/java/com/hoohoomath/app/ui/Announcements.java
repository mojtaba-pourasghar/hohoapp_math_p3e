package com.hoohoomath.app.ui;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.hoohoomath.app.MainActivity;
import com.hoohoomath.app.R;
import com.hoohoomath.app.data.Notices;

import java.util.List;

/**
 * Putting a notice in the phone's own notification tray.
 *
 * The app has no push service and no account — nothing knows this phone exists. What it has is a
 * file on the host that it already reads at launch, and that turns out to be enough for what the
 * notices are for: «a new worksheet is up», «the online exam opens on Thursday». The child sees
 * it next time they open the app; the tray entry is the part that brings them back.
 *
 * Which means a notice can arrive late, when the app is next started, and never while the phone
 * is asleep. For news that has to land the moment it is sent — «the exam starts in half an
 * hour» — there is Metrix, which the build pulls in only when there is a Firebase config and a
 * pair of Metrix ids to go with it (see app/build.gradle). That SDK does its own work: it
 * registers itself, builds its own notifications and opens the app on a tap, so there is no code
 * here for it beyond honouring the parent's switch.
 */
public final class Announcements {

    public static final String CHANNEL = "hoohoo_news";
    public static final int PERMISSION_REQUEST = 7301;

    private Announcements() {}

    /** Android 13 and up will not show a notification until the person has said yes once. */
    public static boolean permitted(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true;
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Asks for it — but only when there is something to say, so a child's first launch is a
     * lesson rather than a permission box.
     */
    public static void askIfUseful(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (permitted(activity) || !Notices.allowed(activity)) return;
        if (Notices.unseen(activity).isEmpty()) return;
        ActivityCompat.requestPermissions(activity,
            new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST);
    }

    // ── the push SDK, if this build has one ──────────────────────────────────
    //
    // ir.metrix.notification is in the APK only when push was configured at build time, so it
    // cannot be named directly — a build without it would not compile. Everything below goes
    // through reflection against the one class, and does nothing at all when it is absent.
    // The names are taken from the published AAR, not from memory:
    //   public static void enableNotifications() / disableNotifications() / isNotificationEnable()
    private static final String METRIX = "ir.metrix.notification.MetrixNotification";

    /** Whether this build was given a push service at all. */
    public static boolean pushBuiltIn() {
        try {
            Class.forName(METRIX);
            return true;
        } catch (ClassNotFoundException notThisBuild) {
            return false;
        }
    }

    /**
     * Points the push service at the parent's switch.
     *
     * «خبرها خاموش» has to mean every kind of news, or the switch is a lie: the card on the map,
     * the tray entry this app posts itself, and anything the push service would deliver.
     */
    public static void applyPushPreference(Context context) {
        boolean on = Notices.allowed(context);
        try {
            Class<?> metrix = Class.forName(METRIX);
            metrix.getMethod(on ? "enableNotifications" : "disableNotifications").invoke(null);
        } catch (ClassNotFoundException notThisBuild) {
            // a build without push; the file-based notices are all there is
        } catch (Exception changed) {
            // the SDK is there but not as expected — better a missed switch than a crash
        }
    }

    private static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL, "خبرهای هوهو",
            NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("درس‌ها و کاربرگ‌های تازه، و خبرِ آزمون‌های آنلاین");
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(channel);
    }

    /** Puts every notice that has not been to the tray yet into it. */
    public static void post(Context context) {
        if (!Notices.allowed(context) || !permitted(context)) return;
        List<Notices.Notice> notices = Notices.unseen(context);
        if (notices.isEmpty()) return;
        ensureChannel(context);

        for (Notices.Notice notice : notices) {
            if (!Notices.claimForTray(context, notice.id)) continue;

            Intent open = new Intent(context, MainActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            if (!notice.screen.isEmpty()) open.putExtra("notice_screen", notice.screen);
            open.putExtra("notice_id", notice.id);

            PendingIntent tap = PendingIntent.getActivity(context, notice.id.hashCode(), open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder built = new NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_owl_face)
                .setContentTitle(notice.title)
                .setContentText(notice.body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notice.body))
                .setAutoCancel(true)
                .setContentIntent(tap)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            try {
                NotificationManagerCompat.from(context)
                    .notify(Math.abs(notice.id.hashCode()), built.build());
            } catch (SecurityException refused) {
                return;     // permission was taken away between the check and here
            }
        }
    }
}
