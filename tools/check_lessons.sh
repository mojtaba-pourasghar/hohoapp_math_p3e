#!/usr/bin/env bash
# Syntax-checks the lesson data classes with plain javac. They are pure Java (no Android
# imports), so a mistake in a Chapter*Pages.java or Chapter*Lessons.java shows up here in a
# second instead of waiting for a whole APK build on CI.
set -e
cd "$(dirname "$0")/.."
SRC=app/src/main/java/com/hoohoomath/app/data
OUT=$(mktemp -d)
trap 'rm -rf "$OUT"' EXIT
javac -nowarn -d "$OUT" \
  "$SRC"/LessonKind.java "$SRC"/LessonStep.java "$SRC"/LessonScript.java "$SRC"/StageSpec.java \
  "$SRC"/Book.java "$SRC"/Lessons.java "$SRC"/PageLessons.java \
  "$SRC"/Chapter*Lessons.java "$SRC"/Chapter*Pages.java
echo "lesson data classes compile OK"
