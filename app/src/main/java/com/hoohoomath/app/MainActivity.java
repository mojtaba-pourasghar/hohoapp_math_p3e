package com.hoohoomath.app;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.hoohoomath.app.data.AppState;
import com.hoohoomath.app.ui.Navigator;
import com.hoohoomath.app.ui.Screen;
import com.hoohoomath.app.ui.mascot.HooHooView;
import com.hoohoomath.app.ui.mascot.MascotController;
import com.hoohoomath.app.ui.screens.ChaptersFragment;
import com.hoohoomath.app.ui.screens.ExamIndexFragment;
import com.hoohoomath.app.ui.screens.LessonFragment;
import com.hoohoomath.app.ui.screens.MapFragment;
import com.hoohoomath.app.ui.screens.ParentGateFragment;
import com.hoohoomath.app.ui.screens.ParentPanelFragment;
import com.hoohoomath.app.ui.screens.ProfileFragment;
import com.hoohoomath.app.ui.screens.QuizFragment;
import com.hoohoomath.app.ui.screens.ResultFragment;
import com.hoohoomath.app.ui.screens.RewardsFragment;
import com.hoohoomath.app.ui.screens.SectionsFragment;
import com.hoohoomath.app.ui.screens.SplashFragment;
import com.hoohoomath.app.ui.screens.WorksheetIndexFragment;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements Navigator {

    private MascotController mascotController;
    private View navBar;
    private View mascotOverlay;
    private Screen currentScreen;

    private View navMap, navSections, navWorksheet, navExam, navParent;

    @Override
    protected void attachBaseContext(Context newBase) {
        Locale fa = new Locale("fa", "IR");
        Configuration config = new Configuration(newBase.getResources().getConfiguration());
        config.setLocale(fa);
        config.setLayoutDirection(fa);
        Context context = newBase.createConfigurationContext(config);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        HooHooView mascotView = findViewById(R.id.mascot_view);
        TextView bubble = findViewById(R.id.mascot_bubble);
        mascotController = new MascotController(mascotView, bubble);

        navBar = findViewById(R.id.nav_bar);
        mascotOverlay = findViewById(R.id.mascot_overlay);

        navMap = findViewById(R.id.nav_map);
        navSections = findViewById(R.id.nav_sections);
        navWorksheet = findViewById(R.id.nav_worksheet);
        navExam = findViewById(R.id.nav_exam);
        navParent = findViewById(R.id.nav_parent);

        bindNavItem(navMap, R.drawable.ic_pin, "نقشه", () -> go(Screen.MAP));
        bindNavItem(navSections, R.drawable.ic_pencil, "تمرین", () -> go(Screen.SECTIONS));
        bindNavItem(navWorksheet, R.drawable.ic_sheet, "کاربرگ", () -> go(Screen.WORKSHEET_INDEX));
        bindNavItem(navExam, R.drawable.ic_target, "آزمون", () -> go(Screen.EXAM_INDEX));
        bindNavItem(navParent, R.drawable.ic_shield, "والدین", this::goParent);

        if (savedInstanceState == null) {
            go(Screen.SPLASH);
        }
    }

    private interface Action { void run(); }

    private void bindNavItem(View item, int iconRes, String label, Action onClick) {
        ImageView icon = item.findViewById(R.id.nav_icon);
        TextView text = item.findViewById(R.id.nav_label);
        icon.setImageResource(iconRes);
        text.setText(label);
        item.setOnClickListener(v -> onClick.run());
    }

    @Override
    public void go(Screen screen) {
        go(screen, null);
    }

    @Override
    public void go(Screen screen, Bundle args) {
        Fragment fragment = fragmentFor(screen, args);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commitAllowingStateLoss();

        boolean chromeVisible = screen != Screen.SPLASH && screen != Screen.PARENT_GATE;
        navBar.setVisibility(chromeVisible ? View.VISIBLE : View.GONE);
        mascotOverlay.setVisibility(screen == Screen.SPLASH ? View.GONE : View.VISIBLE);

        if (screen != Screen.SPLASH && currentScreen != null) {
            mascotController.onScreenTransition();
        }
        currentScreen = screen;
        updateNavHighlight(screen);
    }

    @Override
    public void goParent() {
        if (AppState.get().parentUnlockedThisSession) {
            go(Screen.PARENT_PANEL);
        } else {
            go(Screen.PARENT_GATE);
        }
    }

    @Override
    public MascotController mascot() {
        return mascotController;
    }

    private void updateNavHighlight(Screen screen) {
        int active = getColor(R.color.orange);
        int inactive = getColor(R.color.text_faint);
        setNavColor(navMap, screen == Screen.MAP ? active : inactive);
        setNavColor(navSections, screen == Screen.SECTIONS ? active : inactive);
        setNavColor(navWorksheet, screen == Screen.WORKSHEET_INDEX ? active : inactive);
        setNavColor(navExam, screen == Screen.EXAM_INDEX ? active : inactive);
        setNavColor(navParent, screen == Screen.PARENT_PANEL || screen == Screen.PARENT_GATE ? active : inactive);
    }

    private void setNavColor(View item, int color) {
        ImageView icon = item.findViewById(R.id.nav_icon);
        TextView text = item.findViewById(R.id.nav_label);
        icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        text.setTextColor(color);
    }

    @NonNull
    private Fragment fragmentFor(Screen screen, Bundle args) {
        Fragment f;
        switch (screen) {
            case SPLASH: f = new SplashFragment(); break;
            case MAP: f = new MapFragment(); break;
            case CHAPTERS: f = new ChaptersFragment(); break;
            case LESSON: f = new LessonFragment(); break;
            case SECTIONS: f = new SectionsFragment(); break;
            case WORKSHEET_INDEX: f = new WorksheetIndexFragment(); break;
            case EXAM_INDEX: f = new ExamIndexFragment(); break;
            case QUIZ: f = new QuizFragment(); break;
            case RESULT: f = new ResultFragment(); break;
            case REWARDS: f = new RewardsFragment(); break;
            case PROFILE: f = new ProfileFragment(); break;
            case PARENT_GATE: f = new ParentGateFragment(); break;
            case PARENT_PANEL: f = new ParentPanelFragment(); break;
            default: throw new IllegalArgumentException("Unknown screen " + screen);
        }
        if (args != null) f.setArguments(args);
        return f;
    }
}
