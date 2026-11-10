# Database Invariants: Artist & Concert Management Hub

## 1. Introduction
This document defines the data invariants—the immutable business rules and architectural constraints—that must hold true at the database schema level. Field-level nullability and datatype constraints are defined in `data-model.md`.

## 2. Entity-Level Invariants

### 2.1 Identity & Access
- **User Identity Uniqueness**: `UserIdentity` must have a `UNIQUE` constraint on `(provider, provider_subject)`.

### 2.2 Preferences & Configuration
- **Subscription Singleton**: `NotificationPreferences` must have a `UNIQUE` constraint on `user_id` (enforcing a 1:1 relationship with `User`).

## 3. Relational & Business Invariants

### 3.1 Referential Integrity & Cascades
- **Foreign Keys**: 
    - `UserIdentity.user_id`, `ArtistLink.artist_id`, `UserLikeConcert.user_id`, `UserLikeConcert.concert_id`, `ConcertArtist.concert_id`, `ConcertArtist.artist_id`, `UserFollowedArtist.user_id`, `UserFollowedArtist.artist_id`, and `NotificationPreferences.user_id` must enforce foreign key referential integrity.
    - `Concert.venue_id` implements `ON DELETE RESTRICT` to prevent orphan venues with active concerts.
    - All user-owned and artist-owned dependent entities implement `ON DELETE CASCADE`.
- **Concert-Artist Binding**: The `ConcertArtist` junction table must have a composite `PRIMARY KEY (concert_id, artist_id)` and foreign keys referencing `Concert.id` and `Artist.id`.

### 3.2 Cardinality & Uniqueness
- **Artist Follows**: `UserFollowedArtist` must have a `UNIQUE` constraint on `(user_id, artist_id)` to prevent duplicate follows.
- **Concert Likes**: `UserLikeConcert` must have a `UNIQUE` constraint on `(user_id, concert_id)` to prevent duplicate likes.

### 3.3 Implicit Inherited Constraints
- **Venue-Concert Binding**: A `Concert` cannot change its `nation` attribute independently. The `nation` is determined by the `Venue` entity; physical schema should avoid denormalizing `nation` into `Concert` to prevent update anomalies.

## 4. Constraint Summary Table

| Entity | Field(s) | Constraint Type |
|--------|----------|-----------------|
| UserIdentity | `(provider, provider_subject)` | `UNIQUE` |
| NotificationPreferences | `user_id` | `UNIQUE` |
| UserFollowedArtist | `(user_id, artist_id)` | `UNIQUE`, `PK` |
| UserLikeConcert | `(user_id, concert_id)` | `UNIQUE`, `PK` |
| ConcertArtist | `(concert_id, artist_id)` | `PK` |
