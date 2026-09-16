package com.hoohoomath.app.data;

import java.util.List;

/** One step of a voice-narrated lesson: هوهو explains, then (sometimes) asks and checks understanding. */
public class LessonStep {
    public final LessonKind kind;
    public final String say;      // what هوهو speaks (TTS)
    public final String caption;  // short on-screen caption reinforcing the spoken line
    public final List<String> options; // MCQ only
    public final int correctIndex;     // MCQ only
    public final String answerFa;      // NUM only, expected Persian-digit answer
    public final String why;           // feedback/explanation shown after answering

    /**
     * Optional counting visual, mirroring the book's "count in groups" pictures:
     * `visualGroups` clusters of `perGroup` dots each, labelled with the running total
     * (3 groups of 5 => ۵ ، ۱۰ ، ۱۵). Zero means this step has no picture.
     */
    public final int visualGroups;
    public final int perGroup;

    private LessonStep(LessonKind kind, String say, String caption, List<String> options, int correctIndex,
                       String answerFa, String why, int visualGroups, int perGroup) {
        this.kind = kind;
        this.say = say;
        this.caption = caption;
        this.options = options;
        this.correctIndex = correctIndex;
        this.answerFa = answerFa;
        this.why = why;
        this.visualGroups = visualGroups;
        this.perGroup = perGroup;
    }

    public boolean hasVisual() {
        return visualGroups > 0 && perGroup > 0;
    }

    public static LessonStep teach(String say, String caption) {
        return teach(say, caption, 0, 0);
    }

    public static LessonStep teach(String say, String caption, int visualGroups, int perGroup) {
        return new LessonStep(LessonKind.TEACH, say, caption, null, -1, null, null, visualGroups, perGroup);
    }

    public static LessonStep mcq(String say, String caption, List<String> options, int correctIndex, String why) {
        return mcq(say, caption, options, correctIndex, why, 0, 0);
    }

    public static LessonStep mcq(String say, String caption, List<String> options, int correctIndex, String why,
                                 int visualGroups, int perGroup) {
        return new LessonStep(LessonKind.MCQ, say, caption, options, correctIndex, null, why, visualGroups, perGroup);
    }

    public static LessonStep num(String say, String caption, String answerFa, String why) {
        return num(say, caption, answerFa, why, 0, 0);
    }

    public static LessonStep num(String say, String caption, String answerFa, String why, int visualGroups, int perGroup) {
        return new LessonStep(LessonKind.NUM, say, caption, null, -1, answerFa, why, visualGroups, perGroup);
    }

    public static LessonStep done(String say, String caption) {
        return new LessonStep(LessonKind.DONE, say, caption, null, -1, null, null, 0, 0);
    }
}
