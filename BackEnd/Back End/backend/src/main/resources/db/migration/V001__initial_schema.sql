CREATE SEQUENCE seq_emr_usuario START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_emr_gasto START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_emr_recebimento START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_emr_investimento START WITH 1 INCREMENT BY 1;

CREATE TABLE tb_emr_usuario (
    cd_usuario BIGINT PRIMARY KEY,
    nm_usuario VARCHAR(100) NOT NULL,
    ds_email VARCHAR(120) NOT NULL UNIQUE,
    ds_senha VARCHAR(100) NOT NULL
);

CREATE TABLE tb_emr_gasto (
    cd_gasto BIGINT PRIMARY KEY,
    ds_descricao VARCHAR(120) NOT NULL,
    ds_categoria VARCHAR(80) NOT NULL,
    vl_gasto NUMERIC(12, 2) NOT NULL,
    dt_gasto DATE NOT NULL,
    ds_forma_pagamento VARCHAR(60),
    ds_observacao VARCHAR(255),
    cd_usuario BIGINT,
    CONSTRAINT fk_emr_gasto_usuario
        FOREIGN KEY (cd_usuario) REFERENCES tb_emr_usuario (cd_usuario)
);

CREATE TABLE tb_emr_recebimento (
    cd_recebimento BIGINT PRIMARY KEY,
    ds_descricao VARCHAR(120) NOT NULL,
    ds_origem VARCHAR(80) NOT NULL,
    vl_recebimento NUMERIC(12, 2) NOT NULL,
    dt_recebimento DATE NOT NULL,
    ds_status VARCHAR(40) NOT NULL,
    cd_usuario BIGINT,
    CONSTRAINT fk_emr_receb_usuario
        FOREIGN KEY (cd_usuario) REFERENCES tb_emr_usuario (cd_usuario)
);

CREATE TABLE tb_emr_investimento (
    cd_investimento BIGINT PRIMARY KEY,
    nm_investimento VARCHAR(120) NOT NULL,
    ds_tipo VARCHAR(80) NOT NULL,
    ds_instituicao VARCHAR(100),
    vl_aplicado NUMERIC(12, 2) NOT NULL,
    nr_rentabilidade NUMERIC(8, 2),
    dt_aplicacao DATE NOT NULL,
    cd_usuario BIGINT,
    CONSTRAINT fk_emr_inv_usuario
        FOREIGN KEY (cd_usuario) REFERENCES tb_emr_usuario (cd_usuario)
);
