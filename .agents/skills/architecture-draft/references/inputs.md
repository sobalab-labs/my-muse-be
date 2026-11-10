# Input Handling and Questioning Rules

`input-template.md` is the single definition of what inputs exist and which are
required. This file says what counts as explicit input, how to default, and how to ask.

## Valid input sources (only these)

1. **Filled form:** `docs/architecture/architecture-input.md` or a file the user names.
2. **Explicit chat input:** what the user states in the message that invoked the skill,
   or in replies to the skill's questions.

Not valid: earlier conversation, other repo files, your own guesses, or "reasonable"
inferences. You may *suggest* a value taken from earlier conversation, but it counts
only after the user confirms it.

A form that is blank, or that still has unticked boxes and empty answers in required
sections, is not a filled form.

## Required vs optional

- **Required** = sections 1-5 of the template (idea, scale, traffic pattern, data,
  service targets). The draft cannot start without them.
- **Optional** = section 6. If empty, apply the defaults below and tag `[assumed]`.

| Optional field | Default if empty |
|---|---|
| Never-lose / never-twice operations | None assumed; list as open question |
| Slow or background work | Assume some exists if the idea implies it |
| Callers | Interactive clients only |
| Geography | Single region |
| Tenancy | Single tenant |
| Hard constraints | None |
| Stack mandated | None; record as given if stated |

## What counts as "filled"

- A ticked option, a range, or an exact number the user provided: `[given]`.
- **Unknown** ticked or "assume" said by the user: `[assumed]`. Offer a sensible
  default from the template's bands, and list it in Open Questions.
  Never tick Unknown or say "assume" on the user's behalf.
- Blank, or free text too vague to size (e.g. "lots of users"): missing.
- Anything you inferred: missing.

## No invented specifics

Do not introduce precise figures the user did not give (retention periods, user
counts, latency numbers, percentages) as if they were requirements. If one seems
needed, put it in Open Questions or use a labelled `[assumed]` band from the template.

## When there is no usable input

Do not draft and do not size. Offer two options and wait:
- fill in the form (copy `input-template.md` to `docs/architecture/architecture-input.md`
  and stop), or
- answer questions in chat.

## How to ask (chat path)

1. **One message, grouped.** Ask all missing required items together, max 6 questions.
2. **Quote what the user has explicitly said** in one line so they see what was understood.
3. **Make answering cheap**: offer the template's options for each question.
   The user can answer "B, mixed, 5-10x".
4. **Never ask about technology** (stack, vendors, databases). Out of scope.
5. **Stop after asking.** Do not draft "in the meantime".
6. If items are still missing after one round, ask once more for only those.
   If the user declines, proceed only if they say "assume"; mark those `[assumed]`.
   Otherwise stop.
7. **Save the answers.** Once the required fields are covered, write them into
   `docs/architecture/architecture-input.md` using the template layout, so the input is
   versioned and the draft can be re-run later.

## File path (form path)

If required fields are empty in the file, do not draft. Name the empty fields and ask
the user to either fill them in the file or answer in chat.

### Question template

> I have: <one-line summary of what the user explicitly said>.
> To size a first draft I still need:
> 1. <missing item>: <options>
> 2. ...
> Rough answers are fine, and ranges work. Say "assume" for any item and I'll
> use a default and flag it.