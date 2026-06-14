-- raw_json column has been NULL since V4; mapper always passes null and no query reads it.
ALTER TABLE winning_numbers DROP COLUMN raw_json;
