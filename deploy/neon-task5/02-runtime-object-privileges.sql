-- PREPARED ONLY. After V001–V006, connect as the object-owning migrator.
-- Database/schema ACLs belong to the owner stage 01; do not run those as migrator.
BEGIN;
DO $$
BEGIN
  IF current_database()<>'emr_task5' OR current_user<>'emr_task5_migrator'
     OR session_user<>'emr_task5_migrator' THEN
    RAISE EXCEPTION 'Wrong database or object owner';
  END IF;
  IF (SELECT count(*) FROM public.flyway_schema_history)<>6 OR EXISTS (
    SELECT 1 FROM (VALUES ('001',-1180337507),('002',-1011916991),('003',839926003),
      ('004',1651334590),('005',349451634),('006',-1758127437)) v(version,checksum)
    LEFT JOIN public.flyway_schema_history h ON h.version=v.version
    WHERE h.version IS NULL OR h.checksum IS DISTINCT FROM v.checksum OR h.success IS NOT TRUE
       OR h.installed_by<>'emr_task5_migrator') THEN
    RAISE EXCEPTION 'Migration history/checksums rejected';
  END IF;
END $$;
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
