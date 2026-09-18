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
        TIMES_TEN,

        // ── the figures the book itself draws, page by page ──
        /** The moon thinning to a crescent, filling to a circle and thinning again. */
        MOON_PHASES,
        /** A block of flats: floors stacked up, each with the same number of units. */
        BUILDING,
        /** A table filling in cell by cell, with the +n jump drawn between the cells. */
        TABLE_PATTERN,
        /** A grid of little squares ringed into equal groups, with the addition written out. */
        GRID_GROUPS,
        /** A month's calendar with every seventh day marked, the way شنبه‌ها repeat. */
        CALENDAR,
        /** The same addends rearranged into friendly pairs, landing on the same total. */
        SUM_REORDER,
        /** A clock face with the afternoon ring ۱۳ to ۲۴ around the outside. */
        CLOCK24,
        /** The ۰ to ۲۴ strip of a whole day, coloured part by part. */
        DAY_STRIP,
        /** Two machines joined together: the answer of the first feeds the second. */
        MACHINE_CHAIN,
        /** A shape with its line of symmetry drawn, then folded over it. */
        SYMMETRY_LINES,
        /** The unfolded net of a cube, folding itself up into the cube. */
        CUBE_NET
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

    // ── the book's own figures ──

    public static StageSpec moonPhases() {
        return new StageSpec(Kind.MOON_PHASES, null, 0, 0, 0);
    }

    /** `floors` storeys with `unitsPerFloor` flats on each, filling from the ground up. */
    public static StageSpec building(int floors, int unitsPerFloor) {
        return new StageSpec(Kind.BUILDING, null, floors, unitsPerFloor, 0);
    }

    /** The table's own numbers, with `step` written as the jump between them. */
    public static StageSpec tablePattern(int step, int... values) {
        return new StageSpec(Kind.TABLE_PATTERN, values, step, 0, 0);
    }

    /** A `rows` × `cols` grid of squares ringed into groups of `groupSize`. */
    public static StageSpec gridGroups(int rows, int cols, int groupSize) {
        return new StageSpec(Kind.GRID_GROUPS, null, rows, cols, groupSize);
    }

    /** A month of `days` starting on weekday `startDay` (0 = شنبه), every 7th day marked. */
    public static StageSpec calendar(int days, int startDay) {
        return new StageSpec(Kind.CALENDAR, null, days, startDay, 0);
    }

    public static StageSpec sumReorder(int... addends) {
        return new StageSpec(Kind.SUM_REORDER, addends, 0, 0, 0);
    }

    /** `hour24` on the clock, with the afternoon ring shown when it is ۱۳ or later. */
    public static StageSpec clock24(int hour24) {
        return new StageSpec(Kind.CLOCK24, null, hour24, 0, 0);
    }

    /** The day strip with the stretch from `fromHour` to `toHour` coloured in. */
    public static StageSpec dayStrip(int fromHour, int toHour) {
        return new StageSpec(Kind.DAY_STRIP, null, fromHour, toHour, 0);
    }

    /** input → (+/− rule1) → (+/− rule2) → out. */
    public static StageSpec machineChain(int input, int rule1, int rule2) {
        return new StageSpec(Kind.MACHINE_CHAIN, new int[]{rule1, rule2}, input, 0, 0);
    }

    /** `cellsPerHalf` coloured cells on one side of the fold, `axes` lines of symmetry. */
    public static StageSpec symmetryLines(int cellsPerHalf, int axes) {
        return new StageSpec(Kind.SYMMETRY_LINES, null, cellsPerHalf, axes, 0);
    }

    public static StageSpec cubeNet() {
        return new StageSpec(Kind.CUBE_NET, null, 0, 0, 0);
    }

    public boolean isNone() {
        return kind == Kind.NONE;
    }
}
