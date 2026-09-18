package com.hoohoomath.app.data;

import java.util.ArrayList;
import java.util.List;

/**
 * The printed book taught page by page. Each page of the real book is its own lesson, sitting
 * next to the section lessons rather than in a separate place — the child works through the
 * book's own pages, in the book's own order.
 *
 * Chapters are added here as their pages are written.
 */
public final class PageLessons {
    private PageLessons() {}

    public static LessonScript forPage(int page) {
        return Chapter1Pages.forPage(page);
    }

    /** The book pages of one chapter that have a lesson, in book order. */
    public static List<Integer> pagesOfChapter(int chapter) {
        if (chapter == 0) return Chapter1Pages.pages();
        return new ArrayList<>();
    }

    /** The pages that belong to one section of a chapter, in book order. */
    public static List<Integer> pagesOfSection(int chapter, int section) {
        List<Integer> out = new ArrayList<>();
        for (int page : pagesOfChapter(chapter)) {
            LessonScript script = forPage(page);
            if (script != null && script.section == section) out.add(page);
        }
        return out;
    }

    public static boolean hasPages(int chapter) {
        return !pagesOfChapter(chapter).isEmpty();
    }
}
