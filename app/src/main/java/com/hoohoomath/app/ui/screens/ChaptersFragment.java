package com.hoohoomath.app.ui.screens;

import android.graphics.Color;
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
import com.hoohoomath.app.ui.BaseFragment;
import com.hoohoomath.app.ui.Screen;
import com.hoohoomath.app.ui.UiKit;

import static com.hoohoomath.app.data.PersianDigits.fa;

public class ChaptersFragment extends BaseFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chapters, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AppState s = state();
        Book.Chapter taught = Book.chapter(s.taughtChapter);
        ((TextView) view.findViewById(R.id.chapters_sub)).setText("همه‌ی فصل‌ها باز است. کلاس تا اینجا رسیده: فصل "
            + taught.numberFa + " — " + taught.sections.get(s.taughtSection));

        LinearLayout container = view.findViewById(R.id.chapter_container);
        container.removeAllViews();
        for (Book.Chapter ch : Book.CHAPTERS) {
            container.addView(buildCard(ch, s));
        }
    }

    private View buildCard(Book.Chapter ch, AppState s) {
        // every chapter is open; the bar now shows the child's own progress, not a permission
        int learned = 0;
        for (int i = 0; i < Book.SECTIONS_PER_CHAPTER; i++) {
            if (s.isSectionLessonDone(ch.index, i)) learned++;
        }
        boolean here = ch.index == s.taughtChapter;
        int pct = Math.round((learned / (float) Book.SECTIONS_PER_CHAPTER) * 100);

        LinearLayout row = UiKit.row(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(UiKit.dp(requireContext(), 14), UiKit.dp(requireContext(), 14), UiKit.dp(requireContext(), 14), UiKit.dp(requireContext(), 14));
        UiKit.applyCardBg(row, requireContext(), here ? R.color.orange_bg : R.color.bg_card, here ? R.color.orange_border : R.color.border_green);
        row.setLayoutParams(UiKit.marginParams(requireContext(), 0, 11));

        TextView chip = new TextView(requireContext());
        chip.setText(ch.numberFa);
        UiKit.applyFont(chip, true);
        chip.setTextColor(Color.WHITE);
        chip.setGravity(Gravity.CENTER);
        chip.setTextSize(17f);
        LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(UiKit.dp(requireContext(), 46), UiKit.dp(requireContext(), 46));
        chip.setLayoutParams(chipLp);
        chip.setBackground(UiKit.roundedBg(ContextCompat.getColor(requireContext(), here ? R.color.orange : R.color.teal), 0, 14f, requireContext()));

        LinearLayout textCol = UiKit.column(requireContext());
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textLp.setMarginStart(UiKit.dp(requireContext(), 12));
        textCol.setLayoutParams(textLp);
        textCol.addView(UiKit.text(requireContext(), ch.title, 15f, R.color.text_primary, true));
        textCol.addView(UiKit.text(requireContext(), String.join(" · ", ch.sections), 11f, R.color.text_muted, false));

        ProgressBar bar = new ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(pct);
        bar.setProgressDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.progress_teal));
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(requireContext(), 6));
        barLp.topMargin = UiKit.dp(requireContext(), 6);
        bar.setLayoutParams(barLp);
        textCol.addView(bar);

        TextView arrow = UiKit.text(requireContext(), "›", 15f, here ? R.color.orange_text : R.color.teal_dark, true);

        row.addView(chip);
        row.addView(textCol);
        row.addView(arrow);

        row.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("chapter", ch.index);
            nav().go(Screen.SECTIONS, args);
        });
        return row;
    }

    @Override
    protected String entryTip() {
        return "هر فصلی را بخواهی باز است؛ ستاره نشان می‌دهد کلاس کجاست.";
    }
}
