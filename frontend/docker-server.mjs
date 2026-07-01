import { createServer } from "node:http";
import { createReadStream } from "node:fs";
import { stat } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const clientDir = path.join(__dirname, "dist", "client");
const serverEntry = await import(pathToFileURL(path.join(__dirname, "dist", "server", "server.js")));
const app = serverEntry.default;

const port = Number(process.env.PORT || 80);
const apiBaseUrl = process.env.API_BASE_URL || "http://api-gateway:8080";

const contentTypes = {
  ".css": "text/css; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".mjs": "text/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".html": "text/html; charset=utf-8",
  ".jpg": "image/jpeg",
  ".jpeg": "image/jpeg",
  ".png": "image/png",
  ".svg": "image/svg+xml",
  ".ico": "image/x-icon",
  ".webp": "image/webp",
  ".woff": "font/woff",
  ".woff2": "font/woff2",
};

function sendNodeResponse(res, webResponse) {
  res.statusCode = webResponse.status;
  webResponse.headers.forEach((value, key) => res.setHeader(key, value));

  if (!webResponse.body) {
    res.end();
    return;
  }

  const reader = webResponse.body.getReader();

  function pump() {
    reader.read().then(({ done, value }) => {
      if (done) {
        res.end();
        return;
      }
      res.write(value, pump);
    }).catch((error) => {
      console.error(error);
      res.destroy(error);
    });
  }

  pump();
}

function createWebRequest(req, url) {
  const init = {
    method: req.method,
    headers: req.headers,
  };

  if (req.method !== "GET" && req.method !== "HEAD") {
    init.body = req;
    init.duplex = "half";
  }

  return new Request(url, init);
}

async function tryServeStatic(pathname, res) {
  const decodedPath = decodeURIComponent(pathname);
  const filePath = path.normalize(path.join(clientDir, decodedPath));

  if (!filePath.startsWith(clientDir) || filePath.endsWith(path.sep)) {
    return false;
  }

  try {
    const fileStat = await stat(filePath);
    if (!fileStat.isFile()) return false;

    res.statusCode = 200;
    res.setHeader("content-type", contentTypes[path.extname(filePath)] || "application/octet-stream");
    res.setHeader("content-length", fileStat.size);
    createReadStream(filePath).pipe(res);
    return true;
  } catch {
    return false;
  }
}

createServer(async (req, res) => {
  try {
    const url = new URL(req.url || "/", `http://${req.headers.host || "localhost"}`);

    if (url.pathname.startsWith("/api/")) {
      const targetUrl = new URL(url.pathname + url.search, apiBaseUrl);
      const response = await fetch(createWebRequest(req, targetUrl));
      sendNodeResponse(res, response);
      return;
    }

    if (await tryServeStatic(url.pathname, res)) {
      return;
    }

    const response = await app.fetch(createWebRequest(req, url), {}, {});
    sendNodeResponse(res, response);
  } catch (error) {
    console.error(error);
    res.statusCode = 500;
    res.end("Internal Server Error");
  }
}).listen(port, "0.0.0.0", () => {
  console.log(`Frontend listening on port ${port}`);
});
