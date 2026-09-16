package com.hoohoomath.app.data;

import static com.hoohoomath.app.data.PersianDigits.fa;

import java.util.ArrayList;
import java.util.List;

/** Builds quiz sessions and computes which chapters/sections the teacher's pace has unlocked. */
public final class QuizBuilder {

    private QuizBuilder() {}

    private static final int[] EXAM_OFFSETS = {1, -1, 2, -2, 3, 10, -10, 5, 4};

    /** Which section indices (0-4) of chapter `ch` are unlocked, given the teacher has taught up to (taughtCh, taughtSec). */
    public static List<Integer> allowedSections(int ch, int taughtCh, int taughtSec) {
        List<Integer> out = new ArrayList<>();
        if (ch < taughtCh) {
            for (int i = 0; i < Book.SECTIONS_PER_CHAPTER; i++) out.add(i);
            return out;
        }
        if (ch > taughtCh) return out;
        for (int i = 0; i <= taughtSec; i++) out.add(i);
        return out;
    }

    public static boolean isChapterOpen(int ch, int taughtCh, int taughtSec) {
        return !allowedSections(ch, taughtCh, taughtSec).isEmpty();
    }

    /**
     * Returns null if the requested section/chapter hasn't been taught yet.
     *
     * `round` shifts the generator's question index, so asking for round 1, 2, 3… of the same
     * section yields a fresh set of questions each time instead of repeating the first fifteen.
     */
    public static QuizSession build(QuizMode mode, int ch, int option, int taughtCh, int taughtSec, int round) {
        List<Integer> secs = allowedSections(ch, taughtCh, taughtSec);
        if (secs.isEmpty()) return null;

        int lv = mode == QuizMode.PRACTICE ? 0 : option;
        int count = mode == QuizMode.EXAM ? 8 : mode == QuizMode.WORKSHEET ? 30 : 15;

        List<QuestionItem> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int sec = mode == QuizMode.PRACTICE ? option : secs.get(i % secs.size());
            int itemLv = mode == QuizMode.PRACTICE ? (i < 5 ? 0 : i < 10 ? 1 : 2) : lv;
            QuestionItem it = QuestionGenerator.item(ch, sec, i + round * count, itemLv);

            if (mode == QuizMode.EXAM) {
                long base = it.rawAnswer;
                List<Long> opts = new ArrayList<>();
                opts.add(base);
                int k = 0;
                while (opts.size() < 4 && k < EXAM_OFFSETS.length) {
                    long cand = base + EXAM_OFFSETS[k];
                    if (cand > 0 && !opts.contains(cand)) opts.add(cand);
                    k++;
                }
                int[] order = {i % 4, (i + 1) % 4, (i + 2) % 4, (i + 3) % 4};
                List<String> options = new ArrayList<>();
                for (int idx : order) {
                    long val = idx < opts.size() ? opts.get(idx) : base;
                    options.add(fa(val));
                }
                it.options = options;
            }
            items.add(it);
        }
        return new QuizSession(mode, ch, option, round, items);
    }
}
