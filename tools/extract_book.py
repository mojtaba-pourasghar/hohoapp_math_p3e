#!/usr/bin/env python3
"""
Turns the printed textbook into the app's «کتاب» mode: one JPEG per page (so the child sees the
real page, with the book's own drawings) plus that page's own sentences in reading order (so هوهو
can read the page line by line).

The PDF stores Persian text in *visual* order — a plain `pdftotext` therefore returns lines whose
letters are scrambled. `pdftohtml -xml` keeps every run positioned, so each run is turned back into
logical order here: the characters are reversed, and runs of digits/formulas are flipped back.

    python3 tools/extract_book.py project/uploads/sevom-riazi-1404.pdf app/src/main/assets/book
"""
import json
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

# the bidirectional control characters the PDF sprinkles through its text
CONTROLS = dict.fromkeys(map(ord, "‎‏‪‫‬‭‮⁦⁧⁨⁩"), None)

LTR_RUN = re.compile(r"[0-9A-Za-z٠-٩۰-۹+\-=×÷/%.,:<>٫]+")
PERSIAN = re.compile(r"[؀-ۿ]")


# لا is one ligature glyph in the PDF, so it has to be flipped as a single unit — otherwise
# «کلاس» comes back as «کالس» and «حالا» as «حاال».
LIGATURES = ("\u0644\u0627", "\u0644\u0622", "\u0644\u0623", "\u0644\u0625")


def reverse_units(text):
    units, i = [], 0
    while i < len(text):
        if text[i:i + 2] in LIGATURES:
            units.append(text[i:i + 2])
            i += 2
        else:
            units.append(text[i])
            i += 1
    units.reverse()
    return "".join(units)


def visual_to_logical(text):
    """The PDF's right-to-left runs come out backwards; put them back in reading order."""
    text = text.translate(CONTROLS)
    if not PERSIAN.search(text):
        return text.strip()
    flipped = reverse_units(text)
    # numbers and formulas read left-to-right, so flip those spans back
    return LTR_RUN.sub(lambda m: m.group(0)[::-1], flipped).strip()


# Putting an RTL run back in reading order runs into one genuine ambiguity: the PDF stores the
# «لا» ligature as a single glyph, so a visual «لا» is either a real ligature (کلاس، حالا) or the
# flip of «ال» (سال، الگو) — the text alone cannot tell which. Reversing as a unit gets the
# ligature words right, and the words below are the finite set from this book that it gets wrong.
LIGATURE_FIXES = {
    "لایت": "الیت", "ریلا": "ریال", "ریلای": "ریالی", "خلای": "خالی",
    "سلا": "سال", "سلاه": "ساله", "ارسلا": "ارسال",
    "لاگو": "الگو", "لاگوها": "الگوها", "لاگو\u200cها": "الگو\u200cها",
    "لاگوهای": "الگوهای", "لاگوی": "الگوی", "لاگویی": "الگویی",
    "لاگوسازی": "الگوسازی", "لاگویابی": "الگویابی",
    "سؤلا": "سؤال", "سؤلا\u200cها": "سؤال\u200cها", "سؤلا\u200cهای": "سؤال\u200cهای",
    "حلات": "حالت", "حلات\u200cها": "حالت\u200cها", "حلات\u200cهای": "حالت\u200cهای",
    "حلات\u200cهایی": "حالت\u200cهایی", "حلا": "حال", "خوشحلا": "خوشحال",
    "احتملا": "احتمال", "احتملای": "احتمالی",
    "مثلا": "مثال", "مثلا\u200cها": "مثال\u200cها",
    "پرتقلا": "پرتقال", "گلابی": "گلابی",
    "فعّلا": "فعّال", "فعّلایت": "فعّالیت", "فعّلایت\u200cهای": "فعّالیت\u200cهای",
    "مطلاعه": "مطالعه", "مطلاب": "مطالب", "ملا": "مال", "تعلای": "تعالی",
    "لاف": "الف", "توچلا": "توچال", "بسکتبلا": "بسکتبال", "فوتبلا": "فوتبال",
    "ولایبلا": "والیبال", "علامیان": "عالمیان", "شملای": "شمالی",
    "لاکترونیکی": "الکترونیکی", "لابته": "البته", "لابتّه": "البتّه", "لاهی": "الهی",
    "دنبلا": "دنبال", "اشکلای": "اشکالی", "تکلایف": "تکالیف", "لاعاده": "العاده",
}

# letters and diacritics only: Persian comma/question mark must not glue onto a word
WORD = re.compile(r"[\u0621-\u063A\u0641-\u0652\u0670-\u06D3\u200c]+")


def fix_ligatures(text):
    return WORD.sub(lambda m: LIGATURE_FIXES.get(m.group(0), m.group(0)), text)


def normalize(text):
    text = text.replace("ي", "ی").replace("ك", "ک")  # Arabic ya/kaf → Persian
    text = re.sub(r"\s+", " ", text)
    # the PDF drops every zero-width non-joiner, which looks wrong and makes the voice read
    # «می کند» as two separate words
    text = re.sub(r"(^|\s)(می|نمی) ([\u0600-\u06FF])", "\\1\\2\u200c\\3", text)
    text = re.sub(r"([\u0600-\u06FF]) (ها|های|هایی|تر|ترین)(?=\s|$)", "\\1\u200c\\2", text)
    return fix_ligatures(text).strip(" ‌")


# the "pupil's handwriting" font stores its glyphs in a scrambled order that cannot be put back
# together from the PDF; those runs are the sample answers already visible in the page picture,
# so they are left to the picture instead of being read out as nonsense.
SKIP_FONTS = ("Tahriri",)


def page_lines(page_el):
    """Every run on the page, grouped into visual lines and ordered right to left."""
    skip_ids = {spec.get("id") for spec in page_el.findall("fontspec")
                if any(bad in (spec.get("family") or "") for bad in SKIP_FONTS)}
    rows = []
    for t in page_el.findall("text"):
        raw = "".join(t.itertext())
        if not raw or not raw.strip():
            continue
        if t.get("font") in skip_ids:
            continue
        rows.append({
            "top": int(t.get("top", "0")),
            "left": int(t.get("left", "0")),
            "font": t.get("font", ""),
            "text": raw,
        })
    rows.sort(key=lambda r: (r["top"], -r["left"]))

    lines, current, current_top = [], [], None
    for r in rows:
        if current_top is None or abs(r["top"] - current_top) <= 9:
            current.append(r)
            current_top = r["top"] if current_top is None else current_top
        else:
            lines.append(current)
            current, current_top = [r], r["top"]
    if current:
        lines.append(current)

    out = []
    for group in lines:
        group.sort(key=lambda r: -r["left"])
        joined = normalize(" ".join(visual_to_logical(r["text"]) for r in group))
        if len(joined) < 2:
            continue
        out.append(joined)
    return out


def main():
    pdf = sys.argv[1]
    out_dir = sys.argv[2]
    os.makedirs(out_dir, exist_ok=True)

    total = int(re.search(r"Pages:\s+(\d+)", subprocess.run(
        ["pdfinfo", pdf], capture_output=True, text=True).stdout).group(1))

    subprocess.run(["pdftoppm", "-r", "110", "-jpeg", "-jpegopt", "quality=72",
                    pdf, os.path.join(out_dir, "p")], check=True)
    # pdftoppm names files p-001.jpg; the app wants p001.jpg
    for name in sorted(os.listdir(out_dir)):
        if name.startswith("p-") and name.endswith(".jpg"):
            os.rename(os.path.join(out_dir, name),
                      os.path.join(out_dir, "p" + name[2:]))

    xml_path = os.path.join(out_dir, "_book.xml")
    subprocess.run(["pdftohtml", "-xml", "-i", "-q", pdf, xml_path[:-4]], check=True)
    tree = ET.parse(xml_path)

    pages = []
    for page_el in tree.getroot().findall("page"):
        number = int(page_el.get("number"))
        pages.append({"p": number, "lines": page_lines(page_el)})
    os.remove(xml_path)

    with open(os.path.join(out_dir, "pages.json"), "w", encoding="utf-8") as f:
        json.dump({"total": total, "pages": pages}, f, ensure_ascii=False)

    spoken = sum(len(p["lines"]) for p in pages)
    print("pages: %d  lines: %d" % (len(pages), spoken))


if __name__ == "__main__":
    main()
