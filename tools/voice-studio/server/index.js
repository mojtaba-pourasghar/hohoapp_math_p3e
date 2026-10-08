// استودیوی صداگذاری هوهو — the local half.
//
// A browser cannot write to a folder you name, and it cannot call the آواشو gateway without
// being stopped by CORS. So the React page talks to this small Express server, which keeps the
// token, reads the manifest, records one line at a time, and writes the files where you said.
// Nothing is uploaded or pushed anywhere: the only things it writes are audio files in your
// output folder and its own config beside this file.

import express from "express";
import fs from "node:fs";
import path from "node:path";
import os from "node:os";
import { spawn } from "node:child_process";
import { fileURLToPath } from "node:url";
import { readManifest, existingKeys, GROUP_LABELS } from "./manifest.js";
import { speak, SPEAKERS, RateLimited } from "./avasho.js";
import {
  AUDIO_BASE, KARBARG_BASE, writeAudioIndex, addWorksheet, listWorksheets,
  removeWorksheet, writeWorksheetIndex,
} from "./publish.js";
import { probe, uploadFiles, uploadTree, audioFilesIn } from "./ftp.js";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(HERE, "..");
const CONFIG_FILE = path.join(ROOT, "voice-studio.config.json");
const PORT = Number(process.env.PORT || 5174);

// ── config ───────────────────────────────────────────────────────────────────
const DEFAULTS = {
  token: "",
  manifestPath: path.resolve(ROOT, "../../app/src/main/res/raw/audio_manifest.txt"),
  // inside the repo, so the finished clips can be committed and uploaded from one place
  outDir: path.resolve(ROOT, "../voice-out"),
  karbargDir: path.resolve(ROOT, "../karbarg-out"),
  audioBase: AUDIO_BASE,
  karbargBase: KARBARG_BASE,
  speaker: "shahrzad",
  speed: 1,
  endpoint: "short",
  useVowels: true,          // send the third column, the one with the vowels
  concurrency: 2,
  delayMs: 400,
  retries: 3,
  convertOgg: false,        // needs ffmpeg on PATH
  oggBitrate: "24k",
  schedule: { enabled: false, everySeconds: 300, batchSize: 25 },

  // where the finished files are sent. The paths are the real ones on the server, under
  // /public_html — the same folders the app's URLs point at.
  ftp: {
    host: "ftp.mp-apdl.ir",
    port: 21,
    user: "",
    password: "",
    secure: false,
    audioDir: "/public_html/grade-3/math/audio",
    karbargDir: "/public_html/grade-3/math/karbarg",
  },
};

let config = { ...DEFAULTS };
try {
  if (fs.existsSync(CONFIG_FILE)) {
    const saved = JSON.parse(fs.readFileSync(CONFIG_FILE, "utf8"));
    config = {
      ...DEFAULTS, ...saved,
      schedule: { ...DEFAULTS.schedule, ...(saved.schedule || {}) },
      ftp: { ...DEFAULTS.ftp, ...(saved.ftp || {}) },
    };
  }
} catch (err) {
  console.error("config خوانده نشد:", err.message);
}

function saveConfig() {
  fs.writeFileSync(CONFIG_FILE, JSON.stringify(config, null, 2), "utf8");
}

// ── the live log, shared with the page over SSE ───────────────────────────────
const listeners = new Set();
const log = [];

function say(level, message) {
  const entry = { at: new Date().toISOString(), level, message };
  log.push(entry);
  if (log.length > 600) log.splice(0, log.length - 600);
  const frame = `data: ${JSON.stringify({ type: "log", entry })}\n\n`;
  for (const res of listeners) res.write(frame);
  console.log(`[${entry.at.slice(11, 19)}] ${level}: ${message}`);
}

function push(type, payload) {
  const frame = `data: ${JSON.stringify({ type, ...payload })}\n\n`;
  for (const res of listeners) res.write(frame);
}

// ── the job ──────────────────────────────────────────────────────────────────
const job = {
  running: false,
  stopping: false,
  total: 0,
  done: 0,
  failed: 0,
  current: [],
  startedAt: null,
  lastError: null,
  shapeShown: false,
};

function snapshot() {
  return {
    running: job.running,
    stopping: job.stopping,
    total: job.total,
    done: job.done,
    failed: job.failed,
    current: job.current,
    startedAt: job.startedAt,
    lastError: job.lastError,
  };
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

function toOgg(src, dst, bitrate) {
  return new Promise((resolve) => {
    const ff = spawn("ffmpeg", ["-y", "-loglevel", "error", "-i", src,
      "-c:a", "libvorbis", "-b:a", bitrate, "-ar", "24000", "-ac", "1", dst]);
    ff.on("error", () => resolve(false));
    ff.on("close", (code) => resolve(code === 0));
  });
}

/** Record one line and write it into the output folder. */
async function recordOne(line) {
  const text = config.useVowels ? line.spoken : line.written;
  let attempt = 0;

  for (;;) {
    attempt += 1;
    try {
      const { buffer, ext, raw } = await speak({
        token: config.token, text, speaker: config.speaker,
        speed: Number(config.speed) || 1, endpoint: config.endpoint,
      });

      if (raw && !job.shapeShown) {
        job.shapeShown = true;
        say("info", `شکلِ پاسخِ سرویس: ${JSON.stringify(raw).slice(0, 400)}`);
      }

      fs.mkdirSync(config.outDir, { recursive: true });
      const target = path.join(config.outDir, line.key + ext);
      fs.writeFileSync(target, buffer);

      let saved = path.basename(target);
      let size = buffer.length;
      if (config.convertOgg && ext !== ".ogg") {
        const ogg = path.join(config.outDir, line.key + ".ogg");
        if (await toOgg(target, ogg, config.oggBitrate)) {
          fs.unlinkSync(target);
          saved = path.basename(ogg);
          size = fs.statSync(ogg).size;
        } else {
          say("warn", "ffmpeg اجرا نشد؛ فایل با فرمتِ خودِ سرویس ماند");
        }
      }
      return { ok: true, saved, size };
    } catch (err) {
      const limited = err instanceof RateLimited;
      if (attempt > Number(config.retries)) {
        return { ok: false, error: err.message };
      }
      const wait = limited ? 20000 * attempt : 1500 * attempt;
      say("warn", `${line.key}: ${err.message} — ${Math.round(wait / 1000)} ثانیه صبر و تلاشِ ${attempt + 1}`);
      await sleep(wait);
    }
  }
}

async function runJob(keys) {
  if (job.running) return { error: "یک کار در حال اجراست" };
  if (!config.token) return { error: "توکن خالی است" };
  if (!keys.length) return { error: "چیزی برای ساختن نیست" };

  const lines = readManifest(config.manifestPath);
  const byKey = new Map(lines.map((l) => [l.key, l]));
  const todo = keys.map((k) => byKey.get(k)).filter(Boolean);

  Object.assign(job, { running: true, stopping: false, total: todo.length, done: 0,
                       failed: 0, current: [], startedAt: Date.now(), lastError: null });
  say("info", `شروع: ${todo.length} کلیپ با صدای ${config.speaker} → ${config.outDir}`);
  push("status", snapshot());

  let cursor = 0;
  const workers = Array.from({ length: Math.max(1, Number(config.concurrency)) }, async () => {
    while (cursor < todo.length && !job.stopping) {
      const line = todo[cursor++];
      job.current = [...job.current.filter((k) => k !== line.key), line.key];
      push("status", snapshot());

      const result = await recordOne(line);
      job.current = job.current.filter((k) => k !== line.key);
      if (result.ok) {
        job.done += 1;
        say("ok", `${line.key} ← ${result.saved} (${Math.round(result.size / 1024)} کیلوبایت)`);
      } else {
        job.failed += 1;
        job.lastError = result.error;
        say("error", `${line.key}: ${result.error}`);
      }
      push("status", snapshot());
      if (Number(config.delayMs) > 0) await sleep(Number(config.delayMs));
    }
  });

  await Promise.all(workers);
  job.running = false;
  job.current = [];
  say("info", job.stopping
    ? `ایستاد — ${job.done} ساخته، ${job.failed} ناموفق`
    : `تمام — ${job.done} ساخته، ${job.failed} ناموفق`);
  job.stopping = false;
  push("status", snapshot());
  return { started: false };
}

// ── the scheduler: wake up now and then and take the next batch ───────────────
let timer = null;

function applySchedule() {
  if (timer) clearInterval(timer);
  timer = null;
  if (!config.schedule.enabled) {
    say("info", "زمان‌بندی خاموش شد");
    return;
  }
  const every = Math.max(30, Number(config.schedule.everySeconds) || 300);
  say("info", `زمان‌بندی روشن: هر ${every} ثانیه، ${config.schedule.batchSize} کلیپ`);
  timer = setInterval(() => {
    if (job.running) return;
    const missing = missingKeys();
    if (!missing.length) {
      say("info", "زمان‌بندی: چیزی باقی نمانده");
      return;
    }
    const batch = missing.slice(0, Math.max(1, Number(config.schedule.batchSize) || 25));
    say("info", `زمان‌بندی: دسته‌ی تازه، ${batch.length} کلیپ`);
    runJob(batch);
  }, every * 1000);
}

function missingKeys() {
  const lines = readManifest(config.manifestPath);
  const have = existingKeys(config.outDir);
  return lines.filter((l) => !have.has(l.key)).map((l) => l.key);
}

// ── HTTP ─────────────────────────────────────────────────────────────────────
const app = express();
app.use(express.json({ limit: "64mb" }));   // a worksheet PDF arrives as base64

const publicConfig = () => ({
  ...config,
  ftp: { ...config.ftp, password: undefined, passwordSet: Boolean(config.ftp.password) },
  token: undefined,
  tokenSet: Boolean(config.token),
  tokenHint: config.token ? `${config.token.slice(0, 6)}…${config.token.slice(-4)}` : "",
  speakers: SPEAKERS,
  groupLabels: GROUP_LABELS,
  ffmpeg: true,
});

app.get("/api/config", (_req, res) => res.json(publicConfig()));

app.post("/api/config", (req, res) => {
  const body = req.body || {};
  for (const key of ["manifestPath", "outDir", "karbargDir", "speaker", "endpoint",
                     "audioBase", "karbargBase"]) {
    if (typeof body[key] === "string" && body[key].trim()) config[key] = body[key].trim();
  }
  for (const key of ["speed", "concurrency", "delayMs", "retries"]) {
    if (body[key] !== undefined && body[key] !== "") config[key] = Number(body[key]);
  }
  for (const key of ["useVowels", "convertOgg"]) {
    if (typeof body[key] === "boolean") config[key] = body[key];
  }
  if (typeof body.oggBitrate === "string" && body.oggBitrate.trim()) {
    config.oggBitrate = body.oggBitrate.trim();
  }
  if (typeof body.token === "string" && body.token.trim()) {
    config.token = body.token.trim();
    say("info", "توکن ذخیره شد");
  }
  if (body.token === "") {
    config.token = "";
    say("info", "توکن پاک شد");
  }
  if (body.schedule) {
    config.schedule = { ...config.schedule, ...body.schedule };
    applySchedule();
  }
  if (body.ftp) {
    const next = { ...config.ftp, ...body.ftp };
    if (body.ftp.password === undefined) next.password = config.ftp.password;  // keep the old one
    if (body.ftp.password === "") next.password = "";                          // unless cleared
    config.ftp = next;
    say("info", "تنظیماتِ FTP ذخیره شد" + (next.password ? "" : " (رمز خالی است)"));
  }
  saveConfig();
  res.json(publicConfig());
});

app.get("/api/manifest", (_req, res) => {
  try {
    const lines = readManifest(config.manifestPath);
    const have = existingKeys(config.outDir);
    res.json({
      outDir: config.outDir,
      lines: lines.map((l) => {
        const file = have.get(l.key);
        return { ...l, done: Boolean(file), file: file?.name ?? null, size: file?.size ?? 0 };
      }),
    });
  } catch (err) {
    res.status(400).json({ error: `منیفست خوانده نشد: ${err.message}` });
  }
});

app.get("/api/status", (_req, res) => res.json({ ...snapshot(), log: log.slice(-120) }));

// ── the two index.json files the host serves ─────────────────────────────────
app.post("/api/publish/audio", (_req, res) => {
  try {
    const index = writeAudioIndex({
      outDir: config.outDir,
      baseUrl: config.audioBase,
      speaker: config.speaker,
      appAssets: path.resolve(ROOT, "../../app/src/main/assets/voice-index.json"),
    });
    say("ok", `فهرستِ صدا ساخته شد: ${index.count} کلیپ، ${Math.round(index.bytes / 1048576)} مگابایت`);
    res.json(index);
  } catch (err) {
    say("error", `فهرستِ صدا ساخته نشد: ${err.message}`);
    res.status(400).json({ error: err.message });
  }
});


// ── sending the finished files to the host ───────────────────────────────────
const upload = { running: false, what: "", done: 0, total: 0, sent: 0, skipped: 0, failed: 0 };

function uploadSnapshot() {
  return { ...upload };
}

function requireFtp(res) {
  const { host, user, password } = config.ftp;
  if (!host || !user || !password) {
    res.status(400).json({ error: "آدرس و نام کاربری و رمزِ FTP را پر کن" });
    return false;
  }
  return true;
}

app.post("/api/ftp/test", async (req, res) => {
  if (!requireFtp(res)) return;
  const which = req.body?.which === "karbarg" ? "karbargDir" : "audioDir";
  try {
    const info = await probe(config.ftp, config.ftp[which]);
    say("ok", `FTP وصل شد. پوشه‌ی ورود: ${info.loginDir} — ${info.remoteDir}: `
      + (info.remoteExists ? `${info.remoteCount} فایل` : "هنوز ساخته نشده (موقع آپلود ساخته می‌شود)"));
    res.json(info);
  } catch (err) {
    say("error", `FTP وصل نشد: ${err.message}`);
    res.status(502).json({ error: err.message });
  }
});

app.post("/api/ftp/upload/audio", async (req, res) => {
  if (!requireFtp(res)) return;
  if (upload.running) return res.status(409).json({ error: "یک آپلود در حال اجراست" });

  const skipExisting = req.body?.all !== true;      // «همه را دوباره بفرست» خاموشش می‌کند
  const files = audioFilesIn(config.outDir);
  if (!files.length) return res.status(400).json({ error: "در پوشه‌ی خروجی کلیپی نیست" });

  // the index must describe what is actually there, so it is rebuilt and sent last
  const index = writeAudioIndex({
    outDir: config.outDir, baseUrl: config.audioBase, speaker: config.speaker,
    appAssets: path.resolve(ROOT, "../../app/src/main/assets/voice-index.json"),
  });

  Object.assign(upload, { running: true, what: "صداها", done: 0, total: files.length + 1,
                          sent: 0, skipped: 0, failed: 0 });
  push("upload", uploadSnapshot());
  res.json({ started: true, total: files.length });
  say("info", `آپلودِ صداها به ${config.ftp.audioDir} — ${files.length} فایل`
    + (skipExisting ? " (آنچه روی سرور هست رد می‌شود)" : " (همه دوباره)"));

  try {
    const outcome = await uploadFiles({
      settings: config.ftp, remoteDir: config.ftp.audioDir, files, skipExisting,
      onStep: (name, state) => {
        upload.done += 1;
        if (state === "sent") upload.sent += 1;
        else if (state === "skipped") upload.skipped += 1;
        else upload.failed += 1;
        if (state !== "skipped" || upload.done % 50 === 0) {
          say(state === "failed" ? "error" : "ok",
              `${upload.done}/${upload.total} ${name} — ${state === "sent" ? "فرستاده شد"
                : state === "skipped" ? "از قبل بود" : "نرفت"}`);
        }
        push("upload", uploadSnapshot());
      },
    });

    await uploadFiles({
      settings: config.ftp, remoteDir: config.ftp.audioDir,
      files: [path.join(config.outDir, "index.json")], skipExisting: false,
      onStep: () => { upload.done += 1; upload.sent += 1; push("upload", uploadSnapshot()); },
    });

    say("info", `آپلود تمام شد — ${outcome.sent} فرستاده، ${outcome.skipped} از قبل بود، `
      + `${outcome.failed} ناموفق. فهرست (${index.count} کلیپ) هم رفت.`);
    for (const line of outcome.errors.slice(0, 10)) say("error", line);
  } catch (err) {
    say("error", `آپلود شکست: ${err.message}`);
  } finally {
    upload.running = false;
    push("upload", uploadSnapshot());
  }
});

app.post("/api/ftp/upload/karbarg", async (req, res) => {
  if (!requireFtp(res)) return;
  if (upload.running) return res.status(409).json({ error: "یک آپلود در حال اجراست" });

  try {
    writeWorksheetIndex({ dir: config.karbargDir, baseUrl: config.karbargBase });
  } catch (err) {
    return res.status(400).json({ error: err.message });
  }

  Object.assign(upload, { running: true, what: "کاربرگ‌ها", done: 0, total: 0,
                          sent: 0, skipped: 0, failed: 0 });
  push("upload", uploadSnapshot());
  res.json({ started: true });
  say("info", `آپلودِ کاربرگ‌ها به ${config.ftp.karbargDir} — پوشه‌ی سرور با اینجا یکی می‌شود`);

  try {
    await uploadTree({
      settings: config.ftp, remoteDir: config.ftp.karbargDir, localDir: config.karbargDir,
      onStep: (name) => {
        upload.sent += 1;
        upload.done += 1;
        push("upload", uploadSnapshot());
      },
    });
    say("ok", "کاربرگ‌ها آپلود شدند");
  } catch (err) {
    say("error", `آپلودِ کاربرگ شکست: ${err.message}`);
  } finally {
    upload.running = false;
    push("upload", uploadSnapshot());
  }
});

app.get("/api/ftp/status", (_req, res) => res.json(uploadSnapshot()));

app.get("/api/karbarg", (_req, res) => {
  try {
    res.json({ dir: config.karbargDir, baseUrl: config.karbargBase,
               sheets: listWorksheets(config.karbargDir) });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

app.post("/api/karbarg", (req, res) => {
  try {
    const { title, note, chapter, fileName, dataBase64 } = req.body || {};
    const data = Buffer.from(String(dataBase64 || "").split(",").pop(), "base64");
    const sheet = addWorksheet({ dir: config.karbargDir, title, note, chapter, fileName, data });
    writeWorksheetIndex({ dir: config.karbargDir, baseUrl: config.karbargBase });
    say("ok", `کاربرگ اضافه شد: ${sheet.title} → ${sheet.slug}/${sheet.file}`);
    res.json(sheet);
  } catch (err) {
    say("error", `کاربرگ اضافه نشد: ${err.message}`);
    res.status(400).json({ error: err.message });
  }
});

app.delete("/api/karbarg/:slug", (req, res) => {
  const gone = removeWorksheet(config.karbargDir, req.params.slug);
  if (gone) {
    writeWorksheetIndex({ dir: config.karbargDir, baseUrl: config.karbargBase });
    say("warn", `کاربرگ پاک شد: ${req.params.slug}`);
  }
  res.json({ ok: gone });
});

app.post("/api/publish/karbarg", (_req, res) => {
  try {
    const index = writeWorksheetIndex({ dir: config.karbargDir, baseUrl: config.karbargBase });
    say("ok", `فهرستِ کاربرگ ساخته شد: ${index.count} کاربرگ`);
    res.json(index);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

app.post("/api/generate", async (req, res) => {
  const keys = Array.isArray(req.body?.keys) ? req.body.keys : [];
  const picked = keys.length ? keys : missingKeys();
  const limit = Number(req.body?.limit);
  const batch = Number.isFinite(limit) && limit > 0 ? picked.slice(0, limit) : picked;
  const outcome = await Promise.resolve(runJob(batch));
  if (outcome?.error) return res.status(409).json(outcome);
  res.json({ ok: true });
});

app.post("/api/stop", (_req, res) => {
  if (job.running) {
    job.stopping = true;
    say("warn", "درخواستِ ایستادن — کلیپ‌های در جریان تمام می‌شوند");
  }
  res.json(snapshot());
});

app.post("/api/test", async (req, res) => {
  if (!config.token) return res.status(400).json({ error: "توکن خالی است" });
  const text = String(req.body?.text || "سلام! من هوهو هستم، مُعَلِّمِ ریاضیِ تو.");
  try {
    const { buffer, ext, raw } = await speak({
      token: config.token, text, speaker: config.speaker,
      speed: Number(config.speed) || 1, endpoint: config.endpoint,
    });
    fs.mkdirSync(config.outDir, { recursive: true });
    const name = `_test-${config.speaker}${ext}`;
    fs.writeFileSync(path.join(config.outDir, name), buffer);
    say("ok", `تستِ صدا ساخته شد: ${name} (${Math.round(buffer.length / 1024)} کیلوبایت)`);
    res.json({ ok: true, file: name, bytes: buffer.length, shape: raw ? JSON.stringify(raw).slice(0, 400) : null });
  } catch (err) {
    say("error", `تستِ صدا نشد: ${err.message}`);
    res.status(502).json({ error: err.message });
  }
});

// play a produced file back in the browser
app.get("/api/audio/:key", (req, res) => {
  const have = existingKeys(config.outDir);
  const file = have.get(req.params.key);
  if (!file) return res.status(404).end();
  res.sendFile(path.join(config.outDir, file.name));
});

app.delete("/api/audio/:key", (req, res) => {
  const have = existingKeys(config.outDir);
  const file = have.get(req.params.key);
  if (!file) return res.status(404).end();
  fs.unlinkSync(path.join(config.outDir, file.name));
  say("warn", `پاک شد: ${file.name}`);
  res.json({ ok: true });
});

app.get("/api/events", (req, res) => {
  res.writeHead(200, {
    "Content-Type": "text/event-stream",
    "Cache-Control": "no-cache",
    Connection: "keep-alive",
  });
  res.write(`data: ${JSON.stringify({ type: "status", ...snapshot() })}\n\n`);
  listeners.add(res);
  req.on("close", () => listeners.delete(res));
});

const dist = path.join(ROOT, "dist");
if (fs.existsSync(dist)) {
  app.use(express.static(dist));
  app.get("*", (_req, res) => res.sendFile(path.join(dist, "index.html")));
}

app.listen(PORT, "127.0.0.1", () => {
  console.log(`\n  استودیوی صداگذاری هوهو → http://127.0.0.1:${PORT}\n`);
  console.log(`  منیفست : ${config.manifestPath}`);
  console.log(`  خروجی  : ${config.outDir}\n`);
  if (config.schedule.enabled) applySchedule();
});
