alter table user_tokens drop constraint activation_pkey;
alter table user_tokens add constraint token_key  primary key(user_id, token_type);