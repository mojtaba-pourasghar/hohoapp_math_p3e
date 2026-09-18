package com.hoohoomath.app.data;

import static com.hoohoomath.app.data.PersianDigits.fa;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the question sets. Nothing is locked: every chapter and every section is open, and the
 * teacher's position is only ever shown as a bookmark ("the class is here"), never as a gate.
 */
public final class QuizBuilder {

    private QuizBuilder() {}

    private static final int[] EXAM_OFFSETS = {1, -1, 2, -2, 3, 10, -10, 5, 4};

    /** Every section of a chapter, in order. */
    public static List<Integer> allSections() {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < Book.SECTIONS_PER_CHAPTER; i++) out.add(i);
        return out;
    }

    /**
     * `round` shifts the generator's question index, so asking for round 1, 2, 3… of the same
     * section yields a fresh set each time instead of repeating the first fifteen.
     */
    /**
     * Each mode starts the generator at a different place, so the exam never repeats the
     * worksheet's questions — before this they shared the first fifteen.
     */
    private static int seedOffset(QuizMode mode) {
        switch (mode) {
            case WORKSHEET: return 137;
            case EXAM: return 613;
            default: return 0;
        }
    }

    public static QuizSession build(QuizMode mode, int ch, int option, int round) {
        List<Integer> secs = allSections();
        int lv = mode == QuizMode.PRACTICE ? 0 : option;
        // a chapter exam covers the whole chapter: three questions from each of the five sections
        int count = mode == QuizMode.EXAM ? 15 : mode == QuizMode.WORKSHEET ? 30 : 15;

        List<QuestionItem> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // the worksheet walks the sections in order; the exam jumps between them, so even
            // the running order feels like a different paper
            int sec = mode == QuizMode.PRACTICE ? option
                : mode == QuizMode.EXAM ? secs.get((i * 3 + 1) % secs.size())
                : secs.get(i % secs.size());
            int itemLv = mode == QuizMode.PRACTICE ? (i < 5 ? 0 : i < 10 ? 1 : 2) : lv;
            QuestionItem it = QuestionGenerator.item(ch, sec,
                seedOffset(mode) + i + round * count, itemLv);

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
