// Putting the finished files on the host.
//
// The clips and the worksheets are made here and have to end up in /public_html/grade-3/math/…
// on the teacher's own server. This does that over FTP: it connects, makes the folder if it is
// not there, and sends the files — skipping the ones already on the server with the same size,
// so a second run only sends what is new. Four thousand clips is not something to upload twice.
//
// The password is only ever read from the studio's own config file on this machine and is never
// written to the log.

import fs from "node:fs";
import path from "node:path";
import { Client } from "basic-ftp";

const AUDIO_EXT = [".ogg", ".mp3", ".wav"];

function clientFor({ host, port, user, password, secure }) {
  const client = new Client(60000);
  client.ftp.verbose = false;
  return {
    client,
    connect: () => client.access({
      host: String(host || "").trim(),
      port: Number(port) || 21,
      user: String(user || "").trim(),
      password: String(password || ""),
      secure: Boolean(secure),               // explicit FTPS when the host wants it
      secureOptions: { rejectUnauthorized: false },
    }),
  };
}

/** Connect, look at the folder, and say what is there. Nothing is written. */
export async function probe(settings, remoteDir) {
  const { client, connect } = clientFor(settings);
  try {
    await connect();
    const root = await client.pwd();
    let files = [];
    let exists = true;
    try {
      files = await client.list(remoteDir);
    } catch {
      exists = false;
    }
    return {
      ok: true,
      loginDir: root,
      remoteDir,
      remoteExists: exists,
      remoteCount: files.filter((f) => f.isFile).length,
    };
  } finally {
    client.close();
  }
}

/**
 * Sends a list of local files into one remote folder.
 *
 * `onStep` is called with (name, state) as it goes — "sent", "skipped" or "failed" — so the
 * studio's log reads like the upload is happening, which with four thousand files it is.
 */
export async function uploadFiles({ settings, remoteDir, files, skipExisting = true, onStep }) {
  const { client, connect } = clientFor(settings);
  const result = { sent: 0, skipped: 0, failed: 0, bytes: 0, errors: [] };
  try {
    await connect();
    await client.ensureDir(remoteDir);       // creates the whole path and moves into it

    let onServer = new Map();
    if (skipExisting) {
      try {
        for (const entry of await client.list()) {
          if (entry.isFile) onServer.set(entry.name, entry.size);
        }
      } catch {
        // an unreadable folder just means nothing can be skipped
      }
    }

    for (const local of files) {
      const name = path.basename(local);
      let size = 0;
      try {
        size = fs.statSync(local).size;
      } catch {
        result.failed += 1;
        result.errors.push(`${name}: فایل نیست`);
        if (onStep) onStep(name, "failed");
        continue;
      }

      if (skipExisting && onServer.get(name) === size) {
        result.skipped += 1;
        if (onStep) onStep(name, "skipped");
        continue;
      }

      try {
        await client.uploadFrom(local, name);
        result.sent += 1;
        result.bytes += size;
        if (onStep) onStep(name, "sent");
      } catch (err) {
        result.failed += 1;
        result.errors.push(`${name}: ${err.message}`);
        if (onStep) onStep(name, "failed");
      }
    }
    return result;
  } finally {
    client.close();
  }
}

/** Mirrors a whole folder tree — used for the worksheets, which live in one folder each. */
export async function uploadTree({ settings, remoteDir, localDir, onStep }) {
  const { client, connect } = clientFor(settings);
  try {
    await connect();
    if (onStep) client.trackProgress((info) => onStep(info.name, "sent"));
    await client.ensureDir(remoteDir);
    await client.clearWorkingDir();          // the index must not keep sheets that were removed
    await client.uploadFromDir(localDir);
    client.trackProgress();
    return { ok: true };
  } finally {
    client.close();
  }
}

/** Every clip in the output folder, plus its index — what actually goes to the audio folder. */
export function audioFilesIn(dir) {
  if (!fs.existsSync(dir)) return [];
  const out = [];
  for (const name of fs.readdirSync(dir).sort()) {
    const ext = path.extname(name).toLowerCase();
    if (name === "index.json") continue;              // sent last, once the clips are all there
    if (!AUDIO_EXT.includes(ext)) continue;
    if (name.startsWith("_test-")) continue;
    out.push(path.join(dir, name));
  }
  return out;
}
