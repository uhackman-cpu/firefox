import test from "node:test";
import assert from "node:assert/strict";
import { GoalRunner, GoalState, validateGoalInput } from "./goal-runner.mjs";

test("validates and trims goal input", () => {
  assert.deepEqual(validateGoalInput({ goal: "  summarize this  ", maxSteps: 3 }), {
    goal: "summarize this",
    maxSteps: 3,
  });
  assert.throws(() => validateGoalInput({ goal: "" }), /1-4000/);
  assert.throws(() => validateGoalInput({ goal: "ok", maxSteps: 26 }), /maxSteps/);
});

test("advances a task until success", async () => {
  let calls = 0;
  const runner = new GoalRunner({
    adapter: { async executeStep() {
      calls += 1;
      return calls === 2
        ? { done: true, result: { summary: "ok" }, description: "Finished" }
        : { done: false, description: "Read page" };
    } },
    clock: () => "2026-01-01T00:00:00.000Z",
  });
  const created = runner.enqueue({ goal: "summarize current article" });
  assert.equal(created.state, GoalState.QUEUED);
  assert.equal((await runner.tick(created.id)).state, GoalState.QUEUED);
  const done = await runner.tick(created.id);
  assert.equal(done.state, GoalState.SUCCEEDED);
  assert.deepEqual(done.result, { summary: "ok" });
});

test("requires explicit confirmation for consequential steps", async () => {
  const runner = new GoalRunner({
    adapter: { async executeStep() {
      return { requiresConfirmation: true, description: "Submit this form?" };
    } },
  });
  const task = runner.enqueue({ goal: "submit the form" });
  const waiting = await runner.tick(task.id);
  assert.equal(waiting.state, GoalState.WAITING_CONFIRMATION);
  assert.equal(runner.get(task.id).confirmation.description, "Submit this form?");
  assert.equal(runner.confirm(task.id, false).state, GoalState.CANCELLED);
});

test("cancelled queued goal does not execute", async () => {
  let calls = 0;
  const runner = new GoalRunner({ adapter: { async executeStep() {
    calls += 1;
    return { done: true };
  } } });
  const task = runner.enqueue({ goal: "read page" });
  assert.equal(runner.cancel(task.id), true);
  assert.equal((await runner.tick(task.id)).state, GoalState.CANCELLED);
  assert.equal(calls, 0);
});
