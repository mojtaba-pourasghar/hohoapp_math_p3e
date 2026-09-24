#!/usr/bin/env python3
"""
Turns every narration line in res/raw/audio_manifest.txt into an .ogg file with a Persian
female voice, ready for the app to play.

The app already speaks every line through the device's text-to-speech engine, so it works
without these files — running this just replaces that with a consistent recorded voice.

Usage
-----
    pip install edge-tts
    python3 tools/make_voice.py                 # all lines
    python3 tools/make_voice.py ch1_s0          # just one section
    python3 tools/make_voice.py --voice fa-IR-DilaraNeural --rate -10%
    python3 tools/make_voice.py --jobs 8         # more at once; the default is 6

Lines that already have a file are skipped, so a second run only fills in what is missing.

Voices (Microsoft Edge TTS, free, no key):
    fa-IR-DilaraNeural   female  <- default
    fa-IR-FaridNeural    male

Output lands in app/src/main/res/raw/<key>.ogg. Android picks the file up automatically;
nothing in the code needs changing, because lines are looked up by name.

Requires ffmpeg on PATH (edge-tts writes mp3; res/raw is happier with ogg).
"""

import argparse
import asyncio
import os
import shutil
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.join(ROOT, "app", "src", "main", "res", "raw")
MANIFEST = os.path.join(RAW, "audio_manifest.txt")


def read_manifest(prefix=None):
    """(key, spoken text) for every narration line.

    A manifest line is «key | text | تلفظ». The third column is the one that gets recorded: it
    is the same sentence with the symbols and numbers written as words and the maths words
    respelled, so the voice says them correctly. Older two-column lines still work — then the
    written text is recorded as-is.
    """
    lines = []
    with open(MANIFEST, encoding="utf-8") as fh:
        for raw in fh:
            raw = raw.strip()
            if not raw or raw.startswith("#") or "|" not in raw:
                continue
            parts = [part.strip() for part in raw.split("|")]
            key, written = parts[0], parts[1]
            say = parts[2] if len(parts) > 2 and parts[2] else written
            if prefix and not key.startswith(prefix):
                continue
            lines.append((key, say))
    return lines


async def synthesize(edge_tts, key, text, voice, rate, pitch, to_ogg):
    mp3_path = os.path.join(RAW, key + ".mp3")

    communicate = edge_tts.Communicate(text, voice, rate=rate, pitch=pitch)
    await communicate.save(mp3_path)

    if not to_ogg:
        return mp3_path  # Android plays mp3 from res/raw just as happily

    ogg_path = os.path.join(RAW, key + ".ogg")
    await asyncio.to_thread(
        subprocess.run,
        # 24 kbps mono at 24 kHz: one voice reading, not music. There are four thousand of
        # these — ten hours of speech — and they all go inside the APK, so every kilobit
        # counts: at 32 kbps the APK came to 155 MB. Vorbis and not Opus, because Ogg/Opus
        # only plays from Android 10 and minSdk here is 26.
        ["ffmpeg", "-y", "-loglevel", "error", "-i", mp3_path,
         "-c:a", "libvorbis", "-b:a", "24k", "-ar", "24000", "-ac", "1", ogg_path],
        check=True,
    )
    os.remove(mp3_path)
    return ogg_path


async def main_async(args):
    try:
        import edge_tts
    except ImportError:
        sys.exit("edge-tts is not installed.  pip install edge-tts")

    to_ogg = args.format == "ogg" or (args.format == "auto" and shutil.which("ffmpeg"))
    if args.format == "ogg" and not shutil.which("ffmpeg"):
        sys.exit("ffmpeg is not on PATH — needed for ogg. Use --format mp3 instead.")

    lines = read_manifest(args.prefix)
    if not lines:
        sys.exit("No lines matched. Check the prefix, e.g. ch1_s0")

    ext = ".ogg" if to_ogg else ".mp3"
    print("Generating %d line(s) with %s as %s (examples at %s)\n"
          % (len(lines), args.voice, ext, args.example_rate))

    # Work out what is actually missing before recording anything, so the progress count means
    # something: with four thousand lines, «3998/3999 skipped» scrolling past is no use.
    pending = []
    for key, text in lines:
        # a zero-byte file is a leftover from a failed run, not a recording — treat it as missing,
        # otherwise that line stays silent for ever because the generator keeps skipping it
        already = False
        for e in (".ogg", ".mp3", ".wav"):
            path = os.path.join(RAW, key + e)
            if not os.path.exists(path):
                continue
            if os.path.getsize(path) < 512:
                os.remove(path)
                continue
            already = True
        if already and not args.force:
            continue
        pending.append((key, text))

    print("  %d already recorded, %d to record, %d at a time\n"
          % (len(lines) - len(pending), len(pending), args.jobs))
    if not pending:
        print("Nothing to do.")
        return

    # Several lines at once: one line is mostly waiting on the network, and four thousand of
    # them one after another takes hours. The semaphore keeps the service from being hammered.
    gate = asyncio.Semaphore(max(1, args.jobs))
    done = [0]
    failed = []

    async def record(key, text):
        async with gate:
            try:
                # an example is the second try at an idea, so هوهو takes her time over it
                rate = args.example_rate if key.endswith("x") else args.rate
                path = await synthesize(edge_tts, key, text, args.voice, rate, args.pitch, to_ogg)
                done[0] += 1
                print("  %4d/%d  %-14s %6.1f KB  %s" % (
                    done[0], len(pending), key, os.path.getsize(path) / 1024, text[:42] + "…"),
                    flush=True)
            except Exception as exc:  # keep going; one bad line shouldn't stop the batch
                # never leave a half-written file behind — it would be mistaken for a recording
                for e in (".ogg", ".mp3"):
                    stale = os.path.join(RAW, key + e)
                    if os.path.exists(stale) and os.path.getsize(stale) < 512:
                        os.remove(stale)
                done[0] += 1
                failed.append(key)
                print("  %4d/%d  %-14s FAILED: %s" % (done[0], len(pending), key, exc), flush=True)

    await asyncio.gather(*(record(key, text) for key, text in pending))

    if failed:
        print("\n%d line(s) did not record: %s" % (len(failed), " ".join(failed[:20])))
        print("Run the same command again — it only retries what is missing.")

    print("\nDone. Rebuild the app and the lessons will use these recordings.")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("prefix", nargs="?", help="only keys starting with this, e.g. ch1_s2")
    parser.add_argument("--voice", default="fa-IR-DilaraNeural")
    parser.add_argument("--rate", default="-8%", help="speaking rate, slower suits young children")
    parser.add_argument("--example-rate", default="-25%",
                        help="rate for the «یک مثال دیگر» lines (keys ending in x), slower still")
    parser.add_argument("--pitch", default="+0Hz")
    parser.add_argument("--format", default="auto", choices=["auto", "ogg", "mp3"],
                        help="auto uses ogg when ffmpeg is available, otherwise mp3")
    parser.add_argument("--force", action="store_true", help="regenerate files that already exist")
    parser.add_argument("--jobs", type=int, default=6,
                        help="how many lines to record at once (default 6)")
    asyncio.run(main_async(parser.parse_args()))


if __name__ == "__main__":
    main()
