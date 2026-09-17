package com.hoohoomath.app.data;

/**
 * Every voice lesson in the book, looked up by chapter and section. All eight chapters are
 * taught: forty sections, each with its own narration, animation and questions.
 */
public final class Lessons {
    private Lessons() {}

    /** The lesson for this section, or null when the book has no lesson for it. */
    public static LessonScript forSection(int chapter, int section) {
        switch (chapter) {
            case 0: return Chapter1Lessons.forSection(section);
            case 1: return Chapter2Lessons.forSection(section);
            case 2: return Chapter3Lessons.forSection(section);
            case 3: return Chapter4Lessons.forSection(section);
            case 4: return Chapter5Lessons.forSection(section);
            case 5: return Chapter6Lessons.forSection(section);
            case 6: return Chapter7Lessons.forSection(section);
            case 7: return Chapter8Lessons.forSection(section);
            default: return null;
        }
    }

    public static boolean has(int chapter, int section) {
        return forSection(chapter, section) != null;
    }
}
