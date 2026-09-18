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
import hashlib
import os
import re

SRC = "app/src/main/java/com/hoohoomath/app/data"
OUT = "app/src/main/res/raw/audio_manifest.txt"

KEY = re.compile(r'"((?:ch\d+_s\d+_\d+|p\d{3}_\d+)x?)"\s*,\s*\n?\s*"((?:[^"\\]|\\.)*)"', re.S)
# «// ۱. ...» in the section lessons, «// ── صفحه‌ی ۷ — ... ──» in the page lessons
SECTION = re.compile(r'^\s*//\s*(?:(?:۰|[۱-۹][۰-۹]*|\d+)\.\s*(.+?)|──\s*(.+?)\s*──)\s*$', re.M)

# ── the words هوهو needs to read a generated question ─────────────────────────
# Questions and their hints are made up on the fly, so they cannot have one recording each.
# Instead every fixed piece of wording in QuestionGenerator gets a recording, and numbers are said
# from recorded number words. tts/QuestionVoice splits a line exactly the same way at run time.

GENERATOR = os.path.join(SRC, "QuestionGenerator.java")
DIGITS = re.compile(r"[0-9\u06f0-\u06f9\u0660-\u0669]+")
LITERAL = re.compile(r'"((?:[^"\\]|\\.)*)"')

ONES = ["صفر", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه", "ده",
        "یازده", "دوازده", "سیزده", "چهارده", "پانزده", "شانزده", "هفده", "هجده", "نوزده"]
TENS = {20: "بیست", 30: "سی", 40: "چهل", 50: "پنجاه", 60: "شصت", 70: "هفتاد",
        80: "هشتاد", 90: "نود"}
HUNDREDS = {100: "صد", 200: "دویست", 300: "سیصد", 400: "چهارصد", 500: "پانصد",
            600: "ششصد", 700: "هفتصد", 800: "هشتصد", 900: "نهصد"}


def number_word(value):
    if value < 20:
        return ONES[value]
    if value in TENS:
        return TENS[value]
    if value < 100:
        return TENS[value // 10 * 10] + " و " + ONES[value % 10]
    if value in HUNDREDS:
        return HUNDREDS[value]
    return None


# the same table as tts/QuestionVoice.SPOKEN — a speech engine cannot read «×» or «⬜»
SPOKEN = [
    ("⬜", "چند"), ("×", "ضربدر"), ("÷", "تقسیم بر"), ("−", "منهای"), ("–", "منهای"),
    ("+", "به‌اضافه‌ی"), ("=", "مساوی"), ("→", "می‌شود"), ("/", "روی"),
    ("«", " "), ("»", " "), ("،", " "), (".", " "), ("؟", " "), ("!", " "), (":", " "),
    ("؛", " "), ("(", " "), (")", " "), ("—", " "), ("…", " "), ("⌫", " "),
]


def spoken_form(piece):
    out = piece
    for symbol, word in SPOKEN:
        out = out.replace(symbol, " " + word + " ")
    return re.sub(r"\s+", " ", out).strip()


def phrase_key(piece):
    return "q_" + hashlib.sha1(piece.encode("utf-8")).hexdigest()[:10]


def question_words():
    """Every piece of wording a generated question can contain, plus the number words."""
    lines = ["\n# ---- واژه‌های شماره برای خواندن سؤال‌ها ----"]
    for value in range(0, 101):
        lines.append("n_%d | %s" % (value, number_word(value)))
    for value in sorted(HUNDREDS):
        if value != 100:
            lines.append("n_%d | %s" % (value, HUNDREDS[value]))
    lines.append("n_1000 | هزار")
    lines.append("va | و")

    pieces = []
    seen = set()
    src = open(GENERATOR, encoding="utf-8").read()
    for statement in re.findall(r"\b(?:q|hint) = (.+?);", src, re.S):
        for literal in LITERAL.findall(statement):
            for piece in DIGITS.split(literal):
                piece = piece.strip()
                if not piece or piece in seen:
                    continue
                said = spoken_form(piece)
                if not said:          # only punctuation: the app skips it too
                    continue
                seen.add(piece)
                pieces.append((piece, said))

    lines.append("\n# ---- تکه‌های متنِ سؤال‌ها ----")
    for piece, said in pieces:
        lines.append("%s | %s" % (phrase_key(piece), said))
    return "\n".join(lines), 102 + 8 + 1 + 1 + len(pieces) - 1


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
    sources = sorted(glob.glob(os.path.join(SRC, "Chapter*Lessons.java")))
    sources += sorted(glob.glob(os.path.join(SRC, "Chapter*Pages.java")))
    for path in sources:
        src = open(path, encoding="utf-8").read()
        marks = [(m.start(), "section", (m.group(1) or m.group(2)).strip())
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

    words, word_count = question_words()
    total += word_count

    with open(OUT, "w", encoding="utf-8") as f:
        f.write(HEADER.format(count=total))
        f.write("\n".join(blocks))
        f.write("\n")
        f.write(words)
        f.write("\n")
    print("narration lines:", total, "(including", word_count, "question words)")


if __name__ == "__main__":
    main()
