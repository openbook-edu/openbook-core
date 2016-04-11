CREATE TABLE activations (
  user_id uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  nonce text UNIQUE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);
-- alter table activations owner to krispii
