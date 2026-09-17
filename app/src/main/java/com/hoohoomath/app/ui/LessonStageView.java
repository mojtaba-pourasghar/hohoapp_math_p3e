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

        switch (spec.kind) {
            case COUNT_GROUPS: drawCountGroups(canvas, w, h); break;
            case STEP_PATTERN: drawStepPattern(canvas, w, h); break;
            case MACHINE: drawMachine(canvas, w, h); break;
            case CLOCK: drawClock(canvas, w, h); break;
            case MIRROR: drawMirror(canvas, w, h); break;
            default: break;
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
        canvas.drawText("+" + PersianDigits.fa(rule), w / 2, cy + sp(9), textPaint);

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
            drawNumberChip(canvas, x, cy, PersianDigits.fa(output), PINK);
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
