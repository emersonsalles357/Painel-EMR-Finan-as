ALTER TABLE tb_emr_usuario
    ADD COLUMN ds_role VARCHAR(20) DEFAULT 'USER' NOT NULL;

ALTER TABLE tb_emr_usuario
    ADD CONSTRAINT ck_emr_usuario_role CHECK (ds_role IN ('USER', 'ADMIN'));
