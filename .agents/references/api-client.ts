/**
 * API Client Contract & TypeScript Definitions
 * Branch: 001-artist-event-management
 */

export interface User {
    id: string;
    email: string;
    name: string;
    oauthProvider: 'google' | 'kakao';
    preferredLocale: 'ja' | 'ko';
    onboardingCompleted: boolean;
}

export interface Artist {
    id: string;
    name: string;
    genre: string;
    nation: string;
    snsUrls: Array<{ platform: string; url: string }>;
    imageUrl?: string;
    isFollowed?: boolean;
}

export interface ConcertItem {
    id: string;
    artists: Artist[];
    title: string;
    date: string;
    place: string;
    duration: string;
    ticketingUrl: string;
    officialAnnouncementUrl: string;
    region: 'japan' | 'korea' | 'other';
}

export interface NotificationSubscription {
    channels: { email: boolean; kakao: boolean };
    artistSubscriptions: Array<{
        artistId: string;
        newConcerts: boolean;
        ticketOpenBundle: boolean;
        discography: boolean;
    }>;
    generalWeeklyUpdates: boolean;
}

export interface ApiClientContract {
    // Auth
    getSession(): Promise<User | null>;
    signIn(provider: 'google' | 'kakao'): Promise<void>;
    signOut(): Promise<void>;
    deleteAccount(): Promise<void>;
    updateLocale(locale: 'ja' | 'ko'): Promise<void>;
    completeOnboarding(artistIds: string[]): Promise<void>;

    // Artists & Following
    searchArtists(query: string, cursor?: string): Promise<{ items: Artist[]; nextCursor?: string }>;
    getArtistDetail(id: string): Promise<Artist>;
    getFollowedArtists(cursor?: string): Promise<{ items: Artist[]; nextCursor?: string }>;
    followArtist(artistId: string): Promise<void>;
    unfollowArtist(artistId: string): Promise<void>;

    // Concerts & Feeds
    searchConcerts(
        query: string,
        sort?: 'dday' | 'recently_announced',
        cursor?: string
    ): Promise<{ items: ConcertItem[]; nextCursor?: string }>;
    getConcertDetail(id: string): Promise<ConcertItem>;

    getFollowingFeed(params: {
        view?: 'list' | 'calendar';
        month?: string;
        sort?: 'dday' | 'recently_announced';
        cursor?: string;
    }): Promise<{ items: ConcertItem[]; nextCursor?: string }>;

    getTrendingFeed(params: {
        view?: 'list' | 'calendar';
        month?: string;
        region?: string;
        sort?: 'dday' | 'recently_announced';
        cursor?: string;
    }): Promise<{ items: ConcertItem[]; nextCursor?: string }>;

    // Notifications
    getNotificationSubscription(): Promise<NotificationSubscription>;
    updateNotificationSubscription(sub: NotificationSubscription): Promise<void>;
}
