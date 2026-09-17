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
import com.hoohoomath.app.data.PersianDigits;
import com.hoohoomath.app.ui.BaseFragment;
import com.hoohoomath.app.ui.Screen;
import com.hoohoomath.app.ui.UiKit;


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
            currentChapter = i;
            render(view);
        }));

        ((TextView) view.findViewById(R.id.chapter_title)).setText("فصل " + ch.numberFa + ": " + ch.title);
        ((TextView) view.findViewById(R.id.chapter_subtitle)).setText("هر بخش تمرین جدا دارد و همه‌شان باز است.");

        LinearLayout list = view.findViewById(R.id.list_container);
        list.removeAllViews();
        for (int i = 0; i < ch.sections.size(); i++) {
            list.addView(buildRow(ch, i));
        }
        list.addView(buildChapterLink("کاربرگ‌های این فصل ›", R.color.orange_bg, R.color.orange_border, R.color.orange_text,
            () -> {
                Bundle args = new Bundle();
                args.putInt("chapter", ch.index);
                nav().go(Screen.WORKSHEET_INDEX, args);
            }));
        list.addView(buildChapterLink("آزمون این فصل ›", R.color.pink_bg, R.color.pink_border, R.color.pink_dark,
            () -> {
                Bundle args = new Bundle();
                args.putInt("chapter", ch.index);
                nav().go(Screen.EXAM_INDEX, args);
            }));
    }

    /** Shortcut off the bottom of the section list, so a finished chapter stays one tap away. */
    private View buildChapterLink(String label, int bgRes, int borderRes, int textRes, Runnable onClick) {
        TextView link = UiKit.text(requireContext(), label, 13.5f, textRes, true);
        link.setGravity(Gravity.CENTER);
        link.setPadding(0, UiKit.dp(requireContext(), 14), 0, UiKit.dp(requireContext(), 14));
        UiKit.applyCardBg(link, requireContext(), bgRes, borderRes);
        link.setLayoutParams(UiKit.marginParams(requireContext(), 0, 8));
        link.setOnClickListener(v -> onClick.run());
        UiKit.tapSound(link);
        return link;
    }

    private View buildRow(Book.Chapter ch, int i) {
        LinearLayout card = UiKit.column(requireContext());
        card.setPadding(UiKit.dp(requireContext(), 14), UiKit.dp(requireContext(), 14), UiKit.dp(requireContext(), 14), UiKit.dp(requireContext(), 14));
        UiKit.applyCardBg(card, requireContext(), R.color.bg_card, R.color.border_green);
        card.setLayoutParams(UiKit.marginParams(requireContext(), 0, 10));

        AppState s = state();
        boolean learned = s.isSectionLessonDone(ch.index, i);
        boolean hasLesson = Chapter1Lessons.forSection(i) != null && ch.index == 0;
        String practiceKey = "PRACTICE_" + ch.index + "_" + i;
        int nextRound = s.getRound(practiceKey) + 1;
        boolean halfDone = s.hasAttempt(practiceKey);

        String title = PersianDigits.fa(i + 1) + ". " + ch.sections.get(i) + (learned ? "  ✓" : "");
        String sub = halfDone
            ? "تمرین نیمه‌کاره داری؛ از همان‌جا ادامه می‌دهی."
            : "۱۵ سؤال تازه در هر دور · دور " + PersianDigits.fa(nextRound);
        card.addView(UiKit.text(requireContext(), title, 14.5f, R.color.text_primary, true));
        card.addView(UiKit.text(requireContext(), sub, 11.5f, R.color.text_muted, false));

        LinearLayout buttons = UiKit.row(requireContext());
        buttons.setLayoutParams(UiKit.marginParams(requireContext(), 10, 0));

        if (hasLesson) {
            TextView lesson = pill(learned ? "دوباره ببین" : "درس هوهو", R.color.orange, R.color.white);
            lesson.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putInt("chapter", ch.index);
                args.putInt("section", i);
                nav().go(Screen.LESSON, args);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMarginEnd(UiKit.dp(requireContext(), 6));
            buttons.addView(lesson, lp);
        }

        TextView practice = pill(halfDone ? "ادامه بده" : "تمرین کن", R.color.teal, R.color.white);
        practice.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("mode", "PRACTICE");
            args.putInt("chapter", ch.index);
            args.putInt("option", i);
            nav().go(Screen.QUIZ, args);
        });
        buttons.addView(practice, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        card.addView(buttons);
        return card;
    }

    private TextView pill(String label, int bgRes, int textRes) {
        TextView tv = UiKit.text(requireContext(), label, 13f, textRes, true);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, UiKit.dp(requireContext(), 11), 0, UiKit.dp(requireContext(), 11));
        tv.setBackground(UiKit.roundedBg(ContextCompat.getColor(requireContext(), bgRes), 0, 999f, requireContext()));
        UiKit.tapSound(tv);
        return tv;
    }

    @Override
    protected String entryTip() {
        return "هر بخش ۱۵ تمرین دارد؛ از آسان شروع می‌شود.";
    }
}
