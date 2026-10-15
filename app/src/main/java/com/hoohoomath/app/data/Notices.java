package com.hoohoomath.app.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.hoohoomath.app.BuildConfig;
import com.hoohoomath.app.net.Net;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * خبرها — what the teacher wants every installed copy to know.
 *
 * A new chapter is up, a printable worksheet has been added, the online exam opens on Thursday,
 * the full version is half price this week. None of that is worth an APK, and none of it needs a
 * push service: the app already asks the host for a worksheet list and a voice catalogue every
 * time it starts, so it asks for this at the same time, from the same folder, over the same FTP
 * upload the teacher already uses.
 *
 *   { "notices": [
 *       { "id": "aban-exam",            // must not change; it is how «already seen» is kept
 *         "title": "آزمون آنلاین آبان",
 *         "body": "پنج‌شنبه ساعت ۱۶، با بچه‌های سراسر کشور.",
 *         "image": "notices/aban.jpg",  // optional, relative to this folder or a full URL
 *         "link": "https://…",          // optional, opens in the browser
 *         "screen": "ONLINE_EXAM",      // optional, a screen inside the app — or ANYWHERE
 *         "from": 1761000000000,        // optional epoch ms; before this it is not shown
 *         "until": 1763000000000 } ] }  // optional epoch ms; after this it is dropped, unshown
 *
 * Every notice carries the hash of its picture (`imageSha256`), because the signature covers
 * this text and not the separate image file beside it.
 *
 * Nothing is sent anywhere. The app reads a file; it does not report who read it, and no account,
 * device id or token leaves the phone.
 *
 * **The file is signed and the signature is checked before any of it is believed** — see
 * {@link NoticeSeal}. The host is plain http on shared hosting; without that check, taking the
 * host would mean writing on the screen of every child who has the app.
 *
 * Two rules keep it from becoming noise, both of which matter most on a phone that has been off
 * the network for weeks and then suddenly sees a month of news at once:
 *
 *   **One a day.** However many notices arrive together, the child is shown one, today. The rest
 *   wait their turn. A fortnight offline must not turn into fourteen pop-ups in one sitting.
 *
 *   **Late news is no news.** A notice whose `until` has already passed by the time the phone
 *   first sees it is dropped without ever being shown. «آزمون پنج‌شنبه ساعت ۱۶» read on Friday is
 *   not information, it is a child being told they missed something.
 */
public final class Notices {

    /** The same folder the voice clips and the worksheets come from. */
    public static final String BASE_URL = "http://mp-apdl.ir/grade-3/math/";
    public static final String URL = BASE_URL + "notices.json";

    /** The teacher's signature over notices.json, written beside it. See {@link NoticeSeal}. */
    public static final String SIGNATURE_URL = BASE_URL + "notices.json.sig";

    /** The `screen` value that means «wherever they are» rather than a screen to open. */
    public static final String ANYWHERE = "ANYWHERE";

    private static final String CACHED = "notices-cache.json";
    private static final String PREFS = "notices";
    private static final String SEEN = "seen";          // waved away by the child
    private static final String TOLD = "told";          // has been to the notification tray
    private static final String SHOWN_DAY = "day";      // the day its dialog was last put up
    private static final String LAST_DIALOG = "lastDialogDay";
    private static final String LAST_CHECK = "lastCheck";
    private static final String LAST_RESULT = "lastResult";
    private static final String LAST_UPDATE_ASK = "lastUpdateAsk";

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Notices() {}

    public static final class Notice {
        public final String id;
        public final String title;
        public final String body;
        public final String image;
        public final String imageSha256;
        public final String link;
        public final String screen;
        public final long from;
        public final long until;

        Notice(String id, String title, String body, String image, String imageSha256,
               String link, String screen, long from, long until) {
            this.id = id;
            this.title = title;
            this.body = body;
            this.image = image;
            this.imageSha256 = imageSha256;
            this.link = link;
            this.screen = screen;
            this.from = from;
            this.until = until;
        }

        boolean current(long now) {
            if (from > 0 && now < from) return false;
            return until <= 0 || now <= until;
        }

        /** The full address of the picture, if there is one. */
        public String imageUrl() {
            if (image.isEmpty()) return "";
            return image.startsWith("http") ? image : BASE_URL + image;
        }

        public boolean hasImage() {
            return !image.isEmpty();
        }

        /**
         * «هر جای برنامه بود، همان‌جا نشانش بده».
         *
         * The ordinary notice waits for the child to come back to the map, which is where news
         * belongs: a lesson should not be interrupted to advertise at a seven-year-old. A notice
         * marked this way does not wait — «the exam starts in ten minutes» is no use to someone
         * who is three pages into chapter four and will not look up for an hour.
         *
         * It is a marker, not a destination, so it never goes into Screen.valueOf.
         */
        public boolean anywhere() {
            return ANYWHERE.equals(screen);
        }

        /** Where tapping it goes, or empty when it only marks the notice as «show at once». */
        public String destination() {
            return anywhere() ? "" : screen;
        }
    }

    /** What the host says the newest build is. */
    public static final class Update {
        public final String version;
        public final String url;
        public final String note;

        Update(String version, String url, String note) {
            this.version = version;
            this.url = url;
            this.note = note;
        }
    }

    /**
     * A newer version than this one, or null.
     *
     * **It rides inside the signed file on purpose.** A download link is the most dangerous
     * thing this app could be told: «there is a new version, here it is» pointing at someone
     * else's APK is how a compromised host would get onto a child's phone. Putting it in
     * notices.json means it is covered by the same signature as everything else and needs no
     * second file, no second signature and no second thing to get wrong.
     */
    public static Update updateWaiting(Context context) {
        if (!NoticeSeal.ready(context)) return null;
        try {
            String json = readCache(context.getApplicationContext());
            if (json == null) return null;
            JSONObject app = new JSONObject(json).optJSONObject("app");
            if (app == null) return null;
            String version = app.optString("version", "").trim();
            String url = app.optString("url", "").trim();
            if (version.isEmpty() || url.isEmpty()) return null;
            if (!url.startsWith("https://") && !url.startsWith("http://")) return null;
            if (!newer(version, BuildConfig.VERSION_NAME)) return null;
            return new Update(version, url, app.optString("note", "").trim());
        } catch (Exception broken) {
            return null;
        }
    }

    /**
     * Whether `theirs` is a later version than `ours`, comparing numbers rather than text —
     * «1.30.0» is after «1.9.0», which a string comparison gets backwards.
     */
    static boolean newer(String theirs, String ours) {
        String[] a = theirs.split("\\."), b = ours.split("\\.");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? digits(a[i]) : 0;
            int y = i < b.length ? digits(b[i]) : 0;
            if (x != y) return x > y;
        }
        return false;
    }

    private static int digits(String part) {
        int value = 0;
        for (int i = 0; i < part.length(); i++) {
            char c = part.charAt(i);
            if (c < '0' || c > '9') break;        // «1.2.0-beta» compares as 1.2.0
            value = value * 10 + (c - '0');
        }
        return value;
    }

    /** At most one «there is a new version» a day, like everything else here. */
    public static boolean updateDue(Context context) {
        return updateWaiting(context) != null
            && prefs(context).getLong(LAST_UPDATE_ASK, 0) != today();
    }

    public static void askedAboutUpdate(Context context) {
        prefs(context).edit().putLong(LAST_UPDATE_ASK, today()).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Whether the parent has left announcements on. On by default; one switch turns them off. */
    public static boolean allowed(Context context) {
        return AppState.get().getBoolean("notices_on", true);
    }

    public static void allow(Context context, boolean on) {
        AppState.get().setBoolean("notices_on", on);
    }

    private static long today() {
        return System.currentTimeMillis() / (24 * 60 * 60 * 1000L);
    }

    // ── reading ──────────────────────────────────────────────────────────────

    /** Everything currently worth showing, from the copy on the device. */
    public static List<Notice> current(Context context) {
        List<Notice> out = new ArrayList<>();
        if (!allowed(context)) return out;
        // belt and braces: only a verified file is ever cached, but a build with no key at all
        // must show nothing whatever is lying in the cache
        if (!NoticeSeal.ready(context)) return out;
        long now = System.currentTimeMillis();
        for (Notice notice : parse(readCache(context))) {
            if (notice.current(now)) out.add(notice);
        }
        return out;
    }

    /** The ones the child has not waved away yet. */
    public static List<Notice> unseen(Context context) {
        List<Notice> out = new ArrayList<>();
        for (Notice notice : current(context)) {
            if (!prefs(context).getBoolean(SEEN + "_" + notice.id, false)) out.add(notice);
        }
        return out;
    }

    /**
     * The one notice to put in front of the child right now, or null.
     *
     * At most one a day, whatever has piled up. The oldest unseen one goes first, so a backlog
     * comes out in the order it was written rather than newest-first.
     */
    public static Notice dueNow(Context context) {
        if (!allowed(context)) return null;
        if (prefs(context).getLong(LAST_DIALOG, 0) == today()) return null;
        List<Notice> waiting = unseen(context);
        if (waiting.isEmpty()) return null;
        return waiting.get(waiting.size() - 1);     // the file is newest-first; take the oldest
    }

    /** Notes that this notice has had its turn today. */
    public static void shownToday(Context context, String id) {
        prefs(context).edit()
            .putLong(LAST_DIALOG, today())
            .putLong(SHOWN_DAY + "_" + id, today())
            .apply();
    }

    public static void markSeen(Context context, String id) {
        prefs(context).edit().putBoolean(SEEN + "_" + id, true).apply();
    }

    /** True the first time only — so the same news never reaches the tray twice. */
    public static boolean claimForTray(Context context, String id) {
        if (prefs(context).getBoolean(TOLD + "_" + id, false)) return false;
        prefs(context).edit().putBoolean(TOLD + "_" + id, true).apply();
        return true;
    }

    /**
     * Fetches the list in the background and calls back on the main thread when it has changed.
     *
     * Anything that has already expired is marked as both seen and told the moment it lands, so
     * a phone that comes back after a month never shows — or rings about — news from three weeks
     * ago that it could not have acted on anyway.
     */
    /** Whether this build can check a signature — if not, no notice will ever be shown. */
    public static boolean sealReady(Context context) {
        return NoticeSeal.ready(context);
    }

    public static void refresh(Context context, Runnable whenChanged) {
        Context app = context.getApplicationContext();
        if (!Net.online(app)) return;
        IO.execute(() -> {
            if (fetchAndStore(app) && whenChanged != null) MAIN.post(whenChanged);
        });
    }

    /**
     * The same, on the calling thread — for the background worker, which has its own thread and
     * nothing to do but wait. Returns whether anything actually changed.
     */
    public static boolean refreshNow(Context context) {
        Context app = context.getApplicationContext();
        return Net.online(app) && fetchAndStore(app);
    }

    private static boolean fetchAndStore(Context app) {
        if (!NoticeSeal.ready(app)) {
            note(app, "این نسخه کلیدِ امضا ندارد — هیچ خبری نمایش داده نمی‌شود");
            return false;
        }

        String fresh = Net.text(URL);
        if (fresh == null) {
            note(app, "فایلِ خبرها از هاست نیامد (notices.json)");
            return false;
        }

        // The signature decides, and it decides before anything is read out of the file. A host
        // that has been taken over can serve any notices.json it likes; what it cannot do is
        // sign one, because the key that signs is on the teacher's machine.
        String signature = Net.text(SIGNATURE_URL);
        if (signature == null) {
            note(app, "امضا روی هاست نیست (notices.json.sig)");
            return false;
        }
        if (!NoticeSeal.verify(app, fresh, signature)) {
            note(app, "امضا با فایل نمی‌خواند — یا فایل دستی عوض شده، یا با کلیدِ دیگری امضا شده");
            return false;
        }

        if (parse(fresh).isEmpty() && !fresh.contains("notices")) {
            note(app, "آنچه آمد فایلِ خبرها نبود");
            return false;
        }
        if (fresh.equals(readCache(app))) {
            note(app, "چیزِ تازه‌ای نبود (" + parse(fresh).size() + " خبر، همان‌ها)");
            return false;
        }
        writeCache(app, fresh);
        burySomethingMissed(app, fresh);
        note(app, parse(fresh).size() + " خبر گرفته شد");
        return true;
    }

    /**
     * What happened on the last look, in one sentence a parent can read.
     *
     * Every way this can fail is silent by design — an unverifiable file is thrown away without
     * a word to the child, which is right — and that makes «I published a notice and nothing
     * happened» impossible to answer from the outside. So the reason is written down, and the
     * parent panel shows it.
     */
    private static void note(Context context, String what) {
        prefs(context).edit()
            .putLong(LAST_CHECK, System.currentTimeMillis())
            .putString(LAST_RESULT, what)
            .apply();
    }

    /** «۳ دقیقه پیش: ۱ خبر گرفته شد», or empty before the first look. */
    public static String lastCheck(Context context) {
        long at = prefs(context).getLong(LAST_CHECK, 0);
        String what = prefs(context).getString(LAST_RESULT, "");
        if (at == 0 || what.isEmpty()) return "";
        long ago = Math.max(0, System.currentTimeMillis() - at) / 60000;
        String when = ago < 1 ? "همین الان" : ago < 60 ? PersianDigits.fa(ago) + " دقیقه پیش"
                    : PersianDigits.fa(ago / 60) + " ساعت پیش";
        return when + " — " + what;
    }

    /**
     * Drops the cached copy so the next look treats whatever is on the host as new.
     *
     * Only for «بررسی کن» in the parent panel: the ordinary check compares bytes and does
     * nothing when they match, which is right for a quarter-hourly poll and useless to someone
     * standing there asking why their notice has not arrived.
     */
    public static void forget(Context context) {
        File file = cacheFile(context.getApplicationContext());
        if (file.exists()) file.delete();
    }

    /** How many are on the device and still worth showing, whatever the child has seen. */
    public static int countHere(Context context) {
        return current(context).size();
    }

    /** Quietly retires every notice whose moment passed while the phone was away. */
    private static void burySomethingMissed(Context context, String json) {
        long now = System.currentTimeMillis();
        SharedPreferences.Editor edit = prefs(context).edit();
        for (Notice notice : parse(json)) {
            if (notice.until > 0 && now > notice.until) {
                edit.putBoolean(SEEN + "_" + notice.id, true)
                    .putBoolean(TOLD + "_" + notice.id, true);
            }
        }
        edit.apply();
    }

    static List<Notice> parse(String json) {
        List<Notice> out = new ArrayList<>();
        if (json == null || json.isEmpty()) return out;
        try {
            JSONObject root = new JSONObject(json);
            JSONArray arr = root.optJSONArray("notices");
            if (arr == null) arr = root.optJSONArray("items");
            for (int i = 0; arr != null && i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String id = o.optString("id", "");
                String title = o.optString("title", "");
                if (id.isEmpty() || title.isEmpty()) continue;   // nothing to show, nothing to keep
                out.add(new Notice(id, title,
                    o.optString("body", o.optString("content", "")),
                    o.optString("image", o.optString("imageUrl", "")),
                    o.optString("imageSha256", ""),
                    o.optString("link", ""),
                    o.optString("screen", ""),
                    o.optLong("from", 0L),
                    o.optLong("until", 0L)));
            }
        } catch (Exception broken) {
            // a half-uploaded file on the host is not worth a crash on a child's phone
        }
        return out;
    }

    private static File cacheFile(Context context) {
        return new File(context.getFilesDir(), CACHED);
    }

    private static String readCache(Context context) {
        File file = cacheFile(context.getApplicationContext());
        if (!file.exists()) return null;
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException unreadable) {
            return null;
        }
    }

    private static void writeCache(Context context, String json) {
        try (FileOutputStream out = new FileOutputStream(cacheFile(context))) {
            out.write(json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            // it will simply be fetched again next time
        }
    }
}
