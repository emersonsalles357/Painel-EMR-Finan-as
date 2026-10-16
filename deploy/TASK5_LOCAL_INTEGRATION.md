# Task 5 — migração separada e runtime restrito

## Limites
Neon e ambientes publicados não foram autorizados nesta etapa A–C. Não usar os comandos de migração abaixo contra Neon sem aprovação expressa. O nome da branch Neon `production` não autoriza deploy. Não executar push/merge para validar esta integração.

## Arquitetura
O mesmo `app.jar` contém duas entradas independentes:
- HTTP: `java -jar app.jar`, profile `prod`, `emr_task5_app`, Flyway **false**, Hibernate **validate**.
- Migração de execução única: `DatabaseMigration`, sem contexto Spring, HTTP ou JPA; somente `emr_task5_migrator`. Após migrar, valida checksums e exige exatamente V001–V006 aplicadas.

O migrador é um comando, não um novo serviço. Não usa o Maven plugin Flyway. Na imagem Docker existente, substituir o entrypoint por `java` apenas para essa operação aprovada.

```text
java -Dloader.main=br.com.emr.emrfinancas.database.DatabaseMigration -cp app.jar org.springframework.boot.loader.launch.PropertiesLauncher
```

Adicionar `--validate-only` para validar sem aplicar migrações. Não há comando de clean, baseline, repair ou undo exposto. Uma migração anterior pode ter sido confirmada antes de uma falha posterior: interromper e diagnosticar; não limpar automaticamente.

## Variáveis por processo
Nunca passar credenciais por argumentos, arquivo versionado, build args ou VITE. Injetar pelo gerenciador seguro do processo e removê-las após a operação.

| Processo | Configuração |
|---|---|
| HTTP prod | `SPRING_PROFILES_ACTIVE=prod`; `DB_URL`, `DB_EXPECTED_HOST`, `DB_USERNAME=emr_task5_app`, `DB_PASSWORD`; demais JWT/CORS/SMTP existentes |
| Migração Neon, somente após autorização | `MIGRATION_MODE=neon`; `MIGRATION_DB_URL`; `MIGRATION_EXPECTED_HOST`; `MIGRATION_DB_USERNAME=emr_task5_migrator`; `MIGRATION_DB_PASSWORD` |
| Migração local descartável | `MIGRATION_MODE=local`; `MIGRATION_DB_URL`; `MIGRATION_DB_USERNAME=emr_task5_migrator`; `MIGRATION_DB_PASSWORD`; `MIGRATION_LOCAL_MARKER` |
| HTTP local descartável | `SPRING_PROFILES_ACTIVE=local-pg`; `EMR_LOCAL_PG_URL`, `EMR_LOCAL_PG_PASSWORD`, `EMR_LOCAL_PG_MARKER`, `JWT_SECRET`; bind somente loopback |

HTTP não pode receber `MIGRATION_DB_*`, `spring.flyway.url/user/password` ou uma configuração alternativa de datasource. O verificador inspeciona o pool efetivo antes de abrir sua conexão; depois verifica banco, session_user/current_user, schema public, PostgreSQL16, atributos e privilégios efetivos antes de Hibernate consumir o datasource.

Neon: host exato aprovado, endpoint direto, banco `emr_task5`, roles explícitas, `verify-full` e `DefaultJavaSSLFactory`. Schema fixado em `public`; sem parâmetros ou credenciais embutidos na URL. A conexão real também verifica TLS ativo. Não reduzir TLS para resolver falhas de confiança.

Local: URL somente em `127.0.0.1`, porta explícita não padrão, nome `emr_task5_it_<12 hex>`, comentário do banco com nonce aleatório de 32 hex. Essa política não admite bancos históricos ou endpoints remotos. TLS desabilitado apenas nesse perfil de loopback; não existe fallback de prod para local.

## Roles e ACLs
Criar a role migradora dedicada só após comprovar autoridade administrativa e aprovar sua criação. LOGIN sem SUPERUSER, CREATEDB, CREATEROLE, REPLICATION ou BYPASSRLS. A migradora deve ser proprietária dos objetos e ter USAGE/CREATE em public. O owner administrativo não é uma credencial de runtime.

Depois da migração, revisar/aplicar `task5-runtime-privileges.sql` no alvo aprovado. O harness aplica esse SQL **somente** no PostgreSQL novo local. O script exige `target_database` explícito e verifica o banco conectado antes de alterar ACLs.

O SQL revoga CREATE do app/PUBLIC no public, remove privilégios públicos no banco exclusivo e concede CONNECT ao app. Concede DML nas quatro tabelas funcionais, SELECT/INSERT/UPDATE nos tokens e USAGE nas cinco sequences. Não concede TRUNCATE, UPDATE de sequences, acesso ao histórico, memberships ou ownership. Novos objetos exigem revisão explícita de ACLs; não há grants genéricos futuros.

## Validação local reproduzível (Windows)
Pré-requisitos: Docker já em execução, imagem `postgres:16.15-bookworm` disponível, JDK17 e Maven/cache configurados. O harness não inicia Docker Desktop. `EMR_JAVA_HOME` e `EMR_MAVEN` permitem informar os caminhos locais das ferramentas.

Na raiz do clone:
```powershell
python tools/local-validation/run_maven.py build-suite -- clean verify
python tools/local-validation/validate_postgres.py
```

O runner Maven remove variáveis de banco, migração, SMTP e JVM herdadas. A suíte comum usa H2 e registra cinco testes PG como skips. O harness habilita esses cinco testes **somente** com a identidade local atestada, após migrar e aplicar ACLs. Os testes usam rollback transacional; a antiga limpeza TRUNCATE foi removida. Testes negativos de DDL usam apenas o banco descartável.

O harness cria um único container com nome/label aleatórios, restart=no, porta loopback e PGDATA em tmpfs, sem bind/volume. Confere imagem, labels, mounts e bindings antes de iniciar; nunca enumera ou monta volumes. Credenciais aleatórias ficam em memória. O HTTP recebe apenas a senha app; owner/migrator não fazem parte de seu ambiente.

Valida: marker incorreto recusado sem tabelas criadas; seis migrações; validação posterior de checksums; testes PG com app restrito; startup Hibernate validate; cadastro/login; CRUD financeiro; dashboard; 401/403 e isolamento entre usuários. O segredo de bootstrap entra por stdin, sem persistir em Config.Env do Docker. Ao encerrar, para e remove somente seu container reatestado e encerra os processos Java que criou. A identidade do container e o isolamento ficam no JSON; a tmpfs é descartada. Não há prune ou remoção global.

Evidências sanitizadas em `.superpowers/sdd/task5-local-integration/`: JSON por execução Maven e `postgres-<nonce>/result.json`, logs de migração e runtime. Relatórios de execução anterior com FAIL são preservados para rastreabilidade; usar a execução final PASS do relatório executivo.

## Próxima fase e rollback
Antes de Neon: aprovação de acesso somente leitura, validação externa do app/TLS, inventário de objetos/roles e SQL final de provisionamento/ACLs. Depois, autorizações específicas para ACLs, migração e publicação. Não presumir isolamento Git/Render/Vercel; reinspecionar integrações antes de qualquer push.

Reverter o diff local desfaz código/configuração, sem alterar banco externo. Para Neon, aprovar recuperação antes de migrar; rollback de código não desfaz schema. Não executar clean, repair, exclusão de banco ou migração reversa automaticamente. SMTP/porta permitida na Render Free e fronteira de confiança do proxy seguem pendentes para validação de publicação.
