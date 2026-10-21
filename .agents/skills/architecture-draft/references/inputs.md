# Required Inputs and Questioning Rules

## Blocking inputs (draft cannot start without these)

| # | Input | Enough detail looks like |
|---|-------|--------------------------|
| B1 | **Idea and core use cases** | One-sentence purpose + the 3-5 most important user actions |
| B2 | **Expected scale** | Users or requests now and at ~12-24 months (a range is fine) |
| B3 | **Traffic pattern** | Average vs peak, spikes (events, time of day), read/write ratio |
| B4 | **Data profile** | Main things stored, rough size per item, growth rate, how long kept |
| B5 | **Service targets** | Acceptable response time for key actions + tolerance for downtime |

## Important, but can be assumed (flag as `[assumed]`)

| # | Input | Default if missing |
|---|-------|--------------------|
| A1 | Correctness-critical operations (money, stock, bookings, anything that must never double-apply or be lost) | Assume none, but list as open question |
| A2 | Hard constraints: compliance, data residency, budget level, team size, deadline, existing systems that must be integrated | Assume none |
| A3 | Users' geography | Single region |
| A4 | Who/what calls the system (web/mobile clients, partner systems, devices) | Interactive clients only |
| A5 | Background or slow work (reports, media processing, notifications, imports) | Assume some exists if the idea implies it |
| A6 | Multi-tenancy / account separation needs | Single tenant |
| A7 | Stack already mandated | None; record as given if stated |

## Rough bands (offer these when the user "doesn't know")

- Scale: `<1k users` / `1k-100k` / `100k-10M` / `>10M`
- Peak vs average: `~2x steady` / `5-10x daily peaks` / `>10x spiky events`
- Read:write: `mostly reads (>90%)` / `mixed` / `write-heavy`
- Data growth: `<10 GB/yr` / `10 GB-1 TB/yr` / `>1 TB/yr`
- Response time: `<200 ms` / `<1 s` / `seconds are fine`
- Downtime tolerance: `hours OK` / `minutes OK` / `near-zero`

## How to ask

1. **One message, grouped.** Ask all missing blocking items together, max 6 questions.
2. **Quote what you already have** in one line so the user sees what was understood.
3. **Make answering cheap**: for each question, give the rough bands above as options.
   The user can answer "B, mixed, 5-10x" in a few words.
4. **Never ask about technology** (stack, vendors, databases). That is out of scope.
5. **Stop after asking.** Do not draft "in the meantime".
6. If still blocking after one round, ask once more for only the remaining items.
   If the user declines, proceed with assumptions tagged `[assumed]`.

### Question template

> I have: <one-line summary of what was given>.
> To size a first draft I still need:
> 1. <missing item>: <options>
> 2. ...
> Rough answers are fine, and ranges work. Say "assume" for any item and I'll
> use a default and flag it.
