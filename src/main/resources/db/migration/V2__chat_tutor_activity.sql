-- Records which specialist agent answers this conversation.
-- Null until the student chooses connectives or expansion.
ALTER TABLE IF EXISTS chat
    ADD COLUMN IF NOT EXISTS tutor_activity varchar(32);
