# Database migrations

`V1__schema_baseline.sql` is a structure-only snapshot of the current MySQL 8 schema. It already contains the legacy migrations kept in the repository-level `sql/` directory.

Rules for future changes:

1. Never edit an applied versioned migration.
2. Add each schema change as the next `V<N>__description.sql` file.
3. Keep Flyway disabled for normal local development unless the local database has been baselined.
4. Enable Flyway in deployed services with `FLYWAY_ENABLED=true`.
5. For an existing non-empty database without `flyway_schema_history`, run exactly one deployment with `FLYWAY_BASELINE_ON_MIGRATE=true`, verify the schema first, then set it back to `false`.

Fresh databases run V1 and all later versions automatically. Existing databases must be backed up before their first Flyway-managed deployment. V3 also removes a redundant legacy unique index from `certificate_exam_answer` when present.
