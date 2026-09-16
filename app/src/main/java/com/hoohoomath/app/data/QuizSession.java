package com.hoohoomath.app.data;

import java.util.ArrayList;
import java.util.List;

/** An in-progress or completed practice/worksheet/exam attempt. */
public class QuizSession {
    public final QuizMode mode;
    public final int chapter;
    public final int option; // section index for PRACTICE, level index for WORKSHEET/EXAM
    public final int round;  // which set of generated questions this attempt used
    public final List<QuestionItem> items;
    public int index = 0;
    public int rightCount = 0;
    public final List<LogEntry> log = new ArrayList<>();

    public QuizSession(QuizMode mode, int chapter, int option, int round, List<QuestionItem> items) {
        this.mode = mode;
        this.chapter = chapter;
        this.option = option;
        this.round = round;
        this.items = items;
    }

    public QuestionItem current() {
        return items.get(index);
    }

    public boolean isLast() {
        return index >= items.size() - 1;
    }

    public static class LogEntry {
        public final String question;
        public final String correctAnswer;
        public final String yourAnswer;
        public final boolean correct;

        public LogEntry(String question, String correctAnswer, String yourAnswer, boolean correct) {
            this.question = question;
            this.correctAnswer = correctAnswer;
            this.yourAnswer = yourAnswer;
            this.correct = correct;
        }
    }
}
