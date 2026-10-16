-- REVIEW BEFORE USE. Local workflow never executes this file against Neon.
-- Prerequisites: migrator owns V001–V006 objects; application role exists.
-- psql -X -v ON_ERROR_STOP=1 -v target_database=<approved database> -f <this file>
\if :{?target_database}
\else
  \echo 'target_database must be explicit'
  \quit 3
\endif
SELECT current_database() = :'target_database'
   AND (current_database() = 'emr_task5'
        OR (current_database() ~ '^emr_task5_it_[a-f0-9]{12}$'
            AND shobj_description(oid,'pg_database') ~ '^emr-task5-disposable:[a-f0-9]{32}$')) AS target_ok
FROM pg_database WHERE datname=current_database() \gset
\if :target_ok
\else
  \echo 'Database target rejected'
  \quit 3
\endif
BEGIN;
DO $$
BEGIN
  IF pg_has_role('emr_task5_app','emr_task5_migrator','MEMBER')
     OR EXISTS (SELECT 1 FROM pg_database WHERE datname=current_database() AND pg_has_role('emr_task5_app',datdba,'MEMBER'))
     OR EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                WHERE n.nspname='public' AND pg_has_role('emr_task5_app',c.relowner,'MEMBER')) THEN
    RAISE EXCEPTION 'Application role has privileged membership or ownership';
  END IF;
END $$;
-- Database exclusive to Task 5: remove inherited PUBLIC privileges here only.
REVOKE ALL ON DATABASE :"target_database" FROM PUBLIC;
REVOKE ALL ON DATABASE :"target_database" FROM emr_task5_app;
GRANT CONNECT ON DATABASE :"target_database" TO emr_task5_app, emr_task5_migrator;
REVOKE CREATE ON SCHEMA public FROM PUBLIC, emr_task5_app;
GRANT USAGE ON SCHEMA public TO emr_task5_app;
REVOKE ALL ON TABLE public.tb_emr_usuario, public.tb_emr_gasto, public.tb_emr_recebimento,
  public.tb_emr_investimento, public.tb_emr_password_reset_token, public.flyway_schema_history FROM emr_task5_app, PUBLIC;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.tb_emr_usuario, public.tb_emr_gasto,
  public.tb_emr_recebimento, public.tb_emr_investimento TO emr_task5_app;
GRANT SELECT, INSERT, UPDATE ON TABLE public.tb_emr_password_reset_token TO emr_task5_app;
REVOKE ALL ON SEQUENCE public.seq_emr_usuario, public.seq_emr_gasto, public.seq_emr_recebimento,
  public.seq_emr_investimento, public.seq_emr_password_reset_token FROM emr_task5_app, PUBLIC;
GRANT USAGE ON SEQUENCE public.seq_emr_usuario, public.seq_emr_gasto, public.seq_emr_recebimento,
  public.seq_emr_investimento, public.seq_emr_password_reset_token TO emr_task5_app;
COMMIT;
-- No blanket/default grants: review runtime ACLs for each future object.
