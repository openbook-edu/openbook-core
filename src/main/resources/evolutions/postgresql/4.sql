-- create table words(word text, type text, lang text, PRIMARY KEY(word, lang))
-- alter table words owner to krispii_dev
--
-- create table word_combinations(type1 text, type2 text, lang text)
-- alter table word_combinations owner to krispii_dev
create table words(word text, lang text, PRIMARY KEY(word, lang));
alter table words owner to krispii_dev
