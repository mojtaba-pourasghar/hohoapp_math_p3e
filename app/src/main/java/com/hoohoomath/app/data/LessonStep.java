package com.hoohoomath.app.data;

import java.util.List;

/**
 * One step of a voice-narrated lesson: هوهو explains over a moving picture, then (on the
 * interactive steps) asks the child and checks the answer.
 *
 * `audioKey` names the recorded narration file in res/raw; when that file isn't there yet the
 * app speaks `say` with text-to-speech instead. Keys are listed in res/raw/audio_manifest.txt.
 */
public class LessonStep {
    public final LessonKind kind;
    public final String audioKey;
    public final String say;      // exactly what هوهو says
    public final String caption;  // short on-screen caption reinforcing the spoken line
    public final StageSpec stage; // the animation that plays while this line is spoken
    public final List<String> options; // MCQ only
    public final int correctIndex;     // MCQ only
    public final String answerFa;      // NUM only, expected Persian-digit answer
    public final String why;           // feedback shown after answering

    private LessonStep(LessonKind kind, String audioKey, String say, String caption, StageSpec stage,
                       List<String> options, int correctIndex, String answerFa, String why) {
        this.kind = kind;
        this.audioKey = audioKey;
        this.say = say;
        this.caption = caption;
        this.stage = stage == null ? StageSpec.NONE : stage;
        this.options = options;
        this.correctIndex = correctIndex;
        this.answerFa = answerFa;
        this.why = why;
    }

    public boolean hasStage() {
        return stage != null && !stage.isNone();
    }

    public static LessonStep teach(String audioKey, String say, String caption, StageSpec stage) {
        return new LessonStep(LessonKind.TEACH, audioKey, say, caption, stage, null, -1, null, null);
    }

    public static LessonStep mcq(String audioKey, String say, String caption, StageSpec stage,
                                 List<String> options, int correctIndex, String why) {
        return new LessonStep(LessonKind.MCQ, audioKey, say, caption, stage, options, correctIndex, null, why);
    }

    public static LessonStep num(String audioKey, String say, String caption, StageSpec stage,
                                 String answerFa, String why) {
        return new LessonStep(LessonKind.NUM, audioKey, say, caption, stage, null, -1, answerFa, why);
    }

    public static LessonStep done(String audioKey, String say, String caption) {
        return new LessonStep(LessonKind.DONE, audioKey, say, caption, StageSpec.NONE, null, -1, null, null);
    }
}
