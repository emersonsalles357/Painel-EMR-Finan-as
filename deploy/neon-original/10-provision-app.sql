-- PROPOSAL ONLY. DO NOT EXECUTE BEFORE EXPLICIT AUTHORIZATION.
-- psql 16+, -X -v ON_ERROR_STOP=1; approved direct host and neondb_owner, external TLS verify-full.
-- Requires -v approve_original_app_provision=yes after review of 00-inspect-readonly.sql.
-- Provisions ONLY emr_financas_app and its object grants; does NOT alter PUBLIC ACLs.
-- New role has NULL password. First credential must be set separately by the operator.
-- All rejections raise SQL exceptions inside BEGIN..COMMIT; no \quit is used (prevents exit 0 false positives).
\set ON_ERROR_STOP on
\if :{?approve_original_app_provision}
\else
  \set approve_original_app_provision ''
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
SELECT set_config('emr.approve', :'approve_original_app_provision', true),
       set_config('emr.client_host', :'HOST', true),
       set_config('emr.client_port', :'PORT', true);
DO $$
BEGIN
  IF current_setting('emr.approve', true) IS DISTINCT FROM 'yes' THEN
    RAISE EXCEPTION 'Explicit approval flag approve_original_app_provision=yes missing; no writes executed';
  END IF;
  -- External TLS verify-full is enforced by libpq client; Neon proxy terminates TLS before postgres backend (pg_stat_ssl.ssl=false).
  IF current_setting('emr.client_host', true) IS DISTINCT FROM 'ep-icy-cherry-acw6ysuv.sa-east-1.aws.neon.tech'
     OR current_setting('emr.client_port', true) IS DISTINCT FROM '5432'
     OR current_database() IS DISTINCT FROM 'neondb'
     OR current_user IS DISTINCT FROM 'neondb_owner'
     OR session_user IS DISTINCT FROM current_user
     OR current_setting('server_version_num')::int NOT BETWEEN 160000 AND 169999
     OR NOT EXISTS (SELECT 1 FROM pg_stat_ssl WHERE pid=pg_backend_pid()) THEN
    RAISE EXCEPTION 'Target/identity/version rejected; no writes executed';
  END IF;
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='emr_financas_app') THEN
    RAISE EXCEPTION 'Role already exists; inspect instead of modifying';
  END IF;
  IF EXISTS (SELECT 1 FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace
             WHERE n.nspname !~ '^pg_' AND n.nspname<>'information_schema') THEN
    RAISE EXCEPTION 'Unexpected user routines; review execution privileges first';
  END IF;
  IF EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
             WHERE n.nspname='public' AND (c.relrowsecurity OR c.relforcerowsecurity)) THEN
    RAISE EXCEPTION 'Unexpected RLS configuration';
  END IF;
  IF (SELECT count(*) FROM public.flyway_schema_history)<>6
     OR EXISTS (SELECT 1 FROM public.flyway_schema_history
       WHERE NOT success OR type<>'SQL' OR checksum IS DISTINCT FROM CASE version::int
         WHEN 1 THEN -1180337507 WHEN 2 THEN -1011916991 WHEN 3 THEN 839926003
         WHEN 4 THEN 1651334590 WHEN 5 THEN 349451634 WHEN 6 THEN -1758127437 END)
     OR (SELECT count(DISTINCT version::int) FROM public.flyway_schema_history)<>6 THEN
    RAISE EXCEPTION 'Unexpected migration history; no repair/baseline permitted';
  END IF;
END $$;
CREATE ROLE emr_financas_app LOGIN PASSWORD NULL NOINHERIT
  NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS CONNECTION LIMIT 3;
-- Role-scoped grants only. Global PUBLIC TEMPORARY revocation is separated into 20-revoke-public-temp.sql.
REVOKE ALL ON DATABASE neondb FROM emr_financas_app;
GRANT CONNECT ON DATABASE neondb TO emr_financas_app;
GRANT USAGE ON SCHEMA public TO emr_financas_app;
GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE public.tb_emr_usuario,public.tb_emr_gasto,
  public.tb_emr_recebimento,public.tb_emr_investimento TO emr_financas_app;
GRANT SELECT,INSERT,UPDATE ON TABLE public.tb_emr_password_reset_token TO emr_financas_app;
GRANT USAGE ON SEQUENCE public.seq_emr_usuario,public.seq_emr_gasto,public.seq_emr_recebimento,
  public.seq_emr_investimento,public.seq_emr_password_reset_token TO emr_financas_app;
-- No schema/table/sequence PUBLIC revokes, ownership changes, default grants or migration.
-- Abort/rollback instead of automatically correcting unexpected PUBLIC privileges.
DO $$
DECLARE obj record; priv text; expected boolean;
BEGIN
  IF NOT has_database_privilege('emr_financas_app','neondb','CONNECT')
     OR has_database_privilege('emr_financas_app','neondb','CREATE')
     OR EXISTS (SELECT 1 FROM pg_database d,
                aclexplode(coalesce(d.datacl, acldefault('d', d.datdba))) a
                WHERE d.datname='neondb' AND a.grantee='emr_financas_app'::regrole
                  AND (a.privilege_type <> 'CONNECT' OR a.is_grantable))
     OR EXISTS (SELECT 1 FROM pg_auth_members WHERE member='emr_financas_app'::regrole)
     OR EXISTS (SELECT 1 FROM pg_database WHERE datdba='emr_financas_app'::regrole)
     OR EXISTS (SELECT 1 FROM pg_roles WHERE rolname='emr_financas_app'
                AND (rolsuper OR rolcreatedb OR rolcreaterole OR rolreplication OR rolbypassrls OR rolinherit)) THEN
    RAISE EXCEPTION 'Administrative or unexpected direct database privileges rejected';
  END IF;
  FOR obj IN SELECT * FROM pg_namespace WHERE nspname !~ '^pg_' AND nspname<>'information_schema' LOOP
    IF has_schema_privilege('emr_financas_app',obj.oid,'CREATE')
       OR obj.nspowner='emr_financas_app'::regrole THEN
      RAISE EXCEPTION 'Schema creation/ownership rejected';
    END IF;
  END LOOP;
  FOR obj IN SELECT c.*,n.nspname FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
             WHERE n.nspname !~ '^pg_' AND n.nspname<>'information_schema' LOOP
    IF obj.relowner='emr_financas_app'::regrole THEN RAISE EXCEPTION 'Object ownership rejected'; END IF;
    IF obj.relkind IN ('r','p','v','m','f') THEN
      FOREACH priv IN ARRAY ARRAY['SELECT','INSERT','UPDATE','DELETE','TRUNCATE','REFERENCES','TRIGGER'] LOOP
        expected := obj.nspname='public' AND
          ((obj.relname IN ('tb_emr_usuario','tb_emr_gasto','tb_emr_recebimento','tb_emr_investimento')
            AND priv IN ('SELECT','INSERT','UPDATE','DELETE')) OR
           (obj.relname='tb_emr_password_reset_token' AND priv IN ('SELECT','INSERT','UPDATE')));
        IF has_table_privilege('emr_financas_app',obj.oid,priv) IS DISTINCT FROM expected
           OR has_table_privilege('emr_financas_app',obj.oid,priv||' WITH GRANT OPTION') THEN
          RAISE EXCEPTION 'Unexpected effective table privileges';
        END IF;
        IF priv IN ('SELECT','INSERT','UPDATE','REFERENCES') AND
           has_any_column_privilege('emr_financas_app',obj.oid,priv) IS DISTINCT FROM expected THEN
          RAISE EXCEPTION 'Unexpected effective column privileges';
        END IF;
      END LOOP;
    ELSIF obj.relkind='S' THEN
      FOREACH priv IN ARRAY ARRAY['USAGE','SELECT','UPDATE'] LOOP
        expected := obj.nspname='public' AND priv='USAGE' AND obj.relname IN
          ('seq_emr_usuario','seq_emr_gasto','seq_emr_recebimento','seq_emr_investimento','seq_emr_password_reset_token');
        IF has_sequence_privilege('emr_financas_app',obj.oid,priv) IS DISTINCT FROM expected
           OR has_sequence_privilege('emr_financas_app',obj.oid,priv||' WITH GRANT OPTION') THEN
          RAISE EXCEPTION 'Unexpected effective sequence privileges';
        END IF;
      END LOOP;
    END IF;
  END LOOP;
END $$;
COMMIT;
SELECT 'PROVISION_COMMITTED:emr_financas_app' AS provision_status
WHERE EXISTS (
  SELECT 1 FROM pg_roles
  WHERE rolname='emr_financas_app' AND rolcanlogin AND NOT rolinherit
    AND NOT (rolsuper OR rolcreatedb OR rolcreaterole OR rolreplication OR rolbypassrls)
    AND rolconnlimit=3
);
-- No password operation here. No change to the existing Render configuration.
