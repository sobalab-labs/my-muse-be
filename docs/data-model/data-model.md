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
- `id`: UUID (PK)
- `email`: string (NULLABLE; Kakao or providers may omit or return unverified email)
- `email_verified`: boolean (DEFAULT: false)
- `name`: string (Display name)
- `preferred_locale`: enum ('ko', 'ja') (REQUIRED)
- `onboarding_completed`: boolean (DEFAULT: false)
- `created_at`: timestamp
- `updated_at`: timestamp

### UserIdentity
- `id`: UUID (PK)
- `user_id`: UUID (FK -> User.id)
- `provider`: enum ('google', 'kakao')
- `provider_subject`: string (Google `sub`, or Kakao numeric `id` as string)
- `created_at`: timestamp
(UNIQUE constraint on `(provider, provider_subject)`)

### Artist
- `id`: UUID (PK)
- `name`: string
- `genre`: string (NULLABLE)
- `nation`: enum ('Japan', 'Korea', 'Global')
- `image_url`: string (NULLABLE)
- `created_at`: timestamp

### ArtistSNS
- `id`: UUID (PK)
- `artist_id`: UUID (FK -> Artist.id)
- `platform`: enum ('instagram', 'youtube', 'tiktok', 'twitter', 'official')
- `url`: string

### Venue
- `id`: UUID (PK)
- `name`: string
- `type`: enum ('hall', 'dome', 'stadium', 'other') (DEFAULT: 'other')
- `region`: enum ('japan', 'korea', 'other')
- `created_at`: timestamp

### Concert
- `id`: UUID (PK)
- `venue_id`: UUID (FK -> Venue.id)
- `title`: string
- `date`: datetime
- `duration_minutes`: integer (NULLABLE)
- `ticketing_url`: string (NULLABLE)
- `official_announcement_url`: string (REQUIRED)
- `created_at`: timestamp

### UserLikeConcert (Junction)
- `user_id`: UUID (FK -> User.id)
- `concert_id`: UUID (FK -> Concert.id)
(PK: user_id, concert_id)

### ConcertArtist (Junction)
- `concert_id`: UUID (FK -> Concert.id)
- `artist_id`: UUID (FK -> Artist.id)
(PK: concert_id, artist_id)

### UserFollowedArtist (Junction)
- `user_id`: UUID (FK -> User.id)
- `artist_id`: UUID (FK -> Artist.id)
(PK: user_id, artist_id)

### NotificationPreferences
- `id`: UUID (PK)
- `user_id`: UUID (FK -> User.id)
- `channel_email_enabled`: boolean (DEFAULT: false)
- `channel_kakao_enabled`: boolean (DEFAULT: false)
- `general_weekly_updates`: boolean (DEFAULT: false)
- `artist_new_concert_enabled`: boolean (DEFAULT: false)
- `bookmarked_event_ticket_open_enabled`: boolean (DEFAULT: false)
- `updated_at`: timestamp

## 2. Relationships

- **User** 1:N **UserIdentity**
- **User** 1:N **UserFollowedArtist** N:1 **Artist**
- **User** 1:N **UserLikeConcert** N:1 **Concert**
- **User** 1:1 **NotificationPreferences**
- **Artist** 1:N **ArtistSNS**
- **Artist** 1:N **ConcertArtist** N:1 **Concert**
- **Venue** 1:N **Concert**

## 3. Authentication & Identity Rules

- **User Identity Key**: Uniquely identified by `(provider, provider_subject)` stored in `UserIdentity`. Email is never used as the identity key.
- **Account Linking**: No automatic account merging by matching email. Account linking requires an explicit authenticated action.
- **Email Verification**: Kakao or other providers may omit or return unverified emails. `email_verified` tracks verification status; unverified or missing emails are treated as unusable for email notifications.
