#!/usr/bin/env python3
"""
The spoken form of a narration line — what the voice should actually say.

A Persian speech engine reads «۶ × ۷ = ⬜» as silence or as English, and it guesses wrong on a
handful of maths words that are written the same but said differently (کسر، مخرج، محور…). So
every line in res/raw/audio_manifest.txt carries a third column: the same sentence rewritten the
way a teacher would pronounce it.

The app never reads this column — it is only for making the voice files. If a word still sounds
wrong, fix it in WORDS below and rebuild the manifest:

    python3 tools/make_manifest.py
"""
import re

# ── digits ───────────────────────────────────────────────────────────────────
ONES = ["صفر", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه", "ده",
        "یازده", "دوازده", "سیزده", "چهارده", "پانزده", "شانزده", "هفده", "هجده", "نوزده"]
TENS = {2: "بیست", 3: "سی", 4: "چهل", 5: "پنجاه", 6: "شصت", 7: "هفتاد", 8: "هشتاد", 9: "نود"}
HUNDREDS = {1: "صد", 2: "دویست", 3: "سیصد", 4: "چهارصد", 5: "پانصد",
            6: "ششصد", 7: "هفتصد", 8: "هشتصد", 9: "نهصد"}


def number_word(value):
    """۱۳۵۷ → «هزار و سیصد و پنجاه و هفت». Falls back to digit-by-digit above 9999."""
    if value < 0:
        return "منهای " + number_word(-value)
    if value > 9999:
        return " ".join(ONES[int(d)] for d in str(value))
    parts = []
    thousands, rest = divmod(value, 1000)
    if thousands:
        parts.append("هزار" if thousands == 1 else ONES[thousands] + " هزار")
    hundreds, rest = divmod(rest, 100)
    if hundreds:
        parts.append(HUNDREDS[hundreds])
    if rest < 20:
        if rest or not parts:
            parts.append(ONES[rest])
    else:
        tens, ones = divmod(rest, 10)
        parts.append(TENS[tens])
        if ones:
            parts.append(ONES[ones])
    return " و ".join(parts)


DIGITS = str.maketrans("۰۱۲۳۴۵۶۷۸۹٠١٢٣٤٥٦٧٨٩", "01234567890123456789")
# Arabic letterforms a Persian voice stumbles over
LETTERS = str.maketrans({"ي": "ی", "ك": "ک", "ة": "ه", "أ": "ا", "إ": "ا", "ؤ": "و"})

# ── symbols ──────────────────────────────────────────────────────────────────
# the same table as tts/SpokenText and tts/QuestionVoice — a voice cannot read «×» or «⬜»
SYMBOLS = [
    ("⬜", " چند "), ("×", " ضربدر "), ("÷", " تقسیم بر "), ("−", " منهای "),
    ("+", " به‌اضافه‌ی "), ("=", " مساوی "), ("→", " می‌شود "),
    ("٪", " درصد "), ("%", " درصد "), ("_", " جای خالی "),
    ("…", "، "), ("«", " "), ("»", " "), ("(", " "), (")", " "),
    ("—", "، "), ("–", "، "), ("⌫", " "), ("🎉", " "),
    ("‎", ""), ("‏", ""),
]

# ── words a Persian voice reads with the wrong vowels ─────────────────────────
# Written the same, said differently. Add to this list whenever a word sounds wrong in the
# finished audio; nothing else needs changing.
WORDS = {
    "کسر": "کَسر",
    "کسری": "کَسری",
    "کسرها": "کَسرها",
    "کسرهای": "کَسرهای",
    "مخرج": "مَخرَج",
    "صورت": "صورَت",
    "ضرب": "ضَرب",
    "ضربدر": "ضَربدَر",
    "جمع": "جَمع",
    "تفریق": "تَفریق",
    "تقسیم": "تَقسیم",
    "عدد": "عَدَد",
    "عددها": "عَدَدها",
    "عددهای": "عَدَدهای",
    "رقم": "رَقَم",
    "رقمی": "رَقَمی",
    "رقمش": "رَقَمش",
    "محور": "مِحوَر",
    "قطر": "قُطر",
    "قطرها": "قُطرها",
    "شعاع": "شُعاع",
    "محیط": "مُحیط",
    "مساحت": "مِساحَت",
    "تقریب": "تَقریب",
    "تقریبی": "تَقریبی",
    "احتمال": "اِحتِمال",
    "مسئله": "مَسئَله",
    "مسئله‌ها": "مَسئَله‌ها",
    "زیرمسئله": "زیرمَسئَله",
    "فرد": "فَرد",
    "زوج": "زوج",
    "نمودار": "نِمودار",
    "تقارن": "تَقارُن",
    "متقارن": "مُتَقارِن",
    "مقایسه": "مُقایِسه",
    "خارج‌قسمت": "خارِج‌قِسمَت",
    "الگو": "اُلگو",
    "الگوی": "اُلگوی",
    "الگوها": "اُلگوها",
    "الگویابی": "اُلگویابی",
    "الگوسازی": "اُلگوسازی",
}
# Only whole words are respelled, plus these endings — so «قطر» is fixed but «قطره» is left
# alone, and «کسرِ» keeps its ezafe.
SUFFIXES = ["", "\u0650", "ی", "یِ", "ها", "هاِ", "های", "هایِ", "ش", "شِ", "شان", "م", "مان",
            "ت", "تان", "‌ها", "‌های", "‌هایِ"]
_DIGIT_RUN = re.compile(r"[0-9]+")
_TOKEN = re.compile(r"[\u0621-\u06CC\u200c\u064B-\u0652]+")


def _respell(token):
    for suffix in SUFFIXES:
        if suffix and not token.endswith(suffix):
            continue
        stem = token[: len(token) - len(suffix)] if suffix else token
        if stem in WORDS:
            return WORDS[stem] + suffix
    return token


def spoken(text):
    """The line rewritten the way it should be said out loud."""
    if not text:
        return text
    out = text.translate(LETTERS).translate(DIGITS)

    # a fraction is read «صورت روی مخرج»; a slash anywhere else is just a pause
    out = re.sub(r"([0-9])\s*/\s*([0-9])", r"\1 روی \2", out)
    out = out.replace("/", " ")

    for symbol, word in SYMBOLS:
        out = out.replace(symbol, word)

    out = _DIGIT_RUN.sub(lambda m: " " + number_word(int(m.group())) + " ", out)
    out = _TOKEN.sub(lambda m: _respell(m.group()), out)

    out = re.sub(r"\s+([،؛,])", r"\1", out)          # no space before a comma
    out = re.sub(r"([،؛])\1+", r"\1", out)             # and never two in a row
    return re.sub(r"\s{2,}", " ", out).strip()
