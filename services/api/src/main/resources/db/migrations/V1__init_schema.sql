-- Migration: Initial Schema
-- Description: Create core tables based on the data model.

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Enums
CREATE TYPE oauth_provider AS ENUM ('google', 'kakao');
CREATE TYPE preferred_locale AS ENUM ('ko', 'ja');
CREATE TYPE country_code AS ENUM ('JP', 'KO', 'Global');
CREATE TYPE sns_platform AS ENUM ('instagram', 'youtube', 'tiktok', 'twitter', 'official');
CREATE TYPE venue_type AS ENUM ('hall', 'dome', 'stadium', 'other');

-- Users
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    oauth_provider oauth_provider NOT NULL,
    oauth_id VARCHAR(255) NOT NULL,
    preferred_locale preferred_locale NOT NULL,
    onboarding_completed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (oauth_provider, oauth_id)
);

-- Artists
CREATE TABLE artists (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    genre VARCHAR(255),
    nation country_code NOT NULL,
    image_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ArtistSNS
CREATE TABLE artist_sns (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    artist_id UUID NOT NULL REFERENCES artists(id) ON DELETE CASCADE,
    platform sns_platform NOT NULL,
    url TEXT NOT NULL
);

-- Venues
CREATE TABLE venues (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    type venue_type DEFAULT 'other',
    region country_code NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Concerts
CREATE TABLE concerts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    venue_id UUID NOT NULL REFERENCES venues(id) ON DELETE RESTRICT,
    title VARCHAR(255) NOT NULL,
    date TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_minutes INTEGER,
    ticketing_url TEXT,
    official_announcement_url TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Junction Tables
CREATE TABLE user_like_concert (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    concert_id UUID NOT NULL REFERENCES concerts(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, concert_id)
);

CREATE TABLE concert_artist (
    concert_id UUID NOT NULL REFERENCES concerts(id) ON DELETE CASCADE,
    artist_id UUID NOT NULL REFERENCES artists(id) ON DELETE CASCADE,
    PRIMARY KEY (concert_id, artist_id)
);

CREATE TABLE user_followed_artist (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    artist_id UUID NOT NULL REFERENCES artists(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, artist_id)
);

-- Configurations (1:1 with User)
CREATE TABLE notification_subscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    email_enabled BOOLEAN DEFAULT FALSE,
    kakao_enabled BOOLEAN DEFAULT FALSE,
    general_weekly_updates BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE artist_subscription_preferences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    new_concerts_enabled BOOLEAN DEFAULT FALSE,
    ticket_open_bundle_enabled BOOLEAN DEFAULT FALSE,
    discography_enabled BOOLEAN DEFAULT FALSE
);
