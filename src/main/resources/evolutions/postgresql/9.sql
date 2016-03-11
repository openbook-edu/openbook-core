CREATE OR REPLACE FUNCTION get_slug(_slug text, _table text, _id uuid) RETURNS text AS $$
DECLARE
 updatedSlug text := $1;
 counter integer := 1;
 isSlugExist integer := 0;
BEGIN
    EXECUTE 'SELECT count(*) FROM ' || $2 || ' WHERE slug=''' || $1 || '''' || ' AND id !=''' || $3 || '''' INTO isSlugExist;

	  WHILE isSlugExist > 0 LOOP
		   updatedSlug := $1 || '-' || counter;
		   EXECUTE 'SELECT count(*) FROM ' || $2 || ' WHERE slug=''' || updatedSlug || '''' || ' AND id !=''' || $3 || '''' INTO isSlugExist;
		   counter := counter + 1;
	  END LOOP;

	  RETURN updatedSlug;
END; $$
LANGUAGE PLPGSQL;
