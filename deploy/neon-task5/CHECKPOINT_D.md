# Checkpoint D — bloqueado por ausência de credencial

16/10/2026. HEAD preservado: 6be16f0560127284cd1f4de737dc86955c7b1ab2.
JAR preservado: SHA256 9e89ab5c8eea7610e042ee739a850721566cd43e6671dee810edfb80af13a7c4.

| Verificação | Resultado desta execução |
|---|---|
| Credencial app disponível no processo | NÃO: DB_PASSWORD e demais variáveis de conexão ausentes |
| Autenticação externa | BLOQUEADA / NÃO TESTADA; não houve rejeição de senha pelo servidor |
| TLS/identidade/endpoint direto | NÃO TESTADOS; hostname aprovado também indisponível |
| Consultas Neon | ZERO: preflight encerrou antes de DriverManager.getConnection |
| Permissões e inventário atuais | NÃO REVALIDADOS |
| Estado conhecido anteriormente | app sem atributos administrativos, CONNECT/USAGE, mas CREATE direto em public; banco sem tabelas/histórico, conforme informações do responsável |
| Runtime pronto | NÃO: banco ainda sem migrations e CREATE informado é incompatível com o runtime restrito |

**NO-GO para autorizar/executar alterações agora.** Falta comprovar login, alvo/TLS, ACLs/inventário e recuperação disponível. O resultado não invalida A–C. Nenhum Spring/Flyway/Docker/serviço publicado foi iniciado nesta etapa, nem foi procurada/redefinida senha. Alterações A–C e JAR foram preservados.

## Retomar somente leitura, sem compartilhar a senha

No mesmo terminal privado em que o processo Java será iniciado, usar o segredo **já existente** do cofre do responsável. Não inserir senha em texto de comando, histórico, chat ou arquivo. O wrapper pede senha sem eco, cria variáveis apenas no processo e restaura o ambiente ao terminar:

```powershell
# Na raiz do clone validado; este comando faz exclusivamente o preflight autorizado.
powershell -NoProfile -File tools/Invoke-NeonPreflight.ps1
```

Antes de informar o host, selecionar no Neon: projeto emr-task5-20261016, branch production, banco emr_task5, role emr_task5_app, pooling desligado. Confirmar Oregon e o host direto nessa seleção. SQL por si só não comprova o mapeamento host→projeto/branch. O preflight exige esse host exato, banco/role/public/PostgreSQL16 e transação read-only. Usa Java17 e o pgJDBC extraído do JAR validado, sem Spring/Flyway. As consultas SELECT incluem atributos, memberships herdadas, ACLs, schemas, ownership, objetos e eventual histórico. A autenticação verify-full valida certificado/hostname; pg_stat_ssl é evidência adicional da conexão no servidor.

Se o segredo não estiver disponível no cofre, parar e solicitar ao responsável sua disponibilização segura; não tentar recuperar, descobrir ou redefinir a senha. Exportar variáveis em outro terminal não as injeta automaticamente neste agente. O responsável pode executar o wrapper nesse terminal e disponibilizar somente o log sanitizado.

## Sequência única de mudanças — PREPARADA, NÃO AUTORIZADA/NÃO EXECUTADA

1. **Gate:** preflight completo aprovado e inventário sem colisões; capturar horário UTC e capacidade real de recuperação. Confirmar autoridade de emr_task5_owner para criar roles e conceder ACLs. App sem ownership/memberships inesperadas; qualquer exceção interrompe o fluxo.
2. **Recuperação:** conferir no Console a janela efetiva e o earliest restore point da branch. Registrar instante anterior à mudança dentro dessa janela; aprovar criação de branch de segurança/restauração separadamente. Não criar nem restaurar nesta etapa. Não contar com rollback de código ou com restauração de senha/ACL sem verificar o comportamento real do provedor. Se não houver recuperação comprovada, NO-GO.
3. **Provisionar via SQL** com owner: executar `01-provision-migrator.sql`. Cria a migradora sem atributos administrativos, remove CREATE do app/PUBLIC, concede conexão e USAGE/CREATE somente à migradora. `\password` pede senha nova da migradora interativamente; nenhuma senha literal no script. Não criar via Console/API/CLI, pois essas interfaces concedem neon_superuser segundo a documentação Neon. O script não modifica a senha app. Se a etapa de senha falhar, interromper.
4. **Migrar uma vez**, em processo separado e limpo, com variáveis MIGRATION_*; nenhuma credencial owner/app nem overrides SPRING_DATASOURCE_/SPRING_FLYWAY_/FLYWAY_. Exigir saída `executed=6 validated=6 http=false`. Falha: interromper, preservar histórico; sem repair/clean/baseline/retry automático.
5. **Revalidar** com `--validate-only`: exigir `executed=0 validated=6`, seis SUCCESS e checksums correspondentes à evidência A–C.
6. **Aplicar DML/sequences** com a identidade migradora proprietária dos objetos, executando `02-runtime-object-privileges.sql`. Não executar antes das seis migrations: seus objetos ainda não existem. Este arquivo contém somente ACLs dos objetos; ACLs de banco/schema já foram aplicadas pelo owner na etapa 01. Não usar o script local combinado como migradora no Neon: ali ela não é proprietária do banco. CREATEROLE não concede propriedade dos objetos.
7. **Preflight app novamente**, sem credencial DDL: conferir LOGIN/TLS/identidade; CONNECT/USAGE/DML e USAGE de sequences; sem CREATE/TEMP, ownership, memberships ou acesso ao histórico. O preflight não inicia HTTP. Somente após novo gate autorizar startup prod/Render com DB_* app e sem MIGRATION_*.

### Comandos exatos para execução FUTURA aprovada

Na raiz do clone. Valores de host/senhas não foram preenchidos. `PGHOST` deve ser o host direto aprovado; `PGDATABASE=emr_task5`, `PGPORT=5432`, `PGSSLMODE=verify-full`, `PGSSLROOTCERT` aponta para um bundle CA confiável validado. Não substituir por require/disable se o truststore falhar. psql instalado nesta máquina: `C:\Program Files\PostgreSQL\17\bin\psql.exe`.

```powershell
# Após autorização de alterações, sessão owner: -W pede senha sem argumento/arquivo.
& 'C:\Program Files\PostgreSQL\17\bin\psql.exe' -X -W -v ON_ERROR_STOP=1 -U emr_task5_owner -f deploy/neon-task5/01-provision-migrator.sql
```

Em processo novo, injetar via gerenciador seguro: MIGRATION_MODE=neon; MIGRATION_DB_URL (JDBC sem query/credenciais, banco emr_task5); MIGRATION_EXPECTED_HOST (mesmo host aprovado); MIGRATION_DB_USERNAME=emr_task5_migrator; MIGRATION_DB_PASSWORD. Não definir DB_PASSWORD nesse processo.

```powershell
$java = Join-Path $env:EMR_JAVA_HOME 'bin/java.exe'
$jar = 'BackEnd/Back End/backend/target/app.jar'
& $java '-Dloader.main=br.com.emr.emrfinancas.database.DatabaseMigration' -cp $jar org.springframework.boot.loader.launch.PropertiesLauncher
if ($LASTEXITCODE -ne 0) { throw 'Migration failed: stop' }
& $java '-Dloader.main=br.com.emr.emrfinancas.database.DatabaseMigration' -cp $jar org.springframework.boot.loader.launch.PropertiesLauncher --validate-only
if ($LASTEXITCODE -ne 0) { throw 'Validation failed: stop' }
# Em outra sessão psql, com identidade migradora, após as seis migrations:
& 'C:\Program Files\PostgreSQL\17\bin\psql.exe' -X -W -v ON_ERROR_STOP=1 -U emr_task5_migrator -f deploy/neon-task5/02-runtime-object-privileges.sql
```

Checksums esperados V001–V006: -1180337507, -1011916991, 839926003, 1651334590, 349451634, -1758127437. A conexão read-only de migradora pode conferir `SELECT version,checksum,success,installed_by FROM public.flyway_schema_history ORDER BY installed_rank;` em transação READ ONLY; app não deve conseguir ler o histórico após ACLs. Não testar DDL negativo no Neon.

## Recuperação Free: documento versus conta

A documentação oficial consultada em 16/10/2026 informa janela Free de **6 horas**. Isso não confirma a janela efetiva, o restore point ou quotas deste projeto. Conferir esses dados no Console antes de aprovar qualquer mudança. Recuperação é operação externa separada; não executar restore nem criar branch sem autorização. Como roles são copiadas entre branches, controlar também credenciais/ACLs da branch de segurança.

Fontes: https://neon.com/blog/neon-free-plan-1-gb-per-project ; https://neon.com/docs/manage/projects ; https://neon.com/docs/manage/roles ; https://neon.com/docs/introduction/branching .
