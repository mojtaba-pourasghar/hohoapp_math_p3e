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

import com.hoohoomath.app.R;
import com.hoohoomath.app.data.AppState;
import com.hoohoomath.app.data.Book;
import com.hoohoomath.app.data.Chapter1Lessons;
import com.hoohoomath.app.ui.BaseFragment;
import com.hoohoomath.app.ui.Screen;
import com.hoohoomath.app.ui.UiKit;

import static com.hoohoomath.app.data.PersianDigits.fa;

public class MapFragment extends BaseFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AppState s = state();
        Book.Chapter ch = Book.chapter(s.taughtChapter);

        view.findViewById(R.id.btn_profile).setOnClickListener(v -> nav().go(Screen.PROFILE));
        view.findViewById(R.id.btn_parent).setOnClickListener(v -> nav().goParent());
        view.findViewById(R.id.see_chapters).setOnClickListener(v -> nav().go(Screen.CHAPTERS));

        LinearLayout chipRow = view.findViewById(R.id.chip_row);
        chipRow.removeAllViews();
        TextView starsChip = UiKit.chip(requireContext(), fa(s.stars) + " ستاره", getColor(R.color.orange_bg), getColor(R.color.orange_border), getColor(R.color.orange_text));
        TextView streakChip = UiKit.chip(requireContext(), fa(s.streak) + " روز", getColor(R.color.pink_bg), getColor(R.color.pink_border), getColor(R.color.pink_dark));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(UiKit.dp(requireContext(), 6));
        chipRow.addView(starsChip, lp);
        chipRow.addView(streakChip, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView greeting = view.findViewById(R.id.map_greeting);
        greeting.setText("سلام قهرمان کوچولو! تا «" + ch.sections.get(s.taughtSection) + "» درس داده شده؛ همین‌جا تمرین کنیم؟");

        TextView subtitle = view.findViewById(R.id.map_subtitle);
        subtitle.setText("فصل " + ch.numberFa + ": " + ch.title + " · " + fa(s.taughtSection + 1) + " بخش از " + fa(Book.SECTIONS_PER_CHAPTER));

        ProgressBar progress = view.findViewById(R.id.map_progress);
        progress.setProgress(Math.round(((s.taughtSection + 1) / (float) Book.SECTIONS_PER_CHAPTER) * 100));

        LinearLayout nodeContainer = view.findViewById(R.id.node_container);
        nodeContainer.removeAllViews();
        for (int i = 0; i < ch.sections.size(); i++) {
            nodeContainer.addView(buildNode(ch, i, s));
        }
        nodeContainer.addView(buildExamNode(ch, s));
    }

    private int getColor(int res) {
        return androidx.core.content.ContextCompat.getColor(requireContext(), res);
    }

    private View buildNode(Book.Chapter ch, int i, AppState s) {
        boolean open = i <= s.taughtSection;
        boolean active = i == s.taughtSection;

        LinearLayout row = UiKit.row(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(UiKit.dp(requireContext(), 12), UiKit.dp(requireContext(), 10), UiKit.dp(requireContext(), 12), UiKit.dp(requireContext(), 10));
        UiKit.applyCardBg(row, requireContext(), active ? R.color.orange_bg : (open ? R.color.teal_bg : R.color.bg_card_muted), active ? R.color.orange_border : (open ? R.color.teal_border : R.color.border_muted));
        row.setLayoutParams(UiKit.marginParams(requireContext(), 0, 8));

        TextView badge = new TextView(requireContext());
        badge.setGravity(Gravity.CENTER);
        badge.setTextColor(Color.WHITE);
        badge.setText(open ? (active ? "اینجا" : "✓") : "قفل");
        badge.setTextSize(active ? 12.5f : 13f);
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(UiKit.dp(requireContext(), 52), UiKit.dp(requireContext(), 52));
        badge.setLayoutParams(badgeLp);
        badge.setBackground(UiKit.roundedBg(getColor(active ? R.color.orange : open ? R.color.teal : R.color.lock_bg), 0, 26f, requireContext()));

        LinearLayout textCol = UiKit.column(requireContext());
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textLp.setMarginStart(UiKit.dp(requireContext(), 12));
        textCol.setLayoutParams(textLp);
        textCol.addView(UiKit.text(requireContext(), fa(i + 1) + ". " + ch.sections.get(i), 14f, R.color.text_primary, true));
        String sub = !open ? "هنوز درس داده نشده" : (Chapter1Lessons.forSection(i) != null ? "درس صوتی + تمرین" : "تمرین این بخش");
        textCol.addView(UiKit.text(requireContext(), sub, 11.5f, R.color.text_muted, false));

        row.addView(badge);
        row.addView(textCol);

        row.setOnClickListener(v -> {
            if (!open) {
                mascot().comfort("معلم تا اینجا درس نداده. وقتی گفت «یاد گرفتیم»، این بخش باز می‌شود.");
                return;
            }
            if (ch.index == 0 && Chapter1Lessons.forSection(i) != null) {
                Bundle args = new Bundle();
                args.putInt("chapter", ch.index);
                args.putInt("section", i);
                nav().go(Screen.LESSON, args);
            } else {
                Bundle args = new Bundle();
                args.putString("mode", "PRACTICE");
                args.putInt("chapter", ch.index);
                args.putInt("option", i);
                nav().go(Screen.QUIZ, args);
            }
        });
        return row;
    }

    private View buildExamNode(Book.Chapter ch, AppState s) {
        LinearLayout row = UiKit.row(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(UiKit.dp(requireContext(), 12), UiKit.dp(requireContext(), 10), UiKit.dp(requireContext(), 12), UiKit.dp(requireContext(), 10));
        UiKit.applyCardBg(row, requireContext(), R.color.pink_bg, R.color.pink_border);
        row.setLayoutParams(UiKit.marginParams(requireContext(), 0, 8));

        TextView badge = new TextView(requireContext());
        badge.setGravity(Gravity.CENTER);
        badge.setTextColor(Color.WHITE);
        badge.setText("آزمون");
        badge.setTextSize(11.5f);
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(UiKit.dp(requireContext(), 52), UiKit.dp(requireContext(), 52));
        badge.setLayoutParams(badgeLp);
        badge.setBackground(UiKit.roundedBg(getColor(R.color.pink), 0, 26f, requireContext()));

        LinearLayout textCol = UiKit.column(requireContext());
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textLp.setMarginStart(UiKit.dp(requireContext(), 12));
        textCol.setLayoutParams(textLp);
        textCol.addView(UiKit.text(requireContext(), "آزمون فصل " + ch.numberFa, 14f, R.color.text_primary, true));
        textCol.addView(UiKit.text(requireContext(), "فقط از بخش‌هایی که درس داده شده", 11.5f, R.color.text_muted, false));

        row.addView(badge);
        row.addView(textCol);
        row.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("chapter", ch.index);
            nav().go(Screen.EXAM_INDEX, args);
        });
        return row;
    }

    @Override
    protected String entryTip() {
        return "هر بخشی که معلم درس داده، اینجا باز می‌شود. روی دایره‌ی نارنجی بزن!";
    }
}
