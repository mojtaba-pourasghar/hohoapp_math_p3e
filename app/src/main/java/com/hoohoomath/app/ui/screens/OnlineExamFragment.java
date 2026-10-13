package com.hoohoomath.app.ui.screens;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.hoohoomath.app.R;
import com.hoohoomath.app.data.AppState;
import com.hoohoomath.app.ui.BaseFragment;
import com.hoohoomath.app.ui.UiKit;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static com.hoohoomath.app.data.PersianDigits.fa;

public class OnlineExamFragment extends BaseFragment {

    private boolean active = false;
    private String examTitle = "آزمون آنلاین ریاضی سوم";
    private String examDescription = "آزمون کشوری ریاضی سال سوم ابتدایی.";
    private String shamsiDate = "1405/08/15";
    private String timeStr = "16:00";
    private int durationMinutes = 45;

    private static class Question {
        String id;
        String title;
        String text;
        List<String> options = new ArrayList<>();
        int correctIndex;
        String explanation;
        int points = 10;
    }

    private final List<Question> questions = new ArrayList<>();
    private final List<Integer> userAnswers = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private boolean isExamRunning = false;

    private ViewGroup containerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list_index, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((TextView) view.findViewById(R.id.screen_title)).setText("آزمون آنلاین");
        containerView = view.findViewById(R.id.list_container);
        fetchExamConfig();
    }

    private void fetchExamConfig() {
        new Thread(() -> {
            try {
                URL url = new URL("http://mp-apdl.ir/grade-3/math/exam.json");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(2500);
                if (conn.getResponseCode() == 200) {
                    InputStream in = conn.getInputStream();
                    Scanner s = new Scanner(in).useDelimiter("\\A");
                    parseExamJson(s.hasNext() ? s.next() : "");
                } else {
                    useFallbackData();
                }
            } catch (Exception e) {
                useFallbackData();
            }
            new Handler(Looper.getMainLooper()).post(this::render);
        }).start();
    }

    private void parseExamJson(String jsonStr) {
        try {
            JSONObject obj = new JSONObject(jsonStr);
            active = obj.optBoolean("active", false);
            examTitle = obj.optString("title", examTitle);
            examDescription = obj.optString("description", examDescription);
            shamsiDate = obj.optString("shamsiDate", shamsiDate);
            timeStr = obj.optString("time", timeStr);

            JSONArray qArr = obj.optJSONArray("questions");
            questions.clear(); userAnswers.clear();
            if (qArr != null) {
                for (int i = 0; i < qArr.length(); i++) {
                    JSONObject qObj = qArr.getJSONObject(i);
                    Question q = new Question();
                    q.id = qObj.optString("id", "q" + i);
                    q.title = qObj.optString("title", "سوال " + fa(i + 1));
                    q.text = qObj.optString("text", "");
                    q.correctIndex = qObj.optInt("correctIndex", 0);
                    q.explanation = qObj.optString("explanation", "");

                    JSONArray opts = qObj.optJSONArray("options");
                    if (opts != null) {
                        for (int j = 0; j < opts.length(); j++) q.options.add(opts.getString(j));
                    }
                    questions.add(q); userAnswers.add(-1);
                }
            }
        } catch (Exception e) {
            useFallbackData();
        }
    }
    private void render() {
        if (containerView == null) return;
        containerView.removeAllViews();

        AppState s = state();
        boolean isRegistered = s.getBoolean("exam_registered_" + shamsiDate, false);
        boolean isCompleted = s.getBoolean("exam_completed_" + shamsiDate, false);

        if (!active && !isCompleted) {
            renderDisabledView();
            return;
        }

        if (isCompleted) {
            renderResultsView();
            return;
        }

        if (isExamRunning) {
            renderActiveExamQuestion();
            return;
        }

        renderRegistrationView(isRegistered);
    }

    private void renderDisabledView() {
        TextView title = UiKit.text(requireContext(), "⛔ آزمون آنلاین در حال حاضر غیرفعال است", 16f, R.color.text_primary, true);
        title.setGravity(Gravity.CENTER);
        title.setLayoutParams(UiKit.marginParams(requireContext(), 0, 16));

        TextView body = UiKit.text(requireContext(),
            "هیچ آزمون آنلاین فعالی ثبت نشده است.\nبه محض فعال‌سازی آزمون توسط مدیریت، اطلاع‌رسانی برای شما ارسال خواهد شد.",
            14f, R.color.text_muted, false);
        body.setGravity(Gravity.CENTER);
        body.setPadding(16, 24, 16, 24);
        body.setBackground(UiKit.roundedBg(ContextCompat.getColor(requireContext(), R.color.bg_card), 0, 16f, requireContext()));

        containerView.addView(title);
        containerView.addView(body);
    }

    private void renderRegistrationView(boolean isRegistered) {
        TextView title = UiKit.text(requireContext(), "🏆 " + examTitle, 17f, R.color.text_primary, true);
        title.setGravity(Gravity.CENTER);

        TextView desc = UiKit.text(requireContext(), examDescription, 13.5f, R.color.text_muted, false);
        desc.setGravity(Gravity.CENTER);
        desc.setPadding(12, 10, 12, 16);

        LinearLayout infoCard = UiKit.column(requireContext());
        infoCard.setPadding(16, 16, 16, 16);
        infoCard.setBackground(UiKit.roundedBg(ContextCompat.getColor(requireContext(), R.color.orange_bg), 0, 16f, requireContext()));

        infoCard.addView(UiKit.text(requireContext(), "📅 تاریخ برگزاری: " + fa(shamsiDate), 14f, R.color.orange_text, true));
        infoCard.addView(UiKit.text(requireContext(), "⏰ ساعت شروع: " + fa(timeStr), 14f, R.color.text_primary, false));

        containerView.addView(title);
        containerView.addView(desc);
        containerView.addView(infoCard);

        if (isRegistered) {
            TextView regStatus = UiKit.text(requireContext(), "✓ شما در این آزمون ثبت‌نام کرده‌اید.", 14.5f, R.color.teal_dark, true);
            regStatus.setGravity(Gravity.CENTER);
            regStatus.setPadding(0, 16, 0, 10);
            containerView.addView(regStatus);

            TextView startBtn = UiKit.primaryButton(requireContext(), "🚀 ورود و شروع آزمون",
                ContextCompat.getColor(requireContext(), R.color.orange), ContextCompat.getColor(requireContext(), R.color.white));
            startBtn.setOnClickListener(v -> { isExamRunning = true; render(); });
            containerView.addView(startBtn);
        } else {
            TextView regBtn = UiKit.primaryButton(requireContext(), "✏️ ثبت‌نام در آزمون آنلاین",
                ContextCompat.getColor(requireContext(), R.color.teal), ContextCompat.getColor(requireContext(), R.color.white));
            regBtn.setOnClickListener(v -> {
                state().setBoolean("exam_registered_" + shamsiDate, true);
                Toast.makeText(requireContext(), "ثبت‌نام شما با موفقیت انجام شد!", Toast.LENGTH_SHORT).show();
                render();
            });
            containerView.addView(regBtn);
        }
    }

    private void useFallbackData() {
        active = state().getBoolean("exam_active", false);
        if (questions.isEmpty()) {
            Question q1 = new Question();
    private void renderActiveExamQuestion() {
        if (currentQuestionIndex < 0 || currentQuestionIndex >= questions.size()) return;
        Question q = questions.get(currentQuestionIndex);

        TextView qTitle = UiKit.text(requireContext(), "سوال " + fa(currentQuestionIndex + 1) + " از " + fa(questions.size()), 15f, R.color.teal_dark, true);
        TextView qText = UiKit.text(requireContext(), q.text, 16f, R.color.text_primary, true);

        containerView.addView(qTitle);
        containerView.addView(qText);

        int selectedOpt = userAnswers.get(currentQuestionIndex);
        for (int i = 0; i < q.options.size(); i++) {
            final int optIdx = i;
            boolean isSel = selectedOpt == optIdx;
            TextView optView = UiKit.text(requireContext(), fa(i + 1) + ") " + q.options.get(i), 15f,
                isSel ? R.color.white : R.color.text_primary, true);
            optView.setPadding(16, 14, 16, 14);
            optView.setBackground(UiKit.roundedBg(
                ContextCompat.getColor(requireContext(), isSel ? R.color.teal : R.color.bg_card), 0, 12f, requireContext()));
            optView.setOnClickListener(v -> { userAnswers.set(currentQuestionIndex, optIdx); render(); });
            containerView.addView(optView);
        }

        TextView finishBtn = UiKit.primaryButton(requireContext(), "پایان آزمون و ثبت", R.color.pink_dark, R.color.white);
        finishBtn.setOnClickListener(v -> finishExam());
        containerView.addView(finishBtn);
    }

    private void finishExam() {
        isExamRunning = false;
        int score = 0;
        for (int i = 0; i < questions.size(); i++) {
            if (userAnswers.get(i) == questions.get(i).correctIndex) {
                score += questions.get(i).points;
            }
        }
        state().setBoolean("exam_completed_" + shamsiDate, true);
        state().setInt("exam_score_" + shamsiDate, score);
        render();
    }

    private void renderResultsView() {
        int score = state().getInt("exam_score_" + shamsiDate, 0);
        TextView title = UiKit.text(requireContext(), "🌟 کارنامه آزمون آنلاین", 17f, R.color.text_primary, true);
        title.setGravity(Gravity.CENTER);

        TextView scoreCard = UiKit.text(requireContext(),
            "امتیاز شما: " + fa(score) + "\nرتبه شما: " + fa(1) + " در بین شرکت‌کنندگان", 16f, R.color.white, true);
        scoreCard.setGravity(Gravity.CENTER);
        scoreCard.setPadding(16, 20, 16, 20);
        scoreCard.setBackground(UiKit.roundedBg(ContextCompat.getColor(requireContext(), R.color.teal), 0, 16f, requireContext()));

        containerView.addView(title);
        containerView.addView(scoreCard);
    }

    @Override
    protected String entryTip() {
        return "در آزمون آنلاین، همزمان با کودکان سراسر کشور رقابت کنید!";
    }
            q1.id = "q1"; q1.title = "سوال ۱: الگوها"; q1.text = "الگوی ۵، ۱۰، ۱۵، ۲۰، ؟ کدام است؟";
            q1.options.add("۲۲"); q1.options.add("۲۴"); q1.options.add("۲۵"); q1.options.add("۳۰");
            q1.correctIndex = 2; q1.explanation = "الگو ۵ تا ۵ تا اضافه می‌شود.";
            questions.add(q1); userAnswers.add(-1);
        }
    }