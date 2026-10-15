package com.hoohoomath.app.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import com.hoohoomath.app.data.NoticeSeal;
import com.hoohoomath.app.data.Notices;
import com.hoohoomath.app.net.Net;
import com.hoohoomath.app.util.Hash;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * The picture on a notice.
 *
 * Kept beside the clips and the worksheets, fetched once and left on the device: a notice that
 * is shown again tomorrow must not cost a second download, and a phone that has gone offline in
 * the meantime should still show the picture it already has.
 *
 * Decoded at a size that suits a phone screen rather than at whatever the teacher uploaded —
 * a photo straight off a camera is several thousand pixels wide, and decoding that whole into a
 * dialog is how a cheap phone runs out of memory.
 *
 * And weighed before it is shown. The signature on notices.json covers the *name* of the
 * picture, not the picture, which sits beside it as its own file — so the signed text carries
 * the picture's SHA-256 and anything that does not match it is thrown away. A host that has been
 * taken over could otherwise leave the words alone and swap only the image.
 */
public final class NoticeArt {

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** Wide enough to fill any phone dialog, small enough to decode on the cheapest one. */
    private static final int WIDEST = 1080;

    private NoticeArt() {}

    private static File dir(Context context) {
        File dir = new File(context.getApplicationContext().getFilesDir(), "notices");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private static File fileFor(Context context, String url) {
        String name = Hash.sha256(url);
        int dot = url.lastIndexOf('.');
        String extension = dot > 0 && url.length() - dot <= 5 ? url.substring(dot) : ".img";
        return new File(dir(context), name + extension);
    }

    /** Whether it is already here — the answer that decides if a dialog waits or shows at once. */
    public static boolean have(Context context, Notices.Notice notice) {
        if (!notice.hasImage()) return false;
        File file = fileFor(context, notice.imageUrl());
        return file.exists() && file.length() > 256;
    }

    /**
     * Hands over the picture, or null if there is none or it could not be had.
     *
     * Always on the main thread, and never before the caller has returned, so a screen can set
     * itself up first and let the picture arrive into it.
     */
    public static void load(Context context, Notices.Notice notice, Consumer<Bitmap> whenReady) {
        Context app = context.getApplicationContext();
        if (!notice.hasImage()) {
            MAIN.post(() -> whenReady.accept(null));
            return;
        }
        String url = notice.imageUrl();
        IO.execute(() -> {
            File file = fileFor(app, url);
            if (!(file.exists() && file.length() > 256) && !Net.download(url, file)) {
                MAIN.post(() -> whenReady.accept(null));
                return;
            }
            if (!matches(file, notice.imageSha256)) {
                file.delete();      // not what was signed for; do not keep it, do not show it
                MAIN.post(() -> whenReady.accept(null));
                return;
            }
            Bitmap bitmap = decode(file);
            MAIN.post(() -> whenReady.accept(bitmap));
        });
    }

    /**
     * Whether the file on disk is the one the signed notice named.
     *
     * A notice published without a hash is from an older studio, and its picture is simply not
     * shown: the alternative is to display an unverified image because the text forgot to say
     * what it should look like, which is the same hole with extra steps.
     */
    private static boolean matches(File file, String expected) {
        if (expected == null || expected.isEmpty()) return false;
        try {
            return expected.equalsIgnoreCase(NoticeSeal.sha256(Files.readAllBytes(file.toPath())));
        } catch (IOException unreadable) {
            return false;
        }
    }

    private static Bitmap decode(File file) {
        try {
            BitmapFactory.Options measure = new BitmapFactory.Options();
            measure.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), measure);

            int sample = 1;
            while (measure.outWidth / sample > WIDEST) sample *= 2;

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        } catch (Throwable tooBig) {
            // a corrupt or enormous file is a missing picture, not a crash
            return null;
        }
    }
}
