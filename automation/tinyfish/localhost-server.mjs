/**
 * Loopback HTTP adapter for the TinyFish-style automation prototype.
 *
 * This is a Node-compatible transport prototype, not a Gecko-native server.
 * Firefox integration must replace this transport and pass security review.
 */
import { createServer } from "node:http";
import { GoalRunner, GoalState } from "./goal-runner.mjs";

const JSON_LIMIT = 16 * 1024;
const HOST = "127.0.0.1";

function send(res, status, payload) {
  const body = JSON.stringify(payload);
  res.writeHead(status, {
    "content-type": "application/json; charset=utf-8",
    "content-length": Buffer.byteLength(body),
    "cache-control": "no-store",
    "x-content-type-options": "nosniff",
  });
  res.end(body);
}

async function readJson(req) {
  let body = "";
  for await (const chunk of req) {
    body += chunk.toString("utf8");
    if (Buffer.byteLength(body) > JSON_LIMIT) {
      const error = new Error("Request body too large");
      error.statusCode = 413;
      throw error;
    }
  }
  if (!body) return {};
  try {
    return JSON.parse(body);
  } catch {
    const error = new Error("Invalid JSON");
    error.statusCode = 400;
    throw error;
  }
}

export function createTinyfishServer({ adapter, port = 8900, authorize } = {}) {
  if (!Number.isInteger(port) || port < 1024 || port > 65535) {
    throw new RangeError("port must be an integer between 1024 and 65535");
  }
  if (typeof authorize !== "function") {
    throw new TypeError("An explicit local authorization callback is required");
  }
  const runner = new GoalRunner({ adapter });
  const server = createServer(async (req, res) => {
    try {
      const host = req.headers.host ?? "";
      if (!new RegExp(`^127\\.0\\.0\\.1(?::${port})?$`).test(host)) {
        return send(res, 403, { error: "Loopback Host header required" });
      }
      const origin = req.headers.origin;
      if (origin && origin !== `http://127.0.0.1:${port}`) {
        return send(res, 403, { error: "Cross-origin requests are not allowed" });
      }
      if (!await authorize(req)) {
        return send(res, 401, { error: "Local authorization required" });
      }

      const url = new URL(req.url ?? "/", `http://127.0.0.1:${port}`);
      if (req.method === "GET" && url.pathname === "/status") {
        return send(res, 200, {
          status: "online",
          service: "Firefox TinyFish prototype",
          stateCounts: runner.list().reduce((counts, goal) => {
            counts[goal.state] = (counts[goal.state] ?? 0) + 1;
            return counts;
          }, {}),
        });
      }
      if (req.method === "GET" && url.pathname === "/goals") {
        return send(res, 200, { goals: runner.list() });
      }
      if (req.method === "POST" && url.pathname === "/goal") {
        const goal = runner.enqueue(await readJson(req));
        return send(res, 202, { goal });
      }
      const match = url.pathname.match(/^\/goal\/([1-9]\d*)(?:\/(cancel|confirm|tick))?$/);
      if (match) {
        const [, id, action] = match;
        if (req.method === "GET" && !action) {
          const goal = runner.get(id);
          return goal ? send(res, 200, { goal }) :
            send(res, 404, { error: "Goal not found" });
        }
        if (req.method === "POST" && action === "cancel") {
          const cancelled = runner.cancel(id);
          const goal = runner.get(id);
          return send(res, !goal ? 404 : cancelled ? 200 : 409, { goal });
        }
        if (req.method === "POST" && action === "confirm") {
          const before = runner.get(id);
          if (!before) return send(res, 404, { error: "Goal not found" });
          if (before.state !== GoalState.WAITING_CONFIRMATION) {
            return send(res, 409, { error: "Goal is not awaiting confirmation", goal: before });
          }
          const body = await readJson(req);
          if (typeof body.approved !== "boolean") {
            return send(res, 400, { error: "approved must be boolean" });
          }
          return send(res, 200, { goal: runner.confirm(id, body.approved) });
        }
        if (req.method === "POST" && action === "tick") {
          const before = runner.get(id);
          if (!before) return send(res, 404, { error: "Goal not found" });
          if (before.state === GoalState.WAITING_CONFIRMATION) {
            return send(res, 409, {
              error: "Goal awaits explicit confirmation",
              goal: before,
            });
          }
          return send(res, 200, { goal: await runner.tick(id) });
        }
      }
      return send(res, 404, { error: "Route not found" });
    } catch (error) {
      return send(res, error.statusCode ?? 400, {
        error: String(error.message ?? error).slice(0, 300),
      });
    }
  });

  return {
    host: HOST,
    port,
    runner,
    start() {
      return new Promise((resolve, reject) => {
        server.once("error", reject);
        server.listen(port, HOST, () => {
          server.removeListener("error", reject);
          resolve({ host: HOST, port });
        });
      });
    },
    stop() {
      return new Promise((resolve, reject) => {
        server.close(error => error ? reject(error) : resolve());
      });
    },
  };
}
