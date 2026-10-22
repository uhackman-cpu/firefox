import test from "node:test";
import assert from "node:assert/strict";
import { createTinyfishServer } from "./localhost-server.mjs";

test("binds to the configured loopback port and requires authorization", async t => {
  const port = 18991;
  const secret = "test-secret-that-is-at-least-32-characters";
  const server = createTinyfishServer({
    port,
    authorize: req => req.headers.authorization === `Bearer ${secret}`,
    adapter: {
      async executeStep() {
        return { done: true, result: { summary: "test" } };
      }
    }
  });
  const address = await server.start();
  t.after(() => server.stop());

  assert.equal(address.host, "127.0.0.1");
  assert.equal(address.port, port);

  const denied = await fetch(`http://127.0.0.1:${port}/status`);
  assert.equal(denied.status, 401);

  const allowed = await fetch(`http://127.0.0.1:${port}/status`, {
    headers: { authorization: `Bearer ${secret}` }
  });
  assert.equal(allowed.status, 200);
  assert.equal((await allowed.json()).status, "online");
});

test("rejects a cross-origin browser request", async t => {
  const port = 18992;
  const server = createTinyfishServer({
    port,
    authorize: () => true,
    adapter: { async executeStep() { return { done: true }; } }
  });
  await server.start();
  t.after(() => server.stop());

  const response = await fetch(`http://127.0.0.1:${port}/status`, {
    headers: { origin: "https://example.invalid" }
  });
  assert.equal(response.status, 403);
});

test("goal endpoints validate input and require confirmation for sensitive actions", async t => {
  const port = 18993;
  const secret = "test-secret-that-is-at-least-32-characters";
  const server = createTinyfishServer({
    port,
    authorize: req => req.headers.authorization === `Bearer ${secret}`,
    adapter: {
      async executeStep() {
        return { requiresConfirmation: true, description: "Submit form?" };
      }
    }
  });
  await server.start();
  t.after(() => server.stop());

  const headers = {
    authorization: `Bearer ${secret}`,
    "content-type": "application/json"
  };
  const createdResponse = await fetch(`http://127.0.0.1:${port}/goal`, {
    method: "POST",
    headers,
    body: JSON.stringify({ goal: "submit form", maxSteps: 2 })
  });
  assert.equal(createdResponse.status, 202);
  const { goal } = await createdResponse.json();

  const tick = await fetch(`http://127.0.0.1:${port}/goal/${goal.id}/tick`, {
    method: "POST", headers
  });
  assert.equal(tick.status, 200);
  assert.equal((await tick.json()).goal.state, "waiting_confirmation");

  const blockedTick = await fetch(`http://127.0.0.1:${port}/goal/${goal.id}/tick`, {
    method: "POST", headers
  });
  assert.equal(blockedTick.status, 409);
});
