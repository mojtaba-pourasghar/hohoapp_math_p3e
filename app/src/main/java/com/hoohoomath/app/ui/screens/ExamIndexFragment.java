package com.hoohoomath.app.ui.screens;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hoohoomath.app.R;
import com.hoohoomath.app.data.AppState;
import com.hoohoomath.app.data.Book;
import com.hoohoomath.app.data.Level;
import com.hoohoomath.app.data.QuizBuilder;
import com.hoohoomath.app.ui.BaseFragment;
import com.hoohoomath.app.ui.Screen;

public class ExamIndexFragment extends BaseFragment {
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
        ((TextView) view.findViewById(R.id.screen_title)).setText("آزمون‌ها");
        render(view);
    }

    private void render(View view) {
        AppState s = state();
        Book.Chapter ch = Book.chapter(currentChapter);
        boolean open = QuizBuilder.isChapterOpen(ch.index, s.taughtChapter, s.taughtSection);

        FrameLayout chipContainer = view.findViewById(R.id.chip_container);
        chipContainer.removeAllViews();
        chipContainer.addView(ScreenHelpers.buildChapterChipRow(requireContext(), s, currentChapter, i -> {
            currentChapter = i;
            render(view);
        }));

        ((TextView) view.findViewById(R.id.chapter_title)).setText("فصل " + ch.numberFa + ": " + ch.title);
        ((TextView) view.findViewById(R.id.chapter_subtitle)).setText(open
            ? "آزمون فقط از بخش‌هایی است که معلم درس داده."
            : "این فصل هنوز درس داده نشده است.");

        LinearLayout list = view.findViewById(R.id.list_container);
        list.removeAllViews();
        for (Level level : Level.values()) {
            list.addView(ScreenHelpers.buildLevelCard(requireContext(), level, "۸ سؤال چهارگزینه‌ای · فقط از بخش‌های درس‌داده‌شده", () -> {
                if (!open) {
                    mascot().comfort("این فصل هنوز درس داده نشده است. در پنل والدین پیشرفت معلم را به‌روز کنید.");
                    return;
                }
                Bundle args = new Bundle();
                args.putString("mode", "EXAM");
                args.putInt("chapter", ch.index);
                args.putInt("option", level.index);
                nav().go(Screen.QUIZ, args);
            }));
        }
    }

    @Override
    protected String entryTip() {
        return "آزمون فقط از بخش‌هایی است که معلم درس داده.";
    }
}
