-- H2 setString(JSON) stores a JSON string literal, unlike MySQL JSON validation.
-- Adapt only test storage; production V002 remains JSON and needs real MySQL acceptance.
ALTER TABLE learning_event ALTER COLUMN payload VARCHAR(1000000) NOT NULL;
ALTER TABLE lesson_progress ALTER COLUMN resource_progress VARCHAR(1000000) NOT NULL;
