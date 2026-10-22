# Transaction Scenarios: Artist & Concert Management Hub

## 1. Introduction
This document outlines the critical transaction scenarios for the Artist & Concert Management Hub. It bridges the logical data model defined in `data-model.md` and the behavioral requirements described in `ia.md` and `openapi.yaml`, ensuring that data integrity and consistency are maintained across core business workflows.

## 2. Transaction Flow Definitions

### 2.1 User Onboarding (Registration)
*   **Trigger**: User signs in via OAuth (`/auth/login`) for the first time.
*   **Logical Steps**:
    1.  Create `User` entry (if not exists) based on `oauthProvider` and `oauthId`.
    2.  Initialize `NotificationSubscription` for the `user_id`.
    3.  Initialize `ArtistSubscriptionPreferences` for the `user_id`.
    4.  *(Optional)* Create `UserFollowedArtist` associations if artists were selected during onboarding (`/users/me/onboarding`).
*   **Invariants**: `User` uniqueness on `(oauthProvider, oauthId)` must be enforced.

### 2.2 Artist Follow/Unfollow
*   **Trigger**: User initiates follow/unfollow action (`/users/me/followed-artists`).
*   **Follow Steps**:
    1.  Validate `artist_id` existence.
    2.  Insert `UserFollowedArtist` record (`user_id`, `artist_id`).
*   **Unfollow Steps**:
    1.  Delete `UserFollowedArtist` record for the given `(user_id, artist_id)`.
*   **Invariants**: `UserFollowedArtist` must have `UNIQUE` constraint on `(user_id, artist_id)` to prevent duplicate entries.

### 2.3 Notification Configuration Update
*   **Trigger**: User updates notification settings (`/users/me/notification-subscription`).
*   **Logical Steps**:
    1.  Update `NotificationSubscription` (email/kakao channels, weekly updates).
    2.  Update `ArtistSubscriptionPreferences` (per-artist toggles for concerts/tickets/discography).
*   **Invariants**: `NotificationSubscription` and `ArtistSubscriptionPreferences` must maintain 1:1 relationship with `User` (`UNIQUE` constraint on `user_id`).

### 2.4 Concert Querying (Feed Generation)
*   **Trigger**: User views Home (`/events` with `scope=following` or `trending`).
*   **Logical Steps**:
    1.  If `scope=following` (Auth Required): Join `UserFollowedArtist` -> `ConcertArtist` -> `Concert` to fetch concerts for followed artists.
    2.  If `scope=trending`: Fetch top concerts based on defined trending algorithms.
    3.  Apply `region` and `artist_id` filters if provided.
*   **Invariants**: `Concert` entity remains source of truth; no `region` denormalization into `Concert`.

### 2.5 Account Deletion
*   **Trigger**: User deletes account (`/users/me` [DELETE]).
*   **Logical Steps**:
    1.  Delete `User` record.
    2.  Cascade/Clean up associated records:
        *   `NotificationSubscription`
        *   `ArtistSubscriptionPreferences`
        *   `UserFollowedArtist`
        *   `UserLikeConcert`
*   **Invariants**: All foreign keys must enforce `ON DELETE CASCADE` or ensure proper application-level cleanup to maintain referential integrity.
