-- PREPARED ONLY. Execute after separate approval, using psql -X -v ON_ERROR_STOP=1.
-- The client must first validate the approved direct hostname with verify-full.
-- This script intentionally stops if the migrator or any public relations already exist.
BEGIN;
DO $$
BEGIN
  IF current_database()<>'emr_task5' OR current_user<>'emr_task5_owner'
     OR session_user<>'emr_task5_owner' OR current_setting('server_version_num')::int NOT BETWEEN 160000 AND 169999 THEN
    RAISE EXCEPTION 'Wrong database, identity or PostgreSQL version';
  END IF;
  IF EXISTS(SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public')
     OR EXISTS(SELECT 1 FROM pg_roles WHERE rolname='emr_task5_migrator') THEN
    RAISE EXCEPTION 'Expected empty target and absent migrator; reassess before proceeding';
  END IF;
  IF EXISTS(SELECT 1 FROM pg_auth_members m JOIN pg_roles r ON r.oid=m.member WHERE r.rolname='emr_task5_app')
     OR NOT EXISTS(SELECT 1 FROM pg_roles WHERE rolname='emr_task5_app' AND rolcanlogin
                   AND NOT (rolsuper OR rolcreatedb OR rolcreaterole OR rolreplication OR rolbypassrls)) THEN
    RAISE EXCEPTION 'Application attributes or memberships require separate review';
  END IF;
  IF EXISTS(SELECT 1 FROM pg_namespace WHERE nspname !~ '^pg_' AND nspname<>'information_schema'
             AND pg_has_role('emr_task5_app',nspowner,'MEMBER')) THEN
    RAISE EXCEPTION 'Application schema ownership requires separate review';
  END IF;
END $$;
-- Create through SQL, not Neon Console/API/CLI: avoid automatic neon_superuser membership.
CREATE ROLE emr_task5_migrator LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
REVOKE ALL ON DATABASE emr_task5 FROM PUBLIC;
REVOKE CREATE, TEMPORARY ON DATABASE emr_task5 FROM emr_task5_app;
GRANT CONNECT ON DATABASE emr_task5 TO emr_task5_app, emr_task5_migrator;
REVOKE CREATE ON SCHEMA public FROM PUBLIC, emr_task5_app;
GRANT USAGE ON SCHEMA public TO emr_task5_app;
GRANT USAGE, CREATE ON SCHEMA public TO emr_task5_migrator;
COMMIT;
-- Interactive psql encrypts the password before submitting it; do not put a literal in SQL.
SET password_encryption='scram-sha-256';
\password emr_task5_migrator
-- If the password step fails, STOP. No runtime or migration may start.
SELECT rolname,rolcanlogin,rolsuper,rolcreatedb,rolcreaterole,rolreplication,rolbypassrls
FROM pg_roles WHERE rolname IN ('emr_task5_app','emr_task5_migrator');
