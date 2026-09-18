package com.hoohoomath.app.tts;

import com.hoohoomath.app.data.SpokenNumber;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Lets هوهو read a generated question in her own recorded voice.
 *
 * A question is always the same thing: fixed pieces of wording from the question templates with
 * numbers dropped in. So the wording is split off and each distinct piece gets its own recording
 * (tools/make_manifest.py collects them straight from QuestionGenerator), while the numbers are
 * said from the recorded number words. Reading a question is then just playing a few clips in a
 * row — no robot voice, and no recording needed per question.
 */
public final class QuestionVoice {
    private QuestionVoice() {}

    /** The clips that read this question, or null when it has something we cannot say. */
    public static List<String> clips(String question) {
        if (question == null || question.isEmpty()) return null;
        List<String> out = new ArrayList<>();

        int i = 0;
        int n = question.length();
        while (i < n) {
            if (isDigit(question.charAt(i))) {
                int start = i;
                while (i < n && isDigit(question.charAt(i))) i++;
                Long value = parse(question.substring(start, i));
                if (value == null) return null;
                List<String> said = SpokenNumber.clips(value);
                if (said == null) return null;
                out.addAll(said);
            } else {
                int start = i;
                while (i < n && !isDigit(question.charAt(i))) i++;
                String piece = question.substring(start, i).trim();
                if (!piece.isEmpty()) out.add(phraseKey(piece));
            }
        }
        return out.isEmpty() ? null : out;
    }

    /** The res/raw name of a piece of wording — the same name tools/make_manifest.py writes. */
    public static String phraseKey(String piece) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] digest = sha1.digest(piece.getBytes(StandardCharsets.UTF_8));
            // hand-rolled hex: the app runs in a Persian locale, and formatting must never
            // come back with Persian digits — the name has to match the file on disk exactly
            char[] hexDigits = "0123456789abcdef".toCharArray();
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                hex.append(hexDigits[(digest[i] >> 4) & 0xF]).append(hexDigits[digest[i] & 0xF]);
            }
            return "q_" + hex;
        } catch (Exception e) {
            return "q_none";
        }
    }

    private static boolean isDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= '۰' && c <= '۹') || (c >= '٠' && c <= '٩');
    }

    private static Long parse(String digits) {
        StringBuilder ascii = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            char c = digits.charAt(i);
            if (c >= '۰' && c <= '۹') ascii.append((char) ('0' + (c - '۰')));
            else if (c >= '٠' && c <= '٩') ascii.append((char) ('0' + (c - '٠')));
            else ascii.append(c);
        }
        try {
            return Long.parseLong(ascii.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
