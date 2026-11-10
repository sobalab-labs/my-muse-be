# Data Model: Artist & Concert Management Hub

## 0. Normalization Validation

<!-- 
    Instructions:
    - AI-Agent: Verify normalization and update the status checkbox and result below.
    - Human: Perform final review and update the status checkbox below.
    - DO NOT modify the structure of this section.
-->

### Validation Checklist

- [x] (AI-Agent) Normalization Verified. Result: 3NF
- [ ] (Human) Normalization Reviewed. Status: OK

## 1. Core Entities (Logical Model)

### User
- `id`: UUID (PK, NOT NULL)
- `email`: string (NULLABLE; Kakao or providers may omit or return unverified email)
- `email_verified`: boolean (DEFAULT: false, NOT NULL)
- `name`: string (Display name, NOT NULL)
- `preferred_locale`: enum ('ko', 'ja') (NOT NULL)
- `onboarding_completed`: boolean (DEFAULT: false, NOT NULL)
- `created_at`: timestamp (NOT NULL, DEFAULT: CURRENT_TIMESTAMP)
- `updated_at`: timestamp (NOT NULL, DEFAULT: CURRENT_TIMESTAMP)

### UserIdentity
- `id`: UUID (PK, NOT NULL)
- `user_id`: UUID (FK -> User.id, NOT NULL)
- `provider`: enum ('google', 'kakao') (NOT NULL)
- `provider_subject`: string (Google `sub`, or Kakao numeric `id` as string, NOT NULL)
- `created_at`: timestamp (NOT NULL, DEFAULT: CURRENT_TIMESTAMP)
(UNIQUE constraint on `(provider, provider_subject)`)

### Artist
- `id`: UUID (PK, NOT NULL)
- `name`: string (NOT NULL)
- `genre`: string (NULLABLE)
- `nation`: enum ('JP', 'KR') (NULLABLE)
- `image_url`: string (NULLABLE)
- `created_at`: timestamp (NOT NULL, DEFAULT: CURRENT_TIMESTAMP)
- `updated_at`: timestamp (NOT NULL, DEFAULT: CURRENT_TIMESTAMP)

### ArtistLink
- `id`: UUID (PK, NOT NULL)
- `artist_id`: UUID (FK -> Artist.id, NOT NULL)
- `platform`: enum ('instagram', 'youtube', 'tiktok', 'twitter', 'official') (NOT NULL)
- `url`: string (NOT NULL)
- `created_at`: timestamp (NOT NULL, DEFAULT: CURRENT_TIMESTAMP)
- `updated_at`: timestamp (NOT NULL, DEFAULT: CURRENT_TIMESTAMP)

### Venue
- `id`: UUID (PK, NOT NULL)
- `name`: string (NOT NULL)
- `type`: enum ('hall', 'dome', 'stadium', 'other') (DEFAULT: 'other')
- `nation`: enum ('JP', 'KR') (NULLABLE)
- `created_at`: timestamp (NOT NULL, DEFAULT: CURRENT_TIMESTAMP)

### Concert
- `id`: UUID (PK, NOT NULL)
- `venue_id`: UUID (FK -> Venue.id, NOT NULL)
- `title`: string (NOT NULL)
- `date`: datetime (NOT NULL)
- `duration_minutes`: integer (NULLABLE)
- `ticketing_url`: string (NULLABLE)
- `official_announcement_url`: string (NOT NULL)
- `created_at`: timestamp (NOT NULL, DEFAULT: CURRENT_TIMESTAMP)
- `updated_at`: timestamp (NOT NULL, DEFAULT: CURRENT_TIMESTAMP)

### UserLikeConcert (Junction)
- `user_id`: UUID (FK -> User.id, NOT NULL)
- `concert_id`: UUID (FK -> Concert.id, NOT NULL)
(PK: user_id, concert_id)

### ConcertArtist (Junction)
- `concert_id`: UUID (FK -> Concert.id, NOT NULL)
- `artist_id`: UUID (FK -> Artist.id, NOT NULL)
(PK: concert_id, artist_id)

### UserFollowedArtist (Junction)
- `user_id`: UUID (FK -> User.id, NOT NULL)
- `artist_id`: UUID (FK -> Artist.id, NOT NULL)
(PK: user_id, artist_id)

### NotificationPreferences
- `id`: UUID (PK, NOT NULL)
- `user_id`: UUID (FK -> User.id, NOT NULL, UNIQUE)
- `channel_email_enabled`: boolean (DEFAULT: false, NOT NULL)
- `channel_kakao_enabled`: boolean (DEFAULT: false, NOT NULL)
- `general_weekly_updates`: boolean (DEFAULT: false, NOT NULL)
- `artist_new_concert_enabled`: boolean (DEFAULT: false, NOT NULL)
- `bookmarked_event_ticket_open_enabled`: boolean (DEFAULT: false, NOT NULL)
- `created_at`: timestamp (NOT NULL, DEFAULT: CURRENT_TIMESTAMP)
- `updated_at`: timestamp (NOT NULL, DEFAULT: CURRENT_TIMESTAMP)

## 2. Relationships

- **User** 1:N **UserIdentity**
- **User** 1:N **UserFollowedArtist** N:1 **Artist**
- **User** 1:N **UserLikeConcert** N:1 **Concert**
- **User** 1:1 **NotificationPreferences**
- **Artist** 1:N **ArtistLink**
- **Artist** 1:N **ConcertArtist** N:1 **Concert**
- **Venue** 1:N **Concert**

## 3. Authentication & Identity Rules

- **User Identity Key**: Uniquely identified by `(provider, provider_subject)` stored in `UserIdentity`. Email is never used as the identity key.
- **Account Linking**: No automatic account merging by matching email. Account linking requires an explicit authenticated action.
- **Email Verification**: Kakao or other providers may omit or return unverified emails. `email_verified` tracks verification status; unverified or missing emails are treated as unusable for email notifications.
