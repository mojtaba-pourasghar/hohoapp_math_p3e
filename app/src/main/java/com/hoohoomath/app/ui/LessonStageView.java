package com.hoohoomath.app.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.hoohoomath.app.data.PersianDigits;
import com.hoohoomath.app.data.StageSpec;

/**
 * The moving picture هوهو draws while explaining: dots grouped and counted, a staircase of
 * numbers growing, a number fed through a machine, clock hands turning to the afternoon hour,
 * a shape folded over its line of symmetry. The scene animates from 0 to 1 alongside the
 * narration so the child sees the idea happen rather than just hearing about it.
 */
public class LessonStageView extends View {

    private static final int TEAL = Color.parseColor("#3AA79A");
    private static final int ORANGE = Color.parseColor("#E8973A");
    private static final int ORANGE_DARK = Color.parseColor("#D8811F");
    private static final int PINK = Color.parseColor("#D94F7A");
    private static final int INK = Color.parseColor("#2E3B36");
    private static final int MUTED = Color.parseColor("#7A8A83");
    private static final int CARD = Color.WHITE;
    private static final int CARD_EDGE = Color.parseColor("#E8EEE6");
    private static final int FACE = Color.parseColor("#FDF4E3");

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dashed = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private StageSpec spec = StageSpec.NONE;
    /** While a question is on screen the scene must not give the answer away. */
    private boolean answersHidden = false;
    private float progress = 0f;
    private ValueAnimator animator;

    public LessonStageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);

        dashed.setStyle(Paint.Style.STROKE);
        dashed.setStrokeWidth(UiKit.dp(context, 2));
        dashed.setColor(PINK);
        dashed.setPathEffect(new DashPathEffect(new float[]{UiKit.dp(context, 7), UiKit.dp(context, 5)}, 0));

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(UiKit.font(context, true));
    }

    public void setSpec(StageSpec spec) {
        this.spec = spec == null ? StageSpec.NONE : spec;
        this.progress = 0f;
        invalidate();
    }

    public StageSpec getSpec() {
        return spec;
    }

    /**
     * Hides the line where each scene writes its result. The child is supposed to work the answer
     * out, so it only comes back when they have answered — or when a parent asks to see it.
     */
    public void setRevealAnswers(boolean reveal) {
        this.answersHidden = !reveal;
        invalidate();
    }

    public boolean answersHidden() {
        return answersHidden;
    }

    /** Runs the scene once over the given duration. */
    public void play(long durationMs) {
        if (animator != null) animator.cancel();
        if (spec.isNone()) return;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(Math.max(1200, durationMs));
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    public void stop() {
        if (animator != null) animator.cancel();
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0 || spec.isNone()) return;

        // every scene writes its result along the bottom; with the answer hidden that band is
        // simply not drawn, so the picture still teaches without handing over the number
        int saved = canvas.save();
        if (answersHidden) canvas.clipRect(0f, 0f, w, h - dp(26));

        switch (spec.kind) {
            case COUNT_GROUPS: drawCountGroups(canvas, w, h); break;
            case STEP_PATTERN: drawStepPattern(canvas, w, h); break;
            case MACHINE: drawMachine(canvas, w, h); break;
            case CLOCK: drawClock(canvas, w, h); break;
            case MIRROR: drawMirror(canvas, w, h); break;
            case PLACE_VALUE: drawPlaceValue(canvas, w, h); break;
            case NUMBER_LINE: drawNumberLine(canvas, w, h); break;
            case MONEY: drawMoney(canvas, w, h); break;
            case FRACTION: drawFraction(canvas, w, h); break;
            case FRACTION_PAIR: drawFractionPair(canvas, w, h); break;
            case ARRAY: drawArray(canvas, w, h); break;
            case LINE_KINDS: drawLineKinds(canvas, w, h); break;
            case PERIMETER: drawPerimeter(canvas, w, h); break;
            case COLUMN_OP: drawColumnOp(canvas, w, h); break;
            case BAR_CHART: drawBarChart(canvas, w, h); break;
            case TIMES_TEN: drawTimesTen(canvas, w, h); break;
            case MOON_PHASES: drawMoonPhases(canvas, w, h); break;
            case BUILDING: drawBuilding(canvas, w, h); break;
            case TABLE_PATTERN: drawTablePattern(canvas, w, h); break;
            case GRID_GROUPS: drawGridGroups(canvas, w, h); break;
            case CALENDAR: drawCalendar(canvas, w, h); break;
            case SUM_REORDER: drawSumReorder(canvas, w, h); break;
            case CLOCK24: drawClock24(canvas, w, h); break;
            case DAY_STRIP: drawDayStrip(canvas, w, h); break;
            case MACHINE_CHAIN: drawMachineChain(canvas, w, h); break;
            case SYMMETRY_LINES: drawSymmetryLines(canvas, w, h); break;
            case CUBE_NET: drawCubeNet(canvas, w, h); break;
            case SHAPE_PATTERN: drawShapePattern(canvas, w, h); break;
            case FIGURE_GROUPS: drawFigureGroups(canvas, w, h); break;
            default: break;
        }
        canvas.restoreToCount(saved);

        if (answersHidden) {
            textPaint.setColor(MUTED);
            textPaint.setTextSize(sp(13));
            canvas.drawText("جوابش را تو پیدا کن", w / 2, h - dp(7), textPaint);
        }
    }

    // ---------- scenes ----------

    /** Groups of dots appear one at a time; the running total counts up underneath. */
    private void drawCountGroups(Canvas canvas, float w, float h) {
        int groups = Math.max(1, spec.a);
        int per = Math.max(1, spec.b);

        int cols = Math.min(groups, 4);
        int rows = (int) Math.ceil(groups / (float) cols);
        float pad = dp(10);
        float totalBand = dp(46);
        float cellW = (w - pad * 2) / cols;
        float cellH = Math.min(cellW, (h - totalBand - pad * 2) / rows);

        float shown = progress * groups;

        for (int i = 0; i < groups; i++) {
            float appear = clamp(shown - i, 0f, 1f);
            if (appear <= 0f) continue;

            int row = i / cols;
            int col = i % cols;
            // right-to-left, so the first group lands on the right
            float cx = w - pad - (col + 0.5f) * cellW;
            float cy = pad + (row + 0.5f) * cellH;

            float scale = 0.7f + 0.3f * appear;
            float boxW = cellW * 0.86f * scale;
            float boxH = cellH * 0.78f * scale;

            fill.setColor(CARD);
            fill.setAlpha((int) (255 * appear));
            RectF box = new RectF(cx - boxW / 2, cy - boxH / 2, cx + boxW / 2, cy + boxH / 2);
            canvas.drawRoundRect(box, dp(12), dp(12), fill);
            stroke.setColor(CARD_EDGE);
            stroke.setStrokeWidth(dp(1.5f));
            stroke.setAlpha((int) (255 * appear));
            canvas.drawRoundRect(box, dp(12), dp(12), stroke);

            // the dots of this group
            int dotCols = Math.min(per, 5);
            int dotRows = (int) Math.ceil(per / (float) dotCols);
            float dotR = Math.min(boxW / (dotCols * 2.8f), boxH / (dotRows * 3.4f));
            float startX = cx + (dotCols - 1) * dotR * 1.4f;
            float startY = cy - boxH * 0.12f - (dotRows - 1) * dotR * 1.4f;
            for (int d = 0; d < per; d++) {
                int dr = d / dotCols;
                int dc = d % dotCols;
                fill.setColor(d % 2 == 0 ? TEAL : ORANGE);
                fill.setAlpha((int) (255 * appear));
                canvas.drawCircle(startX - dc * dotR * 2.8f, startY + dr * dotR * 2.8f, dotR, fill);
            }

            // running total for this group
            textPaint.setColor(INK);
            textPaint.setAlpha((int) (255 * appear));
            textPaint.setTextSize(sp(13));
            canvas.drawText(PersianDigits.fa((i + 1) * per), cx, cy + boxH * 0.40f, textPaint);
        }

        int counted = (int) Math.floor(shown) * per;
        if (counted > 0) {
            textPaint.setAlpha(255);
            textPaint.setColor(TEAL);
            textPaint.setTextSize(sp(26));
            canvas.drawText(PersianDigits.fa(counted), w / 2, h - dp(14), textPaint);
            textPaint.setColor(MUTED);
            textPaint.setTextSize(sp(11));
            canvas.drawText("تا اینجا شمردیم", w / 2, h - dp(34), textPaint);
        }
    }

    /** A staircase of bars growing left to right with the +۲ ، +۳ ، +۴ jumps between them. */
    private void drawStepPattern(Canvas canvas, float w, float h) {
        int[] values = spec.values;
        if (values == null || values.length == 0) return;
        int n = values.length;
        int max = 1;
        for (int v : values) max = Math.max(max, v);

        float pad = dp(14);
        float baseline = h - dp(34);
        float topRoom = dp(40);
        float slot = (w - pad * 2) / n;
        float barW = slot * 0.5f;

        float shown = progress * n;

        for (int i = 0; i < n; i++) {
            float grow = clamp(shown - i, 0f, 1f);
            if (grow <= 0f) continue;
            // right-to-left
            float cx = w - pad - (i + 0.5f) * slot;
            float full = (baseline - topRoom) * (values[i] / (float) max);
            float barH = full * grow;

            fill.setColor(i % 2 == 0 ? TEAL : ORANGE);
            RectF bar = new RectF(cx - barW / 2, baseline - barH, cx + barW / 2, baseline);
            canvas.drawRoundRect(bar, dp(6), dp(6), fill);

            textPaint.setColor(INK);
            textPaint.setAlpha(255);
            textPaint.setTextSize(sp(13));
            canvas.drawText(PersianDigits.fa(values[i]), cx, baseline + dp(20), textPaint);

            // the jump from the previous bar
            if (i > 0) {
                float prevCx = w - pad - (i - 0.5f) * slot;
                float jumpAlpha = clamp(shown - i, 0f, 1f);
                float prevH = (baseline - topRoom) * (values[i - 1] / (float) max);
                Path arc = new Path();
                float startX = prevCx - barW / 2;
                float endX = cx + barW / 2;
                float peakY = baseline - Math.max(prevH, barH) - dp(16);
                arc.moveTo(startX, baseline - prevH - dp(4));
                arc.quadTo((startX + endX) / 2, peakY, endX, baseline - barH - dp(4));
                stroke.setColor(PINK);
                stroke.setStrokeWidth(dp(2));
                stroke.setAlpha((int) (255 * jumpAlpha));
                canvas.drawPath(arc, stroke);

                textPaint.setColor(PINK);
                textPaint.setAlpha((int) (255 * jumpAlpha));
                textPaint.setTextSize(sp(12));
                canvas.drawText("+" + PersianDigits.fa(values[i] - values[i - 1]),
                    (startX + endX) / 2, peakY - dp(4), textPaint);
            }
        }
        stroke.setAlpha(255);
        textPaint.setAlpha(255);
    }

    /** A number rolls into the rule box and the answer pops out the far side. */
    private void drawMachine(Canvas canvas, float w, float h) {
        int rule = spec.a;
        int input = spec.b;
        int output = input + rule;

        float cy = h * 0.46f;
        float boxW = Math.min(w * 0.34f, dp(130));
        float boxH = Math.min(h * 0.44f, dp(96));

        float pulse = 0f;
        if (progress > 0.42f && progress < 0.62f) {
            pulse = (float) Math.sin((progress - 0.42f) / 0.20f * Math.PI) * 0.10f;
        }
        float bw = boxW * (1 + pulse);
        float bh = boxH * (1 + pulse);

        // the machine itself
        RectF box = new RectF(w / 2 - bw / 2, cy - bh / 2, w / 2 + bw / 2, cy + bh / 2);
        fill.setColor(ORANGE);
        canvas.drawRoundRect(box, dp(18), dp(18), fill);
        fill.setColor(FACE);
        RectF inner = new RectF(box.left + dp(10), box.top + dp(10), box.right - dp(10), box.bottom - dp(10));
        canvas.drawRoundRect(inner, dp(12), dp(12), fill);
        textPaint.setColor(ORANGE_DARK);
        textPaint.setTextSize(sp(24));
        // a machine can take away as well as add, so the sign follows the rule
        canvas.drawText((rule >= 0 ? "+" : "−") + PersianDigits.fa(Math.abs(rule)),
            w / 2, cy + sp(9), textPaint);

        float trackRight = w - dp(24);
        float trackLeft = dp(24);

        // labels at both ends
        textPaint.setColor(MUTED);
        textPaint.setTextSize(sp(11));
        canvas.drawText("ورودی", trackRight - dp(10), h - dp(10), textPaint);
        canvas.drawText("خروجی", trackLeft + dp(10), h - dp(10), textPaint);

        if (progress <= 0.5f) {
            float t = clamp(progress / 0.5f, 0f, 1f);
            float x = trackRight - t * (trackRight - (w / 2 + bw / 2));
            drawNumberChip(canvas, x, cy, PersianDigits.fa(input), TEAL);
        } else if (progress >= 0.62f) {
            float t = clamp((progress - 0.62f) / 0.38f, 0f, 1f);
            float x = (w / 2 - bw / 2) - t * ((w / 2 - bw / 2) - trackLeft);
            // the number coming out is the answer, so it stays a blank box until the child says it
            drawNumberChip(canvas, x, cy, answersHidden ? "⬜" : PersianDigits.fa(output), PINK);
        }
    }

    private void drawNumberChip(Canvas canvas, float cx, float cy, String label, int color) {
        float r = dp(23);
        fill.setColor(color);
        canvas.drawCircle(cx, cy, r, fill);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(sp(17));
        canvas.drawText(label, cx, cy + sp(6), textPaint);
    }

    /** Clock hands turn to the hour, then the 24-hour reading appears. */
    private void drawClock(Canvas canvas, float w, float h) {
        int hour12 = spec.a;
        boolean pm = spec.b == 1;

        float faceR = Math.min(w, h * (pm ? 0.66f : 0.9f)) * 0.36f;
        float cx = w / 2;
        float cy = pm ? h * 0.38f : h * 0.5f;

        fill.setColor(CARD);
        canvas.drawCircle(cx, cy, faceR, fill);
        stroke.setColor(ORANGE);
        stroke.setStrokeWidth(dp(4));
        canvas.drawCircle(cx, cy, faceR, stroke);

        // hour ticks, with ۱۲ ۳ ۶ ۹ written out
        for (int i = 0; i < 12; i++) {
            double ang = Math.toRadians(i * 30 - 90);
            float outer = faceR - dp(5);
            float inner = faceR - (i % 3 == 0 ? dp(13) : dp(9));
            stroke.setColor(i % 3 == 0 ? ORANGE_DARK : CARD_EDGE);
            stroke.setStrokeWidth(i % 3 == 0 ? dp(3) : dp(2));
            canvas.drawLine(
                cx + (float) Math.cos(ang) * inner, cy + (float) Math.sin(ang) * inner,
                cx + (float) Math.cos(ang) * outer, cy + (float) Math.sin(ang) * outer, stroke);
        }
        textPaint.setColor(INK);
        textPaint.setTextSize(sp(12));
        canvas.drawText("۱۲", cx, cy - faceR + dp(26), textPaint);
        canvas.drawText("۶", cx, cy + faceR - dp(15), textPaint);
        canvas.drawText("۳", cx + faceR - dp(18), cy + sp(4), textPaint);
        canvas.drawText("۹", cx - faceR + dp(18), cy + sp(4), textPaint);

        // hour hand sweeps to the hour over the first half of the scene
        float sweep = clamp(progress / 0.55f, 0f, 1f);
        double hourAng = Math.toRadians(-90 + 30 * hour12 * sweep);
        stroke.setColor(INK);
        stroke.setStrokeWidth(dp(5));
        canvas.drawLine(cx, cy,
            cx + (float) Math.cos(hourAng) * faceR * 0.52f,
            cy + (float) Math.sin(hourAng) * faceR * 0.52f, stroke);
        stroke.setColor(TEAL);
        stroke.setStrokeWidth(dp(3));
        canvas.drawLine(cx, cy, cx, cy - faceR * 0.74f, stroke); // minutes at twelve
        fill.setColor(ORANGE_DARK);
        canvas.drawCircle(cx, cy, dp(5), fill);

        if (!pm) return;

        // the 24-hour conversion, counting up from the 12-hour reading
        float reveal = clamp((progress - 0.58f) / 0.42f, 0f, 1f);
        if (reveal <= 0f) return;

        float boxW = Math.min(w - dp(40), dp(300));
        float boxH = dp(56);
        RectF box = new RectF(cx - boxW / 2, h - boxH - dp(12), cx + boxW / 2, h - dp(12));
        fill.setColor(FACE);
        fill.setAlpha((int) (255 * reveal));
        canvas.drawRoundRect(box, dp(16), dp(16), fill);
        stroke.setColor(ORANGE);
        stroke.setStrokeWidth(dp(1.5f));
        stroke.setAlpha((int) (255 * reveal));
        canvas.drawRoundRect(box, dp(16), dp(16), stroke);

        int counted = hour12 + Math.round(12 * reveal);
        textPaint.setAlpha((int) (255 * reveal));
        textPaint.setColor(ORANGE_DARK);
        textPaint.setTextSize(sp(17));
        canvas.drawText(PersianDigits.fa(hour12) + " بعدازظهر  ←  " + PersianDigits.fa(counted),
            cx, box.centerY() + sp(6), textPaint);
        textPaint.setAlpha(255);
        stroke.setAlpha(255);
        fill.setAlpha(255);
    }

    /** One half of a shape fills in, then folds across the line onto the other half. */
    private void drawMirror(Canvas canvas, float w, float h) {
        int perHalf = Math.max(1, spec.a);
        int cols = Math.min(perHalf, 4);
        int rows = (int) Math.ceil(perHalf / (float) cols);

        float pad = dp(16);
        float labelBand = dp(34);
        float cell = Math.min((w / 2 - pad * 1.5f) / cols, (h - labelBand - pad * 2) / rows);
        float midX = w / 2;
        float topY = pad + ((h - labelBand - pad * 2) - rows * cell) / 2;

        // the fold line
        Path fold = new Path();
        fold.moveTo(midX, pad * 0.5f);
        fold.lineTo(midX, h - labelBand);
        canvas.drawPath(fold, dashed);

        float leftShown = clamp(progress / 0.5f, 0f, 1f) * perHalf;
        float rightShown = clamp((progress - 0.5f) / 0.5f, 0f, 1f) * perHalf;

        for (int i = 0; i < perHalf; i++) {
            int r = i / cols;
            int c = i % cols;
            float appearL = clamp(leftShown - i, 0f, 1f);
            if (appearL > 0f) {
                float x = midX - dp(6) - (c + 1) * cell;
                float y = topY + r * cell;
                drawCell(canvas, x, y, cell, TEAL, appearL);
            }
            float appearR = clamp(rightShown - i, 0f, 1f);
            if (appearR > 0f) {
                float x = midX + dp(6) + c * cell;
                float y = topY + r * cell;
                drawCell(canvas, x, y, cell, ORANGE, appearR);
            }
        }

        int left = (int) Math.floor(leftShown);
        int right = (int) Math.floor(rightShown);
        if (left > 0) {
            textPaint.setColor(right > 0 ? ORANGE_DARK : TEAL);
            textPaint.setTextSize(sp(15));
            String label = right > 0
                ? PersianDigits.fa(left) + " + " + PersianDigits.fa(right) + " = " + PersianDigits.fa(left + right)
                : PersianDigits.fa(left) + " خانه در این نیمه";
            canvas.drawText(label, w / 2, h - dp(10), textPaint);
        }
    }


    // ---------- chapters ۲ to ۸ ----------

    /**
     * Builds a number out of what the book calls its place-value pieces: thousand-cubes,
     * hundred-plates, ten-rods and single ones, each group appearing in turn.
     */
    private void drawPlaceValue(Canvas canvas, float w, float h) {
        int number = Math.max(0, spec.a);
        int[] digits = {number / 1000, (number / 100) % 10, (number / 10) % 10, number % 10};
        String[] names = {"هزار", "صد", "ده", "یک"};
        int[] colors = {PINK, ORANGE, TEAL, INK};

        float pad = dp(10);
        float labelBand = dp(40);
        float colW = (w - pad * 2) / 4;
        float top = pad + dp(6);
        float boxH = h - labelBand - top - pad;

        // right to left, the way the columns sit in the book's table
        for (int col = 0; col < 4; col++) {
            float appear = clamp(progress * 4f - col, 0f, 1f);
            float x = w - pad - (col + 1) * colW;

            stroke.setColor(CARD_EDGE);
            stroke.setStrokeWidth(dp(1.5f));
            RectF box = new RectF(x + dp(4), top, x + colW - dp(4), top + boxH);
            fill.setColor(CARD);
            canvas.drawRoundRect(box, dp(10), dp(10), fill);
            canvas.drawRoundRect(box, dp(10), dp(10), stroke);

            textPaint.setColor(MUTED);
            textPaint.setTextSize(sp(11));
            canvas.drawText(names[col], box.centerX(), h - dp(20), textPaint);

            int count = digits[3 - col];
            if (appear <= 0f || count == 0) {
                textPaint.setColor(MUTED);
                textPaint.setTextSize(sp(15));
                canvas.drawText(PersianDigits.fa(count), box.centerX(), box.centerY(), textPaint);
                continue;
            }

            // little squares standing for the pieces, at most nine of them
            int perRow = 3;
            float cell = Math.min((box.width() - dp(12)) / perRow, (box.height() - dp(26)) / 3f);
            float startX = box.centerX() - (perRow * cell) / 2f;
            float startY = box.top + dp(8);
            int shown = (int) Math.ceil(count * appear);
            for (int i = 0; i < shown; i++) {
                drawCell(canvas, startX + (i % perRow) * cell, startY + (i / perRow) * cell, cell,
                    colors[col], clamp(count * appear - i, 0f, 1f));
            }
            textPaint.setColor(colors[col]);
            textPaint.setTextSize(sp(14));
            canvas.drawText(PersianDigits.fa(count), box.centerX(), box.bottom - dp(7), textPaint);
        }

        if (progress > 0.85f) {
            textPaint.setColor(INK);
            textPaint.setTextSize(sp(17));
            canvas.drawText(PersianDigits.fa(number), w / 2, h - dp(4), textPaint);
        }
    }

    /** A number line: the ticks are drawn first, then each mark drops onto its place. */
    private void drawNumberLine(Canvas canvas, float w, float h) {
        int from = spec.a;
        int to = Math.max(spec.a + 1, spec.b);
        int[] marks = spec.values == null ? new int[0] : spec.values;

        float pad = dp(22);
        float lineY = h * 0.6f;
        float left = pad, right = w - pad;
        float span = right - left;

        stroke.setColor(INK);
        stroke.setStrokeWidth(dp(2.5f));
        canvas.drawLine(left, lineY, right, lineY, stroke);

        int ticks = 10;
        textPaint.setTextSize(sp(10));
        for (int i = 0; i <= ticks; i++) {
            float t = i / (float) ticks;
            float x = left + span * t;
            boolean major = i % 5 == 0;
            stroke.setColor(major ? INK : CARD_EDGE);
            stroke.setStrokeWidth(dp(major ? 2.5f : 1.5f));
            canvas.drawLine(x, lineY - dp(major ? 10 : 6), x, lineY + dp(major ? 10 : 6), stroke);
            if (major) {
                textPaint.setColor(MUTED);
                canvas.drawText(PersianDigits.fa(from + Math.round((to - from) * t)), x, lineY + dp(26), textPaint);
            }
        }

        for (int i = 0; i < marks.length; i++) {
            float appear = clamp(progress * marks.length - i, 0f, 1f);
            if (appear <= 0f) continue;
            float t = (marks[i] - from) / (float) (to - from);
            float x = left + span * clamp(t, 0f, 1f);
            float y = lineY - dp(16) - appear * dp(6);
            int color = i == 0 ? TEAL : ORANGE;
            fill.setColor(color);
            fill.setAlpha((int) (255 * appear));
            canvas.drawCircle(x, lineY, dp(7), fill);
            fill.setAlpha(255);
            textPaint.setColor(color);
            textPaint.setTextSize(sp(14));
            canvas.drawText(PersianDigits.fa(marks[i]), x, y, textPaint);
        }
    }

    /** ۵۰۰-toman notes then ۱۰۰-toman coins, with the total counting up. */
    private void drawMoney(Canvas canvas, float w, float h) {
        int notes = Math.max(0, spec.a);
        int coins = Math.max(0, spec.b);
        int total = notes * 500 + coins * 100;

        float pad = dp(12);
        float band = dp(40);
        float rowH = (h - band - pad * 2) / 2f;
        float shownNotes = clamp(progress / 0.5f, 0f, 1f) * notes;
        float shownCoins = clamp((progress - 0.5f) / 0.5f, 0f, 1f) * coins;

        float noteW = Math.min(dp(64), (w - pad * 2) / Math.max(1, notes));
        for (int i = 0; i < notes; i++) {
            float appear = clamp(shownNotes - i, 0f, 1f);
            if (appear <= 0f) continue;
            float x = w - pad - (i + 1) * noteW;
            RectF r = new RectF(x + dp(3), pad + rowH * 0.18f, x + noteW - dp(3), pad + rowH * 0.82f);
            fill.setColor(TEAL);
            fill.setAlpha((int) (255 * appear));
            canvas.drawRoundRect(r, dp(6), dp(6), fill);
            fill.setAlpha(255);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(sp(11));
            canvas.drawText("۵۰۰", r.centerX(), r.centerY() + dp(4), textPaint);
        }

        float coinD = Math.min(dp(44), (w - pad * 2) / Math.max(1, coins));
        for (int i = 0; i < coins; i++) {
            float appear = clamp(shownCoins - i, 0f, 1f);
            if (appear <= 0f) continue;
            float cx = w - pad - (i + 0.5f) * coinD;
            float cy = pad + rowH + rowH / 2f;
            fill.setColor(ORANGE);
            fill.setAlpha((int) (255 * appear));
            canvas.drawCircle(cx, cy, coinD * 0.42f, fill);
            fill.setAlpha(255);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(sp(10));
            canvas.drawText("۱۰۰", cx, cy + dp(3.5f), textPaint);
        }

        int running = (int) Math.floor(shownNotes) * 500 + (int) Math.floor(shownCoins) * 100;
        textPaint.setColor(progress > 0.95f ? ORANGE_DARK : MUTED);
        textPaint.setTextSize(sp(15));
        canvas.drawText(PersianDigits.fa(running) + " تومان" + (progress > 0.95f && running == total ? " ✓" : ""),
            w / 2, h - dp(12), textPaint);
    }

    /** A bar or a pie split into equal parts, the coloured ones filling in one by one. */
    private void drawFraction(Canvas canvas, float w, float h) {
        drawOneFraction(canvas, spec.a, spec.b, spec.c == 1, 0, 0, w, h - dp(34), progress);
        textPaint.setColor(TEAL);
        textPaint.setTextSize(sp(16));
        canvas.drawText(PersianDigits.fa(spec.b) + " از " + PersianDigits.fa(spec.a) + " قسمت",
            w / 2, h - dp(10), textPaint);
    }

    /** Two fractions one above the other — how تساوی and مقایسه are shown in the book. */
    private void drawFractionPair(Canvas canvas, float w, float h) {
        int[] v = spec.values;
        if (v == null || v.length < 4) return;
        float band = dp(30);
        float rowH = (h - band) / 2f;
        drawOneFraction(canvas, v[0], v[1], false, 0, 0, w, rowH - dp(6), clamp(progress / 0.5f, 0f, 1f));
        drawOneFraction(canvas, v[2], v[3], false, 0, rowH, w, rowH - dp(6), clamp((progress - 0.45f) / 0.55f, 0f, 1f));

        if (progress > 0.9f) {
            float f1 = v[1] / (float) v[0];
            float f2 = v[3] / (float) v[2];
            String sign = Math.abs(f1 - f2) < 0.001f ? "=" : f1 > f2 ? "بزرگ‌تر است ↑" : "بزرگ‌تر است ↓";
            textPaint.setColor(ORANGE_DARK);
            textPaint.setTextSize(sp(15));
            canvas.drawText(sign.equals("=") ? "این دو کسر مساوی‌اند" : "کسری که " + sign,
                w / 2, h - dp(8), textPaint);
        }
    }

    private void drawOneFraction(Canvas canvas, int parts, int shaded, boolean circle,
                                 float x, float y, float w, float h, float appear) {
        parts = Math.max(1, parts);
        shaded = Math.max(0, Math.min(shaded, parts));
        float grown = shaded * appear;

        if (circle) {
            float r = Math.min(w, h) * 0.42f;
            float cx = x + w / 2, cy = y + h / 2;
            RectF oval = new RectF(cx - r, cy - r, cx + r, cy + r);
            float sweep = 360f / parts;
            for (int i = 0; i < parts; i++) {
                float part = clamp(grown - i, 0f, 1f);
                fill.setColor(part > 0 ? TEAL : CARD);
                if (part > 0 && part < 1) fill.setAlpha((int) (255 * part));
                canvas.drawArc(oval, -90 + i * sweep, sweep, true, fill);
                fill.setAlpha(255);
                stroke.setColor(INK);
                stroke.setStrokeWidth(dp(2));
                canvas.drawArc(oval, -90 + i * sweep, sweep, true, stroke);
            }
            return;
        }

        float barH = Math.min(h * 0.6f, dp(74));
        float pad = dp(16);
        float barW = w - pad * 2;
        float top = y + (h - barH) / 2f;
        float cellW = barW / parts;
        for (int i = 0; i < parts; i++) {
            float part = clamp(grown - i, 0f, 1f);
            RectF r = new RectF(x + w - pad - (i + 1) * cellW, top, x + w - pad - i * cellW, top + barH);
            fill.setColor(CARD);
            canvas.drawRect(r, fill);
            if (part > 0) {
                fill.setColor(TEAL);
                fill.setAlpha((int) (255 * part));
                canvas.drawRect(r, fill);
                fill.setAlpha(255);
            }
            stroke.setColor(INK);
            stroke.setStrokeWidth(dp(2));
            canvas.drawRect(r, stroke);
        }
    }

    /** A rows × cols array of squares; with a group size the squares get ringed into groups. */
    private void drawArray(Canvas canvas, float w, float h) {
        int rows = Math.max(1, spec.a);
        int cols = Math.max(1, spec.b);
        int group = spec.c;

        float pad = dp(10);
        float band = dp(34);
        float cell = Math.min((w - pad * 2) / cols, (h - band - pad * 2) / rows);
        float startX = w / 2 + (cols * cell) / 2f;   // right to left
        float startY = pad + ((h - band - pad * 2) - rows * cell) / 2f;

        int count = rows * cols;
        float shown = progress * count;
        for (int i = 0; i < count; i++) {
            float appear = clamp(shown - i, 0f, 1f);
            if (appear <= 0f) continue;
            int r = i / cols, c = i % cols;
            drawCell(canvas, startX - (c + 1) * cell, startY + r * cell, cell,
                group > 0 ? (i / group) % 2 == 0 ? TEAL : ORANGE : TEAL, appear);
        }

        textPaint.setTextSize(sp(15));
        textPaint.setColor(ORANGE_DARK);
        String label = group > 0
            ? PersianDigits.fa(count) + " تا در دسته‌های " + PersianDigits.fa(group) + " تایی = "
                + PersianDigits.fa(count / group) + " دسته"
            : PersianDigits.fa(rows) + " × " + PersianDigits.fa(cols) + " = " + PersianDigits.fa(count);
        if (progress > 0.6f) canvas.drawText(label, w / 2, h - dp(10), textPaint);
    }

    /** خط، نیم‌خط و پاره‌خط, drawn one under the other and labelled. */
    private void drawLineKinds(Canvas canvas, float w, float h) {
        String[] names = {"خط (دو طرف ادامه دارد)", "نیم‌خط (یک طرف ادامه دارد)", "پاره‌خط (دو سر دارد)"};
        float pad = dp(20);
        float rowH = h / 3f;
        for (int i = 0; i < 3; i++) {
            float appear = clamp(progress * 3f - i, 0f, 1f);
            if (appear <= 0f) continue;
            float y = rowH * i + rowH * 0.42f;
            float left = pad, right = w - pad;
            float drawnRight = left + (right - left) * appear;

            stroke.setColor(i == 0 ? TEAL : i == 1 ? ORANGE : PINK);
            stroke.setStrokeWidth(dp(3.5f));
            canvas.drawLine(left, y, drawnRight, y, stroke);

            fill.setColor(INK);
            if (i >= 1) canvas.drawCircle(right, y, dp(5), fill);          // the fixed end
            if (i == 2) canvas.drawCircle(left, y, dp(5), fill);           // and the second one
            if (i <= 1) {                                                  // arrow where it keeps going
                Path arrow = new Path();
                arrow.moveTo(left, y);
                arrow.lineTo(left + dp(12), y - dp(6));
                arrow.lineTo(left + dp(12), y + dp(6));
                arrow.close();
                canvas.drawPath(arrow, fill);
            }
            if (i == 0) {
                Path arrow = new Path();
                arrow.moveTo(right, y);
                arrow.lineTo(right - dp(12), y - dp(6));
                arrow.lineTo(right - dp(12), y + dp(6));
                arrow.close();
                canvas.drawPath(arrow, fill);
            }

            textPaint.setColor(MUTED);
            textPaint.setTextSize(sp(11));
            canvas.drawText(names[i], w / 2, y + dp(20), textPaint);
        }
    }

    /** A labelled rectangle: the outline is traced for محیط, or tiled with units for مساحت. */
    private void drawPerimeter(Canvas canvas, float w, float h) {
        int len = Math.max(1, spec.a);
        int wid = Math.max(1, spec.b);
        boolean area = spec.c == 1;

        float pad = dp(26);
        float band = dp(30);
        float maxW = w - pad * 2, maxH = h - band - pad * 2;
        float unit = Math.min(maxW / len, maxH / wid);
        float rectW = unit * len, rectH = unit * wid;
        float x = (w - rectW) / 2f, y = pad + (maxH - rectH) / 2f;

        fill.setColor(CARD);
        canvas.drawRect(x, y, x + rectW, y + rectH, fill);

        if (area) {
            int count = len * wid;
            float shown = progress * count;
            for (int i = 0; i < count; i++) {
                float appear = clamp(shown - i, 0f, 1f);
                if (appear <= 0f) continue;
                drawCell(canvas, x + (i % len) * unit, y + (i / len) * unit, unit, TEAL, appear);
            }
        } else {
            // walk the outline, one side at a time
            float[] sides = {rectW, rectH, rectW, rectH};
            float total = rectW * 2 + rectH * 2;
            float walked = progress * total;
            float px = x, py = y;
            stroke.setColor(ORANGE);
            stroke.setStrokeWidth(dp(5));
            for (int i = 0; i < 4 && walked > 0; i++) {
                float part = Math.min(walked, sides[i]);
                float nx = px, ny = py;
                if (i == 0) nx = px + part;
                else if (i == 1) ny = py + part;
                else if (i == 2) nx = px - part;
                else ny = py - part;
                canvas.drawLine(px, py, nx, ny, stroke);
                walked -= part;
                if (i == 0) px = x + rectW;
                else if (i == 1) py = y + rectH;
                else if (i == 2) px = x;
            }
        }

        stroke.setColor(INK);
        stroke.setStrokeWidth(dp(2));
        canvas.drawRect(x, y, x + rectW, y + rectH, stroke);

        textPaint.setColor(MUTED);
        textPaint.setTextSize(sp(12));
        canvas.drawText(PersianDigits.fa(len), x + rectW / 2, y - dp(8), textPaint);
        canvas.drawText(PersianDigits.fa(wid), x + rectW + dp(14), y + rectH / 2, textPaint);

        if (progress > 0.75f) {
            textPaint.setColor(ORANGE_DARK);
            textPaint.setTextSize(sp(15));
            String label = area
                ? "مساحت = " + PersianDigits.fa(len) + " × " + PersianDigits.fa(wid) + " = " + PersianDigits.fa(len * wid)
                : "محیط = (" + PersianDigits.fa(len) + " + " + PersianDigits.fa(wid) + ") × ۲ = "
                    + PersianDigits.fa((len + wid) * 2);
            canvas.drawText(label, w / 2, h - dp(8), textPaint);
        }
    }

    /** Vertical addition or subtraction, one place-value column at a time, right to left. */
    private void drawColumnOp(Canvas canvas, float w, float h) {
        int x = spec.a, y = spec.b;
        boolean sub = spec.c == 1;
        int result = sub ? x - y : x + y;

        String[] heads = {"یکان", "دهگان", "صدگان", "هزار"};
        int places = Math.max(String.valueOf(Math.max(x, Math.max(y, result))).length(), 2);
        places = Math.min(places, 4);

        float pad = dp(14);
        float colW = Math.min(dp(56), (w - pad * 2) / places);
        float startX = w / 2 + (places * colW) / 2f;
        float rowH = (h - dp(30)) / 4f;

        textPaint.setTextSize(sp(11));
        for (int i = 0; i < places; i++) {
            float cx = startX - (i + 0.5f) * colW;
            textPaint.setColor(MUTED);
            canvas.drawText(heads[i], cx, pad + dp(2), textPaint);
        }

        stroke.setColor(CARD_EDGE);
        stroke.setStrokeWidth(dp(1.5f));
        for (int i = 1; i < places; i++) {
            float lx = startX - i * colW;
            canvas.drawLine(lx, pad + dp(8), lx, pad + rowH * 3, stroke);
        }

        textPaint.setTextSize(sp(20));
        for (int i = 0; i < places; i++) {
            float cx = startX - (i + 0.5f) * colW;
            int dx = digitAt(x, i), dy = digitAt(y, i);
            textPaint.setColor(INK);
            canvas.drawText(PersianDigits.fa(dx), cx, pad + rowH * 1.1f, textPaint);
            textPaint.setColor(sub ? PINK : TEAL);
            canvas.drawText(PersianDigits.fa(dy), cx, pad + rowH * 2f, textPaint);
        }

        textPaint.setColor(sub ? PINK : TEAL);
        textPaint.setTextSize(sp(18));
        canvas.drawText(sub ? "−" : "+", startX - places * colW - dp(10), pad + rowH * 2f, textPaint);

        stroke.setColor(INK);
        stroke.setStrokeWidth(dp(2.5f));
        canvas.drawLine(startX - places * colW, pad + rowH * 2.3f, startX, pad + rowH * 2.3f, stroke);

        // the answer arrives column by column, starting from the ones
        for (int i = 0; i < places && !answersHidden; i++) {
            float appear = clamp(progress * places - i, 0f, 1f);
            if (appear <= 0f) continue;
            float cx = startX - (i + 0.5f) * colW;
            textPaint.setColor(ORANGE_DARK);
            textPaint.setTextSize(sp(22) * (0.7f + 0.3f * appear));
            canvas.drawText(PersianDigits.fa(digitAt(result, i)), cx, pad + rowH * 3.1f, textPaint);
        }

        if (progress > 0.9f) {
            textPaint.setColor(ORANGE_DARK);
            textPaint.setTextSize(sp(15));
            canvas.drawText(PersianDigits.fa(x) + (sub ? " − " : " + ") + PersianDigits.fa(y)
                + " = " + PersianDigits.fa(result), w / 2, h - dp(6), textPaint);
        }
    }

    private static int digitAt(int value, int place) {
        int v = Math.abs(value);
        for (int i = 0; i < place; i++) v /= 10;
        return v % 10;
    }

    /** Bars growing out of their data, the way the book's نمودار ستونی is drawn. */
    private void drawBarChart(Canvas canvas, float w, float h) {
        int[] data = spec.values;
        if (data == null || data.length == 0) return;
        int max = 1;
        for (int d : data) max = Math.max(max, d);

        float pad = dp(16);
        float band = dp(26);
        float barArea = h - band - pad;
        float slot = (w - pad * 2) / data.length;

        stroke.setColor(CARD_EDGE);
        stroke.setStrokeWidth(dp(2));
        canvas.drawLine(pad, h - band, w - pad, h - band, stroke);

        for (int i = 0; i < data.length; i++) {
            float appear = clamp(progress * data.length - i, 0f, 1f);
            float barH = (data[i] / (float) max) * (barArea - dp(22)) * appear;
            float cx = w - pad - (i + 0.5f) * slot;
            RectF r = new RectF(cx - slot * 0.3f, h - band - barH, cx + slot * 0.3f, h - band);
            fill.setColor(i % 2 == 0 ? TEAL : ORANGE);
            canvas.drawRoundRect(r, dp(4), dp(4), fill);
            if (appear > 0.4f) {
                textPaint.setColor(INK);
                textPaint.setTextSize(sp(12));
                canvas.drawText(PersianDigits.fa(data[i]), cx, h - band - barH - dp(6), textPaint);
            }
        }
    }

    /** n and n × ۱۰ side by side, with the extra zero sliding into place. */
    private void drawTimesTen(Canvas canvas, float w, float h) {
        int n = Math.max(0, spec.a);
        float cy = h * 0.5f;

        textPaint.setColor(INK);
        textPaint.setTextSize(sp(30));
        canvas.drawText(PersianDigits.fa(n), w * 0.78f, cy, textPaint);

        textPaint.setColor(MUTED);
        textPaint.setTextSize(sp(18));
        canvas.drawText("× ۱۰", w * 0.56f, cy, textPaint);
        canvas.drawText("=", w * 0.40f, cy, textPaint);

        float appear = clamp(progress / 0.7f, 0f, 1f);
        textPaint.setColor(ORANGE_DARK);
        textPaint.setTextSize(sp(30));
        canvas.drawText(PersianDigits.fa(n), w * 0.20f + dp(10) * (1 - appear), cy, textPaint);
        if (appear > 0.2f) {
            textPaint.setColor(PINK);
            textPaint.setTextSize(sp(30) * (0.5f + 0.5f * appear));
            canvas.drawText("۰", w * 0.20f - dp(22), cy + dp(4) * (1 - appear), textPaint);
        }

        if (progress > 0.85f) {
            textPaint.setColor(MUTED);
            textPaint.setTextSize(sp(13));
            canvas.drawText("یک صفر به آخرش اضافه می‌شود", w / 2, h - dp(12), textPaint);
        }
    }


    // ---------- the book's own figures, page by page ----------

    /** صفحه‌ی ۷: the moon thinning, filling and thinning again — the book's opening pattern. */
    private void drawMoonPhases(Canvas canvas, float w, float h) {
        int count = 5;
        float pad = dp(8);
        float slot = (w - pad * 2) / count;
        float r = Math.min(slot * 0.38f, h * 0.28f);
        float cy = h * 0.45f;
        String[] names = {"شب ۱", "شب ۷", "شب ۱۴", "شب ۲۱", "شب ۲۹"};
        String[] shapes = {"هلال", "نیمه", "کامل", "نیمه", "هلال"};
        // how much of the disc is lit on each of those nights
        float[] lit = {0.10f, 0.5f, 1f, 0.5f, 0.10f};

        for (int i = 0; i < count; i++) {
            float appear = clamp(progress * count - i, 0f, 1f);
            if (appear <= 0f) continue;
            float cx = w - pad - (i + 0.5f) * slot;

            fill.setColor(Color.parseColor("#1F2A44"));
            fill.setAlpha((int) (255 * appear));
            canvas.drawCircle(cx, cy, r, fill);
            fill.setAlpha(255);

            // the lit part: a slice that grows from one edge, so the crescent really fills up
            fill.setColor(Color.parseColor("#F6E7B4"));
            RectF oval = new RectF(cx - r, cy - r, cx + r, cy + r);
            float sweep = 360f * lit[i] * appear;
            canvas.drawArc(oval, -90 - sweep / 2, sweep, true, fill);
            if (lit[i] < 0.95f) {   // round off the inner edge so it reads as a crescent
                fill.setColor(Color.parseColor("#1F2A44"));
                float inset = r * (1f - lit[i] * 1.6f);
                canvas.drawOval(new RectF(cx - r + inset * 0.2f, cy - r, cx + r - inset, cy + r), fill);
            }

            stroke.setColor(CARD_EDGE);
            stroke.setStrokeWidth(dp(1.5f));
            canvas.drawCircle(cx, cy, r, stroke);

            textPaint.setColor(INK);
            textPaint.setTextSize(sp(10.5f));
            canvas.drawText(shapes[i], cx, cy + r + dp(15), textPaint);
            textPaint.setColor(MUTED);
            textPaint.setTextSize(sp(9.5f));
            canvas.drawText(names[i], cx, cy + r + dp(28), textPaint);
        }

        if (progress > 0.85f) {
            textPaint.setColor(ORANGE_DARK);
            textPaint.setTextSize(sp(14));
            canvas.drawText("این الگو هر ماه یک بار تکرار می‌شود", w / 2, h - dp(8), textPaint);
        }
    }

    /** صفحه‌های ۸ و ۹: the block of flats, floors filling from the ground up. */
    private void drawBuilding(Canvas canvas, float w, float h) {
        int floors = Math.max(1, spec.a);
        int per = Math.max(1, spec.b);

        float pad = dp(12);
        float band = dp(32);
        float available = h - band - pad * 2;
        float floorH = Math.min(dp(34), available / floors);
        float unitW = Math.min(dp(40), (w - pad * 2 - dp(40)) / per);
        float left = w / 2 - (per * unitW) / 2f;
        float bottom = pad + available;

        float shown = progress * floors;
        for (int f = 0; f < floors; f++) {
            float appear = clamp(shown - f, 0f, 1f);
            if (appear <= 0f) continue;
            float y = bottom - (f + 1) * floorH;
            for (int u = 0; u < per; u++) {
                RectF r = new RectF(left + u * unitW + dp(2), y + dp(2),
                    left + (u + 1) * unitW - dp(2), y + floorH - dp(2));
                fill.setColor(f % 2 == 0 ? TEAL : ORANGE);
                fill.setAlpha((int) (255 * appear));
                canvas.drawRoundRect(r, dp(3), dp(3), fill);
                fill.setAlpha(255);
            }
            textPaint.setColor(MUTED);
            textPaint.setTextSize(sp(10));
            canvas.drawText("طبقه‌ی " + PersianDigits.fa(f + 1), left - dp(22), y + floorH * 0.66f, textPaint);
        }

        int done = (int) Math.floor(shown);
        if (done > 0) {
            textPaint.setColor(ORANGE_DARK);
            textPaint.setTextSize(sp(15));
            canvas.drawText(PersianDigits.fa(done) + " طبقه × " + PersianDigits.fa(per)
                + " واحد = " + PersianDigits.fa(done * per), w / 2, h - dp(8), textPaint);
        }
    }

    /** صفحه‌های ۹ و ۲۳: the book's table, filling cell by cell with its +n jumps. */
    private void drawTablePattern(Canvas canvas, float w, float h) {
        int[] values = spec.values;
        if (values == null || values.length == 0) return;

        float pad = dp(10);
        float cellW = (w - pad * 2) / values.length;
        float cellH = Math.min(dp(56), h * 0.42f);
        float top = h * 0.30f;

        for (int i = 0; i < values.length; i++) {
            float appear = clamp(progress * values.length - i, 0f, 1f);
            float x = w - pad - (i + 1) * cellW;   // right to left
            RectF cell = new RectF(x + dp(3), top, x + cellW - dp(3), top + cellH);

            fill.setColor(CARD);
            canvas.drawRoundRect(cell, dp(8), dp(8), fill);
            stroke.setColor(CARD_EDGE);
            stroke.setStrokeWidth(dp(2));
            canvas.drawRoundRect(cell, dp(8), dp(8), stroke);

            if (appear > 0f) {
                textPaint.setColor(INK);
                textPaint.setTextSize(sp(17) * (0.7f + 0.3f * appear));
                canvas.drawText(PersianDigits.fa(values[i]), cell.centerX(), cell.centerY() + dp(6), textPaint);
            }

            // the jump arrow to the next cell, labelled with the real difference — patterns like
            // ۰ ، ۱ ، ۴ ، ۹ do not grow by a fixed amount
            if (i > 0 && appear > 0.3f) {
                int jump = values[i - 1] - values[i];
                float ax = cell.right, bx = cell.right + cellW - dp(6);
                float ay = top - dp(12);
                Path arc = new Path();
                arc.moveTo(bx, ay);
                arc.quadTo((ax + bx) / 2, ay - dp(18), ax, ay);
                canvas.drawPath(arc, dashed);
                textPaint.setColor(PINK);
                textPaint.setTextSize(sp(11));
                canvas.drawText((jump >= 0 ? "+" : "−") + PersianDigits.fa(Math.abs(jump)),
                    (ax + bx) / 2, ay - dp(20), textPaint);
            }
        }
    }

    /** صفحه‌های ۱۰ و ۱۱: the grid of little squares, ringed into equal groups. */
    private void drawGridGroups(Canvas canvas, float w, float h) {
        int rows = Math.max(1, spec.a);
        int cols = Math.max(1, spec.b);
        int group = Math.max(1, spec.c);
        int count = rows * cols;
        int groups = (int) Math.ceil(count / (float) group);

        float pad = dp(10);
        float band = dp(38);
        float cell = Math.min((w - pad * 2) / cols, (h - band - pad * 2) / rows);
        float startX = w / 2 + (cols * cell) / 2f;
        float startY = pad + ((h - band - pad * 2) - rows * cell) / 2f;

        for (int i = 0; i < count; i++) {
            int r = i / cols, c = i % cols;
            float x = startX - (c + 1) * cell, y = startY + r * cell;
            drawCell(canvas, x, y, cell, CARD_EDGE, 1f);
        }

        float shownGroups = progress * groups;
        for (int g = 0; g < groups; g++) {
            float appear = clamp(shownGroups - g, 0f, 1f);
            if (appear <= 0f) continue;
            int from = g * group, to = Math.min(count, from + group);
            for (int i = from; i < to; i++) {
                int r = i / cols, c = i % cols;
                drawCell(canvas, startX - (c + 1) * cell, startY + r * cell, cell,
                    g % 2 == 0 ? TEAL : ORANGE, appear);
            }
        }

        int doneGroups = (int) Math.floor(shownGroups);
        if (doneGroups > 0) {
            StringBuilder sum = new StringBuilder();
            for (int g = 0; g < doneGroups; g++) {
                int size = Math.min(group, count - g * group);
                if (g > 0) sum.append(" + ");
                sum.append(PersianDigits.fa(size));
            }
            textPaint.setColor(ORANGE_DARK);
            textPaint.setTextSize(sp(14));
            canvas.drawText(sum + " = " + PersianDigits.fa(Math.min(count, doneGroups * group)),
                w / 2, h - dp(10), textPaint);
        }
    }

    /** صفحه‌ی ۱۲: a month's calendar with every seventh day marked. */
    private void drawCalendar(Canvas canvas, float w, float h) {
        int days = Math.max(1, spec.a);
        int start = Math.max(0, spec.b);
        String[] head = {"ش", "ی", "د", "س", "چ", "پ", "ج"};

        float pad = dp(8);
        float cell = Math.min((w - pad * 2) / 7, (h - dp(46)) / 6);
        float startX = w / 2 + (7 * cell) / 2f;
        float top = dp(18);

        textPaint.setTextSize(sp(10));
        for (int c = 0; c < 7; c++) {
            textPaint.setColor(MUTED);
            canvas.drawText(head[c], startX - (c + 0.5f) * cell, top - dp(4), textPaint);
        }

        int marked = 0;
        for (int d = 1; d <= days; d++) {
            int index = start + d - 1;
            int r = index / 7, c = index % 7;
            float x = startX - (c + 1) * cell, y = top + r * cell;
            boolean isClassDay = (d - 1) % 7 == 0;
            if (isClassDay) marked++;

            float appear = isClassDay ? clamp(progress * 5f - marked + 1, 0f, 1f) : 1f;
            RectF box = new RectF(x + dp(1.5f), y + dp(1.5f), x + cell - dp(1.5f), y + cell - dp(1.5f));
            fill.setColor(isClassDay && appear > 0.1f ? ORANGE : CARD);
            canvas.drawRoundRect(box, dp(4), dp(4), fill);
            stroke.setColor(CARD_EDGE);
            stroke.setStrokeWidth(dp(1));
            canvas.drawRoundRect(box, dp(4), dp(4), stroke);

            textPaint.setColor(isClassDay && appear > 0.1f ? Color.WHITE : INK);
            textPaint.setTextSize(sp(9.5f));
            canvas.drawText(PersianDigits.fa(d), box.centerX(), box.centerY() + dp(3.5f), textPaint);
        }

        if (progress > 0.8f) {
            textPaint.setColor(ORANGE_DARK);
            textPaint.setTextSize(sp(13));
            canvas.drawText("هر ۷ روز یک بار، دوباره شنبه", w / 2, h - dp(6), textPaint);
        }
    }

    /** صفحه‌های ۱۲ و ۲۴: the same addends paired up into tens, landing on the same total. */
    private void drawSumReorder(Canvas canvas, float w, float h) {
        int[] addends = spec.values;
        if (addends == null || addends.length == 0) return;
        int total = 0;
        for (int a : addends) total += a;

        float pad = dp(12);
        float slot = (w - pad * 2) / addends.length;
        float topRow = h * 0.26f;
        float bottomRow = h * 0.62f;

        // as the book writes it
        textPaint.setTextSize(sp(17));
        for (int i = 0; i < addends.length; i++) {
            float cx = w - pad - (i + 0.5f) * slot;
            textPaint.setColor(INK);
            canvas.drawText(PersianDigits.fa(addends[i]), cx, topRow, textPaint);
            if (i < addends.length - 1) {
                textPaint.setColor(MUTED);
                canvas.drawText("+", cx - slot / 2, topRow, textPaint);
            }
        }

        // …and rearranged, biggest with smallest, so friendly pairs appear
        int[] sorted = addends.clone();
        java.util.Arrays.sort(sorted);
        int[] paired = new int[sorted.length];
        int lo = 0, hi = sorted.length - 1, k = 0;
        while (lo <= hi) {
            paired[k++] = sorted[hi--];
            if (lo <= hi) paired[k++] = sorted[lo++];
        }

        float move = clamp(progress / 0.7f, 0f, 1f);
        for (int i = 0; i < paired.length; i++) {
            float cx = w - pad - (i + 0.5f) * slot;
            float y = topRow + (bottomRow - topRow) * move;
            textPaint.setColor(i % 2 == 0 ? TEAL : ORANGE);
            textPaint.setTextSize(sp(17));
            canvas.drawText(PersianDigits.fa(paired[i]), cx, y, textPaint);
            if (i < paired.length - 1 && move > 0.5f) {
                textPaint.setColor(MUTED);
                canvas.drawText("+", cx - slot / 2, y, textPaint);
            }
        }

        if (progress > 0.8f) {
            textPaint.setColor(ORANGE_DARK);
            textPaint.setTextSize(sp(16));
            canvas.drawText("جمع هر دو راه = " + PersianDigits.fa(total), w / 2, h - dp(10), textPaint);
        }
    }

    /** صفحه‌های ۱۶ تا ۱۸: the clock with the afternoon ring ۱۳ to ۲۴ around the outside. */
    private void drawClock24(Canvas canvas, float w, float h) {
        int hour24 = ((spec.a % 24) + 24) % 24;
        int hour12 = hour24 % 12 == 0 ? 12 : hour24 % 12;
        boolean afternoon = hour24 >= 12;

        float cx = w / 2, cy = h * 0.47f;
        float outer = Math.min(w, h) * 0.40f;
        float face = outer * 0.78f;

        // the afternoon ring first, so the ۱۳ to ۲۴ numbers sit outside the face
        textPaint.setTextSize(sp(10));
        for (int i = 0; i < 12; i++) {
            int label = 13 + i;
            double angle = Math.toRadians(-90 + (i + 1) * 30);
            float x = cx + (float) Math.cos(angle) * outer;
            float y = cy + (float) Math.sin(angle) * outer;
            boolean isNow = afternoon && label == (hour24 == 12 ? 24 : hour24 < 13 ? 24 : hour24);
            textPaint.setColor(isNow ? PINK : MUTED);
            canvas.drawText(PersianDigits.fa(label), x, y + dp(3.5f), textPaint);
        }

        fill.setColor(FACE);
        canvas.drawCircle(cx, cy, face, fill);
        stroke.setColor(ORANGE);
        stroke.setStrokeWidth(dp(3));
        canvas.drawCircle(cx, cy, face, stroke);

        textPaint.setTextSize(sp(12));
        for (int i = 1; i <= 12; i++) {
            double angle = Math.toRadians(-90 + i * 30);
            float x = cx + (float) Math.cos(angle) * (face - dp(15));
            float y = cy + (float) Math.sin(angle) * (face - dp(15));
            textPaint.setColor(i == hour12 ? ORANGE_DARK : INK);
            canvas.drawText(PersianDigits.fa(i), x, y + dp(4), textPaint);
        }

        double target = Math.toRadians(-90 + hour12 * 30);
        double turned = -Math.PI / 2 + (target + Math.PI / 2) * progress;
        stroke.setColor(ORANGE_DARK);
        stroke.setStrokeWidth(dp(4.5f));
        canvas.drawLine(cx, cy, cx + (float) Math.cos(turned) * face * 0.55f,
            cy + (float) Math.sin(turned) * face * 0.55f, stroke);
        stroke.setColor(TEAL);
        stroke.setStrokeWidth(dp(3));
        canvas.drawLine(cx, cy, cx, cy - face * 0.78f, stroke);
        fill.setColor(ORANGE_DARK);
        canvas.drawCircle(cx, cy, dp(4.5f), fill);

        if (progress > 0.7f && !answersHidden) {
            textPaint.setColor(afternoon ? PINK : TEAL);
            textPaint.setTextSize(sp(14));
            String label = afternoon
                ? "ساعت " + PersianDigits.fa(hour12) + " بعدازظهر = " + PersianDigits.fa(hour24)
                : "ساعت " + PersianDigits.fa(hour12) + " قبل‌از‌ظهر";
            canvas.drawText(label, w / 2, h - dp(8), textPaint);
        }
    }

    /** صفحه‌ی ۱۸: the whole day as one ۰ to ۲۴ strip, with a stretch coloured in. */
    private void drawDayStrip(Canvas canvas, float w, float h) {
        int from = Math.max(0, Math.min(24, spec.a));
        int to = Math.max(from, Math.min(24, spec.b));

        float pad = dp(14);
        float barH = dp(40);
        float top = h * 0.34f;
        float span = w - pad * 2;
        float hour = span / 24f;

        for (int i = 0; i < 24; i++) {
            float x = w - pad - (i + 1) * hour;   // ۰ on the right
            RectF cell = new RectF(x, top, x + hour, top + barH);
            fill.setColor(i < 12 ? Color.parseColor("#EAF3F1") : Color.parseColor("#F4E7EC"));
            canvas.drawRect(cell, fill);
        }

        float grown = (to - from) * clamp(progress / 0.8f, 0f, 1f);
        RectF lit = new RectF(w - pad - (from + grown) * hour, top, w - pad - from * hour, top + barH);
        fill.setColor(ORANGE);
        canvas.drawRect(lit, fill);

        stroke.setColor(INK);
        stroke.setStrokeWidth(dp(2));
        canvas.drawRect(w - pad - span, top, w - pad, top + barH, stroke);

        textPaint.setTextSize(sp(10));
        for (int i = 0; i <= 24; i += 6) {
            float x = w - pad - i * hour;
            textPaint.setColor(MUTED);
            canvas.drawText(PersianDigits.fa(i), x, top + barH + dp(15), textPaint);
        }
        textPaint.setColor(TEAL);
        canvas.drawText("قبل‌از‌ظهر", w - pad - 6 * hour, top - dp(8), textPaint);
        textPaint.setColor(PINK);
        canvas.drawText("بعدازظهر", w - pad - 18 * hour, top - dp(8), textPaint);

        if (progress > 0.85f) {
            textPaint.setColor(ORANGE_DARK);
            textPaint.setTextSize(sp(14));
            canvas.drawText("از ساعت " + PersianDigits.fa(from) + " تا " + PersianDigits.fa(to)
                + " می‌شود " + PersianDigits.fa(to - from) + " ساعت", w / 2, h - dp(8), textPaint);
        }
    }

    /** صفحه‌های ۱۴ و ۱۵: two machines joined, the first one's answer feeding the second. */
    private void drawMachineChain(Canvas canvas, float w, float h) {
        int[] rules = spec.values;
        if (rules == null || rules.length < 2) return;
        int input = spec.a;
        int middle = input + rules[0];
        int out = middle + rules[1];

        float boxW = w * 0.22f, boxH = h * 0.40f;
        float cy = h * 0.44f;
        float[] centers = {w * 0.60f, w * 0.30f};   // right to left

        textPaint.setTextSize(sp(16));
        textPaint.setColor(INK);
        canvas.drawText(PersianDigits.fa(input), w * 0.90f, cy + dp(5), textPaint);

        for (int m = 0; m < 2; m++) {
            float appear = clamp(progress * 2f - m, 0f, 1f);
            RectF box = new RectF(centers[m] - boxW / 2, cy - boxH / 2, centers[m] + boxW / 2, cy + boxH / 2);
            fill.setColor(appear > 0f ? (m == 0 ? TEAL : ORANGE) : CARD_EDGE);
            canvas.drawRoundRect(box, dp(12), dp(12), fill);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(sp(15));
            canvas.drawText((rules[m] >= 0 ? "+" : "−") + PersianDigits.fa(Math.abs(rules[m])),
                box.centerX(), box.centerY() + dp(5), textPaint);

            // the number travelling between the machines
            if (appear > 0f) {
                float labelX = m == 0 ? (w * 0.90f + centers[0]) / 2 : (centers[0] + centers[1]) / 2;
                textPaint.setColor(MUTED);
                textPaint.setTextSize(sp(13));
                canvas.drawText(PersianDigits.fa(m == 0 ? input : middle), labelX, cy - boxH / 2 - dp(8), textPaint);
            }
        }

        float outAppear = clamp((progress - 0.6f) / 0.4f, 0f, 1f);
        if (outAppear > 0f && !answersHidden) {
            textPaint.setColor(ORANGE_DARK);
            textPaint.setTextSize(sp(20) * (0.6f + 0.4f * outAppear));
            canvas.drawText(PersianDigits.fa(out), w * 0.10f, cy + dp(6), textPaint);
        }

        if (progress > 0.9f) {
            textPaint.setColor(MUTED);
            textPaint.setTextSize(sp(12.5f));
            canvas.drawText(PersianDigits.fa(input) + " ← " + PersianDigits.fa(middle)
                + " ← " + PersianDigits.fa(out), w / 2, h - dp(8), textPaint);
        }
    }

    /** صفحه‌های ۱۹ تا ۲۱: the line of symmetry drawn, then the shape folded over it. */
    private void drawSymmetryLines(Canvas canvas, float w, float h) {
        int perHalf = Math.max(1, spec.a);
        int axes = Math.max(1, spec.b);

        int cols = Math.min(perHalf, 3);
        int rows = (int) Math.ceil(perHalf / (float) cols);
        float pad = dp(16);
        float band = dp(34);
        float cell = Math.min((w / 2 - pad * 1.5f) / cols, (h - band - pad * 2) / rows);
        float midX = w / 2;
        float topY = pad + ((h - band - pad * 2) - rows * cell) / 2;

        // the axis is drawn first, the way the book asks the child to draw it with a ruler
        float axisGrow = clamp(progress / 0.3f, 0f, 1f);
        Path fold = new Path();
        fold.moveTo(midX, h * 0.5f - (h * 0.5f - dp(4)) * axisGrow);
        fold.lineTo(midX, h * 0.5f + (h * 0.5f - band) * axisGrow);
        canvas.drawPath(fold, dashed);
        if (axes > 1) {
            float axisY = topY + (rows * cell) / 2f;
            Path second = new Path();
            second.moveTo(midX - (w * 0.5f - dp(6)) * axisGrow, axisY);
            second.lineTo(midX + (w * 0.5f - dp(6)) * axisGrow, axisY);
            canvas.drawPath(second, dashed);
        }

        float leftShown = clamp((progress - 0.25f) / 0.35f, 0f, 1f) * perHalf;
        float rightShown = clamp((progress - 0.6f) / 0.4f, 0f, 1f) * perHalf;
        for (int i = 0; i < perHalf; i++) {
            int r = i / cols, c = i % cols;
            float appearL = clamp(leftShown - i, 0f, 1f);
            if (appearL > 0f) drawCell(canvas, midX - dp(5) - (c + 1) * cell, topY + r * cell, cell, TEAL, appearL);
            float appearR = clamp(rightShown - i, 0f, 1f);
            if (appearR > 0f) drawCell(canvas, midX + dp(5) + c * cell, topY + r * cell, cell, ORANGE, appearR);
        }

        if (progress > 0.9f) {
            textPaint.setColor(ORANGE_DARK);
            textPaint.setTextSize(sp(14));
            canvas.drawText(PersianDigits.fa(perHalf) + " + " + PersianDigits.fa(perHalf)
                + " = " + PersianDigits.fa(perHalf * 2) + " خانه", w / 2, h - dp(8), textPaint);
        }
    }

    /** The cube activities: the net drawn flat, then folding up into the cube. */
    private void drawCubeNet(Canvas canvas, float w, float h) {
        float cell = Math.min(w / 5.2f, h / 4.4f);
        float cx = w / 2, cy = h * 0.46f;
        // the six faces of the net: a cross, as the book draws it
        int[][] netCells = {{0, -1}, {0, 0}, {0, 1}, {0, 2}, {-1, 0}, {1, 0}};

        float fold = clamp((progress - 0.45f) / 0.55f, 0f, 1f);

        for (int i = 0; i < netCells.length; i++) {
            float appear = clamp(progress / 0.45f * netCells.length - i, 0f, 1f);
            if (appear <= 0f) continue;
            float gx = netCells[i][0], gy = netCells[i][1];
            // as it folds, every face slides towards the middle
            float x = cx + (gx * cell) * (1 - fold) - cell / 2 + (fold * gx * cell * 0.18f);
            float y = cy + (gy * cell) * (1 - fold) - cell / 2 + (fold * gy * cell * 0.18f);
            RectF r = new RectF(x, y, x + cell, y + cell);
            fill.setColor(i % 2 == 0 ? TEAL : ORANGE);
            fill.setAlpha((int) (255 * appear * (1 - fold * 0.25f)));
            canvas.drawRoundRect(r, dp(4), dp(4), fill);
            fill.setAlpha(255);
            stroke.setColor(Color.WHITE);
            stroke.setStrokeWidth(dp(2));
            canvas.drawRoundRect(r, dp(4), dp(4), stroke);
        }

        if (progress > 0.9f) {
            textPaint.setColor(ORANGE_DARK);
            textPaint.setTextSize(sp(14));
            canvas.drawText("۶ مربّع، یک مکعّب", w / 2, h - dp(8), textPaint);
        }
    }


    /** The book's own growing figures — ۱، ۳، ۶، ۱۰ squares — with the jumps drawn between them. */
    private void drawShapePattern(Canvas canvas, float w, float h) {
        int[] counts = spec.values;
        if (counts == null || counts.length == 0) return;

        float pad = dp(8);
        float band = dp(30);
        float slot = (w - pad * 2) / counts.length;
        int biggest = 0;
        for (int c : counts) biggest = Math.max(biggest, c);
        int maxRows = rowsFor(biggest).length;
        float cell = Math.min(slot / (maxRows + 0.6f), (h - band - pad * 2) / (maxRows + 0.6f));

        for (int i = 0; i < counts.length; i++) {
            float appear = clamp(progress * counts.length - i, 0f, 1f);
            if (appear <= 0f) continue;
            int[] rows = rowsFor(counts[i]);
            float rightEdge = w - pad - i * slot - dp(6);
            float bottom = pad + (h - band - pad * 2);

            int drawn = 0;
            for (int r = 0; r < rows.length; r++) {
                for (int c = 0; c < rows[r]; c++) {
                    float cellAppear = clamp(counts[i] * appear - drawn, 0f, 1f);
                    drawn++;
                    if (cellAppear <= 0f) continue;
                    drawCell(canvas, rightEdge - (c + 1) * cell, bottom - (r + 1) * cell, cell, TEAL, cellAppear);
                }
            }

            textPaint.setColor(INK);
            textPaint.setTextSize(sp(13));
            canvas.drawText(PersianDigits.fa(counts[i]), rightEdge - cell / 2, h - dp(10), textPaint);

            // the jump to the next figure, the way the book writes it above the arrow
            if (i < counts.length - 1 && appear > 0.6f) {
                float ax = w - pad - (i + 1) * slot;
                Path arc = new Path();
                arc.moveTo(ax + dp(10), h - dp(22));
                arc.quadTo(ax, h - dp(38), ax - dp(10), h - dp(22));
                canvas.drawPath(arc, dashed);
                textPaint.setColor(PINK);
                textPaint.setTextSize(sp(11));
                canvas.drawText("+" + PersianDigits.fa(counts[i + 1] - counts[i]), ax, h - dp(40), textPaint);
            }
        }
    }

    /** Squares per row of the staircase figure that holds `count` squares. */
    private static int[] rowsFor(int count) {
        int n = 0;
        while ((n + 1) * (n + 2) / 2 <= count) n++;
        int used = n * (n + 1) / 2;
        int extra = count - used;
        int[] rows = new int[extra > 0 ? n + 1 : Math.max(n, 1)];
        for (int i = 0; i < n; i++) rows[i] = n - i;
        if (extra > 0) rows[n] = extra;
        if (n == 0 && rows.length > 0) rows[0] = count;
        return rows;
    }

    /** One figure of squares, ringed into equal groups with its addition written underneath. */
    private void drawFigureGroups(Canvas canvas, float w, float h) {
        int[] rowCounts = spec.values;
        if (rowCounts == null || rowCounts.length == 0) return;
        int group = Math.max(1, spec.a);

        int total = 0, widest = 0;
        for (int r : rowCounts) { total += r; widest = Math.max(widest, r); }
        int groups = (int) Math.ceil(total / (float) group);

        float pad = dp(10);
        float band = dp(36);
        float cell = Math.min((w - pad * 2) / widest, (h - band - pad * 2) / rowCounts.length);
        float startX = w / 2 + (widest * cell) / 2f;
        float startY = pad + ((h - band - pad * 2) - rowCounts.length * cell) / 2f;

        float shown = progress * total;
        int index = 0;
        for (int r = 0; r < rowCounts.length; r++) {
            for (int c = 0; c < rowCounts[r]; c++) {
                float appear = clamp(shown - index, 0f, 1f);
                int groupIndex = index / group;
                index++;
                if (appear <= 0f) {
                    drawCell(canvas, startX - (c + 1) * cell, startY + r * cell, cell, CARD_EDGE, 1f);
                    continue;
                }
                drawCell(canvas, startX - (c + 1) * cell, startY + r * cell, cell,
                    groupIndex % 2 == 0 ? TEAL : ORANGE, appear);
            }
        }

        int doneGroups = Math.min(groups, (int) Math.floor(shown / group));
        if (doneGroups > 0) {
            StringBuilder sum = new StringBuilder();
            for (int g = 0; g < doneGroups; g++) {
                if (g > 0) sum.append(" + ");
                sum.append(PersianDigits.fa(Math.min(group, total - g * group)));
            }
            textPaint.setColor(ORANGE_DARK);
            textPaint.setTextSize(sp(13.5f));
            canvas.drawText(sum + " = " + PersianDigits.fa(Math.min(total, doneGroups * group)),
                w / 2, h - dp(10), textPaint);
        }
    }

    private void drawCell(Canvas canvas, float x, float y, float cell, int color, float appear) {
        float inset = cell * 0.08f + (1 - appear) * cell * 0.3f;
        RectF rect = new RectF(x + inset, y + inset, x + cell - inset, y + cell - inset);
        fill.setColor(color);
        fill.setAlpha((int) (255 * appear));
        canvas.drawRoundRect(rect, dp(5), dp(5), fill);
        fill.setAlpha(255);
    }

    // ---------- helpers ----------

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    private float dp(float value) {
        return UiKit.dp(getContext(), value);
    }

    private float sp(float value) {
        return UiKit.sp(getContext(), value);
    }
}
