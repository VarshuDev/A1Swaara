-- ==============================================================================
-- A1 SWAARA CLOUD - SUPABASE POSTGRESQL SCHEMA & ROW LEVEL SECURITY
-- Project: https://vdfwhjsjkvmcoalgjtke.supabase.co
-- ==============================================================================

-- 1. PROFILES TABLE (Mirrors Supabase Auth Users)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT,
    display_name TEXT NOT NULL DEFAULT 'Swaara Listener',
    avatar_url TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Trigger to auto-create profile on new user signup
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, email, display_name, avatar_url)
    VALUES (
        NEW.id,
        NEW.email,
        COALESCE(NEW.raw_user_meta_data->>'full_name', NEW.raw_user_meta_data->>'name', 'Swaara Listener'),
        NEW.raw_user_meta_data->>'avatar_url'
    )
    ON CONFLICT (id) DO UPDATE
    SET email = EXCLUDED.email,
        display_name = COALESCE(EXCLUDED.display_name, public.profiles.display_name),
        avatar_url = COALESCE(EXCLUDED.avatar_url, public.profiles.avatar_url),
        updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT OR UPDATE ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- 2. USER FAVORITES SYNC (Liked Songs)
CREATE TABLE IF NOT EXISTS public.user_favorites (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    song_id TEXT NOT NULL,
    title TEXT NOT NULL,
    artist TEXT NOT NULL,
    artwork_url TEXT,
    duration_seconds INT DEFAULT 0,
    added_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, song_id)
);

CREATE INDEX IF NOT EXISTS idx_user_favorites_user_id ON public.user_favorites(user_id);

-- 3. USER PLAYLISTS & TRACKS SYNC
CREATE TABLE IF NOT EXISTS public.user_playlists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    is_public BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_playlists_user_id ON public.user_playlists(user_id);

CREATE TABLE IF NOT EXISTS public.user_playlist_tracks (
    id BIGSERIAL PRIMARY KEY,
    playlist_id UUID NOT NULL REFERENCES public.user_playlists(id) ON DELETE CASCADE,
    song_id TEXT NOT NULL,
    title TEXT NOT NULL,
    artist TEXT NOT NULL,
    artwork_url TEXT,
    position INT NOT NULL,
    added_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_playlist_tracks_playlist ON public.user_playlist_tracks(playlist_id);

-- 4. TOP LISTENER & STREAMING STATS
CREATE TABLE IF NOT EXISTS public.user_artist_stats (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    artist_name TEXT NOT NULL,
    total_listen_seconds BIGINT DEFAULT 0,
    play_count INT DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, artist_name)
);

CREATE INDEX IF NOT EXISTS idx_user_artist_stats_artist ON public.user_artist_stats(artist_name);
CREATE INDEX IF NOT EXISTS idx_user_artist_stats_user ON public.user_artist_stats(user_id);

CREATE TABLE IF NOT EXISTS public.global_listening_stats (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    total_seconds BIGINT DEFAULT 0,
    total_tracks_played BIGINT DEFAULT 0,
    current_streak_days INT DEFAULT 1,
    last_listened_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. LIVE JAMMING & DEDICATIONS
CREATE TABLE IF NOT EXISTS public.jam_rooms (
    room_code TEXT PRIMARY KEY,
    host_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    host_name TEXT NOT NULL,
    room_name TEXT NOT NULL,
    current_song_id TEXT,
    current_title TEXT,
    current_artist TEXT,
    current_artwork_url TEXT,
    is_playing BOOLEAN DEFAULT FALSE,
    playback_position_ms BIGINT DEFAULT 0,
    playback_updated_at TIMESTAMPTZ DEFAULT NOW(),
    is_public BOOLEAN DEFAULT TRUE,
    allow_dedications BOOLEAN DEFAULT TRUE,
    active_listeners_count INT DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_jam_rooms_public ON public.jam_rooms(is_public);

CREATE TABLE IF NOT EXISTS public.jam_dedications (
    id BIGSERIAL PRIMARY KEY,
    room_code TEXT NOT NULL REFERENCES public.jam_rooms(room_code) ON DELETE CASCADE,
    sender_name TEXT NOT NULL,
    dedicated_to TEXT NOT NULL,
    song_id TEXT NOT NULL,
    song_title TEXT NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    is_active BOOLEAN DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_jam_dedications_room ON public.jam_dedications(room_code);

CREATE TABLE IF NOT EXISTS public.jam_queue (
    id BIGSERIAL PRIMARY KEY,
    room_code TEXT NOT NULL REFERENCES public.jam_rooms(room_code) ON DELETE CASCADE,
    song_id TEXT NOT NULL,
    title TEXT NOT NULL,
    artist TEXT NOT NULL,
    artwork_url TEXT,
    added_by_name TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_jam_queue_room ON public.jam_queue(room_code);

-- 6. ENABLE ROW LEVEL SECURITY (RLS)
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_favorites ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_playlists ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_playlist_tracks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_artist_stats ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.global_listening_stats ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.jam_rooms ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.jam_dedications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.jam_queue ENABLE ROW LEVEL SECURITY;

-- 7. RLS POLICIES

-- Profiles: Anyone authenticated or anon can read profile names/avatars; only owner can update
DROP POLICY IF EXISTS "Public profiles read" ON public.profiles;
CREATE POLICY "Public profiles read" ON public.profiles FOR SELECT USING (true);

DROP POLICY IF EXISTS "Owner profile update" ON public.profiles;
CREATE POLICY "Owner profile update" ON public.profiles FOR UPDATE USING (auth.uid() = id);

-- Favorites: User has full access to their own favorites
DROP POLICY IF EXISTS "User favorites full" ON public.user_favorites;
CREATE POLICY "User favorites full" ON public.user_favorites FOR ALL USING (auth.uid() = user_id);

-- Playlists: Owner has full access; public playlists are readable by all
DROP POLICY IF EXISTS "User playlists owner" ON public.user_playlists;
CREATE POLICY "User playlists owner" ON public.user_playlists FOR ALL USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Public playlists read" ON public.user_playlists;
CREATE POLICY "Public playlists read" ON public.user_playlists FOR SELECT USING (is_public = true);

DROP POLICY IF EXISTS "User playlist tracks owner" ON public.user_playlist_tracks;
CREATE POLICY "User playlist tracks owner" ON public.user_playlist_tracks FOR ALL USING (
    EXISTS (SELECT 1 FROM public.user_playlists p WHERE p.id = playlist_id AND p.user_id = auth.uid())
);

-- Stats: Owner can insert/update; all can read for leaderboards
DROP POLICY IF EXISTS "User artist stats owner" ON public.user_artist_stats;
CREATE POLICY "User artist stats owner" ON public.user_artist_stats FOR ALL USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Public leaderboard read" ON public.user_artist_stats;
CREATE POLICY "Public leaderboard read" ON public.user_artist_stats FOR SELECT USING (true);

DROP POLICY IF EXISTS "Global stats owner" ON public.global_listening_stats;
CREATE POLICY "Global stats owner" ON public.global_listening_stats FOR ALL USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Global stats read" ON public.global_listening_stats;
CREATE POLICY "Global stats read" ON public.global_listening_stats FOR SELECT USING (true);

-- Jamming & Dedications: Open to all listeners (both signed in and guest rooms)
DROP POLICY IF EXISTS "Jam rooms open select" ON public.jam_rooms;
CREATE POLICY "Jam rooms open select" ON public.jam_rooms FOR SELECT USING (true);

DROP POLICY IF EXISTS "Jam rooms open insert" ON public.jam_rooms;
CREATE POLICY "Jam rooms open insert" ON public.jam_rooms FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Jam rooms open update" ON public.jam_rooms;
CREATE POLICY "Jam rooms open update" ON public.jam_rooms FOR UPDATE USING (true);

DROP POLICY IF EXISTS "Jam rooms open delete" ON public.jam_rooms;
CREATE POLICY "Jam rooms open delete" ON public.jam_rooms FOR DELETE USING (auth.uid() = host_id OR host_id IS NULL);

DROP POLICY IF EXISTS "Jam dedications open select" ON public.jam_dedications;
CREATE POLICY "Jam dedications open select" ON public.jam_dedications FOR SELECT USING (true);

DROP POLICY IF EXISTS "Jam dedications open insert" ON public.jam_dedications;
CREATE POLICY "Jam dedications open insert" ON public.jam_dedications FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Jam queue open select" ON public.jam_queue;
CREATE POLICY "Jam queue open select" ON public.jam_queue FOR SELECT USING (true);

DROP POLICY IF EXISTS "Jam queue open insert" ON public.jam_queue;
CREATE POLICY "Jam queue open insert" ON public.jam_queue FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Jam queue open delete" ON public.jam_queue;
CREATE POLICY "Jam queue open delete" ON public.jam_queue FOR DELETE USING (true);

-- 8. ENABLE REALTIME BROADCASTING
DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.jam_rooms;
EXCEPTION WHEN duplicate_object THEN
    NULL;
END $$;

DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.jam_dedications;
EXCEPTION WHEN duplicate_object THEN
    NULL;
END $$;

DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.jam_queue;
EXCEPTION WHEN duplicate_object THEN
    NULL;
END $$;

-- 9. AUTOMATIC 10-MINUTE INACTIVITY PURGE
CREATE INDEX IF NOT EXISTS idx_jam_rooms_playback_updated_at ON public.jam_rooms(playback_updated_at);

CREATE OR REPLACE FUNCTION public.cleanup_inactive_jam_rooms()
RETURNS void AS $$
BEGIN
    DELETE FROM public.jam_rooms
    WHERE playback_updated_at < NOW() - INTERVAL '10 minutes';
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
