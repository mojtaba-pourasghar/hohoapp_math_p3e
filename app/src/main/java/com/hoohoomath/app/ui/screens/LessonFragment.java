package com.hoohoomath.app.ui.screens;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.hoohoomath.app.R;
import com.hoohoomath.app.data.AppState;
import com.hoohoomath.app.data.Book;
import com.hoohoomath.app.data.Chapter1Lessons;
import com.hoohoomath.app.data.LessonScript;
import com.hoohoomath.app.data.LessonStep;
import com.hoohoomath.app.ui.BaseFragment;
import com.hoohoomath.app.ui.FeedbackDialog;
import com.hoohoomath.app.ui.Screen;
import com.hoohoomath.app.ui.UiKit;

import static com.hoohoomath.app.data.PersianDigits.fa;

public class LessonFragment extends BaseFragment {

    private int chapter, section;
    private LessonScript script;
    private int stepIndex = 0;
    private final StringBuilder typed = new StringBuilder();
    private boolean answered = false;
    private int starsThisLesson = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lesson, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        chapter = args != null ? args.getInt("chapter", 0) : 0;
        section = args != null ? args.getInt("section", 0) : 0;
        script = Chapter1Lessons.forSection(section);

        view.findViewById(R.id.lesson_back).setOnClickListener(v -> nav().go(Screen.MAP));

        if (script == null) {
            ((TextView) view.findViewById(R.id.lesson_caption)).setText("درسِ صوتی این بخش هنوز آماده نیست.");
            return;
        }

        Book.Chapter ch = Book.chapter(chapter);
        ((TextView) view.findViewById(R.id.lesson_title)).setText("درس " + fa(section + 1) + ": " + ch.sections.get(section));

        view.findViewById(R.id.lesson_replay).setOnClickListener(v -> repeatCurrentStep());

        renderStep(view);
    }

    private void repeatCurrentStep() {
        mascot().say(script.steps.get(stepIndex).say);
    }

    private void renderStep(View root) {
        LessonStep step = script.steps.get(stepIndex);
        answered = false;
        typed.setLength(0);

        ((TextView) root.findViewById(R.id.lesson_step_count)).setText(
            "صفحه‌ی " + fa(script.bookPage) + " کتاب · گام " + fa(stepIndex + 1) + " از " + fa(script.steps.size()));
        ((TextView) root.findViewById(R.id.lesson_say)).setText(step.say);
        ((TextView) root.findViewById(R.id.lesson_caption)).setText(step.caption);

        LinearLayout dots = root.findViewById(R.id.lesson_dots);
        dots.removeAllViews();
        for (int i = 0; i < script.steps.size(); i++) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(UiKit.dp(requireContext(), 14), UiKit.dp(requireContext(), 6));
            lp.setMargins(UiKit.dp(requireContext(), 2), 0, UiKit.dp(requireContext(), 2), 0);
            dot.setLayoutParams(lp);
            int colorRes = i < stepIndex ? R.color.teal : i == stepIndex ? R.color.orange : R.color.border_card;
            dot.setBackground(UiKit.roundedBg(ContextCompat.getColor(requireContext(), colorRes), 0, 999f, requireContext()));
            dots.addView(dot);
        }

        View visualCard = root.findViewById(R.id.lesson_visual_card);
        FrameLayout visualHolder = root.findViewById(R.id.lesson_visual_holder);
        visualHolder.removeAllViews();
        if (step.hasVisual()) {
            visualCard.setVisibility(View.VISIBLE);
            ((TextView) root.findViewById(R.id.lesson_visual_caption)).setText(step.caption);
            visualHolder.addView(UiKit.buildCountingGroups(requireContext(), step.visualGroups, step.perGroup));
        } else {
            visualCard.setVisibility(View.GONE);
        }

        buildContent(root, step);
        mascot().say(step.say);
    }

    private void buildContent(View root, LessonStep step) {
        LinearLayout content = root.findViewById(R.id.lesson_content);
        content.removeAllViews();

        switch (step.kind) {
            case TEACH: {
                content.addView(sectionHeading("فهمیدی؟"));
                content.addView(UiKit.text(requireContext(),
                    "اگر روشن بود برویم گام بعد؛ اگر نه، «دوباره بگو» را بزن تا هوهو همین قسمت را دوباره بگوید.",
                    12.5f, R.color.text_muted, false), UiKit.marginParams(requireContext(), 4, 12));

                LinearLayout buttons = UiKit.row(requireContext());
                TextView go = bigButton("فهمیدم، برویم!", R.color.teal, R.color.white);
                go.setOnClickListener(v -> advance(root));
                TextView again = bigButton("دوباره بگو", R.color.bg_card, R.color.text_primary);
                again.setBackground(UiKit.roundedBg(ContextCompat.getColor(requireContext(), R.color.bg_card),
                    ContextCompat.getColor(requireContext(), R.color.border_input), 16f, requireContext()));
                again.setOnClickListener(v -> repeatCurrentStep());

                LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                half.setMarginEnd(UiKit.dp(requireContext(), 5));
                LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                half2.setMarginStart(UiKit.dp(requireContext(), 5));
                buttons.addView(go, half);
                buttons.addView(again, half2);
                content.addView(buttons, UiKit.marginParams(requireContext(), 0, 0));
                break;
            }
            case DONE: {
                state().markSectionLessonDone(chapter, section);

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
                    fa(starsThisLesson) + " ستاره گرفتی. حالا تمرین‌های همین بخش را حل کن.",
                    13f, R.color.text_muted, false);
                sub.setGravity(Gravity.CENTER);
                card.addView(sub, UiKit.marginParams(requireContext(), 6, 12));

                TextView cta = bigButton("تمرین‌های این بخش", R.color.teal, R.color.white);
                cta.setOnClickListener(v -> startPractice());
                card.addView(cta);

                content.addView(card);
                break;
            }
            case MCQ: {
                content.addView(sectionHeading("جواب را انتخاب کن"));
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
                        onLessonAnswer(root, idx == step.correctIndex, step.why);
                    });
                    content.addView(btn);
                }
                break;
            }
            case NUM: {
                content.addView(sectionHeading("عدد را بنویس"));

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
                    onLessonAnswer(root, typed.toString().equals(step.answerFa), step.why);
                });
                content.addView(submit, UiKit.marginParams(requireContext(), 12, 0));
                break;
            }
        }
    }

    private TextView sectionHeading(String label) {
        TextView tv = UiKit.text(requireContext(), label, 15f, R.color.text_primary, true);
        LinearLayout.LayoutParams lp = UiKit.marginParams(requireContext(), 0, 10);
        tv.setLayoutParams(lp);
        return tv;
    }

    private TextView bigButton(String label, int bgColorRes, int textColorRes) {
        TextView btn = UiKit.text(requireContext(), label, 15f, textColorRes, true);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(0, UiKit.dp(requireContext(), 15), 0, UiKit.dp(requireContext(), 15));
        btn.setBackground(UiKit.roundedBg(ContextCompat.getColor(requireContext(), bgColorRes), 0, 16f, requireContext()));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btn.setLayoutParams(lp);
        return btn;
    }

    private void startPractice() {
        Bundle args = new Bundle();
        args.putString("mode", "PRACTICE");
        args.putInt("chapter", chapter);
        args.putInt("option", section);
        nav().go(Screen.QUIZ, args);
    }

    private void onLessonAnswer(View root, boolean correct, String why) {
        AppState s = state();
        if (correct) {
            s.addStars(1);
            starsThisLesson++;
            mascot().celebrate(why);
            FeedbackDialog.show(requireContext(), true, why, "گام بعد", () -> advance(root));
        } else {
            mascot().comfort("نزدیک بودی. " + why);
            FeedbackDialog.show(requireContext(), false, why, "دوباره تلاش کن", () -> {
                answered = false;
                buildContent(root, script.steps.get(stepIndex));
            });
        }
    }

    private void advance(View root) {
        stepIndex = Math.min(stepIndex + 1, script.steps.size() - 1);
        renderStep(root);
    }

    @Override
    protected String entryTip() {
        return null; // the lesson script itself narrates every step
    }
}
