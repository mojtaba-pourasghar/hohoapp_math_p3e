package com.hoohoomath.app.ui.screens;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hoohoomath.app.R;
import com.hoohoomath.app.data.AppState;
import com.hoohoomath.app.data.Book;
import com.hoohoomath.app.data.BookText;
import com.hoohoomath.app.tts.TtsManager;
import com.hoohoomath.app.ui.BaseFragment;
import com.hoohoomath.app.ui.UiKit;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.hoohoomath.app.data.PersianDigits.fa;

/**
 * The whole textbook, page by page and line by line: the real page picture (so every drawing and
 * table the book has is there) with that page's own sentences under it. هوهو reads the highlighted
 * sentence, and «تا آخر صفحه بخوان» walks the page from the current line to the end on its own.
 *
 * The page the child was on, and the line inside it, are remembered.
 */
public class BookFragment extends BaseFragment {

    private BookText book;
    private int page;
    private int line;
    private boolean autoplay = false;

    private View rootView;
    private Bitmap pageBitmap;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_book, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        book = BookText.get(requireContext());
        AppState s = state();

        Bundle args = getArguments();
        int requested = args != null ? args.getInt("page", 0) : 0;
        if (requested > 0) {
            page = clampPage(requested);
            line = 0;
        } else {
            page = clampPage(s.bookPage());
            line = s.bookLine();
        }

        FrameLayout chips = view.findViewById(R.id.book_chip_container);
        chips.addView(ScreenHelpers.buildChapterChipRow(requireContext(), s, chapterOfPage(page),
            i -> goToPage(Book.chapter(i).firstPage)));

        view.findViewById(R.id.book_prev_page).setOnClickListener(v -> goToPage(page - 1));
        view.findViewById(R.id.book_next_page).setOnClickListener(v -> goToPage(page + 1));
        view.findViewById(R.id.book_prev_line).setOnClickListener(v -> goToLine(line - 1, false));
        view.findViewById(R.id.book_next_line).setOnClickListener(v -> goToLine(line + 1, false));
        view.findViewById(R.id.book_read).setOnClickListener(v -> readCurrentLine(false));
        view.findViewById(R.id.book_autoplay).setOnClickListener(v -> toggleAutoplay());

        render();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopReading();
        state().saveBookPlace(page, line);
    }

    @Override
    public void onDestroyView() {
        stopReading();
        releaseBitmap();
        super.onDestroyView();
    }

    // ---------- navigation ----------

    private int clampPage(int candidate) {
        int last = Math.max(BookText.FIRST_PAGE, book.pageCount());
        return Math.max(BookText.FIRST_PAGE, Math.min(candidate, last));
    }

    private static int chapterOfPage(int page) {
        for (Book.Chapter ch : Book.CHAPTERS) {
            if (page >= ch.firstPage && page <= ch.lastPage) return ch.index;
        }
        return 0;
    }

    private void goToPage(int target) {
        int clamped = clampPage(target);
        if (clamped == page) {
            mascot().say(target < BookText.FIRST_PAGE
                ? "این اولین صفحه‌ی کتاب است."
                : "به آخرین صفحه‌ی کتاب رسیدیم.");
            return;
        }
        stopReading();
        page = clamped;
        line = 0;
        state().saveBookPlace(page, line);
        render();
    }

    private void goToLine(int target, boolean keepAutoplay) {
        List<String> lines = book.lines(page);
        if (lines.isEmpty()) return;
        if (target < 0) {
            mascot().say("این اولین خطِ این صفحه است.");
            return;
        }
        if (target >= lines.size()) {
            if (!keepAutoplay) stopReading();
            autoplay = false;
            updateAutoplayLabel();
            mascot().say("این صفحه تمام شد. «صفحه‌ی بعد» را بزن تا برویم صفحه‌ی بعد.");
            return;
        }
        if (!keepAutoplay) stopReading();
        line = target;
        state().saveBookPlace(page, line);
        render();
        if (!keepAutoplay) readCurrentLine(false);
    }

    // ---------- reading ----------

    private void toggleAutoplay() {
        if (autoplay) {
            stopReading();
            autoplay = false;
            updateAutoplayLabel();
            return;
        }
        if (book.lines(page).isEmpty()) {
            mascot().say("این صفحه فقط تصویر است؛ نوشته‌ای برای خواندن ندارد.");
            return;
        }
        autoplay = true;
        updateAutoplayLabel();
        readCurrentLine(true);
    }

    private void stopReading() {
        TtsManager tts = TtsManager.get();
        if (tts != null) tts.stop();
    }

    /** Reads the highlighted sentence; in autoplay it moves on to the next one when it ends. */
    private void readCurrentLine(boolean chain) {
        List<String> lines = book.lines(page);
        if (lines.isEmpty() || line >= lines.size()) return;
        String text = lines.get(line);
        TtsManager tts = TtsManager.get();
        mascot().showBubble(text, true);
        if (tts == null) return;

        final int spoken = line;
        tts.speak(text, new TtsManager.Callback() {
            @Override public void onDone() {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    mascot().showBubble(text, false);
                    if (!chain || !autoplay || spoken != line) return;
                    int next = line + 1;
                    if (next >= book.lines(page).size()) {
                        autoplay = false;
                        updateAutoplayLabel();
                        mascot().say("این صفحه تمام شد. «صفحه‌ی بعد» را بزن تا برویم صفحه‌ی بعد.");
                        return;
                    }
                    goToLine(next, true);
                    readCurrentLine(true);
                });
            }
            @Override public void onError() {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    autoplay = false;
                    if (isAdded()) updateAutoplayLabel();
                });
            }
        });
    }

    // ---------- drawing the screen ----------

    private void render() {
        List<String> lines = book.lines(page);
        if (line >= lines.size()) line = 0;

        Book.Chapter ch = Book.chapter(chapterOfPage(page));
        ((TextView) rootView.findViewById(R.id.book_subtitle)).setText(
            "صفحه‌ی " + fa(page) + " از " + fa(book.pageCount())
                + " · فصل " + ch.numberFa + ": " + ch.title
                + (lines.isEmpty() ? " · این صفحه فقط تصویر است" : " · " + fa(lines.size()) + " خط"));

        showPageImage();

        TextView current = rootView.findViewById(R.id.book_current);
        current.setText(lines.isEmpty()
            ? "این صفحه نوشته‌ای ندارد؛ با «صفحه‌ی بعد» جلو برو."
            : fa(line + 1) + ". " + lines.get(line));

        rootView.findViewById(R.id.book_prev_page).setVisibility(
            page <= BookText.FIRST_PAGE ? View.INVISIBLE : View.VISIBLE);
        rootView.findViewById(R.id.book_next_page).setVisibility(
            page >= book.pageCount() ? View.INVISIBLE : View.VISIBLE);
        rootView.findViewById(R.id.book_prev_line).setVisibility(line == 0 ? View.INVISIBLE : View.VISIBLE);
        rootView.findViewById(R.id.book_next_line).setVisibility(
            lines.isEmpty() || line >= lines.size() - 1 ? View.INVISIBLE : View.VISIBLE);
        updateAutoplayLabel();

        LinearLayout list = rootView.findViewById(R.id.book_lines);
        list.removeAllViews();
        for (int i = 0; i < lines.size(); i++) {
            list.addView(buildLineRow(i, lines.get(i)));
        }
    }

    private View buildLineRow(int index, String text) {
        boolean active = index == line;
        LinearLayout row = UiKit.column(requireContext());
        row.setPadding(UiKit.dp(requireContext(), 13), UiKit.dp(requireContext(), 11),
            UiKit.dp(requireContext(), 13), UiKit.dp(requireContext(), 11));
        UiKit.applyCardBg(row, requireContext(),
            active ? R.color.orange_bg : R.color.bg_card,
            active ? R.color.orange_border : R.color.border_card);
        row.setLayoutParams(UiKit.marginParams(requireContext(), 0, 7));

        TextView number = UiKit.text(requireContext(), "خط " + fa(index + 1), 10.5f,
            active ? R.color.orange_text : R.color.text_faint, true);
        row.addView(number);

        TextView body = UiKit.text(requireContext(), text, 13.5f, R.color.text_primary, active);
        body.setLayoutParams(UiKit.marginParams(requireContext(), 3, 0));
        row.addView(body);

        row.setOnClickListener(v -> goToLine(index, false));
        UiKit.tapSound(row);
        return row;
    }

    private void updateAutoplayLabel() {
        TextView button = rootView.findViewById(R.id.book_autoplay);
        button.setText(autoplay ? "بس است، نگه دار" : "هوهو از اینجا تا آخر صفحه بخواند");
    }

    private void showPageImage() {
        ImageView image = rootView.findViewById(R.id.book_page_image);
        releaseBitmap();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565; // half the memory, no visible loss
        try (InputStream in = requireContext().getAssets().open(BookText.imagePath(page))) {
            pageBitmap = BitmapFactory.decodeStream(in, null, options);
        } catch (IOException e) {
            pageBitmap = null;
        }
        if (pageBitmap != null) {
            image.setImageBitmap(pageBitmap);
            image.setVisibility(View.VISIBLE);
        } else {
            image.setImageDrawable(null);
            image.setVisibility(View.GONE);
        }
    }

    private void releaseBitmap() {
        ImageView image = rootView == null ? null : (ImageView) rootView.findViewById(R.id.book_page_image);
        if (image != null) image.setImageDrawable(null);
        if (pageBitmap != null) {
            pageBitmap.recycle();
            pageBitmap = null;
        }
    }

    @Override
    protected String entryTip() {
        return "این خودِ کتاب است. روی هر خط بزنی برایت می‌خوانم.";
    }
}
