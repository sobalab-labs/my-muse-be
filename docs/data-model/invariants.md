# Database Invariants: Artist & Concert Management Hub

## 1. Introduction
This document defines the data invariants—the immutable rules and constraints—that must hold true at the database schema level. These invariants define the logical data model and dictate the constraints required for the physical database implementation to ensure data integrity and consistency.

## 2. Entity-Level Invariants

### 2.1 Identity & Access
- **User Uniqueness**: `User` must have a `UNIQUE` constraint on the tuple `(oauthProvider, oauthId)`.
- **Locale Constraint**: `User.preferred_locale` must be `NOT NULL` with a `CHECK` constraint validating values `('ko', 'ja')`.

### 2.2 Domain Integrity
- **Artist Integrity**: `Artist.name` must be `NOT NULL`.
- **Venue Integrity**: `Venue.name` and `Venue.type` must be `NOT NULL`.
- **Concert Integrity**: 
    - `Concert.title`, `Concert.date`, and `Concert.official_announcement_url` must be `NOT NULL`.
    - `Concert.venue_id` must be `NOT NULL` (enforcing strict binding to a `Venue`).

### 2.3 Preferences & Configuration
- **Subscription Singletons**: `NotificationSubscription` and `ArtistSubscriptionPreferences` must have a `UNIQUE` constraint on `user_id` (enforcing a 1:1 relationship with `User`).

## 3. Relational & Business Invariants

### 3.1 Referential Integrity
- **Foreign Keys**: All foreign keys (e.g., `Concert.venue_id`, `ArtistSNS.artist_id`, `UserFollowedArtist.user_id`) must have `NOT NULL` constraints where applicable and implement `ON DELETE CASCADE` or `ON DELETE RESTRICT` based on ownership rules.
- **Concert-Artist Binding**: The `ConcertArtist` junction table must have a composite `PRIMARY KEY (concert_id, artist_id)` and foreign keys referencing `Concert.id` and `Artist.id`.

### 3.2 Cardinality & Uniqueness
- **Artist Follows**: `UserFollowedArtist` must have a `UNIQUE` constraint on `(user_id, artist_id)` to prevent duplicate follows.
- **Concert Likes**: `UserLikeConcert` must have a `UNIQUE` constraint on `(user_id, concert_id)` to prevent duplicate likes.

### 3.3 Implicit Inherited Constraints
- **Venue-Concert Binding**: An `Concert` cannot change its `region` attribute independently. The `region` is determined by the `Venue` entity; physical schema should avoid denormalizing `region` into `Concert` to prevent update anomalies.

## 4. Constraint Summary Table

| Entity | Field | Constraint Type |
|--------|-------|-----------------|
| User | `(oauthProvider, oauthId)` | `UNIQUE` |
| User | `preferred_locale` | `NOT NULL`, `CHECK` |
| Artist | `name` | `NOT NULL` |
| Venue | `name`, `type` | `NOT NULL` |
| Concert | `title`, `date`, `official_announcement_url`, `venue_id` | `NOT NULL` |
| UserFollowedArtist | `(user_id, artist_id)` | `UNIQUE`, `PK` |
| UserLikeConcert | `(user_id, concert_id)` | `UNIQUE`, `PK` |
| NotificationSubscription | `user_id` | `UNIQUE` |
| ArtistSubscriptionPreferences | `user_id` | `UNIQUE` |
