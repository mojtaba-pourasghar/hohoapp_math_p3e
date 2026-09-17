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
        MIRROR,
        /** Thousand-cubes, hundred-plates, ten-rods and ones build a number up. */
        PLACE_VALUE,
        /** A number line with ticks; the marks land on it one after another. */
        NUMBER_LINE,
        /** ۵۰۰-toman notes and ۱۰۰-toman coins pile up with the running total. */
        MONEY,
        /** A bar or a circle divided into equal parts, some of them coloured. */
        FRACTION,
        /** Two fractions side by side, for equivalence and comparison. */
        FRACTION_PAIR,
        /** A rows × cols array of unit squares, optionally ringed into equal groups. */
        ARRAY,
        /** خط، نیم‌خط و پاره‌خط drawn one under the other. */
        LINE_KINDS,
        /** A rectangle whose sides are labelled and whose outline is traced (محیط, مساحت). */
        PERIMETER,
        /** Vertical addition or subtraction in place-value columns, carry and all. */
        COLUMN_OP,
        /** A bar chart growing out of its data. */
        BAR_CHART,
        /** A number and its ten-times answer, with the extra zero sliding in. */
        TIMES_TEN
    }

    public final Kind kind;
    public final int[] values;
    public final int a;
    public final int b;
    public final int c;

    private StageSpec(Kind kind, int[] values, int a, int b, int c) {
        this.kind = kind;
        this.values = values;
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public static final StageSpec NONE = new StageSpec(Kind.NONE, null, 0, 0, 0);

    public static StageSpec countGroups(int groups, int perGroup) {
        return new StageSpec(Kind.COUNT_GROUPS, null, groups, perGroup, 0);
    }

    public static StageSpec stepPattern(int... values) {
        return new StageSpec(Kind.STEP_PATTERN, values, 0, 0, 0);
    }

    public static StageSpec machine(int rule, int input) {
        return new StageSpec(Kind.MACHINE, null, rule, input, 0);
    }

    public static StageSpec clock(int hour12, boolean pm) {
        return new StageSpec(Kind.CLOCK, null, hour12, pm ? 1 : 0, 0);
    }

    public static StageSpec mirror(int cellsPerHalf) {
        return new StageSpec(Kind.MIRROR, null, cellsPerHalf, 0, 0);
    }

    /** Builds `number` out of thousands, hundreds, tens and ones. */
    public static StageSpec placeValue(int number) {
        return new StageSpec(Kind.PLACE_VALUE, null, number, 0, 0);
    }

    /** A number line from `from` to `to`, with each of `marks` landing in turn. */
    public static StageSpec numberLine(int from, int to, int... marks) {
        return new StageSpec(Kind.NUMBER_LINE, marks, from, to, 0);
    }

    public static StageSpec money(int notes500, int coins100) {
        return new StageSpec(Kind.MONEY, null, notes500, coins100, 0);
    }

    /** A bar (circle = false) or a pie (circle = true) split into `parts`, `shaded` coloured. */
    public static StageSpec fraction(int parts, int shaded, boolean circle) {
        return new StageSpec(Kind.FRACTION, null, parts, shaded, circle ? 1 : 0);
    }

    public static StageSpec fractionPair(int parts1, int shaded1, int parts2, int shaded2) {
        return new StageSpec(Kind.FRACTION_PAIR, new int[]{parts1, shaded1, parts2, shaded2}, 0, 0, 0);
    }

    /** `groupSize` > 0 rings the squares into equal groups, the way تقسیم is drawn. */
    public static StageSpec array(int rows, int cols, int groupSize) {
        return new StageSpec(Kind.ARRAY, null, rows, cols, groupSize);
    }

    public static StageSpec lineKinds() {
        return new StageSpec(Kind.LINE_KINDS, null, 0, 0, 0);
    }

    /** A `length` × `width` rectangle; `showArea` fills it with unit squares instead of tracing it. */
    public static StageSpec perimeter(int length, int width, boolean showArea) {
        return new StageSpec(Kind.PERIMETER, null, length, width, showArea ? 1 : 0);
    }

    public static StageSpec columnOp(int x, int y, boolean subtract) {
        return new StageSpec(Kind.COLUMN_OP, null, x, y, subtract ? 1 : 0);
    }

    public static StageSpec barChart(int... values) {
        return new StageSpec(Kind.BAR_CHART, values, 0, 0, 0);
    }

    public static StageSpec timesTen(int n) {
        return new StageSpec(Kind.TIMES_TEN, null, n, 0, 0);
    }

    public boolean isNone() {
        return kind == Kind.NONE;
    }
}
