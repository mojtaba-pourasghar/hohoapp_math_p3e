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
import com.hoohoomath.app.data.Lessons;
import com.hoohoomath.app.data.PageLessons;
import com.hoohoomath.app.ui.BaseFragment;
import com.hoohoomath.app.ui.PathMapView;
import com.hoohoomath.app.ui.Screen;
import com.hoohoomath.app.ui.UiKit;

import static com.hoohoomath.app.data.PersianDigits.fa;

public class MapFragment extends BaseFragment {

    /** {top%, left%} stops along the trail — five sections then the chapter exam. */
    private static final float[][] NODE_POSITIONS = {
        {80, 72}, {69, 44}, {57, 66}, {45, 38}, {33, 62}, {16, 44}
    };

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
        TextView starsChip = UiKit.chip(requireContext(), fa(s.stars) + " ستاره",
            color(R.color.orange_bg), color(R.color.orange_border), color(R.color.orange_text));
        TextView streakChip = UiKit.chip(requireContext(), fa(s.streak) + " روز",
            color(R.color.pink_bg), color(R.color.pink_border), color(R.color.pink_dark));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(UiKit.dp(requireContext(), 6));
        chipRow.addView(starsChip, lp);
        chipRow.addView(streakChip, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        String name = s.learnerName == null || s.learnerName.isEmpty() ? "قهرمان کوچولو" : s.learnerName;
        ((TextView) view.findViewById(R.id.map_greeting)).setText(
            "سلام " + name + "! کلاس تا «" + ch.sections.get(s.taughtSection) + "» رسیده؛ اما هر بخشی را بخواهی باز است.");
        ((TextView) view.findViewById(R.id.map_subtitle)).setText(
            "فصل " + ch.numberFa + ": " + ch.title + " · " + fa(s.taughtSection + 1) + " بخش از " + fa(Book.SECTIONS_PER_CHAPTER));
        ((ProgressBar) view.findViewById(R.id.map_progress))
            .setProgress(Math.round(((s.taughtSection + 1) / (float) Book.SECTIONS_PER_CHAPTER) * 100));

        PathMapView map = view.findViewById(R.id.path_map);
        map.clearNodes();
        for (int i = 0; i < ch.sections.size(); i++) {
            map.addNode(buildSectionNode(ch, i, s), NODE_POSITIONS[i][0], NODE_POSITIONS[i][1]);
        }
        map.addNode(buildExamNode(ch), NODE_POSITIONS[5][0], NODE_POSITIONS[5][1]);
    }

    private int color(int res) {
        return ContextCompat.getColor(requireContext(), res);
    }

    private View buildSectionNode(Book.Chapter ch, int i, AppState s) {
        // nothing on the trail is locked; the orange node is only where the class has got to
        boolean active = i == s.taughtSection;
        boolean childDone = s.isSectionLessonDone(ch.index, i);

        int circleColor = active ? R.color.orange : childDone ? R.color.teal : R.color.teal_light;
        String badge = active ? "اینجا" : childDone ? "✓" : "برو";
        int size = active ? 88 : 68;

        LinearLayout node = UiKit.column(requireContext());
        node.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView circle = UiKit.text(requireContext(), badge, active ? 13f : 14f, R.color.white, true);
        circle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams circleLp = new LinearLayout.LayoutParams(UiKit.dp(requireContext(), size), UiKit.dp(requireContext(), size));
        circle.setLayoutParams(circleLp);
        circle.setBackground(UiKit.ringedCircle(color(circleColor), Color.WHITE, UiKit.dp(requireContext(), 5)));
        circle.setElevation(UiKit.dp(requireContext(), 4));

        TextView label = UiKit.text(requireContext(), ch.sections.get(i), 11.5f, R.color.text_primary, true);
        label.setGravity(Gravity.CENTER);
        label.setMaxWidth(UiKit.dp(requireContext(), 150));
        label.setPadding(UiKit.dp(requireContext(), 9), UiKit.dp(requireContext(), 4), UiKit.dp(requireContext(), 9), UiKit.dp(requireContext(), 4));
        label.setBackground(UiKit.roundedBg(Color.parseColor("#F2FFFFFF"), 0, 9f, requireContext()));
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        labelLp.topMargin = UiKit.dp(requireContext(), 7);

        node.addView(circle);
        node.addView(label, labelLp);

        node.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("chapter", ch.index);
            // the book comes first: open the page of this section the child has not finished yet
            java.util.List<Integer> pages = PageLessons.pagesOfSection(ch.index, i);
            int nextPage = 0;
            for (int page : pages) {
                if (!state().isPageDone(page)) { nextPage = page; break; }
            }
            if (nextPage == 0 && !pages.isEmpty()) nextPage = pages.get(0);

            if (nextPage > 0) {
                args.putInt("page", nextPage);
                nav().go(Screen.LESSON, args);
            } else if (Lessons.has(ch.index, i)) {
                args.putInt("section", i);
                nav().go(Screen.LESSON, args);
            } else {
                args.putString("mode", "PRACTICE");
                args.putInt("option", i);
                nav().go(Screen.QUIZ, args);
            }
        });
        return node;
    }

    private View buildExamNode(Book.Chapter ch) {
        LinearLayout node = UiKit.column(requireContext());
        node.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView circle = UiKit.text(requireContext(), "آزمون", 12f, R.color.white, true);
        circle.setGravity(Gravity.CENTER);
        circle.setLayoutParams(new LinearLayout.LayoutParams(UiKit.dp(requireContext(), 72), UiKit.dp(requireContext(), 72)));
        circle.setBackground(UiKit.ringedCircle(color(R.color.pink), Color.WHITE, UiKit.dp(requireContext(), 5)));
        circle.setElevation(UiKit.dp(requireContext(), 4));

        TextView label = UiKit.text(requireContext(), "آزمون فصل " + ch.numberFa, 11.5f, R.color.text_primary, true);
        label.setGravity(Gravity.CENTER);
        label.setPadding(UiKit.dp(requireContext(), 9), UiKit.dp(requireContext(), 4), UiKit.dp(requireContext(), 9), UiKit.dp(requireContext(), 4));
        label.setBackground(UiKit.roundedBg(Color.parseColor("#F2FFFFFF"), 0, 9f, requireContext()));
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        labelLp.topMargin = UiKit.dp(requireContext(), 7);

        node.addView(circle);
        node.addView(label, labelLp);
        node.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("chapter", ch.index);
            nav().go(Screen.EXAM_INDEX, args);
        });
        return node;
    }

    @Override
    protected String entryTip() {
        return "هر بخشی که معلم درس داده، اینجا باز می‌شود. روی دایره‌ی نارنجی بزن!";
    }
}
