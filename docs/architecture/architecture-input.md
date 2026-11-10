# Architecture Input

> Fill this in and save as `docs/architecture/architecture-input.md`.
> Rough answers and ranges are fine. Tick one box per question, and add an exact number
> in the notes if you have one. If you don't know, tick **Unknown** and the draft will
> use a flagged assumption. Fields marked **(required)** must not be left empty.

## 1. Idea (required)
**What does the system do, and for whom? (1-2 sentences)**
(1) given list of sources (e.g. artists' official websites, instagrams, ...etc) for every artists in database, regurarly (e.g. once a day) scrape the sources to check if new concert announcement is identified. (2) when a new concert announcement announces a concert that's new to the system, immediately send emails and kakao messages to the users who requested the alert and followed one of the artists that participates that system. 


**Top 3-5 user actions:**
1. Gets the email or kakao message or both for the new concert where following artist participates 
2. View list of concerts that following artist participates, aggregated for all following artists.
3. Do general search against artist or concert database.

## 2. Scale (required)
**Users or requests today:** [X] <1k  [ ] 1k-100k  [ ] 100k-10M  [ ] >10M  [ ] Unknown
**In 12-24 months:** [X] <1k  [ ] 1k-100k  [ ] 100k-10M  [ ] >10M  [ ] Unknown
Exact figures / notes:

## 3. Traffic pattern (required)
**Peak vs average load:** [ ] ~2x steady  [X] 5-10x daily peaks  [ ] >10x spiky events  [ ] Unknown
**Read vs write:** [X] mostly reads (>90%)  [ ] mixed  [ ] write-heavy  [ ] Unknown
When do peaks happen (time of day, events, campaigns)?
The scraping of new concert announcement and possible new concert identification is regurlary done, and when new concert is identified, we must (1) send the emails and messages, and (2) users are expected to immediately view the data as they recieve email or message, so that's highly focused peak. Users only read, but the scraper and messengers should write maybe.


## 4. Data (required)
**What are the main things stored?** (e.g. users, orders, messages, photos)
Refer to `../data-model/data-model.md` (**relative to this file `architecture-input.md`)

**Growth per year:** [X] <10 GB  [ ] 10 GB-1 TB  [ ] >1 TB  [ ] Unknown
**Large files (images, video, documents)?** [ ] No  [ ] Yes, roughly: Unknown
**How long must data be kept?** Unknown

## 5. Service targets (required)
**Response time for key actions:** [ ] <200 ms  [X] <1 s  [ ] seconds are fine  [ ] Unknown
Response time for new concert identification and messaging isn't critical.
**Downtime tolerance:** [ ] hours OK  [X] minutes OK  [ ] near-zero  [ ] Unknown

## 6. Optional (leave blank to use defaults)
**Operations that must never be lost or applied twice** (payments, stock, bookings):
user must get 1 message per a new concert of following artist identified, no more and no less.

**Slow or background work** (reports, media processing, notifications, imports):
the regurar source scraping and messaging are background works.

**Who or what calls the system** (web/mobile clients, partner systems, devices):
Users access through mobile web (80%), desktop web (20%)

**Users' geography** (single region, several, global):
Mostly Japan and Korea only

**Separate customer accounts / tenants needed?** [ ] No  [ ] Yes:

**Hard constraints** (compliance, data residency, budget, team size, deadline, existing systems):

**Stack already mandated** (recorded as given, not evaluated):  the user-facing interface (e.g. follow/unfollowing artists, configuring email / kakao message request settings...) should be fixed as spring boot api server implementation
