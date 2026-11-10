-- Migration: Initial Schema
-- Description: Create core tables based on the data model.

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Enums
CREATE TYPE oauth_provider AS ENUM ('google', 'kakao');

CREATE TYPE locale AS ENUM ('ko', 'ja');

CREATE TYPE country_code AS ENUM ('JP', 'KR');

CREATE TYPE link_platform AS ENUM ('instagram', 'youtube', 'tiktok', 'twitter', 'official');

CREATE TYPE venue_type AS ENUM ('hall', 'dome', 'stadium', 'other');

-- Users
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4 (),
    email VARCHAR(255),
    email_verified BOOLEAN DEFAULT FALSE NOT NULL,
    name VARCHAR(255) NOT NULL,
    preferred_locale locale NOT NULL,
    onboarding_completed BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- User Identities
CREATE TABLE user_identities (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4 (),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider oauth_provider NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE (provider, provider_subject)
);

-- Artists
CREATE TABLE artists (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4 (),
    name VARCHAR(255) NOT NULL,
    genre VARCHAR(255),
    nation country_code,
    image_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- ArtistLink
CREATE TABLE artist_link (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4 (),
    artist_id UUID NOT NULL REFERENCES artists (id) ON DELETE CASCADE,
    platform link_platform NOT NULL,
    url TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Venues
CREATE TABLE venues (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4 (),
    name VARCHAR(255) NOT NULL,
    type venue_type DEFAULT 'other',
    nation country_code,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Concerts
CREATE TABLE concerts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4 (),
    venue_id UUID NOT NULL REFERENCES venues (id) ON DELETE RESTRICT,
    title VARCHAR(255) NOT NULL,
    date TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_minutes INTEGER,
    ticketing_url TEXT,
    official_announcement_url TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Junction Tables
CREATE TABLE user_like_concert (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    concert_id UUID NOT NULL REFERENCES concerts (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, concert_id)
);

CREATE TABLE concert_artist (
    concert_id UUID NOT NULL REFERENCES concerts (id) ON DELETE CASCADE,
    artist_id UUID NOT NULL REFERENCES artists (id) ON DELETE CASCADE,
    PRIMARY KEY (concert_id, artist_id)
);

CREATE TABLE user_followed_artist (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    artist_id UUID NOT NULL REFERENCES artists (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, artist_id)
);

-- Configurations (1:1 with User)
CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4 (),
    user_id UUID NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    channel_email_enabled BOOLEAN DEFAULT FALSE NOT NULL,
    channel_kakao_enabled BOOLEAN DEFAULT FALSE NOT NULL,
    general_weekly_updates BOOLEAN DEFAULT FALSE NOT NULL,
    artist_new_concert_enabled BOOLEAN DEFAULT FALSE NOT NULL,
    bookmarked_event_ticket_open_enabled BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
