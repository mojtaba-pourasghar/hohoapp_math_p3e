package com.hoohoomath.app.data;

import java.util.List;

/** The full voice-guided lesson for one (chapter, section). */
public class LessonScript {
    public final int chapter;
    public final int section;
    public final List<LessonStep> steps;

    public LessonScript(int chapter, int section, List<LessonStep> steps) {
        this.chapter = chapter;
        this.section = section;
        this.steps = steps;
    }
}
