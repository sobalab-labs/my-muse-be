# Architecture Input

> Fill this in and save as `docs/architecture/architecture-input.md`.
> Rough answers and ranges are fine. Tick one box per question, and add an exact number
> in the notes if you have one. If you don't know, tick **Unknown** and the draft will
> use a flagged assumption. Fields marked **(required)** must not be left empty.

## 1. Idea (required)
**What does the system do, and for whom? (1-2 sentences)**


**Top 3-5 user actions:**
1.
2.
3.

## 2. Scale (required)
**Users or requests today:** [ ] <1k  [ ] 1k-100k  [ ] 100k-10M  [ ] >10M  [ ] Unknown
**In 12-24 months:** [ ] <1k  [ ] 1k-100k  [ ] 100k-10M  [ ] >10M  [ ] Unknown
Exact figures / notes:

## 3. Traffic pattern (required)
**Peak vs average load:** [ ] ~2x steady  [ ] 5-10x daily peaks  [ ] >10x spiky events  [ ] Unknown
**Read vs write:** [ ] mostly reads (>90%)  [ ] mixed  [ ] write-heavy  [ ] Unknown
When do peaks happen (time of day, events, campaigns)?


## 4. Data (required)
**What are the main things stored?** (e.g. users, orders, messages, photos)


**Growth per year:** [ ] <10 GB  [ ] 10 GB-1 TB  [ ] >1 TB  [ ] Unknown
**Large files (images, video, documents)?** [ ] No  [ ] Yes, roughly:
**How long must data be kept?**

## 5. Service targets (required)
**Response time for key actions:** [ ] <200 ms  [ ] <1 s  [ ] seconds are fine  [ ] Unknown
**Downtime tolerance:** [ ] hours OK  [ ] minutes OK  [ ] near-zero  [ ] Unknown

## 6. Optional (leave blank to use defaults)
**Operations that must never be lost or applied twice** (payments, stock, bookings):

**Slow or background work** (reports, media processing, notifications, imports):

**Who or what calls the system** (web/mobile clients, partner systems, devices):

**Users' geography** (single region, several, global):

**Separate customer accounts / tenants needed?** [ ] No  [ ] Yes:

**Hard constraints** (compliance, data residency, budget, team size, deadline, existing systems):

**Stack already mandated** (recorded as given, not evaluated):
