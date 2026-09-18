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
import com.hoohoomath.app.tts.LessonAudio;
import com.hoohoomath.app.tts.QuestionVoice;
import com.hoohoomath.app.ui.BaseFragment;
import com.hoohoomath.app.ui.FeedbackDialog;
import com.hoohoomath.app.ui.Screen;
import com.hoohoomath.app.ui.UiKit;

import java.util.List;

import static com.hoohoomath.app.data.PersianDigits.fa;

/**
 * Practice, worksheet and exam all run through here. The whole set is built up front and the
 * child may move between questions in any order, change an earlier answer, hear the question
 * again, and leave in the middle — the attempt is saved and picked up exactly where it stopped.
 */
public class QuizFragment extends BaseFragment {
    private QuizSession session;
    private String attemptKey;
    private boolean[] starAwarded;
    private final StringBuilder typed = new StringBuilder();

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
        int chapter = args != null ? args.getInt("chapter", 0) : 0;
        int option = args != null ? args.getInt("option", 0) : 0;

        String key = roundKey(mode, chapter, option);
        int round = s.getRound(key);
        session = QuizBuilder.build(mode, chapter, option, round);
        attemptKey = key;
        starAwarded = new boolean[session.items.size()];

        // pick the half-finished attempt back up, answers and position included
        List<String> saved = s.attemptAnswers(attemptKey);
        if (saved != null) {
            session.restoreAnswers(saved);
            for (int i = 0; i < starAwarded.length; i++) starAwarded[i] = session.isCorrectAt(i);
            int savedIndex = s.attemptIndex(attemptKey);
            session.index = savedIndex >= 0 && savedIndex < session.items.size() ? savedIndex : 0;
        }

        view.findViewById(R.id.quiz_exit).setOnClickListener(v -> exitQuiz());

        Book.Chapter ch = Book.chapter(chapter);
        String modeTitle = mode == QuizMode.PRACTICE ? "تمرین بخش" : mode == QuizMode.WORKSHEET ? "کاربرگ" : "آزمون";
        ((TextView) view.findViewById(R.id.quiz_title)).setText(modeTitle + " — فصل " + ch.numberFa + ": " + ch.title);

        String sub = mode == QuizMode.PRACTICE ? ch.sections.get(option) : Level.fromIndex(option).title;
        ((TextView) view.findViewById(R.id.quiz_subtitle)).setText(sub + " · دور " + fa(round + 1));

        view.findViewById(R.id.quiz_read_aloud).setOnClickListener(v -> readQuestionAloud());
        view.findViewById(R.id.quiz_repeat).setOnClickListener(v -> readQuestionAloud());
        view.findViewById(R.id.quiz_prev).setOnClickListener(v -> goTo(view, session.index - 1));
        view.findViewById(R.id.quiz_next).setOnClickListener(v -> goTo(view, session.index + 1));
        view.findViewById(R.id.quiz_finish).setOnClickListener(v -> finish());

        view.findViewById(R.id.quiz_hint).setOnClickListener(v -> {
            QuestionItem it = session.current();
            // the hint is هوهو's own voice too, not the phone's
            mascot().showBubble(it.hint, true);
            LessonAudio.playSequence(requireContext(), QuestionVoice.clips(it.hint), it.hint,
                new LessonAudio.PlaybackListener() {
                    @Override public void onStarted(long durationMs) {}
                    @Override public void onFinished() {
                        if (isAdded()) mascot().showBubble(it.hint, false);
                    }
                });
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

    /** Moving between questions never loses anything, so the position is saved on every step. */
    private void goTo(View root, int target) {
        if (target < 0) {
            mascot().say("این اولین سؤال است؛ عقب‌تر از این نداریم.");
            return;
        }
        if (target >= session.items.size()) {
            mascot().say("این آخرین سؤال است. اگر تمام شد، «پایان و دیدن کارنامه» را بزن.");
            return;
        }
        session.index = target;
        saveAttempt();
        renderQuestion(root);
    }

    private void saveAttempt() {
        state().saveAttempt(attemptKey, session.index, session.answers());
    }

    /** هوهو reads the question herself, from her recorded words. */
    private void readQuestionAloud() {
        String question = session.current().question;
        LessonAudio.playSequence(requireContext(), QuestionVoice.clips(question), question, null);
    }

    private void exitQuiz() {
        LessonAudio.stop();
        saveAttempt();
        if (session.mode == QuizMode.PRACTICE) nav().go(Screen.SECTIONS);
        else if (session.mode == QuizMode.WORKSHEET) nav().go(Screen.WORKSHEET_INDEX);
        else nav().go(Screen.EXAM_INDEX);
    }

    @Override
    public void onPause() {
        super.onPause();
        LessonAudio.stop();
        if (session != null && attemptKey != null) saveAttempt();
    }

    private void renderQuestion(View root) {
        typed.setLength(0);
        QuestionItem it = session.current();
        String mine = session.currentAnswer();

        ((TextView) root.findViewById(R.id.quiz_number)).setText("سؤال " + fa(session.index + 1));
        ((TextView) root.findViewById(R.id.quiz_count)).setText(fa(session.index + 1) + " / " + fa(session.items.size()));
        ((ProgressBar) root.findViewById(R.id.quiz_progress)).setProgress(
            Math.round((session.answeredCount() / (float) session.items.size()) * 100));
        ((TextView) root.findViewById(R.id.quiz_question)).setText(it.question);

        AppState s = state();
        TextView reveal = root.findViewById(R.id.quiz_reveal);
        reveal.setText(s.parentUnlockedThisSession ? "نمایش پاسخ (باز است)" : "نمایش پاسخ — ورود والدین");
        ((TextView) root.findViewById(R.id.quiz_reveal_note)).setText(s.parentUnlockedThisSession
            ? "دسترسی والدین باز است؛ پاسخ‌ها نشان داده می‌شوند."
            : "پاسخ‌ها فقط با رمز والدین دیده می‌شوند تا خودت تمرین کنی.");

        int remaining = session.items.size() - session.answeredCount();
        ((TextView) root.findViewById(R.id.quiz_progress_note)).setText(
            "جواب داده‌ای: " + fa(session.answeredCount()) + " از " + fa(session.items.size())
                + " · درست: " + fa(session.rightCount())
                + (remaining == 0 ? " · همه را جواب دادی!" : " · " + fa(remaining) + " سؤال مانده")
                + "\nمی‌توانی با «سؤال قبل» و «سؤال بعد» آزادانه جابه‌جا شوی و جوابت را عوض کنی.");

        TextView finish = root.findViewById(R.id.quiz_finish);
        finish.setText(remaining == 0
            ? "پایان و دیدن کارنامه"
            : "پایان و دیدن کارنامه (" + fa(remaining) + " سؤال بی‌جواب)");

        root.findViewById(R.id.quiz_prev).setVisibility(session.index == 0 ? View.INVISIBLE : View.VISIBLE);
        root.findViewById(R.id.quiz_next).setVisibility(session.isLast() ? View.INVISIBLE : View.VISIBLE);

        LinearLayout area = root.findViewById(R.id.quiz_answer_area);
        area.removeAllViews();

        if (session.mode == QuizMode.EXAM) {
            for (String optionLabel : it.options) {
                boolean chosen = optionLabel.equals(mine);
                TextView btn = UiKit.text(requireContext(), optionLabel, 19f, R.color.text_primary, true);
                btn.setGravity(Gravity.CENTER);
                btn.setPadding(0, UiKit.dp(requireContext(), 18), 0, UiKit.dp(requireContext(), 18));
                btn.setBackground(UiKit.roundedBg(
                    ContextCompat.getColor(requireContext(), chosen ? R.color.orange_bg : R.color.bg_card),
                    ContextCompat.getColor(requireContext(), chosen ? R.color.orange : R.color.border_input),
                    17f, requireContext()));
                btn.setLayoutParams(UiKit.marginParams(requireContext(), 0, 9));
                btn.setOnClickListener(v -> onAnswer(root, optionLabel));
                area.addView(btn);
            }
        } else {
            if (mine != null) typed.append(mine);
            TextView display = UiKit.text(requireContext(),
                typed.length() == 0 ? "⬜" : typed.toString(), 30f, R.color.text_primary, true);
            display.setGravity(Gravity.CENTER);
            display.setPadding(0, UiKit.dp(requireContext(), 16), 0, UiKit.dp(requireContext(), 16));
            display.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_answer_box));
            area.addView(display, UiKit.marginParams(requireContext(), 0, 12));

            LinearLayout keypadWrap = new LinearLayout(requireContext());
            keypadWrap.setGravity(Gravity.CENTER);
            keypadWrap.addView(UiKit.buildKeypad(requireContext(), key -> {
                if (key.equals("C")) typed.setLength(0);
                else if (key.equals("⌫")) { if (typed.length() > 0) typed.deleteCharAt(typed.length() - 1); }
                else if (typed.length() < 5) typed.append(key);
                display.setText(typed.length() == 0 ? "⬜" : typed.toString());
            }));
            area.addView(keypadWrap);

            TextView submit = UiKit.text(requireContext(),
                mine == null ? "بفرست" : "جواب را عوض کن", 15f, R.color.white, true);
            submit.setGravity(Gravity.CENTER);
            submit.setPadding(0, UiKit.dp(requireContext(), 15), 0, UiKit.dp(requireContext(), 15));
            submit.setBackground(UiKit.roundedBg(ContextCompat.getColor(requireContext(), R.color.orange), 0, 16f, requireContext()));
            submit.setOnClickListener(v -> {
                if (typed.length() == 0) return;
                onAnswer(root, typed.toString());
            });
            area.addView(submit, UiKit.marginParams(requireContext(), 12, 0));
        }

        // a 3rd-grader may not read fluently yet, so هوهو reads every question out loud
        if (s.readAloud()) readQuestionAloud();
    }

    private void onAnswer(View root, String candidate) {
        int at = session.index;
        QuestionItem it = session.items.get(at);
        boolean correct = candidate.equals(it.answerFa);
        session.record(at, candidate);
        saveAttempt();

        // a star the first time a question is right, so re-answering can't farm stars
        if (correct && !starAwarded[at]) {
            starAwarded[at] = true;
            state().addStars(1);
        }

        boolean last = at >= session.items.size() - 1;
        boolean done = session.allAnswered();
        String btnLabel = last ? (done ? "دیدن کارنامه" : "برو به سؤال بی‌جواب") : "سؤال بعد";
        Runnable proceed = () -> {
            if (!last) goTo(root, at + 1);
            else if (done) finish();
            else goTo(root, session.firstUnanswered());
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
        // the attempt is over: forget it and generate a fresh round of questions next time
        s.clearAttempt(attemptKey);
        s.bumpRound(attemptKey);
        QuizSessionHolder.set(session);
        nav().go(Screen.RESULT);
    }

    @Override
    protected String entryTip() {
        return null;
    }
}
