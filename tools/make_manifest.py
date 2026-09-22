#!/usr/bin/env python3
"""
Rebuilds res/raw/audio_manifest.txt from the lesson scripts, so the list of narration files is
never out of step with what هوهو actually says.

Every lesson step names its audio file first and the spoken line right after it, which is all
this needs: it walks the lesson sources chapter by chapter and writes «key | text | تلفظ» lines,
grouped under each chapter, and inside it under each page and each section.

    python3 tools/make_manifest.py
"""
import glob
import hashlib
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pronounce import spoken, number_word, HUNDREDS  # noqa: E402

SRC = "app/src/main/java/com/hoohoomath/app/data"
OUT = "app/src/main/res/raw/audio_manifest.txt"

KEY = re.compile(r'"((?:ch\d+_s\d+_\d+|p\d{3}_\d+)x?)"\s*,\s*\n?\s*"((?:[^"\\]|\\.)*)"', re.S)
# «// ۱. ...» in the section lessons, «// ── صفحه‌ی ۷ — ... ──» in the page lessons
SECTION = re.compile(r'^\s*//\s*(?:(?:۰|[۱-۹][۰-۹]*|\d+)\.\s*(.+?)|──\s*(.+?)\s*──)\s*$', re.M)

CHAPTERS = [
    ("۱", "الگوها"),
    ("۲", "عددهای چهار رقمی"),
    ("۳", "عددهای کسری"),
    ("۴", "ضرب و تقسیم"),
    ("۵", "محیط و مساحت"),
    ("۶", "جمع و تفریق"),
    ("۷", "آمار و احتمال"),
    ("۸", "ضرب عددها"),
]

# ── the words هوهو needs to read a generated question ─────────────────────────
# Questions and their hints are made up on the fly, so they cannot have one recording each.
# Instead every fixed piece of wording in QuestionGenerator gets a recording, and numbers are said
# from recorded number words. tts/QuestionVoice splits a line exactly the same way at run time.

GENERATOR = os.path.join(SRC, "QuestionGenerator.java")
DIGITS = re.compile(r"[0-9۰-۹٠-٩]+")
LITERAL = re.compile(r'"((?:[^"\\]|\\.)*)"')

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
    lines = ["\n\n# ══════════ واژه‌های خواندنِ سؤال‌های تمرین و آزمون ══════════",
             "# این‌ها جمله نیستند؛ تکه‌هایی‌اند که کنارِ هم گذاشته می‌شوند تا هر سؤالِ تولیدشده خوانده شود.",
             "\n# ---- عددها ----"]
    count = 0
    for value in range(0, 101):
        word = number_word(value)
        lines.append("n_%d | %s | %s" % (value, word, word))
        count += 1
    for hundred, word in sorted(HUNDREDS.items()):
        if hundred != 1:
            lines.append("n_%d | %s | %s" % (hundred * 100, word, word))
            count += 1
    lines.append("n_1000 | هزار | هزار")
    lines.append("va | و | و")
    count += 2

    lines.append("\n# ---- تکه‌های متنِ سؤال‌ها ----")
    seen = set()
    src = open(GENERATOR, encoding="utf-8").read()
    for statement in re.finditer(r"\b(?:q|hint)\s*=\s*([^;]+);", src, re.S):
        for literal in LITERAL.finditer(statement.group(1)):
            text = literal.group(1).replace("\\n", " ")
            for piece in DIGITS.split(text):
                piece = spoken_form(piece)
                if not piece or piece in seen:
                    continue
                seen.add(piece)
                key = phrase_key(piece)
                lines.append("%s | %s | %s" % (key, piece, spoken(piece)))
                count += 1
    return "\n".join(lines), count


HEADER = """# فهرست گفتارهای درس — هوهو ریاضی
#
# هر خط سه ستون دارد:
#     <نام فایل> | <متنی که هوهو می‌گوید> | <تلفظ آوایی برای ضبط صدا>
#
# ستون دوم همان چیزی است که در اپ نوشته و خوانده می‌شود.
# ستون سوم فقط برای ساختِ صداست: نمادها و عددها به واژه تبدیل شده‌اند و واژه‌هایی که موتور
# گفتار اشتباه می‌خواند (کسر، مخرج، محور، …) با اعراب نوشته شده‌اند. اپ این ستون را نمی‌خواند.
# اگر واژه‌ای هنوز بد تلفظ شد، آن را در جدولِ WORDS در tools/pronounce.py اصلاح کن.
#
# کلیدهایی که به x ختم می‌شوند، مثالِ بیشترِ همان گام هستند (دکمه‌ی «یک مثال دیگر بزن») و با
# سرعتِ کمتری ضبط می‌شوند.
#
# این فایل با دست نوشته نمی‌شود — از خودِ درس‌ها ساخته می‌شود:
#     python3 tools/make_manifest.py
#
# ── ساخت فایل‌های صوتی ────────────────────────────────────────────────
#     pip install edge-tts
#     python3 tools/make_voice.py
#
# اگر خودت ضبط می‌کنی، فایل را با همان نام در app/src/main/res/raw/ بگذار
# (مثلاً p007_01.ogg). پسوندهای ogg و mp3 و wav هر سه کار می‌کنند.
#
# اگر فایلی نباشد، اپ همان متن را با موتور گفتار فارسی دستگاه می‌خواند.
#
# تعداد کل گفتارها: {count}
"""


def read_block(path):
    """«key | text» lines of one source file, with its own heading comments in place."""
    src = open(path, encoding="utf-8").read()
    marks = [(m.start(), "section", (m.group(1) or m.group(2)).strip())
             for m in SECTION.finditer(src)]
    marks += [(m.start(), "line", (m.group(1), m.group(2))) for m in KEY.finditer(src)]
    marks.sort(key=lambda t: t[0])

    lines, count = [], 0
    for _, kind, value in marks:
        if kind == "section":
            lines.append("\n# ---- %s ----" % value)
        else:
            key, text = value
            text = text.replace("\\n", " ")
            lines.append("%s | %s | %s" % (key, text, spoken(text)))
            count += 1
    return lines, count


def main():
    blocks = []
    total = 0
    for index, (number, title) in enumerate(CHAPTERS, 1):
        chapter = ["\n\n# ══════════ فصل %s — %s ══════════" % (number, title)]
        pages = os.path.join(SRC, "Chapter%dPages.java" % index)
        lessons = os.path.join(SRC, "Chapter%dLessons.java" % index)

        if os.path.exists(pages):
            lines, count = read_block(pages)
            if lines:
                chapter.append("\n## کتاب، صفحه به صفحه")
                chapter.extend(lines)
                total += count
        if os.path.exists(lessons):
            lines, count = read_block(lessons)
            if lines:
                chapter.append("\n\n## خلاصه‌ی بخش‌ها")
                chapter.extend(lines)
                total += count
        blocks.append("\n".join(chapter))

    for path in sorted(glob.glob(os.path.join(SRC, "Chapter*.java"))):
        name = os.path.basename(path)
        if not re.match(r"Chapter[1-8](Pages|Lessons)\.java$", name):
            print("note: %s is not in the chapter list and was skipped" % name)

    words, word_count = question_words()
    total += word_count

    with open(OUT, "w", encoding="utf-8") as f:
        f.write(HEADER.format(count=total))
        f.write("".join(blocks))
        f.write(words)
        f.write("\n")
    print("narration lines:", total, "(including", word_count, "question words)")


if __name__ == "__main__":
    main()
