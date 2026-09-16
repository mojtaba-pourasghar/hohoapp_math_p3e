package com.hoohoomath.app.ui.screens;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.hoohoomath.app.R;
import com.hoohoomath.app.data.AppState;
import com.hoohoomath.app.data.Book;
import com.hoohoomath.app.data.Level;
import com.hoohoomath.app.data.QuestionItem;
import com.hoohoomath.app.data.QuizBuilder;
import com.hoohoomath.app.data.QuizMode;
import com.hoohoomath.app.data.QuizSession;
import com.hoohoomath.app.data.QuizSessionHolder;
import com.hoohoomath.app.tts.TtsManager;
import com.hoohoomath.app.ui.BaseFragment;
import com.hoohoomath.app.ui.FeedbackDialog;
import com.hoohoomath.app.ui.Screen;
import com.hoohoomath.app.ui.UiKit;

import static com.hoohoomath.app.data.PersianDigits.fa;

public class QuizFragment extends BaseFragment {
    private QuizSession session;
    private final StringBuilder typed = new StringBuilder();
    private boolean answered = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quiz, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AppState s = state();
        Bundle args = getArguments();
        QuizMode mode = QuizMode.valueOf(args != null ? args.getString("mode", "PRACTICE") : "PRACTICE");
        int chapter = args != null ? args.getInt("chapter", s.taughtChapter) : s.taughtChapter;
        int option = args != null ? args.getInt("option", 0) : 0;

        int round = s.getRound(roundKey(mode, chapter, option));
        session = QuizBuilder.build(mode, chapter, option, s.taughtChapter, s.taughtSection, round);
        if (session == null) {
            FeedbackDialog.info(requireContext(), "این بخش هنوز درس داده نشده است. در پنل والدین مشخص کنید معلم تا کجا پیش رفته.");
            nav().go(Screen.MAP);
            return;
        }

        view.findViewById(R.id.quiz_exit).setOnClickListener(v -> exitQuiz());

        Book.Chapter ch = Book.chapter(chapter);
        String modeTitle = mode == QuizMode.PRACTICE ? "تمرین بخش" : mode == QuizMode.WORKSHEET ? "کاربرگ" : "آزمون";
        ((TextView) view.findViewById(R.id.quiz_title)).setText(modeTitle + " — فصل " + ch.numberFa + ": " + ch.title);

        String sub = mode == QuizMode.PRACTICE ? ch.sections.get(option) : Level.fromIndex(option).title;
        ((TextView) view.findViewById(R.id.quiz_subtitle)).setText(sub + " · دور " + fa(round + 1));

        view.findViewById(R.id.quiz_read_aloud).setOnClickListener(v -> readQuestionAloud());
        view.findViewById(R.id.quiz_hint).setOnClickListener(v -> {
            QuestionItem it = session.current();
            mascot().say(it.hint);
        });
        view.findViewById(R.id.quiz_reveal).setOnClickListener(v -> {
            if (state().parentUnlockedThisSession) {
                QuestionItem it = session.current();
                FeedbackDialog.info(requireContext(), "پاسخ درست: " + it.answerFa + " — " + it.hint);
            } else {
                nav().goParent();
            }
        });

        renderQuestion(view);
    }

    private static String roundKey(QuizMode mode, int chapter, int option) {
        return mode.name() + "_" + chapter + "_" + option;
    }

    private void readQuestionAloud() {
        TtsManager tts = TtsManager.get();
        if (tts != null) tts.speak(session.current().question);
    }

    private void exitQuiz() {
        if (session.mode == QuizMode.PRACTICE) nav().go(Screen.SECTIONS);
        else if (session.mode == QuizMode.WORKSHEET) nav().go(Screen.WORKSHEET_INDEX);
        else nav().go(Screen.EXAM_INDEX);
    }

    private void renderQuestion(View root) {
        answered = false;
        typed.setLength(0);
        QuestionItem it = session.current();

        ((TextView) root.findViewById(R.id.quiz_number)).setText("سؤال " + fa(session.index + 1));
        ((TextView) root.findViewById(R.id.quiz_count)).setText(fa(session.index + 1) + " / " + fa(session.items.size()));
        ((ProgressBar) root.findViewById(R.id.quiz_progress)).setProgress(Math.round((session.index / (float) session.items.size()) * 100));
        ((TextView) root.findViewById(R.id.quiz_question)).setText(it.question);

        AppState s = state();
        TextView reveal = root.findViewById(R.id.quiz_reveal);
        reveal.setText(s.parentUnlockedThisSession ? "نمایش پاسخ (باز است)" : "نمایش پاسخ — ورود والدین");
        ((TextView) root.findViewById(R.id.quiz_reveal_note)).setText(s.parentUnlockedThisSession
            ? "دسترسی والدین باز است؛ پاسخ‌ها نشان داده می‌شوند."
            : "پاسخ‌ها فقط با رمز والدین دیده می‌شوند تا خودت تمرین کنی.");
        ((TextView) root.findViewById(R.id.quiz_progress_note)).setText(
            "درست تا حالا: " + fa(session.rightCount) + " از " + fa(session.index)
                + " · این تمرین فقط از بخش‌هایی است که معلم درس داده.");

        LinearLayout area = root.findViewById(R.id.quiz_answer_area);
        area.removeAllViews();

        if (session.mode == QuizMode.EXAM) {
            for (String optionLabel : it.options) {
                TextView btn = UiKit.text(requireContext(), optionLabel, 19f, R.color.text_primary, true);
                btn.setGravity(Gravity.CENTER);
                btn.setPadding(0, UiKit.dp(requireContext(), 18), 0, UiKit.dp(requireContext(), 18));
                btn.setBackground(UiKit.roundedBg(ContextCompat.getColor(requireContext(), R.color.bg_card),
                    ContextCompat.getColor(requireContext(), R.color.border_input), 17f, requireContext()));
                btn.setLayoutParams(UiKit.marginParams(requireContext(), 0, 9));
                btn.setOnClickListener(v -> {
                    if (answered) return;
                    answered = true;
                    onAnswer(root, optionLabel);
                });
                area.addView(btn);
            }
        } else {
            TextView display = UiKit.text(requireContext(), "⬜", 30f, R.color.text_primary, true);
            display.setGravity(Gravity.CENTER);
            display.setPadding(0, UiKit.dp(requireContext(), 16), 0, UiKit.dp(requireContext(), 16));
            display.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_answer_box));
            area.addView(display, UiKit.marginParams(requireContext(), 0, 12));

            LinearLayout keypadWrap = new LinearLayout(requireContext());
            keypadWrap.setGravity(Gravity.CENTER);
            keypadWrap.addView(UiKit.buildKeypad(requireContext(), key -> {
                if (answered) return;
                if (key.equals("C")) typed.setLength(0);
                else if (key.equals("⌫")) { if (typed.length() > 0) typed.deleteCharAt(typed.length() - 1); }
                else if (typed.length() < 5) typed.append(key);
                display.setText(typed.length() == 0 ? "⬜" : typed.toString());
            }));
            area.addView(keypadWrap);

            TextView submit = UiKit.text(requireContext(), "بفرست", 15f, R.color.white, true);
            submit.setGravity(Gravity.CENTER);
            submit.setPadding(0, UiKit.dp(requireContext(), 15), 0, UiKit.dp(requireContext(), 15));
            submit.setBackground(UiKit.roundedBg(ContextCompat.getColor(requireContext(), R.color.orange), 0, 16f, requireContext()));
            submit.setOnClickListener(v -> {
                if (answered || typed.length() == 0) return;
                answered = true;
                onAnswer(root, typed.toString());
            });
            area.addView(submit, UiKit.marginParams(requireContext(), 12, 0));
        }

        // a 3rd-grader may not read fluently yet, so هوهو reads every question out loud
        if (state().settings[0]) readQuestionAloud();
    }

    private void onAnswer(View root, String candidate) {
        QuestionItem it = session.current();
        boolean correct = candidate.equals(it.answerFa);
        session.log.add(new QuizSession.LogEntry(it.question, it.answerFa, candidate, correct));
        boolean done = session.index >= session.items.size() - 1;
        if (correct) session.rightCount++;

        if (correct) state().addStars(1);

        String btnLabel = done ? "دیدن کارنامه" : "سؤال بعد";
        Runnable proceed = () -> {
            if (done) finish();
            else {
                session.index++;
                renderQuestion(root);
            }
        };

        if (correct) {
            mascot().celebrate(it.hint);
            FeedbackDialog.show(requireContext(), true, it.hint, btnLabel, proceed);
        } else {
            String msg = "جواب درست " + it.answerFa + " است. " + it.hint;
            mascot().comfort(msg);
            FeedbackDialog.show(requireContext(), false, msg, btnLabel, proceed);
        }
    }

    private void finish() {
        AppState s = state();
        s.recordResult(session);
        // next time this same set is opened, generate a fresh round of questions
        s.bumpRound(roundKey(session.mode, session.chapter, session.option));
        QuizSessionHolder.set(session);
        nav().go(Screen.RESULT);
    }

    @Override
    protected String entryTip() {
        return null;
    }
}
