package com.hoohoomath.app.ui.screens;

import android.os.Bundle;
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
import com.hoohoomath.app.data.QuizSession;
import com.hoohoomath.app.data.QuizSessionHolder;
import com.hoohoomath.app.ui.BaseFragment;
import com.hoohoomath.app.ui.Screen;
import com.hoohoomath.app.ui.UiKit;

import static com.hoohoomath.app.data.PersianDigits.fa;

public class ResultFragment extends BaseFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        QuizSession res = QuizSessionHolder.get();
        if (res == null) {
            nav().go(Screen.MAP);
            return;
        }
        AppState s = state();
        boolean good = res.rightCount >= res.items.size() * 0.8;

        ((TextView) view.findViewById(R.id.result_score)).setText(fa(res.rightCount) + "/" + fa(res.items.size()));
        ((TextView) view.findViewById(R.id.result_title)).setText(good ? "عالی بود!" : "خوب شروع کردی");
        ((TextView) view.findViewById(R.id.result_msg)).setText(good
            ? "این بخش را خوب یاد گرفته‌ای. سطح بالاتر را امتحان کن."
            : "چند سؤال را با هوهو دوباره کار کن، بعد همین تمرین را تکرار کن.");

        LinearLayout list = view.findViewById(R.id.result_list);
        list.removeAllViews();
        int shown = Math.min(6, res.log.size());
        for (int i = 0; i < shown; i++) {
            QuizSession.LogEntry e = res.log.get(i);
            LinearLayout row = UiKit.column(requireContext());
            row.setPadding(UiKit.dp(requireContext(), 12), UiKit.dp(requireContext(), 10), UiKit.dp(requireContext(), 12), UiKit.dp(requireContext(), 10));
            UiKit.applyCardBg(row, requireContext(), R.color.bg_card, e.correct ? R.color.teal_border : R.color.pink_border);
            row.setLayoutParams(UiKit.marginParams(requireContext(), 0, 7));
            row.addView(UiKit.text(requireContext(), (e.correct ? "✓ " : "✕ ") + e.question, 13f, R.color.text_primary, true));
            String answerLine = e.correct ? ("پاسخ تو: " + e.yourAnswer)
                : (s.parentUnlockedThisSession ? ("پاسخ درست: " + e.correctAnswer + " · پاسخ تو: " + e.yourAnswer) : "پاسخ درست با ورود والدین دیده می‌شود");
            TextView answerView = UiKit.text(requireContext(), answerLine, 11.5f, e.correct ? R.color.teal_dark : R.color.pink_dark, false);
            answerView.setLayoutParams(UiKit.marginParams(requireContext(), 3, 0));
            row.addView(answerView);
            list.addView(row);
        }

        TextView answersBtn = view.findViewById(R.id.result_answers_btn);
        answersBtn.setText(s.parentUnlockedThisSession ? "پاسخ‌ها نمایش داده شد" : "دیدن پاسخ‌ها (والدین)");
        answersBtn.setOnClickListener(v -> {
            if (!s.parentUnlockedThisSession) nav().goParent();
        });

        view.findViewById(R.id.result_retry).setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("mode", res.mode.name());
            args.putInt("chapter", res.chapter);
            args.putInt("option", res.option);
            nav().go(Screen.QUIZ, args);
        });
        view.findViewById(R.id.result_map).setOnClickListener(v -> nav().go(Screen.MAP));
    }

    @Override
    protected String entryTip() {
        return "کارنامه‌ات را ببین؛ سؤال‌های اشتباه را با هم دوباره کار می‌کنیم.";
    }
}
