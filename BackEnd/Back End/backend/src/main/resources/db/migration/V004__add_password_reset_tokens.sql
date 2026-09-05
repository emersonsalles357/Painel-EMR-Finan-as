CREATE SEQUENCE seq_emr_password_reset_token START WITH 1 INCREMENT BY 1;

CREATE TABLE tb_emr_password_reset_token (
    cd_token BIGINT PRIMARY KEY,
    cd_usuario BIGINT NOT NULL,
    ds_token_hash VARCHAR(64) NOT NULL UNIQUE,
    dt_criacao TIMESTAMP NOT NULL,
    dt_expiracao TIMESTAMP NOT NULL,
    dt_utilizacao TIMESTAMP,
    CONSTRAINT fk_emr_token_usuario
        FOREIGN KEY (cd_usuario) REFERENCES tb_emr_usuario (cd_usuario)
);

CREATE INDEX idx_emr_token_hash ON tb_emr_password_reset_token (ds_token_hash);
CREATE INDEX idx_emr_token_usuario ON tb_emr_password_reset_token (cd_usuario);
