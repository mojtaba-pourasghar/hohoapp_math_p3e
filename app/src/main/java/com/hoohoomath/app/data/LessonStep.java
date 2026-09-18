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

    // BUILD only: the child colours squares on a grid until the figure is right
    public final int buildRows;
    public final int buildCols;
    public final int buildTarget;      // how many squares the finished figure has
    public final int[] buildShape;     // squares per row of the book's own figure, bottom row first

    /** A second, fuller go at the same idea, offered behind the "یک مثال دیگر" button. */
    public final String exampleAudioKey;
    public final String exampleSay;

    private LessonStep(LessonKind kind, String audioKey, String say, String caption, StageSpec stage,
                       List<String> options, int correctIndex, String answerFa, String why,
                       String exampleAudioKey, String exampleSay,
                       int buildRows, int buildCols, int buildTarget, int[] buildShape) {
        this.kind = kind;
        this.audioKey = audioKey;
        this.say = say;
        this.caption = caption;
        this.stage = stage == null ? StageSpec.NONE : stage;
        this.options = options;
        this.correctIndex = correctIndex;
        this.answerFa = answerFa;
        this.why = why;
        this.exampleAudioKey = exampleAudioKey;
        this.exampleSay = exampleSay;
        this.buildRows = buildRows;
        this.buildCols = buildCols;
        this.buildTarget = buildTarget;
        this.buildShape = buildShape;
    }

    public boolean hasStage() {
        return stage != null && !stage.isNone();
    }

    public boolean hasExample() {
        return exampleSay != null && !exampleSay.isEmpty();
    }

    /** Attaches the extra example a child can ask for on this step. */
    public LessonStep withExample(String exampleAudioKey, String exampleSay) {
        return new LessonStep(kind, audioKey, say, caption, stage, options, correctIndex, answerFa, why,
            exampleAudioKey, exampleSay, buildRows, buildCols, buildTarget, buildShape);
    }

    public static LessonStep teach(String audioKey, String say, String caption, StageSpec stage) {
        return new LessonStep(LessonKind.TEACH, audioKey, say, caption, stage, null, -1, null, null, null, null, 0, 0, 0, null);
    }

    public static LessonStep mcq(String audioKey, String say, String caption, StageSpec stage,
                                 List<String> options, int correctIndex, String why) {
        return new LessonStep(LessonKind.MCQ, audioKey, say, caption, stage, options, correctIndex, null, why, null, null, 0, 0, 0, null);
    }

    public static LessonStep num(String audioKey, String say, String caption, StageSpec stage,
                                 String answerFa, String why) {
        return new LessonStep(LessonKind.NUM, audioKey, say, caption, stage, null, -1, answerFa, why, null, null, 0, 0, 0, null);
    }

    /**
     * The child builds the figure themselves: they colour squares on a grid until it has as many
     * squares as the next figure of the pattern, and هوهو checks it. `shape` is the book's own
     * arrangement (squares per row, bottom row first), shown once the count is right.
     */
    public static LessonStep build(String audioKey, String say, String caption, StageSpec stage,
                                   int rows, int cols, int target, int[] shape, String why) {
        return new LessonStep(LessonKind.BUILD, audioKey, say, caption, stage, null, -1, null, why,
            null, null, rows, cols, target, shape);
    }

    public static LessonStep done(String audioKey, String say, String caption) {
        return new LessonStep(LessonKind.DONE, audioKey, say, caption, StageSpec.NONE, null, -1, null, null, null, null, 0, 0, 0, null);
    }
}
