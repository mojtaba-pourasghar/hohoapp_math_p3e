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

/**
 * Where the finished file is collected from. The service answers a request with an id and
 * «wait about two seconds», not with audio, so the id has to be taken back to a tracking
 * address. `{id}` is replaced. The first of these that answers is remembered for the rest of
 * the run, and the studio says in the log which one it was, so it can be pinned in settings.
 */
export const TRACKING_URLS = [
  "https://partai.gw.isahab.ir/avasho/v2/avasho/tracking/{id}",
  "https://partai.gw.isahab.ir/avasho/v2/avasho/tracking/file-url/{id}",
  "https://partai.gw.isahab.ir/avasho/v2/avasho/result/{id}",
  "https://partai.gw.isahab.ir/avasho/v2/tracking/{id}",
];

let learnedTracking = null;       // the shape that worked, kept for the rest of the process

export function trackingInUse() {
  return learnedTracking;
}

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

/** The id and the service's own estimate, out of a «pending» reply. */
function pendingJob(payload) {
  const inner = payload?.data?.data ?? payload?.data ?? payload;
  const id = inner?.id ?? inner?.requestId ?? inner?.trackId;
  if (!id) return null;
  const seconds = Number(inner?.estimationTime);
  return { id: String(id), waitMs: Math.max(800, (Number.isFinite(seconds) ? seconds : 2) * 1000) };
}

function statusOf(payload) {
  return String(payload?.data?.status ?? payload?.status ?? "").toLowerCase();
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

/**
 * Goes back for the finished file.
 *
 * Waits the service's own estimate, then asks until the audio is there. `onNote` gets a line
 * whenever something worth saying happens, so a long wait does not look like a hang.
 */
async function collect({ token, id, waitMs, tries = 40, onNote }) {
  const templates = learnedTracking ? [learnedTracking] : TRACKING_URLS;
  await sleep(waitMs);

  for (let attempt = 1; attempt <= tries; attempt++) {
    for (const template of templates) {
      const url = template.replace("{id}", encodeURIComponent(id));
      let response;
      try {
        response = await fetch(url, {
          method: "GET",
          headers: { "gateway-token": token, accept: "application/json" },
        });
      } catch (err) {
        continue;                                   // this shape is not reachable; try the next
      }
      if (response.status === 404 || response.status === 405) continue;
      if (response.status === 429) throw new RateLimited("سهمیه پر شد (۴۲۹) هنگامِ گرفتنِ نتیجه");

      const bytes = Buffer.from(await response.arrayBuffer());
      if (!response.ok) {
        if (attempt === 1 && onNote) {
          onNote(`tracking ${response.status}: ${bytes.toString("utf8").slice(0, 160)}`);
        }
        continue;
      }

      if (!learnedTracking) {
        learnedTracking = template;
        if (onNote) onNote(`آدرسِ گرفتنِ نتیجه: ${template}`);
      }

      const direct = fromBytes(bytes, response.headers.get("content-type"));
      if (direct) return direct;

      let payload;
      try {
        payload = JSON.parse(bytes.toString("utf8"));
      } catch {
        continue;
      }

      const state = statusOf(payload);
      if (state && state !== "pending" && state !== "processing" && state !== "inprogress") {
        const got = await fromPayload(payload);
        if (got) return got;
        if (state !== "success" && state !== "done" && state !== "completed") {
          throw new Error(`سرویس خطا داد (${state}): ${JSON.stringify(payload).slice(0, 240)}`);
        }
      }
      const got = await fromPayload(payload);         // some replies carry it without a status
      if (got) return got;
    }
    if (onNote && attempt % 10 === 0) onNote(`هنوز آماده نیست — تلاشِ ${attempt}`);
    await sleep(Math.min(5000, 1000 + attempt * 250));
  }
  throw new Error("نتیجه در زمانِ معقول آماده نشد");
}

/** Audio straight out of a response body, if that is what it is. */
function fromBytes(bytes, contentType) {
  const sniffed = extFromBytes(bytes);
  const byHeader = AUDIO_EXT[(contentType || "").split(";")[0].trim()];
  if (!sniffed && !byHeader) return null;
  return { buffer: bytes, ext: sniffed || byHeader, raw: null };
}

/** Audio pointed at, or embedded, in a JSON reply. */
async function fromPayload(payload) {
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
  return null;
}

/**
 * Records one line. Resolves to { buffer, ext, raw } — `raw` is the JSON reply when there was
 * one, so the caller can show it once. Throws RateLimited on 429 so the queue can back off.
 *
 * The service is asynchronous: the first reply is a receipt with an id and an estimate, and the
 * audio is collected afterwards. All of that happens inside this call, so a caller only ever
 * waits for one thing. `onNote` is for the log while that wait goes on.
 */
export async function speak({ token, text, speaker, speed = 1, endpoint = "short",
                              timeout = 180000, onNote = null }) {
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

  const direct = fromBytes(bytes, response.headers.get("content-type"));
  if (direct) return direct;

  let payload;
  try {
    payload = JSON.parse(bytes.toString("utf8"));
  } catch {
    throw new Error(`پاسخِ ناشناخته: ${bytes.toString("utf8").slice(0, 300)}`);
  }

  // the usual case: the reply is a receipt, and the audio is collected with its id
  const job = pendingJob(payload);
  if (job) {
    const collected = await collect({ token, id: job.id, waitMs: job.waitMs, onNote });
    return { ...collected, raw: collected.raw ?? payload };
  }

  // some replies carry the file outright
  const got = await fromPayload(payload);
  if (got) return got;

  throw new Error(`در پاسخ فایلی پیدا نشد: ${JSON.stringify(payload).slice(0, 300)}`);
}
