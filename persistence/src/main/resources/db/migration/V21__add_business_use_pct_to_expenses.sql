-- The share of an expense that is business use, as a percentage.
--
-- Defaulted to 100 so every expense already recorded keeps the meaning it had: no stated share is
-- the same as wholly business, and no allowable total changes because the column arrived. This
-- mirrors the desktop store's migration, so the two schemas describe the same thing.

ALTER TABLE expenses ADD COLUMN business_use_pct INTEGER DEFAULT 100 NOT NULL;
