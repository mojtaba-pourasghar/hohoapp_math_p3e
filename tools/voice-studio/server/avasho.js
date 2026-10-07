// The آواشو (Sahab / پارت) text-to-speech endpoint.
//
// Two endpoints, one for short lines and one for long ones; the token travels in a
// «gateway-token» header. What comes back is not fixed: it may be the audio itself, or JSON
// holding a link to it, or JSON holding base64. All three are handled, and the first JSON reply
// of a run is reported verbatim so the shape is visible in the log rather than guessed at.

export const ENDPOINTS = {
  short: "https://partai.gw.isahab.ir/avasho/v2/avasho/request",
  long: "https://partai.gw.isahab.ir/avasho/v2/avasho-large/request",
};

export const SPEAKERS = ["sara", "pune", "bahar", "shahrzad", "sheyda", "shirin"];

const AUDIO_EXT = { "audio/mpeg": ".mp3", "audio/mp3": ".mp3", "audio/wav": ".wav",
                    "audio/x-wav": ".wav", "audio/ogg": ".ogg", "audio/wave": ".wav" };

/** Sniff the container from the first bytes — more reliable than a content-type header. */
function extFromBytes(buf) {
  if (buf.length < 4) return null;
  const head = buf.subarray(0, 4).toString("latin1");
  if (head === "OggS") return ".ogg";
  if (head === "RIFF") return ".wav";
  if (head.startsWith("ID3")) return ".mp3";
  if (buf[0] === 0xff && (buf[1] & 0xe0) === 0xe0) return ".mp3";
  return null;
}

/** Walk any JSON and return the first string that looks like a link to an audio file. */
function findAudioUrl(value) {
  if (typeof value === "string") {
    return /^https?:\/\//.test(value) && /\.(mp3|wav|ogg)(\?|$)/i.test(value) ? value : null;
  }
  if (Array.isArray(value)) {
    for (const item of value) {
      const hit = findAudioUrl(item);
      if (hit) return hit;
    }
    return null;
  }
  if (value && typeof value === "object") {
    // filePath / fileUrl / url first, then anything else
    for (const key of ["filePath", "fileUrl", "url", "data"]) {
      if (key in value) {
        const hit = findAudioUrl(value[key]);
        if (hit) return hit;
      }
    }
    for (const item of Object.values(value)) {
      const hit = findAudioUrl(item);
      if (hit) return hit;
    }
  }
  return null;
}

/** Any long base64 blob in the reply is probably the audio. */
function findBase64(value, depth = 0) {
  if (depth > 6) return null;
  if (typeof value === "string") {
    return value.length > 2000 && /^[A-Za-z0-9+/=\s]+$/.test(value) ? value : null;
  }
  if (value && typeof value === "object") {
    for (const item of Object.values(value)) {
      const hit = findBase64(item, depth + 1);
      if (hit) return hit;
    }
  }
  return null;
}

export class RateLimited extends Error {}

/**
 * Records one line. Resolves to { buffer, ext, raw } — `raw` is the JSON reply when there was
 * one, so the caller can show it once. Throws RateLimited on 429 so the queue can back off.
 */
export async function speak({ token, text, speaker, speed = 1, endpoint = "short",
                              timeout = 180000 }) {
  const url = ENDPOINTS[endpoint] || ENDPOINTS.short;
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeout);

  let response;
  try {
    response = await fetch(url, {
      method: "POST",
      signal: controller.signal,
      headers: {
        "gateway-token": token,
        accept: "application/json",
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ text, speaker, speed, timestamp: false }),
    });
  } finally {
    clearTimeout(timer);
  }

  const bytes = Buffer.from(await response.arrayBuffer());

  if (response.status === 429) {
    throw new RateLimited(`سهمیه پر شد (۴۲۹): ${bytes.toString("utf8").slice(0, 200)}`);
  }
  if (!response.ok) {
    throw new Error(`HTTP ${response.status} — ${bytes.toString("utf8").slice(0, 300)}`);
  }

  const sniffed = extFromBytes(bytes);
  const byHeader = AUDIO_EXT[(response.headers.get("content-type") || "").split(";")[0].trim()];
  if (sniffed || byHeader) return { buffer: bytes, ext: sniffed || byHeader, raw: null };

  let payload;
  try {
    payload = JSON.parse(bytes.toString("utf8"));
  } catch {
    throw new Error(`پاسخِ ناشناخته: ${bytes.toString("utf8").slice(0, 300)}`);
  }

  const link = findAudioUrl(payload);
  if (link) {
    const file = await fetch(link);
    if (!file.ok) throw new Error(`دانلودِ فایل نشد: HTTP ${file.status} — ${link}`);
    const data = Buffer.from(await file.arrayBuffer());
    const ext = extFromBytes(data) || (link.match(/\.(mp3|wav|ogg)/i)?.[0] ?? ".mp3");
    return { buffer: data, ext, raw: payload };
  }

  const b64 = findBase64(payload);
  if (b64) {
    const data = Buffer.from(b64.replace(/\s/g, ""), "base64");
    return { buffer: data, ext: extFromBytes(data) || ".mp3", raw: payload };
  }

  throw new Error(`در پاسخ فایلی پیدا نشد: ${JSON.stringify(payload).slice(0, 300)}`);
}
