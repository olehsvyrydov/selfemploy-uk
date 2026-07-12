-- This comment deliberately contains a semicolon; the loader must strip line
-- comments before splitting on ';' so this does not break the statement below.
CREATE TABLE demo (id TEXT PRIMARY KEY, note TEXT);
CREATE INDEX idx_demo_note ON demo(note);
