---
name: architecture-draft
description: Produce an initial DRAFT of a technology-neutral architectural design document from a product idea plus constraints (expected traffic, traffic pattern, data size, latency and availability targets). Use when the user wants a first-pass architecture outline, component breakdown, or "how should this system be structured" for a new idea. Pauses and asks the user when required constraints are missing. Does NOT choose technologies, products, frameworks, or cloud services, and does NOT produce a complete or final architecture.
---

# Architecture Draft

Turn an idea and its constraints into a short, **initial draft** architecture document
using only generic component types (API service, database, message queue, ...).

## Scope rules (read first)

- **Draft only.** The output is a starting point for discussion, not a complete or
  validated architecture. Say so in the document.
- **No technology choices.** Never name products, languages, frameworks, cloud
  services, or vendors (no "PostgreSQL", "Spring Boot", "SQS", "Kafka", "Redis").
  Use generic component types from `references/components.md`, and state the
  *required characteristics* of a component instead (e.g. "database with strong
  consistency for orders"). If the user names a stack, record it under constraints
  as given; do not evaluate or build on it.
- **No invented numbers.** Every figure is tagged `[given]`, `[derived]`, or
  `[assumed]`. Never present an assumption as a fact.
- **Stay short.** Target 2-3 pages. Depth belongs in later work.

## Workflow

### Step 1: Gap check (blocking)

Read the user's input and compare it with the checklist in `references/inputs.md`.

- If **any blocking input is missing or too vague to size anything**, STOP.
  Do not draft. Ask for the missing items in one message, following the
  questioning rules in `references/inputs.md`, then wait for the answer.
- If only non-blocking inputs are missing, continue and record them as
  `[assumed]` with a reason.
- If the user says they don't know a number, offer the rough bands in
  `references/inputs.md` and accept a range. A range is enough.
- If the user says "just assume" or "use defaults", proceed, and mark every
  filled gap `[assumed]` and list it in Open Questions.

Ask again only for what is still blocking; never re-ask for things already answered.

### Step 2: Size roughly

Use `references/sizing.md` to derive peak request rate, read/write split, data
volume, and growth. Show the arithmetic in one line each. Keep it order-of-magnitude.

### Step 3: Draft the document

Fill `references/output-template.md`. Use the sizing results and the pattern hints
in `references/components.md` to decide which generic components are needed and
**why each one earns its place**. Prefer the fewest components that satisfy the
constraints; if a simple single deployable unit would do at this scale, say so.

### Step 4: Deliver

- Save as Markdown at the path the user gave, otherwise `docs/architecture-draft.md`.
- In chat, reply with 3-5 lines: what was drafted, the top 3 assumptions, and the
  most important open question. Do not paste the whole document.

## Self-check before delivering

- No product, framework, language, or vendor names appear (except under "constraints as given").
- Every number has a tag, and every `[assumed]` appears in Open Questions.
- Every component in the diagram appears in the component table with a reason.
- The document states it is an initial draft and lists what it does not cover.
