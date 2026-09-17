package com.hoohoomath.app.data;

import android.content.Context;
import android.content.res.AssetManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The printed textbook, page by page: every page's own sentences in reading order, next to the
 * picture of that page in assets/book/p###.jpg. Both are produced from the real PDF by
 * tools/extract_book.py, so what the app shows and reads is the book itself.
 */
public final class BookText {

    public static final int FIRST_PAGE = 1;

    private static BookText instance;

    private final List<List<String>> pages = new ArrayList<>();

    private BookText(Context context) {
        String raw = readAsset(context, "book/pages.json");
        if (raw == null) return;
        try {
            JSONObject root = new JSONObject(raw);
            JSONArray arr = root.getJSONArray("pages");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject page = arr.getJSONObject(i);
                JSONArray lines = page.optJSONArray("lines");
                List<String> out = new ArrayList<>();
                if (lines != null) {
                    for (int k = 0; k < lines.length(); k++) {
                        String line = lines.optString(k, "").trim();
                        if (!line.isEmpty()) out.add(line);
                    }
                }
                pages.add(out);
            }
        } catch (JSONException ignored) {
        }
    }

    public static synchronized BookText get(Context context) {
        if (instance == null) instance = new BookText(context.getApplicationContext());
        return instance;
    }

    public int pageCount() {
        return pages.isEmpty() ? 0 : pages.size();
    }

    /** The sentences printed on this page, in reading order; empty for a picture-only page. */
    public List<String> lines(int page) {
        int i = page - FIRST_PAGE;
        if (i < 0 || i >= pages.size()) return Collections.emptyList();
        return pages.get(i);
    }

    /** assets/book/p012.jpg for page 12. */
    public static String imagePath(int page) {
        return String.format("book/p%03d.jpg", page);
    }

    private static String readAsset(Context context, String path) {
        AssetManager assets = context.getAssets();
        try (InputStream in = assets.open(path)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(1024, in.available()));
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);
            return new String(out.toByteArray(), "UTF-8");
        } catch (IOException e) {
            return null;
        }
    }
}
