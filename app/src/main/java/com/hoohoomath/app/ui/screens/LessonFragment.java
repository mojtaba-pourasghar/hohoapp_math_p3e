package com.hoohoomath.app.ui.screens;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.hoohoomath.app.R;
import com.hoohoomath.app.data.AppState;
import com.hoohoomath.app.data.Book;
import com.hoohoomath.app.data.Lessons;
import com.hoohoomath.app.data.PageLessons;
import com.hoohoomath.app.data.LessonKind;
import com.hoohoomath.app.data.LessonScript;
import com.hoohoomath.app.data.LessonStep;
import com.hoohoomath.app.tts.LessonAudio;
import com.hoohoomath.app.tts.SoundManager;
import com.hoohoomath.app.ui.BaseFragment;
import com.hoohoomath.app.ui.FeedbackDialog;
import com.hoohoomath.app.ui.LessonStageView;
import com.hoohoomath.app.ui.Screen;
import com.hoohoomath.app.ui.UiKit;

import java.util.List;

import static com.hoohoomath.app.data.PersianDigits.fa;

/**
 * The lesson itself: هوهو narrates each step while a matching animation plays, then — once the
 * explanation is finished — the child gets their turn. Narration comes from a recorded file when
 * one exists in res/raw, otherwise from the device's Persian voice.
 */
public class LessonFragment extends BaseFragment {

    private int chapter, section;
    /** > 0 when this is a lesson for one page of the printed book. */
    private int page;
    private LessonScript script;
    private int stepIndex = 0;
    private final StringBuilder typed = new StringBuilder();
    private boolean answered = false;
    private boolean narrating = false;
    private int starsThisLesson = 0;

    private LessonStageView stage;
    private View rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lesson, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        Bundle args = getArguments();
        chapter = args != null ? args.getInt("chapter", 0) : 0;
        section = args != null ? args.getInt("section", 0) : 0;
        page = args != null ? args.getInt("page", 0) : 0;
        // a page of the book, or the section's own summary lesson
        script = page > 0 ? PageLessons.forPage(page) : Lessons.forSection(chapter, section);
        if (script != null) section = script.section;

        stage = view.findViewById(R.id.lesson_stage);
        view.findViewById(R.id.lesson_back).setOnClickListener(v -> {
            LessonAudio.stop();
            nav().go(Screen.MAP);
        });

        if (script == null) {
            ((TextView) view.findViewById(R.id.lesson_caption)).setText("درسِ صوتی این بخش هنوز آماده نیست.");
            return;
        }

        Book.Chapter ch = Book.chapter(chapter);
        ((TextView) view.findViewById(R.id.lesson_title)).setText(page > 0
            ? "کتاب، صفحه‌ی " + fa(page) + " — " + ch.sections.get(section)
            : "درس " + fa(section + 1) + ": " + ch.sections.get(section));
        view.findViewById(R.id.lesson_replay).setOnClickListener(v -> narrateCurrentStep());
        view.findViewById(R.id.lesson_prev).setOnClickListener(v -> goToStep(stepIndex - 1));
        view.findViewById(R.id.lesson_next).setOnClickListener(v -> goToStep(stepIndex + 1));

        // come back to the step the child had reached instead of starting the lesson over
        int saved = page > 0 ? state().pageStep(page) : state().lessonStep(chapter, section);
        stepIndex = saved > 0 && saved < script.steps.size() ? saved : 0;

        renderStep();
    }

    @Override
    public void onDestroyView() {
        LessonAudio.stop();
        if (stage != null) stage.stop();
        super.onDestroyView();
    }

    // ---------- step rendering ----------

    private void renderStep() {
        LessonStep step = script.steps.get(stepIndex);
        answered = false;
        typed.setLength(0);

        ((TextView) rootView.findViewById(R.id.lesson_step_count)).setText(
            "صفحه‌ی " + fa(script.bookPage) + " کتاب · گام " + fa(stepIndex + 1) + " از " + fa(script.steps.size()));
        ((TextView) rootView.findViewById(R.id.lesson_say)).setText(step.say);
        ((TextView) rootView.findViewById(R.id.lesson_caption)).setText(step.caption);

        boolean teaching = step.kind == LessonKind.TEACH || step.kind == LessonKind.DONE;
        ((TextView) rootView.findViewById(R.id.lesson_phase)).setText(teaching ? "هوهو توضیح می‌دهد" : "نوبت توست");

        rootView.findViewById(R.id.lesson_stage_card).setVisibility(step.hasStage() ? View.VISIBLE : View.GONE);
        stage.setSpec(step.stage);

        rootView.findViewById(R.id.lesson_prev).setVisibility(stepIndex == 0 ? View.INVISIBLE : View.VISIBLE);
        rootView.findViewById(R.id.lesson_next).setVisibility(
            stepIndex >= script.steps.size() - 1 ? View.INVISIBLE : View.VISIBLE);

        if (page > 0) state().savePageStep(page, stepIndex);
        else state().saveLessonStep(chapter, section, stepIndex);

        renderDots();
        buildContent(step);
        narrateCurrentStep();
    }

    /** Free movement through the lesson, in either direction, at any time. */
    private void goToStep(int target) {
        if (target < 0 || target >= script.steps.size()) return;
        LessonAudio.stop();
        stepIndex = target;
        renderStep();
    }

    private void renderDots() {
        LinearLayout dots = rootView.findViewById(R.id.lesson_dots);
        dots.removeAllViews();
        for (int i = 0; i < script.steps.size(); i++) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(UiKit.dp(requireContext(), 12), UiKit.dp(requireContext(), 6));
            lp.setMargins(UiKit.dp(requireContext(), 2), 0, UiKit.dp(requireContext(), 2), 0);
            dot.setLayoutParams(lp);
            int colorRes = i < stepIndex ? R.color.teal : i == stepIndex ? R.color.orange : R.color.border_card;
            dot.setBackground(UiKit.roundedBg(ContextCompat.getColor(requireContext(), colorRes), 0, 999f, requireContext()));
            int target = i;
            dot.setOnClickListener(v -> goToStep(target));
            dots.addView(dot);
        }
    }

    /** Speaks the current line and runs its animation for exactly as long as the voice lasts. */
    private void narrateCurrentStep() {
        LessonStep step = script.steps.get(stepIndex);
        narrate(step.audioKey, step.say, true);
    }

    /** The extra explanation the child asked for with "یک مثال دیگر". */
    private void narrateExample() {
        LessonStep step = script.steps.get(stepIndex);
        if (!step.hasExample()) return;
        narrate(step.exampleAudioKey, step.exampleSay, false);
    }

    private void narrate(String audioKey, String text, boolean replayStage) {
        narrating = true;
        updateContinueEnabled();
        mascot().showBubble(text, true);

        // fly over to the picture being explained, so the child looks where هوهو is looking
        View stageCard = rootView.findViewById(R.id.lesson_stage_card);
        if (stageCard.getVisibility() == View.VISIBLE) mascot().flyTo(stageCard);

        LessonAudio.play(requireContext(), audioKey, text, new LessonAudio.PlaybackListener() {
            @Override public void onStarted(long durationMs) {
                if (!isAdded()) return;
                if (replayStage) stage.play(durationMs);
            }
            @Override public void onFinished() {
                if (!isAdded()) return;
                narrating = false;
                mascot().showBubble(text, false);
                mascot().returnHome();
                updateContinueEnabled();
            }
        });
    }

    // ---------- the child's turn ----------

    private TextView continueButton;

    private void buildContent(LessonStep step) {
        LinearLayout content = rootView.findViewById(R.id.lesson_content);
        content.removeAllViews();
        continueButton = null;

        switch (step.kind) {
            case TEACH: {
                LinearLayout buttons = UiKit.row(requireContext());
                continueButton = bigButton("فهمیدم، برویم!", R.color.teal, R.color.white);
                continueButton.setOnClickListener(v -> advance());

                TextView again = bigButton("دوباره بگو", R.color.bg_card, R.color.text_primary);
                again.setBackground(UiKit.roundedBg(ContextCompat.getColor(requireContext(), R.color.bg_card),
                    ContextCompat.getColor(requireContext(), R.color.border_input), 16f, requireContext()));
                again.setOnClickListener(v -> narrateCurrentStep());

                LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                left.setMarginEnd(UiKit.dp(requireContext(), 5));
                LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                right.setMarginStart(UiKit.dp(requireContext(), 5));
                buttons.addView(continueButton, left);
                buttons.addView(again, right);
                content.addView(buttons);

                if (step.hasExample()) {
                    TextView example = bigButton("یک مثال دیگر بزن", R.color.orange_bg, R.color.orange_text);
                    example.setBackground(UiKit.roundedBg(
                        ContextCompat.getColor(requireContext(), R.color.orange_bg),
                        ContextCompat.getColor(requireContext(), R.color.orange_border), 16f, requireContext()));
                    example.setOnClickListener(v -> narrateExample());
                    content.addView(example, UiKit.marginParams(requireContext(), 10, 0));
                }

                updateContinueEnabled();
                break;
            }
            case DONE: {
                if (page > 0) {
                    state().markPageDone(page);
                    state().savePageStep(page, 0);
                } else {
                    state().markSectionLessonDone(chapter, section);
                    state().saveLessonStep(chapter, section, 0);
                }
                content.addView(buildDoneCard());
                break;
            }
            case MCQ: {
                content.addView(heading("جواب را انتخاب کن"));
                for (int i = 0; i < step.options.size(); i++) {
                    int idx = i;
                    TextView btn = UiKit.text(requireContext(), step.options.get(i), 24f, R.color.text_primary, true);
                    btn.setGravity(Gravity.CENTER);
                    btn.setPadding(0, UiKit.dp(requireContext(), 20), 0, UiKit.dp(requireContext(), 20));
                    btn.setBackground(UiKit.roundedBg(ContextCompat.getColor(requireContext(), R.color.bg_card),
                        ContextCompat.getColor(requireContext(), R.color.border_input), 18f, requireContext()));
                    btn.setLayoutParams(UiKit.marginParams(requireContext(), 0, 10));
                    btn.setOnClickListener(v -> {
                        if (answered) return;
                        answered = true;
                        onLessonAnswer(idx == step.correctIndex, step.why);
                    });
                    content.addView(btn);
                }
                break;
            }
            case BUILD: {
                content.addView(buildGrid(step));
                break;
            }
            case NUM: {
                content.addView(heading("عدد را بنویس"));

                TextView display = UiKit.text(requireContext(), "⬜", 30f, R.color.text_primary, true);
                display.setGravity(Gravity.CENTER);
                display.setPadding(0, UiKit.dp(requireContext(), 16), 0, UiKit.dp(requireContext(), 16));
                display.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_answer_box));
                content.addView(display, UiKit.marginParams(requireContext(), 0, 12));

                LinearLayout keypadWrap = new LinearLayout(requireContext());
                keypadWrap.setGravity(Gravity.CENTER);
                keypadWrap.addView(UiKit.buildKeypad(requireContext(), key -> {
                    if (answered) return;
                    if (key.equals("C")) typed.setLength(0);
                    else if (key.equals("⌫")) { if (typed.length() > 0) typed.deleteCharAt(typed.length() - 1); }
                    else if (typed.length() < 5) typed.append(key);
                    display.setText(typed.length() == 0 ? "⬜" : typed.toString());
                }));
                content.addView(keypadWrap);

                TextView submit = bigButton("بفرست", R.color.orange, R.color.white);
                submit.setOnClickListener(v -> {
                    if (answered || typed.length() == 0) return;
                    answered = true;
                    onLessonAnswer(typed.toString().equals(step.answerFa), step.why);
                });
                content.addView(submit, UiKit.marginParams(requireContext(), 12, 0));
                break;
            }
        }
    }

    /**
     * The child's own drawing board: a grid of squares they tap to colour in, the way the book
     * asks them to draw the next figure of a pattern. هوهو counts along with them and checks.
     */
    private View buildGrid(LessonStep step) {
        LinearLayout box = UiKit.column(requireContext());
        box.addView(heading("خودت شکل بعدی را بساز — روی خانه‌ها بزن"));

        final boolean[][] filled = new boolean[step.buildRows][step.buildCols];
        final int[] count = {0};

        TextView counter = UiKit.text(requireContext(), "تا حالا: ۰ خانه", 14f, R.color.text_muted, true);
        counter.setGravity(Gravity.CENTER);

        LinearLayout grid = UiKit.column(requireContext());
        grid.setGravity(Gravity.CENTER_HORIZONTAL);
        int cellDp = Math.max(22, Math.min(38, 300 / Math.max(1, step.buildCols)));
        final TextView[][] cells = new TextView[step.buildRows][step.buildCols];

        for (int r = 0; r < step.buildRows; r++) {
            LinearLayout rowView = UiKit.row(requireContext());
            rowView.setGravity(Gravity.CENTER);
            for (int c = 0; c < step.buildCols; c++) {
                final int rr = r, cc = c;
                TextView cell = new TextView(requireContext());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    UiKit.dp(requireContext(), cellDp), UiKit.dp(requireContext(), cellDp));
                lp.setMargins(UiKit.dp(requireContext(), 2), UiKit.dp(requireContext(), 2),
                    UiKit.dp(requireContext(), 2), UiKit.dp(requireContext(), 2));
                cell.setLayoutParams(lp);
                paintCell(cell, false);
                cell.setOnClickListener(v -> {
                    if (answered) return;
                    filled[rr][cc] = !filled[rr][cc];
                    count[0] += filled[rr][cc] ? 1 : -1;
                    paintCell(cell, filled[rr][cc]);
                    counter.setText("تا حالا: " + fa(count[0]) + " خانه");
                    SoundManager sound = SoundManager.get();
                    if (sound != null) sound.tap();
                });
                cells[r][c] = cell;
                rowView.addView(cell);
            }
            grid.addView(rowView);
        }
        box.addView(grid);
        box.addView(counter, UiKit.marginParams(requireContext(), 8, 0));

        TextView check = bigButton("تمام شد، نگاه کن", R.color.orange, R.color.white);
        check.setOnClickListener(v -> {
            if (answered) return;
            if (count[0] == 0) {
                mascot().say("هنوز هیچ خانه‌ای رنگ نکرده‌ای. روی خانه‌ها بزن تا شکل ساخته شود.");
                return;
            }
            boolean right = count[0] == step.buildTarget;
            if (right) {
                answered = true;
                showBookShape(cells, step);
            } else {
                String hint = count[0] < step.buildTarget
                    ? "کمی کم است؛ " + fa(step.buildTarget - count[0]) + " خانه‌ی دیگر لازم داری."
                    : "کمی زیاد شد؛ " + fa(count[0] - step.buildTarget) + " خانه را بردار.";
                mascot().comfort(hint);
            }
            onLessonAnswer(right, right ? step.why : "شکل بعدی " + fa(step.buildTarget) + " خانه دارد. دوباره بشمار.");
        });
        box.addView(check, UiKit.marginParams(requireContext(), 12, 0));
        return box;
    }

    /** Once the count is right, the book's own arrangement is laid over the child's grid. */
    private void showBookShape(TextView[][] cells, LessonStep step) {
        if (step.buildShape == null) return;
        for (int r = 0; r < cells.length; r++) {
            int fromBottom = cells.length - 1 - r;
            int inRow = fromBottom < step.buildShape.length ? step.buildShape[fromBottom] : 0;
            for (int c = 0; c < cells[r].length; c++) {
                boolean inBookShape = c < inRow;
                cells[r][c].setBackground(UiKit.roundedBg(
                    ContextCompat.getColor(requireContext(), inBookShape ? R.color.orange : R.color.bg_card),
                    ContextCompat.getColor(requireContext(), inBookShape ? R.color.orange_dark : R.color.border_input),
                    6f, requireContext()));
            }
        }
    }

    private void paintCell(TextView cell, boolean on) {
        cell.setBackground(UiKit.roundedBg(
            ContextCompat.getColor(requireContext(), on ? R.color.teal : R.color.bg_card),
            ContextCompat.getColor(requireContext(), on ? R.color.teal_dark : R.color.border_input),
            6f, requireContext()));
    }

    private View buildDoneCard() {
        LinearLayout card = UiKit.column(requireContext());
        card.setGravity(Gravity.CENTER);
        card.setPadding(UiKit.dp(requireContext(), 20), UiKit.dp(requireContext(), 20), UiKit.dp(requireContext(), 20), UiKit.dp(requireContext(), 20));
        card.setBackground(UiKit.roundedBg(ContextCompat.getColor(requireContext(), R.color.orange_bg),
            ContextCompat.getColor(requireContext(), R.color.orange_border), 22f, requireContext()));

        LinearLayout diamonds = UiKit.row(requireContext());
        diamonds.setGravity(Gravity.CENTER);
        for (int i = 0; i < 3; i++) {
            View d = new View(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(UiKit.dp(requireContext(), 20), UiKit.dp(requireContext(), 20));
            lp.setMargins(UiKit.dp(requireContext(), 4), 0, UiKit.dp(requireContext(), 4), 0);
            d.setLayoutParams(lp);
            d.setRotation(45f);
            d.setBackground(UiKit.roundedBg(ContextCompat.getColor(requireContext(), R.color.orange), 0, 4f, requireContext()));
            diamonds.addView(d);
        }
        card.addView(diamonds);

        TextView title = UiKit.text(requireContext(), "درس تمام شد!", 18f, R.color.text_primary, true);
        title.setGravity(Gravity.CENTER);
        card.addView(title, UiKit.marginParams(requireContext(), 12, 0));

        TextView sub = UiKit.text(requireContext(),
            fa(starsThisLesson) + " ستاره گرفتی. حالا تمرین‌های همین بخش را حل کن.", 13f, R.color.text_muted, false);
        sub.setGravity(Gravity.CENTER);
        card.addView(sub, UiKit.marginParams(requireContext(), 6, 12));

        int next = page > 0 ? nextPage() : 0;
        TextView cta = bigButton(next > 0 ? "برویم صفحه‌ی " + fa(next) + " کتاب" : "تمرین‌های این بخش",
            R.color.teal, R.color.white);
        cta.setOnClickListener(v -> {
            LessonAudio.stop();
            Bundle args = new Bundle();
            args.putInt("chapter", chapter);
            if (next > 0) {
                args.putInt("page", next);
                nav().go(Screen.LESSON, args);
            } else {
                args.putString("mode", "PRACTICE");
                args.putInt("option", section);
                nav().go(Screen.QUIZ, args);
            }
        });
        card.addView(cta);

        if (next > 0) {
            TextView practice = bigButton("تمرین‌های این بخش", R.color.bg_card, R.color.text_primary);
            practice.setBackground(UiKit.roundedBg(
                ContextCompat.getColor(requireContext(), R.color.bg_card),
                ContextCompat.getColor(requireContext(), R.color.border_input), 16f, requireContext()));
            practice.setOnClickListener(v -> {
                LessonAudio.stop();
                Bundle args = new Bundle();
                args.putString("mode", "PRACTICE");
                args.putInt("chapter", chapter);
                args.putInt("option", section);
                nav().go(Screen.QUIZ, args);
            });
            card.addView(practice, UiKit.marginParams(requireContext(), 9, 0));
        }
        return card;
    }

    /** The page after this one in the book, or 0 when this chapter's pages are finished. */
    private int nextPage() {
        List<Integer> pages = PageLessons.pagesOfChapter(chapter);
        int at = pages.indexOf(page);
        return at >= 0 && at < pages.size() - 1 ? pages.get(at + 1) : 0;
    }

    /** While هوهو is still talking, the "got it" button waits — the child listens first. */
    private void updateContinueEnabled() {
        if (continueButton == null) return;
        boolean ready = !narrating;
        continueButton.setAlpha(ready ? 1f : 0.45f);
        continueButton.setEnabled(ready);
        continueButton.setText(ready ? "فهمیدم، برویم!" : "هوهو دارد توضیح می‌دهد…");
    }

    private TextView heading(String label) {
        TextView tv = UiKit.text(requireContext(), label, 15f, R.color.text_primary, true);
        tv.setLayoutParams(UiKit.marginParams(requireContext(), 0, 10));
        return tv;
    }

    private TextView bigButton(String label, int bgColorRes, int textColorRes) {
        TextView btn = UiKit.text(requireContext(), label, 15f, textColorRes, true);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(0, UiKit.dp(requireContext(), 15), 0, UiKit.dp(requireContext(), 15));
        btn.setBackground(UiKit.roundedBg(ContextCompat.getColor(requireContext(), bgColorRes), 0, 16f, requireContext()));
        btn.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        UiKit.tapSound(btn);
        return btn;
    }

    private void onLessonAnswer(boolean correct, String why) {
        LessonAudio.stop();
        AppState s = state();
        if (correct) {
            s.addStars(1);
            starsThisLesson++;
            mascot().celebrate(why);
            FeedbackDialog.show(requireContext(), true, why, "گام بعد", this::advance);
        } else {
            mascot().comfort("نزدیک بودی. " + why);
            FeedbackDialog.show(requireContext(), false, why, "دوباره تلاش کن", () -> {
                answered = false;
                buildContent(script.steps.get(stepIndex));
                narrateCurrentStep();
            });
        }
    }

    private void advance() {
        goToStep(Math.min(stepIndex + 1, script.steps.size() - 1));
    }

    @Override
    protected String entryTip() {
        return null; // the lesson narrates itself
    }
}
