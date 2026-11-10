---
name: architecture-draft
description: Produce an initial DRAFT of a technology-neutral architectural design document, but only from explicit user input - either a filled input form or a clearly stated idea with constraints (expected traffic, traffic pattern, data size, latency and availability targets). Use when the user wants a first-pass architecture outline or component breakdown for a new idea. Never infers requirements from earlier conversation; stops and asks when required constraints are missing. Does NOT choose technologies, products, frameworks, or cloud services, and does NOT produce a complete or final architecture.
---

# Architecture Draft

Turn an idea and its constraints into a short, **initial draft** architecture document
using only generic component types (API service, database, message queue, ...).

## Scope rules (read first)

- **Explicit input only.** The core logic (sizing and drafting) runs only on input the
  user has explicitly given for this skill (see Step 1). Earlier conversation, repo
  contents, and your own inferences are not input.
- **Draft only.** The output is a starting point for discussion, not a complete or
  validated architecture. Say so in the document.
- **No technology choices.** Never name products, languages, frameworks, cloud
  services, or vendors (no "PostgreSQL", "Spring Boot", "SQS", "Kafka", "Redis").
  Use generic component types from `references/components.md`, and state the
  *required characteristics* of a component instead (e.g. "database that supports
  transactions across order and payment records"). Do not name a database family
  (e.g. "relational") unless the user's input requires it. If the user names a stack,
  record it under constraints as given; do not evaluate or build on it.
- **No invented numbers.** Every figure is tagged `[given]`, `[derived]`, or
  `[assumed]`. A specific figure (a duration, a count, a percentage) may appear as a
  requirement only if the user stated it. Otherwise phrase it as an open question.
- **Stay short.** Target 2-3 pages. Depth belongs in later work.

## Files

| Path | Purpose |
|---|---|
| `docs/architecture/architecture-input.md` | The user's filled input form (read; created if answers come from chat) |
| `docs/architecture/architecture-draft.md` | The output of this skill |
| `references/input-template.md` | Blank input form, the single definition of required fields |
| `references/inputs.md` | What counts as explicit input, defaults, and how to ask |
| `references/sizing.md` | Rough sizing method |
| `references/components.md` | Generic component vocabulary and pattern hints |
| `references/output-template.md` | Structure of the draft |

The user may name different paths; use theirs.

## Workflow

### Step 1: Gate - is there explicit input?

Exactly two sources count as input:

- **A. Filled form:** `docs/architecture/architecture-input.md` (or the file the user
  names), with required fields filled in by the user.
- **B. Explicit chat input:** an idea and constraints the user states in the message
  that invoked this skill, or in replies to this skill's questions.

Nothing else counts. If earlier conversation seems relevant, you may only *suggest* a
value ("Earlier you mentioned X. Use it?"); it counts only after the user confirms.

If neither A nor B contains a clear idea, STOP. Do not draft and do not start sizing.
Offer two options and wait: fill in the form (copy `references/input-template.md` to
`docs/architecture/architecture-input.md`), or answer questions in chat.

### Step 2: Gate - are the required fields explicit?

Check required fields (template sections 1-5) against `references/inputs.md`.

- A required field counts only if the user explicitly stated it, in the form or in chat.
  Anything you would have to infer is missing.
- If **any required field is missing or too vague to size anything**, STOP. Do not
  draft. Ask only for the missing items, following `references/inputs.md`, then wait.
- "Unknown" or "assume" counts only if the user said or ticked it. Never choose it
  on their behalf. Such fields become `[assumed]` and go in Open Questions.
- Optional fields (section 6) may be defaulted as `[assumed]`.

Sizing and drafting must not begin until both gates pass.

### Step 3: Record and echo the input

- If input came from chat (source B), save it to `docs/architecture/architecture-input.md`
  using the template layout.
- Before drafting, show a compact table in chat: each required field, its value, and
  `[given]` or `[assumed]`. No confirmation is needed; this lets the user catch
  misreadings.

### Step 4: Size roughly

Use `references/sizing.md` to derive peak request rate, read/write split, data
volume, and growth. Show the arithmetic in one line each. Keep it order-of-magnitude.

### Step 5: Draft the document

Fill `references/output-template.md`. Use the sizing results and the pattern hints
in `references/components.md` to decide which generic components are needed and
**why each one earns its place**. Prefer the fewest components that satisfy the
constraints; if a simple single deployable unit would do at this scale, say so.

### Step 6: Deliver

- Save to `docs/architecture/architecture-draft.md` (create the folder if needed).
- In chat, reply with 3-5 lines: what was drafted, the top 3 assumptions, and the
  most important open question. Do not paste the whole document.

## Self-check before delivering

- Every required field traces to something the user explicitly provided or confirmed.
- No product, framework, language, or vendor names appear (except under "constraints as given").
- Every number has a tag; every `[assumed]` appears in Open Questions; no specific
  requirement figure appears that the user did not state.
- Every component in the diagram appears in the component table with a reason.
- The document states it is an initial draft and lists what it does not cover.