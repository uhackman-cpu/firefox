/**
 * Minimal, dependency-free task lifecycle primitives for the TinyFish-style
 * Firefox automation prototype.
 *
 * This module intentionally does not interact with web content itself. A
 * reviewed Firefox actor/browser-chrome adapter must supply those operations.
 */
export const GoalState = Object.freeze({
  QUEUED: "queued",
  RUNNING: "running",
  WAITING_CONFIRMATION: "waiting_confirmation",
  SUCCEEDED: "succeeded",
  FAILED: "failed",
  CANCELLED: "cancelled",
});

const TERMINAL = new Set([
  GoalState.SUCCEEDED,
  GoalState.FAILED,
  GoalState.CANCELLED,
]);

const MAX_GOAL_LENGTH = 4_000;
const MAX_STEPS = 25;

/** Validate untrusted goal input before storing it. */
export function validateGoalInput(input) {
  if (!input || typeof input !== "object" || Array.isArray(input)) {
    throw new TypeError("Goal must be a JSON object");
  }
  if (typeof input.goal !== "string") {
    throw new TypeError("goal must be a string");
  }
  const goal = input.goal.trim();
  if (!goal || goal.length > MAX_GOAL_LENGTH) {
    throw new RangeError(`goal must be 1-${MAX_GOAL_LENGTH} characters`);
  }
  const maxSteps = input.maxSteps ?? 10;
  if (!Number.isInteger(maxSteps) || maxSteps < 1 || maxSteps > MAX_STEPS) {
    throw new RangeError(`maxSteps must be an integer from 1 to ${MAX_STEPS}`);
  }
  return Object.freeze({ goal, maxSteps });
}

/**
 * In-memory task registry. The adapter is injected so this file is safe to
 * import without granting any browser/page/network capabilities.
 */
export class GoalRunner {
  #tasks = new Map();
  #adapter;
  #clock;
  #nextId = 1;

  constructor({ adapter, clock = () => new Date().toISOString() } = {}) {
    if (!adapter || typeof adapter.executeStep !== "function") {
      throw new TypeError("An explicit executeStep adapter is required");
    }
    this.#adapter = adapter;
    this.#clock = clock;
  }

  enqueue(input) {
    const valid = validateGoalInput(input);
    const id = String(this.#nextId++);
    const task = {
      id,
      goal: valid.goal,
      maxSteps: valid.maxSteps,
      state: GoalState.QUEUED,
      createdAt: this.#clock(),
      updatedAt: this.#clock(),
      steps: [],
      result: null,
      error: null,
      cancelRequested: false,
      confirmation: null,
    };
    this.#tasks.set(id, task);
    return this.get(id);
  }

  get(id) {
    const task = this.#tasks.get(String(id));
    if (!task) return null;
    return this.#publicTask(task);
  }

  list() {
    return [...this.#tasks.values()].map(task => this.#publicTask(task));
  }

  cancel(id) {
    const task = this.#tasks.get(String(id));
    if (!task || TERMINAL.has(task.state)) return false;
    task.cancelRequested = true;
    if (task.state === GoalState.QUEUED ||
        task.state === GoalState.WAITING_CONFIRMATION) {
      task.state = GoalState.CANCELLED;
      task.updatedAt = this.#clock();
    }
    return true;
  }

  confirm(id, approved) {
    const task = this.#tasks.get(String(id));
    if (!task || task.state !== GoalState.WAITING_CONFIRMATION) {
      throw new Error("Goal is not awaiting confirmation");
    }
    if (approved !== true) {
      task.confirmation = null;
      task.state = GoalState.CANCELLED;
      task.updatedAt = this.#clock();
      return this.get(id);
    }
    task.confirmation = { approved: true };
    task.state = GoalState.QUEUED;
    task.updatedAt = this.#clock();
    return this.get(id);
  }

  /**
   * Execute one adapter step per call; a scheduler can invoke this gradually
   * and remain responsive to cancellation. The adapter must return
   * {done, result?, description?, requiresConfirmation?}.
   */
  async tick(id) {
    const task = this.#tasks.get(String(id));
    if (!task) throw new Error("Goal not found");
    if (TERMINAL.has(task.state)) return this.get(id);
    if (task.cancelRequested) {
      task.state = GoalState.CANCELLED;
      task.updatedAt = this.#clock();
      return this.get(id);
    }
    if (task.state === GoalState.WAITING_CONFIRMATION) return this.get(id);

    task.state = GoalState.RUNNING;
    task.updatedAt = this.#clock();
    try {
      const step = await this.#adapter.executeStep({
        id: task.id,
        goal: task.goal,
        stepIndex: task.steps.length,
        maxSteps: task.maxSteps,
        previousSteps: task.steps.map(({ description, outcome }) => ({
          description, outcome,
        })),
      });
      if (!step || typeof step !== "object") {
        throw new TypeError("Adapter returned an invalid step");
      }
      if (step.requiresConfirmation) {
        task.confirmation = {
          description: String(step.description ?? "Action needs approval").slice(0, 300),
        };
        task.state = GoalState.WAITING_CONFIRMATION;
        task.updatedAt = this.#clock();
        return this.get(id);
      }
      task.steps.push({
        index: task.steps.length,
        description: String(step.description ?? "Step completed").slice(0, 300),
        outcome: step.done ? "done" : "continued",
        completedAt: this.#clock(),
      });
      if (step.done) {
        task.state = GoalState.SUCCEEDED;
        task.result = step.result ?? null;
      } else if (task.steps.length >= task.maxSteps) {
        task.state = GoalState.FAILED;
        task.error = "Maximum step count reached";
      } else {
        task.state = GoalState.QUEUED;
      }
    } catch (error) {
      task.state = GoalState.FAILED;
      task.error = String(error?.message ?? error).slice(0, 500);
    }
    task.updatedAt = this.#clock();
    return this.get(id);
  }

  #publicTask(task) {
    return {
      id: task.id,
      goal: task.goal,
      maxSteps: task.maxSteps,
      state: task.state,
      createdAt: task.createdAt,
      updatedAt: task.updatedAt,
      steps: task.steps.map(step => ({ ...step })),
      result: task.result,
      error: task.error,
      confirmation: task.confirmation ? { ...task.confirmation } : null,
    };
  }
}
