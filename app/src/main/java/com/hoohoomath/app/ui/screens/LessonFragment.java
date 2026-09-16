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
import com.hoohoomath.app.data.Chapter1Lessons;
import com.hoohoomath.app.data.LessonKind;
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
    private StringBuilder typed = new StringBuilder();
    private boolean answered = false;

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
        ((TextView) view.findViewById(R.id.lesson_title)).setText(ch.sections.get(section));

        renderStep(view);
    }

    private void renderStep(View root) {
        LessonStep step = script.steps.get(stepIndex);
        answered = false;
        typed.setLength(0);

        ((TextView) root.findViewById(R.id.lesson_step_count)).setText("گام " + fa(stepIndex + 1) + " از " + fa(script.steps.size()));
        ((TextView) root.findViewById(R.id.lesson_caption)).setText(step.caption);

        LinearLayout dots = root.findViewById(R.id.lesson_dots);
        dots.removeAllViews();
        for (int i = 0; i < script.steps.size(); i++) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(UiKit.dp(requireContext(), 8), UiKit.dp(requireContext(), 8));
            lp.setMargins(UiKit.dp(requireContext(), 3), 0, UiKit.dp(requireContext(), 3), 0);
            dot.setLayoutParams(lp);
            int colorRes = i < stepIndex ? R.color.teal : i == stepIndex ? R.color.orange : R.color.border_card;
            dot.setBackground(UiKit.roundedBg(ContextCompat.getColor(requireContext(), colorRes), 0, 999f, requireContext()));
            dots.addView(dot);
        }

        buildContent(root, step);
        mascot().say(step.say);
    }

    private void buildContent(View root, LessonStep step) {
        LinearLayout content = root.findViewById(R.id.lesson_content);
        content.removeAllViews();

        switch (step.kind) {
            case TEACH: {
                content.addView(nextButton(root, "گام بعد"));
                break;
            }
            case DONE: {
                content.addView(nextButton(root, "برویم تمرین‌های همین بخش؟", true));
                break;
            }
            case MCQ: {
                for (int i = 0; i < step.options.size(); i++) {
                    int idx = i;
                    TextView btn = UiKit.text(requireContext(), step.options.get(i), 15f, R.color.text_primary, true);
                    btn.setGravity(Gravity.CENTER);
                    btn.setPadding(0, UiKit.dp(requireContext(), 16), 0, UiKit.dp(requireContext(), 16));
                    btn.setBackground(UiKit.roundedBg(ContextCompat.getColor(requireContext(), R.color.bg_card), ContextCompat.getColor(requireContext(), R.color.border_input), 14f, requireContext()));
                    LinearLayout.LayoutParams lp = UiKit.marginParams(requireContext(), 0, 10);
                    btn.setLayoutParams(lp);
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
                TextView display = UiKit.text(requireContext(), "⬜", 22f, R.color.text_primary, true);
                display.setGravity(Gravity.CENTER);
                display.setPadding(0, UiKit.dp(requireContext(), 14), 0, UiKit.dp(requireContext(), 14));
                display.setBackground(UiKit.roundedBg(ContextCompat.getColor(requireContext(), R.color.bg_card), ContextCompat.getColor(requireContext(), R.color.border_input), 14f, requireContext()));
                content.addView(display, UiKit.marginParams(requireContext(), 0, 10));

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

                TextView submit = submitButtonView();
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

    private TextView submitButtonView() {
        TextView submit = UiKit.text(requireContext(), "بررسی پاسخ", 15f, R.color.white, true);
        submit.setGravity(Gravity.CENTER);
        submit.setPadding(0, UiKit.dp(requireContext(), 14), 0, UiKit.dp(requireContext(), 14));
        submit.setBackground(UiKit.roundedBg(ContextCompat.getColor(requireContext(), R.color.teal), 0, 14f, requireContext()));
        return submit;
    }

    private View nextButton(View root, String label) {
        return nextButton(root, label, false);
    }

    private View nextButton(View root, String label, boolean isDone) {
        TextView btn = UiKit.text(requireContext(), label, 15f, R.color.white, true);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(0, UiKit.dp(requireContext(), 15), 0, UiKit.dp(requireContext(), 15));
        btn.setBackground(UiKit.roundedBg(ContextCompat.getColor(requireContext(), R.color.teal), 0, 14f, requireContext()));
        btn.setOnClickListener(v -> {
            if (isDone) {
                Bundle args = new Bundle();
                args.putString("mode", "PRACTICE");
                args.putInt("chapter", chapter);
                args.putInt("option", section);
                nav().go(Screen.QUIZ, args);
            } else {
                advance(root);
            }
        });
        return btn;
    }

    private void onLessonAnswer(View root, boolean correct, String why) {
        AppState s = state();
        if (correct) {
            s.addStars(1);
            mascot().celebrate(why);
            FeedbackDialog.show(requireContext(), true, why, stepIndex >= script.steps.size() - 2 ? "دیدن ادامه" : "گام بعد", () -> advance(root));
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
