#!/usr/bin/env python3
"""
Records one narration line with every آواشو voice, so they can be heard side by side.

The point is a listening test, not a pipeline: it takes one key from
res/raw/audio_manifest.txt, sends that line to Sahab's آواشو service once per speaker, and
writes the results into project/avasho-sample/ next to the clip edge-tts already made for the
same line. Then a human can play them in a row and say which voice teaches best.

The token is read from the IVIRA_TOKEN environment variable and is never printed.

    IVIRA_TOKEN=… python3 tools/avasho_sample.py                    # every speaker, key p008_01
    IVIRA_TOKEN=… python3 tools/avasho_sample.py --key ch1_s0_01
    IVIRA_TOKEN=… python3 tools/avasho_sample.py --speakers shahrzad,bahar --plain
"""
import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.join(ROOT, "app", "src", "main", "res", "raw")
MANIFEST = os.path.join(RAW, "audio_manifest.txt")
OUT = os.path.join(ROOT, "project", "avasho-sample")

SHORT = "https://partai.gw.isahab.ir/avasho/v2/avasho/request"
LONG = "https://partai.gw.isahab.ir/avasho/v2/avasho-large/request"
SPEAKERS = ["sara", "pune", "bahar", "shahrzad", "sheyda", "shirin"]
URL_IN_JSON = re.compile(r"https?://[^\s\"']+")


def manifest_line(key):
    """(written text, spoken spelling) for one narration key."""
    for raw in open(MANIFEST, encoding="utf-8"):
        raw = raw.strip()
        if not raw.startswith(key + " |"):
            continue
        cols = [c.strip() for c in raw.split("|")]
        return cols[1], (cols[2] if len(cols) > 2 and cols[2] else cols[1])
    sys.exit("key %s is not in the manifest" % key)


def ask(url, token, text, speaker, speed, timestamp):
    """One request. Returns (status, content-type, body bytes)."""
    body = json.dumps({"text": text, "speaker": speaker,
                       "speed": speed, "timestamp": timestamp}).encode("utf-8")
    request = urllib.request.Request(url, data=body, method="POST", headers={
        "gateway-token": token,
        "accept": "application/json",
        "Content-Type": "application/json",
    })
    try:
        with urllib.request.urlopen(request, timeout=180) as response:
            return response.status, response.headers.get("Content-Type", ""), response.read()
    except urllib.error.HTTPError as err:          # the error body documents the API
        return err.code, err.headers.get("Content-Type", ""), err.read()
    except Exception as exc:                       # DNS, TLS, a blocked egress, a timeout
        return 0, "", str(exc).encode("utf-8")


def audio_from(status, ctype, body, report):
    """The service may hand back the audio itself, or JSON pointing at it. Handle both."""
    if status == 0:
        report.append("    به سرویس نرسید: %s" % body.decode("utf-8", "replace")[:300])
        return None, None
    if status != 200:
        report.append("    HTTP %d — %s" % (status, body[:400].decode("utf-8", "replace")))
        return None, None
    if "audio" in ctype or body[:4] in (b"OggS", b"RIFF", b"ID3\x03") or body[:2] == b"\xff\xfb":
        return body, ".wav" if body[:4] == b"RIFF" else ".mp3"

    try:
        payload = json.loads(body)
    except ValueError:
        report.append("    unexpected body (%s): %s" % (ctype, body[:300].decode("utf-8", "replace")))
        return None, None

    # keep the whole reply in the report: it is the only documentation we have of the schema
    report.append("    JSON: " + json.dumps(payload, ensure_ascii=False)[:600])
    found = URL_IN_JSON.search(json.dumps(payload))
    if not found:
        return None, None
    link = found.group(0).rstrip('",}')
    report.append("    audio URL: " + link)
    with urllib.request.urlopen(link, timeout=180) as response:
        data = response.read()
    ext = os.path.splitext(link.split("?")[0])[1] or ".mp3"
    return data, ext


def to_ogg(src, dst):
    """The same 24 kbps mono the app ships, so the comparison is what a child would hear."""
    if not shutil.which("ffmpeg"):
        return False
    return subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", src,
                           "-c:a", "libvorbis", "-b:a", "24k", "-ar", "24000", "-ac", "1", dst]
                          ).returncode == 0


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--key", default="p008_01", help="which narration line to record")
    parser.add_argument("--speakers", default=",".join(SPEAKERS))
    parser.add_argument("--speed", type=float, default=1)
    parser.add_argument("--timestamp", action="store_true", help="ask for word timings too")
    parser.add_argument("--plain", action="store_true",
                        help="send the written text instead of the vowelled spelling")
    parser.add_argument("--long", action="store_true", help="use the avasho-large endpoint")
    args = parser.parse_args()

    token = os.environ.get("IVIRA_TOKEN")
    if not token:
        sys.exit("IVIRA_TOKEN is not set")

    written, spoken = manifest_line(args.key)
    text = written if args.plain else spoken
    url = LONG if args.long else SHORT
    os.makedirs(OUT, exist_ok=True)

    report = ["آواشو — نمونه‌ی صدا", "=" * 60,
              "endpoint : " + url,
              "key      : " + args.key,
              "text sent: " + ("متنِ نوشتاری" if args.plain else "ستونِ تلفظِ اعراب‌گذاری‌شده"),
              "", text, ""]

    for speaker in [s.strip() for s in args.speakers.split(",") if s.strip()]:
        print("→", speaker, flush=True)
        report.append("── %s" % speaker)
        status, ctype, body = ask(url, token, text, speaker, args.speed, args.timestamp)
        data, ext = audio_from(status, ctype, body, report)
        if not data:
            print("   failed", flush=True)
            continue
        src = os.path.join(OUT, "avasho-%s%s" % (speaker, ext))
        with open(src, "wb") as fh:
            fh.write(data)
        size = len(data) / 1024
        line = "    ok — %s (%.0f KB)" % (os.path.basename(src), size)
        ogg = os.path.join(OUT, "avasho-%s.ogg" % speaker)
        if ext != ".ogg" and to_ogg(src, ogg):
            line += " → %s (%.0f KB)" % (os.path.basename(ogg), os.path.getsize(ogg) / 1024)
        report.append(line)
        print("  ", line.strip(), flush=True)

    # the voice the app ships today, for the same sentence, to compare against
    current = os.path.join(RAW, args.key + ".ogg")
    if os.path.exists(current):
        shutil.copy(current, os.path.join(OUT, "edge-dilara-%s.ogg" % args.key))
        report.append("\nصدای فعلیِ اپ برای همین جمله: edge-dilara-%s.ogg" % args.key)

    with open(os.path.join(OUT, "report.txt"), "w", encoding="utf-8") as fh:
        fh.write("\n".join(report) + "\n")
    print("\n" + "\n".join(report[-12:]))


if __name__ == "__main__":
    main()
