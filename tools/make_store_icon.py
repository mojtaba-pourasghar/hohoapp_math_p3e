#!/usr/bin/env python3
"""The 512×512 store icon, drawn from the app's own launcher vector.

Bazaar wants a flat PNG; the app has an adaptive icon made of vector paths. Rather than
redrawing هوهو by hand and ending up with a store icon that is nearly the launcher, the very
same geometry is read off res/drawable/ic_launcher_foreground.xml and rendered here — so the
icon on the store page and the icon on the home screen are the same owl.

Drawn at 4× and shrunk, which is the cheapest honest antialiasing there is.
"""
from PIL import Image, ImageDraw

SIZE = 512
SUPER = 4                      # draw big, shrink down: smooth edges without a vector library
VIEWPORT = 108                 # the vector's own coordinate space

BACKGROUND = "#FDF4E3"
EAR = "#D8811F"
BODY = "#E8973A"
FACE = "#FDF4E3"
EYE_WHITE = "#FFFFFF"
PUPIL = "#3B3027"
BEAK = "#D8811F"
PETAL = "#D94F7A"
POLLEN = "#F7C94B"

# the launcher's foreground reaches the edge of the 108 box, where the system mask would cut it.
# A flat store icon has no mask, so the owl is pulled in a little and given room to breathe.
INSET = 0.88


def bezier(p0, p1, p2, p3, steps=48):
    """A cubic curve as points — Pillow draws polygons, not paths."""
    out = []
    for i in range(steps + 1):
        t = i / steps
        u = 1 - t
        out.append((
            u * u * u * p0[0] + 3 * u * u * t * p1[0] + 3 * u * t * t * p2[0] + t * t * t * p3[0],
            u * u * u * p0[1] + 3 * u * u * t * p1[1] + 3 * u * t * t * p2[1] + t * t * t * p3[1],
        ))
    return out


class Canvas:
    """Draws in the vector's 108×108 coordinates, onto a much larger bitmap."""

    def __init__(self):
        self.image = Image.new("RGB", (SIZE * SUPER, SIZE * SUPER), BACKGROUND)
        self.draw = ImageDraw.Draw(self.image)
        self.scale = SIZE * SUPER / VIEWPORT

    def at(self, point):
        middle = VIEWPORT / 2
        x = middle + (point[0] - middle) * INSET
        y = middle + (point[1] - middle) * INSET
        return (x * self.scale, y * self.scale)

    def circle(self, cx, cy, r, fill):
        self.polygon([(cx + r * _cos(i), cy + r * _sin(i)) for i in range(180)], fill)

    def polygon(self, points, fill):
        self.draw.polygon([self.at(p) for p in points], fill=fill)

    def finish(self, path):
        self.image.resize((SIZE, SIZE), Image.LANCZOS).save(path, "PNG")


def _cos(step):
    import math
    return math.cos(step * math.pi / 90)


def _sin(step):
    import math
    return math.sin(step * math.pi / 90)


def main():
    c = Canvas()

    # ear tufts
    c.polygon(bezier((24, 48), (18, 50), (14, 58), (16, 66))
              + bezier((16, 66), (18, 72), (26, 74), (30, 70)) + [(34, 56)], EAR)
    c.polygon(bezier((84, 48), (90, 50), (94, 58), (92, 66))
              + bezier((92, 66), (90, 72), (82, 74), (78, 70)) + [(74, 56)], EAR)

    c.circle(54, 54, 30, BODY)                                    # body

    c.polygon(bezier((38, 44), (38, 34), (46, 28), (54, 28))      # face
              + bezier((54, 28), (62, 28), (70, 34), (70, 44))
              + [(70, 58)]
              + bezier((70, 58), (70, 68), (62, 72), (54, 72))
              + bezier((54, 72), (46, 72), (38, 68), (38, 58)), FACE)

    for eye_x in (46, 62):                                        # eyes
        c.circle(eye_x, 48, 8, EYE_WHITE)
        c.circle(eye_x, 48, 3.4, PUPIL)

    c.polygon([(54, 56), (60, 64), (48, 64)], BEAK)               # beak

    for px, py in ((31, 30), (36, 25), (36, 35), (41, 30)):       # the flower over her ear
        c.circle(px, py, 5, PETAL)
    c.circle(36, 30, 4, POLLEN)

    out = "project/store/icon-512.png"
    c.finish(out)
    print("نوشته شد:", out)


if __name__ == "__main__":
    main()
