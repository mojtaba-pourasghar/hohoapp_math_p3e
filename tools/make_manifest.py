#!/usr/bin/env python3
"""
Rebuilds res/raw/audio_manifest.txt from the lesson scripts, so the list of narration files is
never out of step with what هوهو actually says.

Every lesson step names its audio file first and the spoken line right after it, which is all
this needs: it walks the Chapter*Lessons.java sources in order and writes «key | text» lines,
grouped under each section's own heading comment.

    python3 tools/make_manifest.py
"""
import glob
import os
import re

SRC = "app/src/main/java/com/hoohoomath/app/data"
OUT = "app/src/main/res/raw/audio_manifest.txt"

KEY = re.compile(r'"(ch\d+_s\d+_\d+x?)"\s*,\s*\n?\s*"((?:[^"\\]|\\.)*)"', re.S)
SECTION = re.compile(r'^\s*//\s*(۰|[۱-۹][۰-۹]*|\d+)\.\s*(.+?)\s*$', re.M)

HEADER = """# فهرست گفتارهای درس — هوهو ریاضی
#
# هر خط:  <نام فایل> | <متنی که هوهو می‌گوید>
# کلیدهایی که به x ختم می‌شوند، مثال بیشترِ همان گام هستند (دکمه‌ی «یک مثال دیگر بزن»).
#
# این فایل با دست نوشته نمی‌شود — از خودِ درس‌ها ساخته می‌شود:
#     python3 tools/make_manifest.py
#
# ── ساخت فایل‌های صوتی ────────────────────────────────────────────────
# این کار خودکار روی GitHub انجام می‌شود؛ بیلد APK خودش فایل‌های تازه را می‌سازد
# و در همین پوشه کامیت می‌کند. دستی هم می‌شود:
#     pip install edge-tts
#     python3 tools/make_voice.py
#
# اگر خودتان ضبط می‌کنید، فایل را با همان نام در app/src/main/res/raw/ بگذارید
# (مثلاً ch1_s0_01.ogg). پسوندهای ogg و mp3 و wav هر سه کار می‌کنند.
#
# اگر فایلی نباشد، اپ همان متن را با موتور گفتار فارسی دستگاه می‌خواند.
#
# تعداد کل گفتارها: {count}
"""


def main():
    blocks = []
    total = 0
    for path in sorted(glob.glob(os.path.join(SRC, "Chapter*Lessons.java"))):
        src = open(path, encoding="utf-8").read()
        marks = [(m.start(), "section", m.group(0).strip().lstrip("/").strip())
                 for m in SECTION.finditer(src)]
        marks += [(m.start(), "line", (m.group(1), m.group(2))) for m in KEY.finditer(src)]
        marks.sort(key=lambda t: t[0])

        lines = []
        for _, kind, value in marks:
            if kind == "section":
                lines.append("\n# ---- %s ----" % value)
            else:
                key, text = value
                lines.append("%s | %s" % (key, text.replace("\\n", " ")))
                total += 1
        if lines:
            blocks.append("\n".join(lines))

    with open(OUT, "w", encoding="utf-8") as f:
        f.write(HEADER.format(count=total))
        f.write("\n".join(blocks))
        f.write("\n")
    print("narration lines:", total)


if __name__ == "__main__":
    main()
