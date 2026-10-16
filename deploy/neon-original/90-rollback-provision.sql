-- CONTROLLED ROLLBACK SCRIPT. DO NOT EXECUTE WITHOUT EXPLICIT OPERATOR AUTHORIZATION.
-- psql 16+, -X -v ON_ERROR_STOP=1; approved direct host and neondb_owner, external TLS verify-full.
-- Requires -v approve_rollback_neon_original=yes.
-- Restores PUBLIC TEMPORARY on neondb and removes emr_financas_app if it owns no objects and has no active sessions.
-- All rejections raise SQL exceptions inside BEGIN..COMMIT; no \quit is used.
\set ON_ERROR_STOP on
\if :{?approve_rollback_neon_original}
\else
  \set approve_rollback_neon_original ''
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
SELECT set_config('emr.approve', :'approve_rollback_neon_original', true),
       set_config('emr.client_host', :'HOST', true),
       set_config('emr.client_port', :'PORT', true);
DO $$
BEGIN
  IF current_setting('emr.approve', true) IS DISTINCT FROM 'yes' THEN
    RAISE EXCEPTION 'Explicit rollback approval flag approve_rollback_neon_original=yes missing; no writes executed';
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
END $$;
GRANT TEMPORARY ON DATABASE neondb TO PUBLIC;
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='emr_financas_app') THEN
    IF EXISTS (SELECT 1 FROM pg_stat_activity WHERE usename='emr_financas_app') THEN
      RAISE EXCEPTION 'Active sessions exist for emr_financas_app; stop client before dropping role';
    END IF;
    REVOKE ALL ON SEQUENCE public.seq_emr_usuario,public.seq_emr_gasto,public.seq_emr_recebimento,
      public.seq_emr_investimento,public.seq_emr_password_reset_token FROM emr_financas_app;
    REVOKE ALL ON TABLE public.tb_emr_usuario,public.tb_emr_gasto,public.tb_emr_recebimento,
      public.tb_emr_investimento,public.tb_emr_password_reset_token,public.flyway_schema_history FROM emr_financas_app;
    REVOKE ALL ON SCHEMA public FROM emr_financas_app;
    REVOKE ALL ON DATABASE neondb FROM emr_financas_app;
    DROP ROLE emr_financas_app;
  END IF;
  IF NOT has_database_privilege('public','neondb','TEMP')
     OR NOT has_database_privilege('public','neondb','CONNECT') THEN
    RAISE EXCEPTION 'PUBLIC database privileges not restored as expected';
  END IF;
END $$;
COMMIT;
SELECT 'ROLLBACK_COMMITTED:neondb' AS rollback_status
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='emr_financas_app');
