#!/usr/bin/env node
/**
 * Localhost-only prototype launcher.
 *
 * Usage:
 *   TINYFISH_TOKEN='long-random-secret' TINYFISH_PORT=8900 node automation/tinyfish/launch.mjs
 *
 * This Node transport is not a Gecko-native Firefox service. A real browser
 * adapter must be injected before goal execution can control Firefox.
 */
import { timingSafeEqual } from "node:crypto";
import { createTinyfishServer } from "./localhost-server.mjs";

const portText = process.env.TINYFISH_PORT ?? "8900";
const port = Number(portText);
const token = process.env.TINYFISH_TOKEN ?? "";

if (!/^\d+$/.test(portText) || !Number.isInteger(port) || port < 1024 || port > 65535) {
  throw new Error("TINYFISH_PORT must be an integer from 1024 to 65535");
}
if (token.length < 32) {
  throw new Error("Set TINYFISH_TOKEN to a random secret of at least 32 characters");
}

function authorize(req) {
  const header = req.headers.authorization ?? "";
  const supplied = header.startsWith("Bearer ") ? header.slice(7) : "";
  const expectedBytes = Buffer.from(token);
  const suppliedBytes = Buffer.from(supplied);
  return expectedBytes.length === suppliedBytes.length &&
    timingSafeEqual(expectedBytes, suppliedBytes);
}

const server = createTinyfishServer({
  port,
  authorize,
  adapter: {
    async executeStep() {
      throw new Error("No Firefox browser adapter is installed yet");
    }
  }
});

const address = await server.start();
console.log(`TinyFish prototype listening at http://${address.host}:${address.port}`);
console.log("This is a control API prototype; page automation is unavailable until a Gecko adapter is integrated.");

function shutdown() {
  server.stop().then(() => process.exit(0), error => {
    console.error("Failed to stop server cleanly:", error);
    process.exit(1);
  });
}
process.once("SIGINT", shutdown);
process.once("SIGTERM", shutdown);
