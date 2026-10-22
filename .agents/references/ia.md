# Information Architecture: Artist & Concert Event Management Hub

**Branch**: `001-artist-event-management` | **Date**: 2026-10-15 | **Spec**: [spec.md](./spec.md) | **Status**: Approved
**Scope of this document**: WHAT routes exist, WHAT each owns, HOW they connect. Nothing about layout, components, or visual design (that is `ui/<route-id>.md`).

## 1. Object Inventory (from spec.md Key Entities)

| Object | Core attributes (from spec) | Relationships | Actions users perform (CTAs) |
|--------|-----------------------------|---------------|------------------------------|
| **User** | ID, OAuth identity, preferred language (JA/KO), profile info, onboarding completion state | has many FollowedArtists, has one NotificationSubscriptionConfig | Sign in (OAuth), complete onboarding (pick artists / skip), switch language, update notification preferences, sign out, delete account |
| **Artist** | ID, name, genre, nation, list of website/SNS URLs | has many Events, belongs to many Users (as followed artist) | Search artists, view artist metadata, view artist detail, follow / unfollow artist (auth required) |
| **Event (Concert)** | ID, associated artist(s), date, place, duration, ticketing URL, official announcement URL | belongs to one or more Artists | Search events, view event details, filter events (by artist/region), sort events (by d-day), view feed as list or month calendar, add to calendar (ICS), share, open ticketing page |
| **NotificationSubscription** | ID, user ID, followed artist notification toggles (new event, ticket open day-before/30min/at-open bundle, discography with priority 1>2>3), general weekly event update toggle, delivery channels (email, Kakao message) | belongs to one User | Configure subscriptions, opt-in/opt-out of notification channels and types (auth required) |

## 2. User Tasks (from spec.md User Stories)

| Task ID | Task (verb + object) | Source (US-#, FR-#) | Frequency | Entry point |
|---------|----------------------|---------------------|-----------|-------------|
| T-01 | Sign in with third-party OAuth (Google, Kakao) | US1, FR-001 | Rare | `/[locale]/login` |
| T-02 | Change application interface language (JP, KR) | US1, FR-002 | Rare | `/[locale]/(app)/settings` |
| T-03 | Search for artists and events using integrated keyword search | US2, US3, FR-003, FR-004, FR-012 | Daily | `/[locale]/(app)/search` |
| T-04 | Select and view artist detail from search results or feeds | US2, FR-003 | Daily | `/[locale]/(app)/search` |
| T-05 | Select and view event detail from search results or feeds | US3, US4, FR-004, FR-005 | Daily | `/[locale]/(app)/home` |
| T-06 | Follow or unfollow favorite artists (auth required) | US2, FR-003 | Weekly | `/[locale]/(app)/search` |
| T-07 | View and manage list of followed favorite artists in "My Artists" (auth required) | US2, FR-003, FR-010 | Weekly | `/[locale]/(app)/artists/followed` |
| T-08 | View future events feed by scope (Following for auth, Trending for all) with followed artists strip | US4, FR-005, FR-010, FR-012 | Daily | `/[locale]/(app)/home` |
| T-09 | Filter and sort future events feed (by artist, region, d-day) | US4, FR-006, FR-007 | Daily | `/[locale]/(app)/home` |
| T-10 | Configure artist-specific notification subscriptions (auth required) | US5, FR-008 | Rare | `/[locale]/(app)/settings` |
| T-11 | Configure general weekly event update subscriptions (auth required) | US6, FR-009 | Rare | `/[locale]/(app)/settings` |
| T-12 | Complete first-time onboarding by picking artists to follow, or skip (auth required) | US1, FR-011 | Rare | `/[locale]/(app)/onboarding` |
| T-13 | Sign out (auth required) | US1, FR-014 | Rare | `/[locale]/(app)/settings` |
| T-14 | Delete account permanently (auth required) | US1, FR-014 | Rare | `/[locale]/(app)/settings` |
| T-15 | Add event to calendar (ICS), share event, open ticketing page | US3, FR-013 | Daily | `/[locale]/(app)/events/[id]` |
| T-16 | View future events feed as month calendar | US4, FR-013 | Weekly | `/[locale]/(app)/home` |
| T-17 | Read terms of service and privacy policy | FR-015 | Rare | `/[locale]/(app)/legal/[doc]` |

## 3. Route Map

| Route ID | Path (App Router) | Route group / layout | Purpose (one sentence) | Primary object | Tasks served | Auth | Data read / mutated | URL state (params / searchParams) | Rendering intent | Links in → out |
|----------|-------------------|----------------------|------------------------|----------------|--------------|------|---------------------|-----------------------------------|------------------|----------------|
| R-01 | `/[locale]/login` | `(auth)` | Authenticate users via third-party OAuth providers (Google, Kakao) and redirect to onboarding (first-time users) or Home upon successful session creation. | User | T-01 | public | Read: OAuth auth providers / Mutate: User session creation | `?next=` | static / dynamic SSR | In: Root, Home (sign-in prompt), Gated routes / Out: Onboarding, Home, Legal |
| R-02 | `/[locale]/(app)/home` | `(app)` | Display the future events feed under explicit scopes (Following for authenticated users, Trending for everyone) with a followed artists strip, list/calendar views, filtering and sorting controls. | Event | T-05, T-08, T-09, T-16 | public | Read: Future events (following or trending), followed artists / Mutate: None | `?scope=` (following [authenticated only] / trending), `?view=` (list/calendar), `?month=`, `?artist=`, `?region=`, `?sort=` | dynamic SSR / client infinite scroll | In: Login, Onboarding, Nav / Out: Search, Event Detail, Artist Detail, My Artists, Login (Following scope for unauthenticated) |
| R-03 | `/[locale]/(app)/search` | `(app)` | Provide integrated keyword search for matching artists and events simultaneously with filtering options and artist following actions (prompting sign-in for unauthenticated visitors). | Artist, Event | T-03, T-06 | public | Read: Artists, Events / Mutate: Follow/unfollow artist (auth required) | `?q=`, `?type=` (all/artist/event), `?region=`, `?artist=` | dynamic SSR / client search with infinite scroll | In: Nav, Home, My Artists / Out: Artist Detail, Event Detail, My Artists |
| R-04 | `/[locale]/(app)/artists/[id]` | `(app)` | Display comprehensive artist profile metadata (genre, nation, website/SNS URLs), associated upcoming events, and follow/unfollow actions (prompting sign-in for unauthenticated visitors). | Artist | T-04, T-06 | public | Read: Artist details, associated events / Mutate: Follow/unfollow artist (auth required) | Route param: `id` | dynamic SSR | In: Search, Home, My Artists, Onboarding / Out: Event Detail |
| R-05 | `/[locale]/(app)/events/[id]` | `(app)` | Display complete event detail including participating artist(s), date, place, duration, ticketing URL, and official announcement URL, with add-to-calendar (ICS), share, and open-ticketing actions. | Event | T-05, T-15 | public | Read: Event details / Mutate: None | Route param: `id` | dynamic SSR | In: Home, Search, Artist Detail / Out: Artist Detail, external ticketing and announcement URLs |
| R-06 | `/[locale]/(app)/artists/followed` | `(app)` | "My Artists" hub: manage the authenticated user's personal list of favorite followed artists with quick unfollow actions and navigation to artist detail. | Artist | T-06, T-07 | public (sign-in prompt in-page) | Read: User followed artists list / Mutate: Unfollow artist | None | dynamic SSR / client infinite scroll | In: Nav, Home (followed artists strip) / Out: Artist Detail, Search |
| R-07 | `/[locale]/(app)/settings` | `(app)` | Manage global application language preferences, notification subscriptions for followed artists (priorities 1>2>3) and general weekly updates, sign-out, and account deletion. | User, NotificationSubscription | T-02, T-10, T-11, T-13, T-14 | public (notification and account sections prompt sign-in in-page) | Read: User profile, notification config / Mutate: Language preference, notification subscriptions, user session (sign-out), user account and associated data (delete) | None | dynamic SSR | In: Nav / Out: Home, Legal |
| R-08 | `/[locale]/(app)/onboarding` | `(app)` | Guide first-time authenticated users through picking artists to follow (skippable) before reaching Home. | Artist, User | T-12, T-06 | public (sign-in prompt in-page) | Read: Suggested/trending artists / Mutate: Follow artist, onboarding completion state | None | dynamic SSR / client search with infinite scroll | In: Login (first-time users) / Out: Home |
| R-09 | `/[locale]/(app)/legal/[doc]` | `(app)` | Display terms of service and privacy policy documents. | — | T-17 | public | Read: Static legal content / Mutate: None | Route param: `doc` (terms/privacy) | static | In: Login, Settings, Nav footer / Out: Back to previous route |

### 3.1 Layout & Segment Tree

```text
app/
└── [locale]/
    ├── not-found.tsx         # Localized not-found page (nonexistent artist/event/page), links back to Home
    ├── login/
    │   └── page.tsx          # R-01: (auth) OAuth Login
    └── (app)/
        ├── layout.tsx        # App shell (Navigation, Language switcher)
        ├── home/
        │   └── page.tsx      # R-02: Future Events Feed (Following / Trending scopes)
        ├── search/
        │   └── page.tsx      # R-03: Integrated Artist & Event Search
        ├── artists/
        │   ├── [id]/
        │   │   └── page.tsx  # R-04: Artist Profile / Detail View
        │   └── followed/
        │       └── page.tsx  # R-06: My Artists (Public; sign-in prompt in-page)
        ├── events/
        │   └── [id]/
        │       └── page.tsx  # R-05: Event Detail View
        ├── onboarding/
        │   └── page.tsx      # R-08: First-time artist picking (Public; sign-in prompt in-page)
        ├── legal/
        │   └── [doc]/
        │       └── page.tsx  # R-09: Terms of Service / Privacy Policy
        └── settings/
            └── page.tsx      # R-07: Global Settings, Notifications & Account (Public; notification/account sections gated in-page)
```

### 3.2 Shared Concerns (cross-route)

- **Navigation model**: Persistent top or bottom navigation bar available on all `(app)` routes with four primary destinations: Home (`R-02`), Search (`R-03`), My Artists (`R-06`), and Settings (`R-07`), plus language switcher and user profile/sign-out (or sign-in CTA for unauthenticated visitors). Legal pages (`R-09`) are reachable from a footer or menu link in the shell, Login, and Settings.
- **Locale handling**: Enforced via `[locale]` segment (`ja`, `ko`). Language switcher in global layout updates locale segment / user preference cookie and reloads/redirects.
- **Auth boundary**: `(auth)` group (`R-01`) is public and redirects authenticated users to Home. `R-02`, `R-03`, `R-04`, `R-05`, `R-09` are publicly accessible to both authenticated and unauthenticated visitors (with unauthenticated visitors viewing the Trending scope and prompted to sign in when attempting auth-critical actions like following artists or selecting the Following scope). All `(app)` routes are public and use one uniform pattern: auth-only content (My Artists `R-06`, Onboarding `R-08`, and the notification and account sections of Settings `R-07`) shows an in-page sign-in prompt (leading to `R-01` with `?next=`) to unauthenticated visitors instead of redirecting. Routes never redirect unauthenticated visitors to Login; Login (`R-01`) is reached only through these sign-in prompts or after an OAuth failure.
- **Post-login routing**: After successful sign-in, first-time users (onboarding not completed) go to Onboarding (`R-08`); returning users go to the `?next=` target or Home (`R-02`). Skipping or completing Onboarding marks it complete and leads to Home.
- **Feed scope defaults**: Home (`R-02`) defaults to `?scope=following` for authenticated users and `?scope=trending` for unauthenticated visitors. There is no global "all events" scope. `?scope=following` is never valid for unauthenticated visitors: a direct hit, shared link, or invalid/missing value is canonicalized (server-side, other params preserved) to `?scope=trending`, and selecting the Following scope in the UI opens the sign-in prompt (with `?next=` set to the Following URL) without changing the URL.
- **Filter/sort/scope state convention**: All scope, view, filter, sort, search query, and type parameters (`?scope=`, `?view=`, `?month=`, `?q=`, `?type=`, `?artist=`, `?region=`, `?sort=`) are stored in URL `searchParams` to ensure shareability, deep linking, and browser back/forward support. Entity IDs use dynamic route segments (`[id]`).
- **Long lists**: Home feed, Search results, My Artists, and Onboarding suggestions use cursor-based infinite scroll; the cursor is not part of the URL. Scroll position and loaded items are restored when returning from a detail route (back navigation).
- **Global states needing a route-level answer**:
  - *Empty account (zero favorite artists for auth users, e.g. onboarding skipped)*: Home (`R-02`, Following scope) displays empty state prompting the user to discover and add favorite artists via Search (`R-03`).
  - *Expired session*: Treated as signed out on every route. The user sees a localized "session expired" notice, Home (`R-02`) resolves to the Trending scope, and auth-only content shows the in-page sign-in prompt (with `?next=` preserved). No automatic redirect to Login.
  - *Backend API failure*: Displays localized error banners and rolls back optimistic UI state (e.g. follow/unfollow toggle or notification toggle).
  - *Not found*: Nonexistent `[id]` or `[doc]` values and unknown paths resolve to the localized `not-found.tsx` with navigation back to Home (`R-02`).
  - *Signed out*: Sign-out ends the session and returns the user to Home (`R-02`, Trending scope). Account deletion (confirmed in `R-07`) removes the user's data and ends the session the same way.

## 4. Traceability Matrix

| Requirement | Covered by route(s) | Notes |
|-------------|--------------------|-------|
| FR-001 | R-01 | OAuth authentication via Google and Kakao |
| FR-002 | R-07 | Global settings page for JA/KO language configuration |
| FR-003 | R-03, R-04, R-06 | Integrated artist search, viewing artist detail (`R-04`), follow/unfollow (auth required), managed followed list |
| FR-004 | R-03, R-05 | Integrated event search and viewing event detail (`R-05`) |
| FR-005 | R-02, R-05 | Aggregated feed of future events (Following for auth, Trending for all) and viewing event detail (`R-05`) |
| FR-006 | R-02 | Filtering events feed by specific artist and region (Japan, Korea) |
| FR-007 | R-02 | Sorting events feed by d-day / chronological proximity |
| FR-008 | R-07 | Artist-specific notification subscriptions with priority 1 > 2 > 3 (new event, ticket open bundle, discography) |
| FR-009 | R-07 | General weekly event update notification subscriptions |
| FR-010 | R-02, R-06 | Home with Following/Trending scopes and followed artists strip; My Artists as primary destination |
| FR-011 | R-08, R-01 | Optional first-time onboarding after sign-in |
| FR-012 | R-02, R-03, R-06, R-08 | Cursor-based infinite scroll with scroll position restoration on feed, search results, and artist lists |
| FR-013 | R-05, R-02 | Add to calendar (ICS), share, open ticketing page on event detail; month calendar view of the feed |
| FR-014 | R-07 | Sign-out and permanent account deletion |
| FR-015 | R-09, `not-found.tsx` | Terms/privacy pages and localized not-found page |

**Orphan check**: Requirements with no route: none. Routes with no requirement: none.

## 5. Decision Log

| # | Decision | Alternatives considered | Why this one | Reversal cost |
|---|----------|-------------------------|--------------|---------------|
| D-1 | Integrated Search Route (`R-03`) combining artists and events with a filter toggle ("all", "artist only", "event only") | Separate search pages for artists and events | Fulfills user requirement that artist and event search be integrated into a single route with default dual display and type filters, streamlining user discovery workflow. | medium |
| D-2 | Public access for Home Feed (`R-02`), Search (`R-03`), and Detail Views (`R-04`, `R-05`) with globally unique trending/recommended feed for unauthenticated users | Restricting all core features exclusively to authenticated users | Broadens product discovery and accessibility for visitors while safely gating auth-critical actions (follow/unfollow, notifications) behind OAuth login. | medium |
| D-3 | Dedicated Artist Detail Route (`R-04`) at `/[locale]/artists/[id]` | Showing artist details solely in modals or search cards | Allows shareable deep-linking to specific artists, displaying full metadata and associated upcoming events independently. | low |
| D-4 | Dedicated Event Detail Route (`R-05`) at `/[locale]/events/[id]` | Showing event details solely in feed expansion or modals | Provides a robust, standalone page for comprehensive concert details (ticketing URLs, official announcements, duration, venue info). | low |
| D-5 | Dedicated "My Artists" Route (`R-06`) as a primary navigation destination, separate from Search | Managing followed artists solely inside search or Home; leaving it as a secondary page | Following is the core engagement loop, so the followed collection gets a tab-level hub for review and management without re-running searches. | low |
| D-6 | Notification settings co-located in Global Settings (`R-07`) | Dedicated notification center sub-route | Keeps user preferences (language + notification channels + subscription priorities) unified in one settings hub. | low |
| D-7 | URL `searchParams` for all filters, sorting, scopes, and search queries | Client-only component state | Enables shareable deep links, browser history navigation, and stateless server rendering hydration. | med |
| D-8 | Home (`R-02`) with explicit `?scope=` of Following and Trending, plus a followed artists strip; no "All" scope | Silently swapping the feed by auth state; adding an "All" events scope | Explicit scopes make the feed predictable and shareable; an "All" scope over the whole database is impractical and would not be used. | medium |
| D-9 | Dedicated skippable Onboarding Route (`R-08`) shown once after first sign-in | Landing first-time users on an empty Home | Avoids an empty first experience and seeds the Following scope and notifications. | low |
| D-10 | Cursor-based infinite scroll with scroll restoration for long lists (cursor not in URL) | Page-based pagination with `?page=` | Matches the modern mobile app interaction model; filters remain shareable while the cursor stays internal. | low |
| D-11 | Calendar view as a `?view=calendar` mode of Home (`R-02`) rather than a separate route | Dedicated calendar route | Reuses the same feed, scopes, and filters; one route owns the feed. | low |
| D-12 | Sign-out and account deletion live in Settings (`R-07`) as actions, not separate routes; Legal (`R-09`) is a single dynamic route | Separate sign-out/delete routes; one route per legal document | Keeps account lifecycle in the existing preferences hub and avoids route sprawl. | low |
| D-13 | Uniform in-page auth prompting: all `(app)` routes are public; auth-only content (`R-06`, `R-08`, notification/account sections of `R-07`) shows a sign-in prompt instead of redirecting | Auth-only routes that redirect to Login | Consistent with the public-first, gate-on-action model (D-2); one pattern everywhere, tabs never bounce signed-out users, and signed-out users can still change language and see what each area offers. | low |
| D-14 | Home scope is resolved on the server from session + `?scope=`; unauthenticated `?scope=following` is canonicalized to trending, and the backend feed endpoint also rejects Following without a session | Client-only guard; blocking the URL with a redirect to Login | Keeps one source of truth, no content flash, shareable links degrade gracefully, and UI guards alone are never the security boundary. | low |

## 6. Open Questions (max 5, each with a recommended default)

1. **Default search scope when keyword is empty**: Should visiting Search (`R-03`) with no keyword show trending/recent artists and events or an empty prompt?
   * *Recommended default*: Show trending or recently added featured artists and upcoming events as initial suggestions.
   * *Impact if wrong*: Minimal; users can immediately type their keyword.

## 7. Route Status (maintained by /speckit.ui)

| Target | File | Status (pending / drafted) |
|--------|------|----------------------------|
| shared | `ui/_shared.md` | drafted |
| R-01 | `ui/R-01-login.md` | drafted |
| R-02 | `ui/R-02-home.md` | drafted |
| R-03 | `ui/R-03-search.md` | drafted |
| R-04 | `ui/R-04-artist-detail.md` | drafted |
| R-05 | `ui/R-05-event-detail.md` | drafted |
| R-06 | `ui/R-06-my-artists.md` | drafted |
| R-07 | `ui/R-07-settings.md` | drafted |
| R-08 | `ui/R-08-onboarding.md` | drafted |
| R-09 | `ui/R-09-legal.md` | drafted |