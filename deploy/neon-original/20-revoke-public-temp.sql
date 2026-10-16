-- PROPOSAL ONLY. DO NOT EXECUTE BEFORE EXPLICIT AUTHORIZATION OF GLOBAL PUBLIC ACL CHANGE.
-- psql 16+, -X -v ON_ERROR_STOP=1; approved direct host and neondb_owner, external TLS verify-full.
-- Requires -v approve_revoke_public_temp=yes after 10-provision-app.sql and client impact confirmation.
-- Revokes TEMPORARY on database neondb from PUBLIC so emr_financas_app satisfies DatabaseTarget.verifyRuntime.
-- All rejections raise SQL exceptions inside BEGIN..COMMIT; no \quit is used.
\set ON_ERROR_STOP on
\if :{?approve_revoke_public_temp}
\else
  \set approve_revoke_public_temp ''
\endif
\if :{?HOST}
\else
  \set HOST ''
\endif
\if :{?PORT}
\else
  \set PORT ''
\endif
\conninfo
BEGIN;
SET LOCAL lock_timeout='3s';
SET LOCAL statement_timeout='30s';
SELECT set_config('emr.approve', :'approve_revoke_public_temp', true),
       set_config('emr.client_host', :'HOST', true),
       set_config('emr.client_port', :'PORT', true);
DO $$
BEGIN
  IF current_setting('emr.approve', true) IS DISTINCT FROM 'yes' THEN
    RAISE EXCEPTION 'Explicit PUBLIC TEMP revocation approval flag approve_revoke_public_temp=yes missing; no writes executed';
  END IF;
  IF current_setting('emr.client_host', true) IS DISTINCT FROM 'ep-icy-cherry-acw6ysuv.sa-east-1.aws.neon.tech'
     OR current_setting('emr.client_port', true) IS DISTINCT FROM '5432'
     OR current_database() IS DISTINCT FROM 'neondb'
     OR current_user IS DISTINCT FROM 'neondb_owner'
     OR session_user IS DISTINCT FROM current_user
     OR current_setting('server_version_num')::int NOT BETWEEN 160000 AND 169999
     OR NOT EXISTS (SELECT 1 FROM pg_stat_ssl WHERE pid=pg_backend_pid()) THEN
    RAISE EXCEPTION 'Target/identity/version rejected; no writes executed';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='emr_financas_app') THEN
    RAISE EXCEPTION 'Role emr_financas_app missing; execute 10-provision-app.sql first';
  END IF;
END $$;
-- Global database ACL change on neondb: removes inherited TEMPORARY from PUBLIC.
-- Direct grants to neondb_owner (CTc) and PUBLIC CONNECT are preserved.
REVOKE TEMPORARY ON DATABASE neondb FROM PUBLIC;
DO $$
BEGIN
  IF NOT has_database_privilege('neondb_owner','neondb','CONNECT')
     OR NOT has_database_privilege('neondb_owner','neondb','TEMP')
     OR NOT has_database_privilege('neondb_owner','neondb','CREATE') THEN
    RAISE EXCEPTION 'Owner database privileges unexpectedly affected; aborting';
  END IF;
  IF NOT has_database_privilege('emr_financas_app','neondb','CONNECT')
     OR has_database_privilege('emr_financas_app','neondb','CREATE')
     OR has_database_privilege('emr_financas_app','neondb','TEMP') THEN
    RAISE EXCEPTION 'Effective database privileges for emr_financas_app rejected';
  END IF;
END $$;
COMMIT;
SELECT 'PUBLIC_TEMP_REVOKED:neondb' AS revoke_status
WHERE NOT has_database_privilege('emr_financas_app','neondb','TEMP')
  AND has_database_privilege('neondb_owner','neondb','TEMP');
