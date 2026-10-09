#!/usr/bin/env python3
"""هوهو's icon — written once, emitted twice.

The launcher icon is an Android vector and the store icon is a 512 PNG, and the two have to be
the same owl. So the drawing lives here, in one list of shapes, and this script writes both:

    app/src/main/res/drawable/ic_launcher_foreground.xml   the launcher
    project/store/icon-512.png                             the Bazaar store page

Keeping the geometry in one place is not tidiness for its own sake: the first store icon was a
hand copy of the launcher, and a hand copy is a thing that drifts the moment either side is
touched.

What she is: the same owl girl as in the app — same orange, same cream face, same pink flower
over her ear — now with proper pointed owl tufts instead of the side blobs that read as
earmuffs, and a teal badge with a plus sign, so the icon says «ریاضی» and not just «a bird».

Everything is kept inside a circle of radius 33 around the centre, because an adaptive icon is
cut to a shape the phone chooses and only that circle is promised to survive.
"""
import math
import os

VIEWPORT = 108                 # the vector's coordinate space
CENTRE = VIEWPORT / 2
SAFE_RADIUS = 33               # what an adaptive mask is guaranteed to keep

SIZE = 512                     # the store icon
SUPER = 4                      # drawn this many times bigger, then shrunk: smooth edges

# The same drawing wants two different sizes.
#
# On the home screen the phone cuts the icon to a shape of its choosing and sets it among other
# icons, so the art has to sit well inside the cut with room around it — filling the mask to its
# edge looks cramped next to everything else on the screen. On a store page there is no mask and
# the icon stands alone, so it can be larger in its square.
LAUNCHER_SCALE = 0.78
STORE_SCALE = 0.92

BACKGROUND = "#FDF4E3"         # orange_bg — the app's own paper colour
ORANGE = "#E8973A"             # orange
ORANGE_DARK = "#D8811F"        # orange_dark
CREAM = "#FDF4E3"
WHITE = "#FFFFFF"
INK = "#3B3027"
TEAL = "#3AA79A"               # teal — the app's second colour, and here the maths
PINK = "#D94F7A"               # pink
POLLEN = "#F7C94B"


# ── the drawing, back to front ───────────────────────────────────────────────

def shapes():
    out = []

    # ear tufts, behind the head, pointing up the way an owl's do
    out.append(("poly", [(38, 36), (36, 26), (51, 31)], ORANGE_DARK))
    out.append(("poly", [(70, 36), (72, 26), (57, 31)], ORANGE_DARK))

    out.append(("ellipse", (54, 58, 25, 26), ORANGE))          # body and head in one round shape
    out.append(("ellipse", (54, 50, 17, 15), CREAM))           # the face plate

    for eye_x in (46, 62):                                      # eyes, wide the way a child draws
        out.append(("circle", (eye_x, 49, 7.5), WHITE))
        out.append(("circle", (eye_x, 49, 3.2), INK))
        out.append(("circle", (eye_x + 1.6, 47.4, 1.3), WHITE))

    out.append(("poly", [(54, 56), (58, 63), (50, 63)], ORANGE_DARK))   # beak

    # the maths: a teal badge with a plus, on her chest. The bars stay well inside the circle —
    # the first try had them longer than the badge was wide, which read as a cross cutting it.
    out.append(("circle", (54, 73, 9), TEAL))
    out.append(("poly", bar(54, 73, 5.6, 1.9), WHITE))
    out.append(("poly", bar(54, 73, 1.9, 5.6), WHITE))

    # the flower, tucked beside her head rather than on top of it — above the ear it swallowed
    # the left tuft and left her looking one-horned
    for px, py in ((29, 45), (33, 41), (33, 49), (37, 45)):
        out.append(("circle", (px, py, 4), PINK))
    out.append(("circle", (33, 45, 3.2), POLLEN))

    return out


def bar(cx, cy, half_w, half_h):
    return [(cx - half_w, cy - half_h), (cx + half_w, cy - half_h),
            (cx + half_w, cy + half_h), (cx - half_w, cy + half_h)]


# ── checking it will survive the mask ────────────────────────────────────────

def furthest():
    """How far the drawing reaches from the centre, and what reaches it."""
    worst = (0, None)
    for kind, data, _ in shapes():
        if kind == "poly":
            points = data
        elif kind == "circle":
            cx, cy, r = data
            points = [(cx + r * math.cos(a * math.pi / 8), cy + r * math.sin(a * math.pi / 8))
                      for a in range(16)]
        else:
            cx, cy, rx, ry = data
            points = [(cx + rx * math.cos(a * math.pi / 8), cy + ry * math.sin(a * math.pi / 8))
                      for a in range(16)]
        for x, y in points:
            d = math.hypot(x - CENTRE, y - CENTRE)
            if d > worst[0]:
                worst = (d, (kind, round(x, 1), round(y, 1)))
    return worst


# ── the Android vector ───────────────────────────────────────────────────────

def resize(kind, data, k):
    """The same shape, drawn k times its size about the middle of the canvas."""
    def pull(v, axis_centre):
        return axis_centre + (v - axis_centre) * k

    if kind == "poly":
        return [(pull(x, CENTRE), pull(y, CENTRE)) for x, y in data]
    if kind == "circle":
        cx, cy, r = data
        return (pull(cx, CENTRE), pull(cy, CENTRE), r * k)
    cx, cy, rx, ry = data
    return (pull(cx, CENTRE), pull(cy, CENTRE), rx * k, ry * k)


def path_for(kind, data):
    if kind == "poly":
        head = "M%s,%s " % (fmt(data[0][0]), fmt(data[0][1]))
        rest = " ".join("L%s,%s" % (fmt(x), fmt(y)) for x, y in data[1:])
        return head + rest + " Z"
    if kind == "circle":
        cx, cy, r = data
        return arc(cx, cy, r, r)
    cx, cy, rx, ry = data
    return arc(cx, cy, rx, ry)


def arc(cx, cy, rx, ry):
    # «<circle>» does not exist in an Android vector; a round shape is two half arcs
    return "M%s,%s a%s,%s 0 1,0 %s,0 a%s,%s 0 1,0 -%s,0" % (
        fmt(cx - rx), fmt(cy), fmt(rx), fmt(ry), fmt(rx * 2), fmt(rx), fmt(ry), fmt(rx * 2))


def fmt(value):
    text = ("%.2f" % value).rstrip("0").rstrip(".")
    return text if text else "0"


def write_vector(path):
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        '<!-- ساخته‌ی tools/make_icon.py — با دست عوضش نکن؛ آن فایل را عوض کن و دوباره بزن. -->',
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        '    android:width="108dp"',
        '    android:height="108dp"',
        '    android:viewportWidth="108"',
        '    android:viewportHeight="108">',
        '',
    ]
    for kind, data, colour in shapes():
        lines.append('    <path android:fillColor="%s" android:pathData="%s" />'
                     % (colour, path_for(kind, resize(kind, data, LAUNCHER_SCALE))))
    lines.append('')
    lines.append('</vector>')
    with open(path, "w", encoding="utf-8") as handle:
        handle.write("\n".join(lines) + "\n")


# ── the store PNG ────────────────────────────────────────────────────────────

def write_png(path):
    from PIL import Image, ImageDraw

    image = Image.new("RGB", (SIZE * SUPER, SIZE * SUPER), BACKGROUND)
    draw = ImageDraw.Draw(image)
    scale = SIZE * SUPER / VIEWPORT

    for kind, raw, colour in shapes():
        data = resize(kind, raw, STORE_SCALE)
        if kind == "poly":
            points = data
        else:
            cx, cy, rx, ry = (data + (data[2],))[:4] if kind == "circle" else data
            points = [(cx + rx * math.cos(i * math.pi / 120), cy + ry * math.sin(i * math.pi / 120))
                      for i in range(240)]
        draw.polygon([(x * scale, y * scale) for x, y in points], fill=colour)

    image.resize((SIZE, SIZE), Image.LANCZOS).save(path, "PNG")


def main():
    root = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
    vector = os.path.join(root, "app/src/main/res/drawable/ic_launcher_foreground.xml")
    png = os.path.join(root, "project/store/icon-512.png")
    os.makedirs(os.path.dirname(png), exist_ok=True)

    reach, what = furthest()
    masked = reach * LAUNCHER_SCALE
    print("دورترین نقطه در لانچر: %.1f (حد امن %d) — %s" % (masked, SAFE_RADIUS, what))
    if masked > SAFE_RADIUS:
        print("⚠ بخشی از نقاشی ممکن است زیر ماسکِ آیکون بریده شود.")

    write_vector(vector)
    write_png(png)
    print("نوشته شد:", os.path.relpath(vector, root))
    print("نوشته شد:", os.path.relpath(png, root))


if __name__ == "__main__":
    main()
