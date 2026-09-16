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

    private LessonStep(LessonKind kind, String say, String caption, List<String> options, int correctIndex, String answerFa, String why) {
        this.kind = kind;
        this.say = say;
        this.caption = caption;
        this.options = options;
        this.correctIndex = correctIndex;
        this.answerFa = answerFa;
        this.why = why;
    }

    public static LessonStep teach(String say, String caption) {
        return new LessonStep(LessonKind.TEACH, say, caption, null, -1, null, null);
    }

    public static LessonStep mcq(String say, String caption, List<String> options, int correctIndex, String why) {
        return new LessonStep(LessonKind.MCQ, say, caption, options, correctIndex, null, why);
    }

    public static LessonStep num(String say, String caption, String answerFa, String why) {
        return new LessonStep(LessonKind.NUM, say, caption, null, -1, answerFa, why);
    }

    public static LessonStep done(String say, String caption) {
        return new LessonStep(LessonKind.DONE, say, caption, null, -1, null, null);
    }
}
