# Task 5 — checkpoints A–C concluídos

Data: 16/10/2026, validação final encerrada às 14:56 BRT. Parecer: **GO para preparação controlada da próxima fase Neon, mediante autorização específica**. Sem bloqueadores locais remanescentes. Migração Neon e publicação não estão autorizadas nem validadas.

## Resultado executivo

- Build Java17 / Spring Boot4.1.1: `clean verify`, exit0.
- Suíte completa: **255 descobertos, 250 executados, 5 skips PostgreSQL, zero falhas/erros**.
- PostgreSQL real: **5 executados, zero falhas/erros/skips**. São os mesmos cinco antes skipped; total de testes únicos verificados: **255**.
- Fluxo HTTP/persistência: **31 verificações**, incluindo 29 respostas HTTP, dashboard e persistência de usuários.
- PostgreSQL real: **16.15 (Debian 16.15-1.pgdg12+2)**, container novo com tmpfs, sem volumes/binds, porta loopback, restart=no.
- Migração standalone aplicou seis versões e validou seus checksums. Segunda execução `--validate-only`: zero migrações executadas, seis validadas.
- HTTP iniciou com profile `local-pg`, Hibernate validate e role `emr_task5_app`. Não recebeu credenciais de owner/migrator e não executou Flyway.
- CREATE TABLE/SCHEMA, ALTER, DROP, TRUNCATE e leitura do histórico negados com SQLState42501; startup também recusa ownership inesperado em outro schema.
- Revisão independente: achados corrigidos e reavaliados, sem pendências locais apontadas. Não é uma auditoria externa dos provedores.

## Separação e segurança

`DatabaseMigration` é uma entrada Java de execução única no mesmo JAR, via PropertiesLauncher, sem iniciar contexto Spring/HTTP/JPA. Usa somente a role migradora dedicada. `ProductionConfigGuard` exige Flyway desabilitado no HTTP, role app, host exato aprovado, banco emr_task5 e TLS verify-full. `DatabaseRuntimeVerifier` confere o Hikari efetivo antes de conectar, depois identidade, versão16, schema, atributos e ACLs antes de Hibernate consumir o datasource.

O SQL revisável concede CONNECT/USAGE, DML explícito e USAGE nas sequences; remove CREATE/ownership herdado como condição de aceitação e não concede acesso ao histórico. O SQL foi executado exclusivamente no container novo local. Owner e privilégios inesperados causam recusa; não há correção automática de ACLs externas.

A antiga limpeza TRUNCATE foi removida dos testes e substituída por rollback transacional. Alvos locais precisam de nome descartável, porta não padrão e marker validado antes do contexto. Produção mantém TLS obrigatório; TLS remoto e autenticação Neon permanecem para a próxima fase autorizada.

## Histórico Flyway observado

| Versão | Checksum Flyway | Estado | Identidade |
|---|---:|---|---|
| 001 | -1180337507 | SUCCESS | emr_task5_migrator |
| 002 | -1011916991 | SUCCESS | emr_task5_migrator |
| 003 | 839926003 | SUCCESS | emr_task5_migrator |
| 004 | 1651334590 | SUCCESS | emr_task5_migrator |
| 005 | 349451634 | SUCCESS | emr_task5_migrator |
| 006 | -1758127437 | SUCCESS | emr_task5_migrator |

V001–V006 permanecem iguais ao HEAD e às cópias empacotadas no JAR; hashes em `verification.json`.

## Evidências locais

Todas ficam em `.superpowers/sdd/task5-local-integration/` (ignoradas pelo Git):
- `reviewed-final-build.json` e `.log`: build e contagens por suíte.
- `postgres-29a9af053c9b/result.json`: isolamento, versão, histórico, ACLs e resultados HTTP.
- `postgres-29a9af053c9b/migration.log` e `migration-validate.log`: execução e revalidação sem HTTP.
- `postgres-29a9af053c9b/runtime.log`: inicialização real do Spring/JPA.
- `postgres-29a9af053c9b/unexpected-schema-rejection.log`: startup recusado por privilégio indevido.
- `postgres-live-29a9af053c9b.json`: cinco testes PostgreSQL executados.
- `verification.json`: hashes, preservação das migrações e inventário do diff.

JAR final: SHA256 `9e89ab5c8eea7610e042ee739a850721566cd43e6671dee810edfb80af13a7c4`. O JAR exercitado na integração é o mesmo produzido pelo build final. Branch task5/render-free-20261016 e HEAD continuam `6be16f0560127284cd1f4de737dc86955c7b1ab2`; alterações locais sem commit, push ou merge.

Tentativas anteriores foram preservadas como FAIL quando encontraram defeitos (logger do migrador e verificação de privilégios fora de public). Esses defeitos foram corrigidos e a execução final passou. Um bloqueio de attach JVM no sandbox foi resolvido executando Maven em processo nativo autorizado, sem desabilitar testes. A revisão também corrigiu persistência de segredo no Config.Env: bootstrap agora usa stdin; os containers anteriores próprios foram reatestados e removidos. Nenhuma credencial aparece neste relatório.

## Arquivos modificados/criados

- `BackEnd/Back End/backend/src/main/java/br/com/emr/emrfinancas/config/DatabaseRuntimeVerifier.java`
- `BackEnd/Back End/backend/src/main/java/br/com/emr/emrfinancas/database/DatabaseMigration.java`
- `BackEnd/Back End/backend/src/main/java/br/com/emr/emrfinancas/database/DatabaseTarget.java`
- `BackEnd/Back End/backend/src/main/resources/application-local-pg.properties`
- `BackEnd/Back End/backend/src/test/java/br/com/emr/emrfinancas/DatabaseTargetTests.java`
- `BackEnd/Back End/backend/src/test/java/br/com/emr/emrfinancas/config/DatabaseRuntimeVerifierTests.java`
- `BackEnd/Back End/backend/src/test/java/br/com/emr/emrfinancas/support/DisposablePostgresInitializer.java`
- `deploy/TASK5_LOCAL_INTEGRATION.md`
- `deploy/task5-runtime-privileges.sql`
- `docs/superpowers/plans/2026-10-16-task5-local-integration.md`
- `tools/local-validation/run_maven.py`
- `tools/local-validation/validate_postgres.py`
- `.env.example`
- `.gitignore`
- `BackEnd/Back End/backend/src/main/java/br/com/emr/emrfinancas/config/ProductionConfigGuard.java`
- `BackEnd/Back End/backend/src/main/resources/application-prod.properties`
- `BackEnd/Back End/backend/src/test/java/br/com/emr/emrfinancas/PostgresDatabaseLiveIntegrationTests.java`
- `BackEnd/Back End/backend/src/test/java/br/com/emr/emrfinancas/ProductionConfigurationTests.java`
- `BackEnd/Back End/backend/src/test/resources/application-postgres-it.properties`
- `README.md`
- `deploy/TASK5_RUNBOOK.md`

Este relatório também foi criado em `deploy/TASK5_LOCAL_RESULTS.md`.

## Limites e próxima fase

Nenhum acesso Neon, Render ou Vercel; nenhum volume histórico consultado, montado, iniciado ou removido. Todos os quatro containers próprios das tentativas foram parados/removidos após atestação; a execução final registrou ausência de segredo de bootstrap no Config.Env. Sem limpeza global Docker.

Próximo passo: autorizar preflight Neon somente leitura para comprovar login externo da role app, TLS com o runtime final, destino e ACLs/ownership. Depois, aprovar separadamente provisionamento da migradora, aplicação do SQL e migração. Antes de publicação, reinspecionar auto-deploy Git/Render/Vercel e resolver SMTP/porta e confiança do proxy. A imagem Docker do backend e o TLS remoto ainda não foram exercitados nesta validação Java17 local. Rollback de código não desfaz schema; não executar clean, repair ou exclusão de banco automaticamente.
