ALTER TABLE works
  ADD COLUMN music_retry_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE works
  ADD CONSTRAINT works_music_retry_count_check CHECK (music_retry_count >= 0);
