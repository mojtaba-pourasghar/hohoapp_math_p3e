package com.hoohoomath.app.data;

/**
 * Describes the little animated scene that plays while هوهو explains a step — the moving
 * picture a teacher would draw on the board. One spec per lesson step; LessonStageView
 * knows how to draw and animate each kind.
 */
public class StageSpec {

    public enum Kind {
        NONE,
        /** Groups of dots appear one by one while the running total counts up (۲ ، ۴ ، ۶ …). */
        COUNT_GROUPS,
        /** A growing staircase of bars with the +۲ ، +۳ ، +۴ jumps drawn between them. */
        STEP_PATTERN,
        /** A number travels into a "+۵" machine and the answer pops out the other side. */
        MACHINE,
        /** A clock face whose hands turn to the hour, then the 24-hour number appears. */
        CLOCK,
        /** Half a shape is coloured, then mirrored across the fold line. */
        MIRROR
    }

    public final Kind kind;
    public final int[] values;   // STEP_PATTERN: the sequence; others: unused
    public final int a;          // COUNT_GROUPS groups | MACHINE rule | CLOCK hour | MIRROR cells per half
    public final int b;          // COUNT_GROUPS per-group | MACHINE input | CLOCK pm flag (1/0)

    private StageSpec(Kind kind, int[] values, int a, int b) {
        this.kind = kind;
        this.values = values;
        this.a = a;
        this.b = b;
    }

    public static final StageSpec NONE = new StageSpec(Kind.NONE, null, 0, 0);

    public static StageSpec countGroups(int groups, int perGroup) {
        return new StageSpec(Kind.COUNT_GROUPS, null, groups, perGroup);
    }

    public static StageSpec stepPattern(int... values) {
        return new StageSpec(Kind.STEP_PATTERN, values, 0, 0);
    }

    public static StageSpec machine(int rule, int input) {
        return new StageSpec(Kind.MACHINE, null, rule, input);
    }

    public static StageSpec clock(int hour12, boolean pm) {
        return new StageSpec(Kind.CLOCK, null, hour12, pm ? 1 : 0);
    }

    public static StageSpec mirror(int cellsPerHalf) {
        return new StageSpec(Kind.MIRROR, null, cellsPerHalf, 0);
    }

    public boolean isNone() {
        return kind == Kind.NONE;
    }
}
