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
import com.hoohoomath.app.data.QuizBuilder;
import com.hoohoomath.app.ui.BaseFragment;
import com.hoohoomath.app.ui.Screen;
import com.hoohoomath.app.ui.UiKit;

import java.util.List;

public class SectionsFragment extends BaseFragment {
    private int currentChapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list_index, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AppState s = state();
        Bundle args = getArguments();
        currentChapter = args != null ? args.getInt("chapter", s.taughtChapter) : s.taughtChapter;
        ((TextView) view.findViewById(R.id.screen_title)).setText("تمرین بخش‌ها");
        render(view);
    }

    private void render(View view) {
        AppState s = state();
        Book.Chapter ch = Book.chapter(currentChapter);

        FrameLayout chipContainer = view.findViewById(R.id.chip_container);
        chipContainer.removeAllViews();
        chipContainer.addView(ScreenHelpers.buildChapterChipRow(requireContext(), s, currentChapter, i -> {
            if (!QuizBuilder.isChapterOpen(i, s.taughtChapter, s.taughtSection)) {
                mascot().comfort("این فصل هنوز درس داده نشده است.");
                return;
            }
            currentChapter = i;
            render(view);
        }));

        ((TextView) view.findViewById(R.id.chapter_title)).setText("فصل " + ch.numberFa + ": " + ch.title);
        ((TextView) view.findViewById(R.id.chapter_subtitle)).setText("هر بخشی که درس داده شده، تمرین جدا دارد.");

        List<Integer> allowed = QuizBuilder.allowedSections(ch.index, s.taughtChapter, s.taughtSection);
        LinearLayout list = view.findViewById(R.id.list_container);
        list.removeAllViews();
        for (int i = 0; i < ch.sections.size(); i++) {
            boolean open = allowed.contains(i);
            list.addView(buildRow(ch, i, open));
        }
    }

    private View buildRow(Book.Chapter ch, int i, boolean open) {
        LinearLayout row = UiKit.row(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(UiKit.dp(requireContext(), 14), UiKit.dp(requireContext(), 14), UiKit.dp(requireContext(), 14), UiKit.dp(requireContext(), 14));
        UiKit.applyCardBg(row, requireContext(), open ? R.color.bg_card : R.color.bg_card_muted, open ? R.color.border_green : R.color.border_muted);
        row.setLayoutParams(UiKit.marginParams(requireContext(), 0, 10));

        LinearLayout textCol = UiKit.column(requireContext());
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textCol.setLayoutParams(textLp);
        AppState s = state();
        boolean learned = s.isSectionLessonDone(ch.index, i);
        int nextRound = s.getRound("PRACTICE_" + ch.index + "_" + i) + 1;
        String title = com.hoohoomath.app.data.PersianDigits.fa(i + 1) + ". " + ch.sections.get(i) + (learned ? "  ✓" : "");
        String sub = !open ? "هنوز درس داده نشده"
            : "۱۵ سؤال تازه در هر دور · دور " + com.hoohoomath.app.data.PersianDigits.fa(nextRound);
        textCol.addView(UiKit.text(requireContext(), title, 14.5f, R.color.text_primary, true));
        textCol.addView(UiKit.text(requireContext(), sub, 11.5f, R.color.text_muted, false));

        TextView cta = UiKit.text(requireContext(), open ? "تمرین کن" : "قفل است", 13f, R.color.white, true);
        cta.setGravity(Gravity.CENTER);
        cta.setPadding(UiKit.dp(requireContext(), 16), UiKit.dp(requireContext(), 10), UiKit.dp(requireContext(), 16), UiKit.dp(requireContext(), 10));
        cta.setBackground(UiKit.roundedBg(ContextCompat.getColor(requireContext(), open ? R.color.teal : R.color.lock_bg), 0, 999f, requireContext()));
        if (!open) cta.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_faint));

        cta.setOnClickListener(v -> {
            if (!open) {
                mascot().comfort("این بخش هنوز درس داده نشده است.");
                return;
            }
            Bundle args = new Bundle();
            args.putString("mode", "PRACTICE");
            args.putInt("chapter", ch.index);
            args.putInt("option", i);
            nav().go(Screen.QUIZ, args);
        });

        row.addView(textCol);
        row.addView(cta);
        return row;
    }

    @Override
    protected String entryTip() {
        return "هر بخش ۱۵ تمرین دارد؛ از آسان شروع می‌شود.";
    }
}
