ALTER TABLE tb_emr_usuario
    ADD COLUMN nr_falhas_login INT DEFAULT 0 NOT NULL;

ALTER TABLE tb_emr_usuario
    ADD COLUMN dt_bloqueio_ate TIMESTAMP;
