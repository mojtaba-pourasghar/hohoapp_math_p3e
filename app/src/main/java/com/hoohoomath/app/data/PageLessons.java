package com.hoohoomath.app.data;

import java.util.ArrayList;
import java.util.List;

/**
 * The printed book taught page by page. Each page of the real book is its own lesson, sitting
 * next to the section lessons rather than in a separate place — the child works through the
 * book's own pages, in the book's own order.
 *
 * All eight chapters are here: صفحه‌های ۷ تا ۱۵۰ of the 1404 edition.
 */
public final class PageLessons {
    private PageLessons() {}

    /** The lesson for one printed page, or null when that page has none. */
    public static LessonScript forPage(int page) {
        LessonScript script = Chapter1Pages.forPage(page);
        if (script == null) script = Chapter2Pages.forPage(page);
        if (script == null) script = Chapter3Pages.forPage(page);
        if (script == null) script = Chapter4Pages.forPage(page);
        if (script == null) script = Chapter5Pages.forPage(page);
        if (script == null) script = Chapter6Pages.forPage(page);
        if (script == null) script = Chapter7Pages.forPage(page);
        if (script == null) script = Chapter8Pages.forPage(page);
        return script;
    }

    /** The book pages of one chapter that have a lesson, in book order. */
    public static List<Integer> pagesOfChapter(int chapter) {
        switch (chapter) {
            case 0: return Chapter1Pages.pages();
            case 1: return Chapter2Pages.pages();
            case 2: return Chapter3Pages.pages();
            case 3: return Chapter4Pages.pages();
            case 4: return Chapter5Pages.pages();
            case 5: return Chapter6Pages.pages();
            case 6: return Chapter7Pages.pages();
            case 7: return Chapter8Pages.pages();
            default: return new ArrayList<>();
        }
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

    /** Every page of the book that has a lesson, chapter after chapter, in book order. */
    public static List<Integer> allPages() {
        List<Integer> out = new ArrayList<>();
        for (int chapter = 0; chapter < Book.CHAPTER_COUNT; chapter++) {
            out.addAll(pagesOfChapter(chapter));
        }
        return out;
    }

    /**
     * The page that comes after this one in the printed book, or 0 at the last page. It walks
     * across chapter borders, so «برویم صفحه‌ی بعد کتاب» carries the child from the end of one
     * chapter straight into the opening page of the next.
     */
    public static int nextPage(int page) {
        List<Integer> pages = allPages();
        int at = pages.indexOf(page);
        return at >= 0 && at < pages.size() - 1 ? pages.get(at + 1) : 0;
    }
}
