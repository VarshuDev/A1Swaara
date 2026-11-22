-- Run against a disposable Supabase database after installing supabase_schema.sql.
-- Requires psql and sufficient privileges to insert auth users and SET ROLE.
\set ON_ERROR_STOP on
BEGIN;
INSERT INTO auth.users (id, email) VALUES
 ('ea825909-3294-4be6-a21f-1697e3f8a101', 'rls-host@example.invalid'),
 ('ea825909-3294-4be6-a21f-1697e3f8a102', 'rls-other@example.invalid');
INSERT INTO public.jam_rooms (room_code, host_id, host_name, room_name, is_public)
VALUES ('TESTPUB', 'ea825909-3294-4be6-a21f-1697e3f8a101', 'Host', 'Public', true),
       ('TESTPRIVATE', 'ea825909-3294-4be6-a21f-1697e3f8a101', 'Host', 'Private', false);
INSERT INTO public.jam_queue (room_code, song_id, title, artist, added_by_name)
VALUES ('TESTPUB', 'song', 'Title', 'Artist', 'Host');

SET LOCAL ROLE anon;
DO $$ BEGIN
  IF (SELECT count(*) FROM public.jam_rooms WHERE room_code = 'TESTPRIVATE') <> 0 THEN
    RAISE EXCEPTION 'anonymous users can read private rooms';
  END IF;
  BEGIN
    PERFORM email FROM public.profiles LIMIT 1;
    RAISE EXCEPTION 'anonymous users can read profile emails';
  EXCEPTION WHEN insufficient_privilege THEN NULL; END;
END $$;
RESET ROLE;

SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', 'ea825909-3294-4be6-a21f-1697e3f8a102', true);
DO $$ DECLARE affected integer; BEGIN
  IF (SELECT count(*) FROM public.jam_rooms WHERE room_code = 'TESTPRIVATE') <> 0 THEN
    RAISE EXCEPTION 'non-host can read private room';
  END IF;
  BEGIN
    INSERT INTO public.jam_rooms (room_code, host_id, host_name, room_name)
      VALUES ('FORGED', 'ea825909-3294-4be6-a21f-1697e3f8a101', 'Forged', 'Forged');
    RAISE EXCEPTION 'non-host can create a room for another host';
  EXCEPTION WHEN insufficient_privilege THEN NULL; END;
  BEGIN
    INSERT INTO public.jam_queue (room_code, song_id, title, artist, added_by_name)
      VALUES ('TESTPRIVATE', 'song', 'Title', 'Artist', 'Listener');
    RAISE EXCEPTION 'non-host can add to private queue';
  EXCEPTION WHEN insufficient_privilege THEN NULL; END;
  UPDATE public.jam_rooms SET room_name = 'hijacked' WHERE room_code = 'TESTPUB';
  GET DIAGNOSTICS affected = ROW_COUNT;
  IF affected <> 0 THEN RAISE EXCEPTION 'non-host can update room'; END IF;
  DELETE FROM public.jam_queue WHERE room_code = 'TESTPUB';
  GET DIAGNOSTICS affected = ROW_COUNT;
  IF affected <> 0 THEN RAISE EXCEPTION 'non-host can delete queue'; END IF;
  INSERT INTO public.jam_queue (room_code, song_id, title, artist, added_by_name)
    VALUES ('TESTPUB', 'new-song', 'Title', 'Artist', 'Listener');
  BEGIN
    PERFORM email FROM public.profiles LIMIT 1;
    RAISE EXCEPTION 'authenticated users can read profile emails';
  EXCEPTION WHEN insufficient_privilege THEN NULL; END;
END $$;
SELECT set_config('request.jwt.claim.sub', 'ea825909-3294-4be6-a21f-1697e3f8a101', true);
DO $$ DECLARE affected integer; BEGIN
  IF (SELECT count(*) FROM public.jam_rooms WHERE room_code = 'TESTPRIVATE') <> 1 THEN
    RAISE EXCEPTION 'host cannot read private room';
  END IF;
  UPDATE public.jam_rooms SET room_name = 'updated' WHERE room_code = 'TESTPUB';
  GET DIAGNOSTICS affected = ROW_COUNT;
  IF affected <> 1 THEN RAISE EXCEPTION 'host cannot update own room'; END IF;
  DELETE FROM public.jam_queue WHERE room_code = 'TESTPUB';
  GET DIAGNOSTICS affected = ROW_COUNT;
  IF affected <> 2 THEN RAISE EXCEPTION 'host cannot delete queue'; END IF;
END $$;
RESET ROLE;
ROLLBACK;
