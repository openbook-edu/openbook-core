ALTER TABLE activations RENAME TO user_tokens;
ALTER TABLE user_tokens ADD COLUMN "token_type" TEXT;
