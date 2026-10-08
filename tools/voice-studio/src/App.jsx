import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";

const fa = (n) => Number(n || 0).toLocaleString("fa-IR");
const api = async (path, options) => {
  const res = await fetch("/api" + path, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  const text = await res.text();
  const body = text ? JSON.parse(text) : {};
  if (!res.ok) throw new Error(body.error || `HTTP ${res.status}`);
  return body;
};

const GROUPS = [
  { id: "pages", label: "کتاب، صفحه به صفحه" },
  { id: "sections", label: "خلاصه‌ی بخش‌ها" },
  { id: "numbers", label: "واژه‌های عددی" },
  { id: "phrases", label: "تکه‌های سؤال" },
];

export default function App() {
  const [config, setConfig] = useState(null);
  const [form, setForm] = useState(null);
  const [token, setToken] = useState("");
  const [lines, setLines] = useState([]);
  const [status, setStatus] = useState({ running: false, done: 0, failed: 0, total: 0, current: [] });
  const [log, setLog] = useState([]);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [filter, setFilter] = useState({ group: "all", chapter: "all", state: "all", q: "" });
  const [showLog, setShowLog] = useState(true);
  const [published, setPublished] = useState(null);
  const [sheets, setSheets] = useState([]);
  const [sheetForm, setSheetForm] = useState({ title: "", note: "", chapter: -1, file: null });
  const logBox = useRef(null);

  const load = useCallback(async () => {
    try {
      const cfg = await api("/config");
      setConfig(cfg);
      setForm((old) => old ?? cfg);
      const snap = await api("/status");
      setStatus(snap);
      setLog(snap.log || []);
      const manifest = await api("/manifest");
      setLines(manifest.lines);
      const karbarg = await api("/karbarg");
      setSheets(karbarg.sheets || []);
      setError("");
    } catch (err) {
      setError(err.message);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  // the server streams every log line and every step of the job
  useEffect(() => {
    const stream = new EventSource("/api/events");
    stream.onmessage = (event) => {
      const data = JSON.parse(event.data);
      if (data.type === "log") setLog((old) => [...old.slice(-400), data.entry]);
      if (data.type === "status") setStatus((old) => ({ ...old, ...data }));
    };
    return () => stream.close();
  }, []);

  // when a job finishes, the files on disk have changed — reread them
  const wasRunning = useRef(false);
  useEffect(() => {
    if (wasRunning.current && !status.running) load();
    wasRunning.current = status.running;
  }, [status.running, load]);

  useEffect(() => {
    if (showLog && logBox.current) logBox.current.scrollTop = logBox.current.scrollHeight;
  }, [log, showLog]);

  const counts = useMemo(() => {
    const done = lines.filter((l) => l.done).length;
    return { total: lines.length, done, left: lines.length - done };
  }, [lines]);

  const chapters = useMemo(
    () => [...new Set(lines.map((l) => l.chapter).filter(Boolean))].sort((a, b) => a - b),
    [lines]
  );

  const shown = useMemo(() => lines.filter((l) => {
    if (filter.group !== "all" && l.group !== filter.group) return false;
    if (filter.chapter !== "all" && String(l.chapter) !== filter.chapter) return false;
    if (filter.state === "done" && !l.done) return false;
    if (filter.state === "todo" && l.done) return false;
    if (filter.q && !(l.key.includes(filter.q) || l.written.includes(filter.q))) return false;
    return true;
  }), [lines, filter]);

  const save = async (patch) => {
    setBusy(true);
    try {
      const next = await api("/config", { method: "POST", body: JSON.stringify(patch) });
      setConfig(next);
      setForm((old) => ({ ...old, ...next }));
      setError("");
      if (patch.manifestPath || patch.outDir) load();
    } catch (err) { setError(err.message); }
    setBusy(false);
  };

  const generate = async (keys, limit) => {
    setBusy(true);
    try {
      await api("/generate", { method: "POST", body: JSON.stringify({ keys, limit }) });
      setError("");
    } catch (err) { setError(err.message); }
    setBusy(false);
  };

  const stop = () => api("/stop", { method: "POST" }).catch((e) => setError(e.message));

  const testVoice = async () => {
    setBusy(true);
    try {
      await api("/test", { method: "POST", body: JSON.stringify({}) });
      load();
    } catch (err) { setError(err.message); }
    setBusy(false);
  };

  const removeFile = async (key) => {
    try {
      await api(`/audio/${key}`, { method: "DELETE" });
      load();
    } catch (err) { setError(err.message); }
  };

  if (!config || !form) {
    return <div className="studio"><div className="card">در حال باز شدن…</div></div>;
  }

  const todoKeys = (subset) => subset.filter((l) => !l.done).map((l) => l.key);
  const progress = status.total ? Math.round((status.done / status.total) * 100) : 0;

  return (
    <div className="studio">
      <div className="top">
        <span className="owl">🦉</span>
        <div>
          <h1>استودیوی صداگذاری هوهو</h1>
          <p>
            متن از <code>audio_manifest.txt</code> · صدا با آواشو ({config.speaker}) ·
            فایل‌ها در <code>{config.outDir}</code>
          </p>
        </div>
        <div className="grow" />
        <button className="ghost" onClick={load} disabled={busy}>تازه‌سازی</button>
        {status.running
          ? <button className="warn" onClick={stop}>ایستادن</button>
          : <button className="go" onClick={() => generate([], 0)} disabled={!config.tokenSet || !counts.left}>
              ساختِ همه‌ی باقی‌مانده‌ها ({fa(counts.left)})
            </button>}
      </div>

      {error && <div className="err">{error}</div>}
      {!config.tokenSet && <div className="err">توکنِ آواشو را پایین وارد کن تا ساخت شروع شود.</div>}

      <div className="stats">
        <div className="stat">
          <div className="label">کلِ جمله‌ها</div>
          <div className="value">{fa(counts.total)}</div>
          <div className="foot">طبق audio_manifest.txt</div>
        </div>
        <div className={"stat" + (counts.done ? " on" : "")}>
          <div className="label">ساخته‌شده</div>
          <div className="value">{fa(counts.done)}</div>
          <div className="bar"><i style={{ width: `${counts.total ? (counts.done / counts.total) * 100 : 0}%` }} /></div>
        </div>
        <div className="stat">
          <div className="label">باقی‌مانده</div>
          <div className="value">{fa(counts.left)}</div>
          <div className="foot">{status.running ? `در حال ساخت: ${status.current.join("، ") || "…"}` : "آماده"}</div>
        </div>
        <div className="stat">
          <div className="label">{status.running ? "پیشرفتِ این دسته" : "ناموفق در آخرین دسته"}</div>
          <div className="value">{status.running ? `${fa(status.done)}/${fa(status.total)}` : fa(status.failed)}</div>
          {status.running && <div className="bar"><i style={{ width: `${progress}%` }} /></div>}
        </div>
      </div>

      <div className="card">
        <h2>⚙️ تنظیمات <span className="hint">در voice-studio.config.json روی سیستمِ خودت ذخیره می‌شود</span></h2>
        <div className="grid">
          <div>
            <label>توکنِ آواشو (gateway-token)</label>
            <input type="password" placeholder={config.tokenHint || "توکن را بچسبان"}
                   value={token} onChange={(e) => setToken(e.target.value)} />
          </div>
          <div>
            <label>مسیرِ ذخیره‌ی فایل‌های صوتی</label>
            <input type="text" value={form.outDir}
                   onChange={(e) => setForm({ ...form, outDir: e.target.value })} />
          </div>
          <div>
            <label>مسیرِ audio_manifest.txt</label>
            <input type="text" value={form.manifestPath}
                   onChange={(e) => setForm({ ...form, manifestPath: e.target.value })} />
          </div>
          <div>
            <label>گوینده</label>
            <select value={form.speaker} onChange={(e) => setForm({ ...form, speaker: e.target.value })}>
              {config.speakers.map((s) => <option key={s} value={s}>{s}</option>)}
            </select>
          </div>
          <div>
            <label>سرعت</label>
            <input type="number" step="0.1" min="0.5" max="2" value={form.speed}
                   onChange={(e) => setForm({ ...form, speed: e.target.value })} />
          </div>
          <div>
            <label>endpoint</label>
            <select value={form.endpoint} onChange={(e) => setForm({ ...form, endpoint: e.target.value })}>
              <option value="short">avasho (متنِ کوتاه)</option>
              <option value="long">avasho-large (متنِ بلند)</option>
            </select>
          </div>
          <div>
            <label>چند تا هم‌زمان</label>
            <input type="number" min="1" max="8" value={form.concurrency}
                   onChange={(e) => setForm({ ...form, concurrency: e.target.value })} />
          </div>
          <div>
            <label>فاصله‌ی بینِ درخواست‌ها (میلی‌ثانیه)</label>
            <input type="number" min="0" step="100" value={form.delayMs}
                   onChange={(e) => setForm({ ...form, delayMs: e.target.value })} />
          </div>
          <div>
            <label>تلاشِ مجدد در خطا</label>
            <input type="number" min="0" max="8" value={form.retries}
                   onChange={(e) => setForm({ ...form, retries: e.target.value })} />
          </div>
          <label className="check">
            <input type="checkbox" checked={form.useVowels}
                   onChange={(e) => setForm({ ...form, useVowels: e.target.checked })} />
            متنِ اعراب‌گذاری‌شده (ستون سوم)
          </label>
          <label className="check">
            <input type="checkbox" checked={form.convertOgg}
                   onChange={(e) => setForm({ ...form, convertOgg: e.target.checked })} />
            تبدیل به ogg با ffmpeg
          </label>
        </div>
        <div className="row" style={{ marginTop: 14 }}>
          <button className="go" disabled={busy}
                  onClick={() => { save({ ...form, token: token || undefined }); setToken(""); }}>
            ذخیره‌ی تنظیمات
          </button>
          <button className="ghost" disabled={busy || !config.tokenSet} onClick={testVoice}>
            تستِ صدا با یک جمله
          </button>
          {config.tokenSet &&
            <button className="ghost" onClick={() => save({ token: "" })}>پاک کردنِ توکن</button>}
        </div>
        <p className="note">
          فایل‌ها با نامِ کلید ذخیره می‌شوند (مثلاً <code>p008_01.mp3</code>). همان‌ها را در
          <code> app/src/main/res/raw/ </code> بگذار؛ اپ خودش برمی‌داردشان.
        </p>
      </div>

      <div className="card">
        <h2>⏱ زمان‌بندی <span className="hint">دسته‌دسته می‌سازد و خودش ادامه می‌دهد</span></h2>
        <div className="grid">
          <div>
            <label>هر چند ثانیه یک دسته</label>
            <input type="number" min="30" step="30" value={form.schedule.everySeconds}
                   onChange={(e) => setForm({ ...form, schedule: { ...form.schedule, everySeconds: Number(e.target.value) } })} />
          </div>
          <div>
            <label>اندازه‌ی هر دسته</label>
            <input type="number" min="1" value={form.schedule.batchSize}
                   onChange={(e) => setForm({ ...form, schedule: { ...form.schedule, batchSize: Number(e.target.value) } })} />
          </div>
          <label className="check">
            <input type="checkbox" checked={config.schedule.enabled}
                   onChange={(e) => save({ schedule: { ...form.schedule, enabled: e.target.checked } })} />
            {config.schedule.enabled ? "روشن است" : "خاموش"}
          </label>
        </div>
        <p className="note">
          تا این پنجره باز است و سرور بالا، هر دوره یک دسته‌ی تازه از جمله‌هایی که فایل ندارند
          ساخته می‌شود. با سهمیه‌ی محدودِ سرویس، دسته‌ی کوچک و فاصله‌ی بلند بهتر جواب می‌دهد.
        </p>
      </div>

      <div className="card">
        <h2>⚡ ساختِ دسته‌ای</h2>
        <div className="row">
          {GROUPS.map((g) => {
            const subset = lines.filter((l) => l.group === g.id);
            const left = todoKeys(subset).length;
            return (
              <button key={g.id} className="chip" disabled={busy || status.running || !left}
                      onClick={() => generate(todoKeys(subset))}>
                {g.label} ({fa(left)})
              </button>
            );
          })}
        </div>
        <div className="row" style={{ marginTop: 10 }}>
          {chapters.map((c) => {
            const subset = lines.filter((l) => l.chapter === c);
            const left = todoKeys(subset).length;
            return (
              <button key={c} className="chip" disabled={busy || status.running || !left}
                      onClick={() => generate(todoKeys(subset))}>
                فصل {fa(c)} ({fa(left)})
              </button>
            );
          })}
        </div>
        <div className="row" style={{ marginTop: 10 }}>
          {[10, 50, 200].map((n) => (
            <button key={n} className="ghost" disabled={busy || status.running || !counts.left}
                    onClick={() => generate([], n)}>
              فقط {fa(n)} تای بعدی
            </button>
          ))}
          <button disabled={busy || status.running || !shown.some((l) => !l.done)}
                  onClick={() => generate(todoKeys(shown))}>
            هرچه در فهرستِ پایین فیلتر شده ({fa(shown.filter((l) => !l.done).length)})
          </button>
        </div>
      </div>

      <div className="card">
        <h2>📤 انتشار روی هاست <span className="hint">فایل‌ها را خودت آپلود می‌کنی؛ اینجا فقط فهرست ساخته می‌شود</span></h2>
        <div className="grid">
          <div>
            <label>آدرسِ پوشه‌ی صداها روی هاست</label>
            <input type="text" value={form.audioBase || ""}
                   onChange={(e) => setForm({ ...form, audioBase: e.target.value })} />
          </div>
          <div>
            <label>آدرسِ پوشه‌ی کاربرگ‌ها روی هاست</label>
            <input type="text" value={form.karbargBase || ""}
                   onChange={(e) => setForm({ ...form, karbargBase: e.target.value })} />
          </div>
        </div>
        <div className="row" style={{ marginTop: 12 }}>
          <button className="go" disabled={busy} onClick={async () => {
            setBusy(true);
            try {
              const index = await api("/publish/audio", { method: "POST" });
              setPublished(index);
              setError("");
            } catch (err) { setError(err.message); }
            setBusy(false);
          }}>
            ساختِ فهرستِ صدا (index.json)
          </button>
          <button className="ghost" disabled={busy} onClick={() => save(form)}>ذخیره‌ی آدرس‌ها</button>
        </div>
        {published && (
          <p className="note">
            ✓ {fa(published.count)} کلیپ، {fa(Math.round(published.bytes / 1048576))} مگابایت.
            <code> index.json </code> کنارِ خودِ فایل‌ها ساخته شد و یک نسخه هم در
            <code> app/src/main/assets/voice-index.json </code> گذاشته شد.
          </p>
        )}
        <p className="note">
          بعدش: محتویاتِ <code>{config.outDir}</code> را با همان <code>index.json</code> در
          <code> {config.audioBase} </code> آپلود کن. اپ همان فهرست را می‌خواند و فایل‌ها را
          خودش دانلود می‌کند.
        </p>
      </div>

      <div className="card">
        <h2>📄 کاربرگ‌های چاپی <span className="hint">{fa(sheets.length)} کاربرگ در {config.karbargDir}</span></h2>
        <div className="grid">
          <div>
            <label>عنوانِ کاربرگ</label>
            <input type="text" value={sheetForm.title}
                   onChange={(e) => setSheetForm({ ...sheetForm, title: e.target.value })} />
          </div>
          <div>
            <label>توضیح (مثلاً «۲ صفحه، جمع و تفریق»)</label>
            <input type="text" value={sheetForm.note}
                   onChange={(e) => setSheetForm({ ...sheetForm, note: e.target.value })} />
          </div>
          <div>
            <label>فصل</label>
            <select value={sheetForm.chapter}
                    onChange={(e) => setSheetForm({ ...sheetForm, chapter: Number(e.target.value) })}>
              <option value={-1}>هر فصلی</option>
              {[1, 2, 3, 4, 5, 6, 7, 8].map((n) => (
                <option key={n} value={n - 1}>فصل {fa(n)}</option>
              ))}
            </select>
          </div>
          <div>
            <label>فایلِ PDF</label>
            <input type="file" accept="application/pdf"
                   onChange={(e) => setSheetForm({ ...sheetForm, file: e.target.files?.[0] || null })} />
          </div>
        </div>
        <div className="row" style={{ marginTop: 12 }}>
          <button className="go" disabled={busy || !sheetForm.title || !sheetForm.file}
                  onClick={async () => {
                    setBusy(true);
                    try {
                      const data = await new Promise((resolve, reject) => {
                        const reader = new FileReader();
                        reader.onload = () => resolve(reader.result);
                        reader.onerror = reject;
                        reader.readAsDataURL(sheetForm.file);
                      });
                      await api("/karbarg", { method: "POST", body: JSON.stringify({
                        title: sheetForm.title, note: sheetForm.note,
                        chapter: sheetForm.chapter, fileName: sheetForm.file.name, dataBase64: data,
                      }) });
                      setSheetForm({ title: "", note: "", chapter: -1, file: null });
                      load();
                      setError("");
                    } catch (err) { setError(err.message); }
                    setBusy(false);
                  }}>
            افزودنِ کاربرگ
          </button>
          <button className="ghost" disabled={busy} onClick={async () => {
            try { await api("/publish/karbarg", { method: "POST" }); setError(""); }
            catch (err) { setError(err.message); }
          }}>
            ساختِ فهرستِ کاربرگ (index.json)
          </button>
        </div>

        {sheets.length > 0 && (
          <div className="files" style={{ marginTop: 12 }}>
            {sheets.map((sheet) => (
              <div className="file" key={sheet.slug}>
                <span className="key">{sheet.slug}</span>
                <span className="say">
                  {sheet.title}
                  {sheet.note ? ` — ${sheet.note}` : ""}
                  {sheet.chapter >= 0 ? ` · فصل ${fa(sheet.chapter + 1)}` : ""}
                </span>
                <span className="row">
                  <span className="tag done">{fa(Math.round(sheet.bytes / 1024))}KB</span>
                  <button className="tiny ghost" onClick={async () => {
                    await api(`/karbarg/${sheet.slug}`, { method: "DELETE" });
                    load();
                  }}>پاک</button>
                </span>
              </div>
            ))}
          </div>
        )}
        <p className="note">
          هر کاربرگ یک پوشه با نامِ خودش می‌گیرد که PDF و <code>info.json</code> در آن است.
          همین ساختار را در <code>{config.karbargBase}</code> آپلود کن؛ اپ فهرست را با
          عنوان و توضیح می‌خواند و لینکِ دانلود می‌سازد.
        </p>
      </div>

      <div className="card">
        <div className="log-head">
          <h2 style={{ margin: 0 }}>لاگِ زنده</h2>
          <button className="tiny ghost" onClick={() => setShowLog(!showLog)}>
            {showLog ? "مخفی کن" : "نشان بده"}
          </button>
        </div>
        {showLog && (
          <div className="log" ref={logBox}>
            {log.length === 0 && <div className="t">— هنوز چیزی نیست —</div>}
            {log.map((entry, i) => (
              <div key={i}>
                <span className="t">[{entry.at.slice(11, 19)}]</span>{" "}
                <span className={entry.level}>{entry.message}</span>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="card">
        <h2>🎧 جمله‌ها و پخش <span className="hint">{fa(shown.length)} از {fa(counts.total)}</span></h2>
        <div className="grid" style={{ marginBottom: 12 }}>
          <div>
            <label>جستجو در کلید یا متن</label>
            <input type="text" value={filter.q} onChange={(e) => setFilter({ ...filter, q: e.target.value })} />
          </div>
          <div>
            <label>گروه</label>
            <select value={filter.group} onChange={(e) => setFilter({ ...filter, group: e.target.value })}>
              <option value="all">همه</option>
              {GROUPS.map((g) => <option key={g.id} value={g.id}>{g.label}</option>)}
            </select>
          </div>
          <div>
            <label>فصل</label>
            <select value={filter.chapter} onChange={(e) => setFilter({ ...filter, chapter: e.target.value })}>
              <option value="all">همه</option>
              {chapters.map((c) => <option key={c} value={String(c)}>فصل {fa(c)}</option>)}
            </select>
          </div>
          <div>
            <label>وضعیت</label>
            <select value={filter.state} onChange={(e) => setFilter({ ...filter, state: e.target.value })}>
              <option value="all">همه</option>
              <option value="done">ساخته‌شده</option>
              <option value="todo">بی‌فایل</option>
            </select>
          </div>
        </div>

        <div className="files">
          {shown.length === 0 && <div className="empty">چیزی با این فیلتر پیدا نشد.</div>}
          {shown.slice(0, 400).map((l) => (
            <div className="file" key={l.key}>
              <span className="key">{l.key}</span>
              <span className="say" title={config.useVowels ? l.spoken : l.written}>
                {config.useVowels ? l.spoken : l.written}
              </span>
              <span className="row">
                {l.done ? (
                  <>
                    <audio controls preload="none" src={`/api/audio/${l.key}`} />
                    <span className="tag done">{Math.round(l.size / 1024)}KB</span>
                    <button className="tiny ghost" onClick={() => removeFile(l.key)}>پاک</button>
                    <button className="tiny" disabled={status.running}
                            onClick={() => removeFile(l.key).then(() => generate([l.key]))}>
                      از نو
                    </button>
                  </>
                ) : (
                  <>
                    <span className="tag">بی‌فایل</span>
                    <button className="tiny go" disabled={busy || status.running || !config.tokenSet}
                            onClick={() => generate([l.key])}>بساز</button>
                  </>
                )}
              </span>
            </div>
          ))}
        </div>
        {shown.length > 400 && <p className="note">۴۰۰ ردیفِ اول نشان داده شد — با فیلتر محدودترش کن.</p>}
      </div>
    </div>
  );
}
