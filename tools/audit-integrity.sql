-- Run only after confirming all V001-V005 tables exist. Read-only; no personal values returned.
BEGIN READ ONLY;
SELECT 'gastos' AS entidade, count(*) AS total,
 count(*) FILTER (WHERE f.cd_usuario IS NULL) AS sem_usuario,
 count(*) FILTER (WHERE f.cd_usuario IS NOT NULL AND u.cd_usuario IS NULL) AS orfaos
FROM tb_emr_gasto f LEFT JOIN tb_emr_usuario u USING (cd_usuario)
UNION ALL
SELECT 'recebimentos', count(*), count(*) FILTER (WHERE f.cd_usuario IS NULL),
 count(*) FILTER (WHERE f.cd_usuario IS NOT NULL AND u.cd_usuario IS NULL)
FROM tb_emr_recebimento f LEFT JOIN tb_emr_usuario u USING (cd_usuario)
UNION ALL
SELECT 'investimentos', count(*), count(*) FILTER (WHERE f.cd_usuario IS NULL),
 count(*) FILTER (WHERE f.cd_usuario IS NOT NULL AND u.cd_usuario IS NULL)
FROM tb_emr_investimento f LEFT JOIN tb_emr_usuario u USING (cd_usuario);
SELECT count(*) AS usuarios FROM tb_emr_usuario;
SELECT count(*) AS grupos_email_duplicado FROM
 (SELECT lower(trim(ds_email)) FROM tb_emr_usuario GROUP BY lower(trim(ds_email)) HAVING count(*) > 1) d;
SELECT count(*) AS roles_invalidas FROM tb_emr_usuario WHERE ds_role IS NULL OR ds_role NOT IN ('USER','ADMIN');
SELECT count(*) AS tokens_total,
 count(*) FILTER (WHERE dt_utilizacao IS NULL AND dt_expiracao > CURRENT_TIMESTAMP AT TIME ZONE 'UTC') AS tokens_ativos
FROM tb_emr_password_reset_token;
SELECT count(*) AS tokens_orfaos FROM tb_emr_password_reset_token t
LEFT JOIN tb_emr_usuario u USING (cd_usuario) WHERE u.cd_usuario IS NULL;
ROLLBACK;
