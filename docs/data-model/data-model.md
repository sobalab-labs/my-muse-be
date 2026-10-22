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
- `email`: string
- `name`: string
- `oauth_provider`: enum ('google', 'kakao')
- `oauth_id`: string
- `preferred_locale`: enum ('ko', 'ja') (REQUIRED)
- `onboarding_completed`: boolean (DEFAULT: false)
- `created_at`: timestamp
- `updated_at`: timestamp

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

### NotificationSubscription
- `id`: UUID (PK)
- `user_id`: UUID (FK -> User.id)
- `email_enabled`: boolean (DEFAULT: false)
- `kakao_enabled`: boolean (DEFAULT: false)
- `general_weekly_updates`: boolean (DEFAULT: false)
- `updated_at`: timestamp

### ArtistSubscriptionPreferences
- `id`: UUID (PK)
- `user_id`: UUID (FK -> User.id)
- `new_concerts_enabled`: boolean (DEFAULT: false)
- `ticket_open_bundle_enabled`: boolean (DEFAULT: false)
- `discography_enabled`: boolean (DEFAULT: false)

## 2. Relationships

- **User** 1:N **UserFollowedArtist** N:1 **Artist**
- **User** 1:N **UserLikeConcert** N:1 **Concert**
- **User** 1:1 **NotificationSubscription**
- **User** 1:1 **ArtistSubscriptionPreferences**
- **Artist** 1:N **ArtistSNS**
- **Artist** 1:N **ConcertArtist** N:1 **Concert**
- **Venue** 1:N **Concert**
