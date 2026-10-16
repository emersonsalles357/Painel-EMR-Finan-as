# Neon original — pacote de segurança para aprovação

16/10/2026, America/Sao_Paulo. **SQL de provisionamento não executado.** Houve somente uma inspeção focal de metadados em BEGIN READ ONLY/ROLLBACK pelo SQL Editor autenticado; não houve autenticação JDBC. O script 00 completo também permanece como procedimento revisável.

Destino: projeto EMR-Financas (`winter-breeze-35941853`), branch production (`br-billowing-wind-ac4rinfg`), host direto `ep-icy-cherry-acw6ysuv.sa-east-1.aws.neon.tech`, banco `neondb`, schema `public`, PostgreSQL16. Pooling desativado. Role HTTP proposta: `emr_financas_app`. Nenhuma autorização de escrita/publicação decorre da preparação destes arquivos.

## Plano mínimo

1. Executar/revisar `00-inspect-readonly.sql` no destino atestado, sem conteúdo de registros. Confirmar a identidade efetiva de conexão da Render por inspeção segura dos parâmetros não secretos no painel (`srv-da3ltrbtqb8s739qcjl0`). A role indicada pelo usuário é `neondb_owner`, mas isso ainda não prova qual role a Render efetivamente usa.
2. Somente após autorização de provisionamento da role, aplicar `10-provision-app.sql` (`-v approve_original_app_provision=yes`): cria exclusivamente `emr_financas_app` sem senha, com `NOINHERIT`, sem atributos administrativos e concede apenas os privilégios mínimos por objeto. **Este script não altera `PUBLIC`.** Qualquer erro aborta a transação.
3. Revisar os clientes antes de alterar `PUBLIC` e aprovar especificamente `20-revoke-public-temp.sql` (`-v approve_revoke_public_temp=yes`) para retirar `TEMPORARY` de `PUBLIC` em `neondb`. A inspeção focal confirmou os dois default ACLs (`cloud_admin`/`public`/tabelas e sequences para `neon_superuser`, sem afetar `emr_financas_app`) e o grant direto `CTc` para `neondb_owner` (que conserva `TEMP`). Outros clientes dependentes somente de `PUBLIC` perdem `TEMP` em `neondb`. Essa etapa é exigida pelo guarda Java (`DatabaseTarget.verifyRuntime`, que exige `NOT has_database_privilege(current_user, current_database(), 'TEMP')`).
4. O operador define a PRIMEIRA senha da nova role em canal interativo seguro (`\password emr_financas_app` via `psql` autenticado como `neondb_owner`, com `HISTFILE` desativado), em etapa expressamente autorizada. Não redefinir `neondb_owner`. Não enviar senha ao chat, usar argumentos de shell, salvar em fontes ou registrá-la em logs/histórico SQL.
5. Executar preflight JDBC da role app (`.\tools\Invoke-OriginalNeonPreflight.ps1`), somente leitura, no JAR local conferido. Só aceitar exit 0 e linha `preflight=PASS`. Não iniciar HTTP/Flyway para testar conectividade.
6. Em caso de necessidade de reversão autorizada, executar `90-rollback-provision.sql` (`-v approve_rollback_neon_original=yes`), que restaura `GRANT TEMPORARY ON DATABASE neondb TO PUBLIC` e remove `emr_financas_app` caso não possua sessões ativas.
7. Apresentar configuração e artefato finais para autorização separada de publicação. Não trocar a credencial da Render histórica nesta etapa. Preferir serviço experimental controlado antes de qualquer substituição de runtime existente.

## SQL revisável e privilégios

`00-inspect-readonly.sql` usa `BEGIN READ ONLY`/`ROLLBACK`, lista atributos/memberships/ACLs, clientes agregados e histórico Flyway. Não imprime senhas, hashes de senha, tokens, conteúdo financeiro ou textos de queries. As credenciais de acesso são fornecidas pelo operador fora do arquivo.

`10-provision-app.sql` requer `psql` 16+, `-v ON_ERROR_STOP=1` e flag explícita `approve_original_app_provision=yes`. Não utiliza `\quit` (que no `psql` ignora argumentos numéricos e encerra com código `0`): todas as pré-condições (`approve_original_app_provision=yes`, `HOST`, `PORT`, `current_database()='neondb'`, `current_user='neondb_owner'`, `session_user=current_user`, versão PostgreSQL 16 e checksums `V001`–`V006`) são avaliadas dentro de `BEGIN; ... DO $$ BEGIN ... RAISE EXCEPTION ... END $$;`, forçando rollback imediato e saída `exit 3` em qualquer rejeição. Na arquitetura de proxy TLS do Neon, o TLS 1.3 (`verify-full`) é validado na ponta externa pelo cliente (`libpq` / `pgJDBC`) e encerrado no proxy antes do processo `postgres` (onde `pg_stat_ssl.ssl` vale `false`); portanto, `pg_stat_ssl.ssl=false` no backend não é tratado como falha de TLS externo. Ao final do `COMMIT`, emite `PROVISION_COMMITTED:emr_financas_app`.

`20-revoke-public-temp.sql` requer `psql` 16+, `-v ON_ERROR_STOP=1` e flag explícita `approve_revoke_public_temp=yes`. Executa isoladamente `REVOKE TEMPORARY ON DATABASE neondb FROM PUBLIC;` com exceções SQL em `BEGIN..COMMIT`, validando que `neondb_owner` preserva `CONNECT`/`TEMP`/`CREATE` e que `emr_financas_app` passa a ter `TEMP=false` e `CREATE=false`.

`90-rollback-provision.sql` requer `psql` 16+, `-v ON_ERROR_STOP=1` e flag explícita `approve_rollback_neon_original=yes`. Restaura `GRANT TEMPORARY ON DATABASE neondb TO PUBLIC`, revoga os grants de `emr_financas_app` e remove a role (desde que sem sessões ativas).

| Objetos | Privilégios app | Necessidade no código |
|---|---|---|
| tb_emr_usuario | SELECT, INSERT, UPDATE, DELETE | autenticação/perfil/admin e UsuarioService.delete |
| tb_emr_gasto, tb_emr_recebimento, tb_emr_investimento | SELECT, INSERT, UPDATE, DELETE | consultas/totais e CRUD via repositories |
| tb_emr_password_reset_token | SELECT, INSERT, UPDATE | criação, localização e invalidação de tokens; sem DELETE |
| cinco seq_emr_* correspondentes | USAGE | geração de IDs via nextval; sem setval/UPDATE ou SELECT |
| neondb / public | CONNECT / USAGE | acesso ao banco/schema |
| flyway_schema_history | nenhum | HTTP não usa Flyway |

A role nasce LOGIN, NOINHERIT, sem senha, sem atributos administrativos e sem memberships recebidas; limite de três conexões. O SQL não transfere ownership, não reescreve objetos/dados/histórico, não concede DDL, grant option, EXECUTE de rotinas de usuário, TRUNCATE, TRIGGER ou REFERENCES. As verificações finais comparam os privilégios efetivos de tabelas/colunas/sequences aos necessários e recusam ownership, memberships ou CREATE/TEMP. Não remove grants públicos de tabelas/schemas/sequences para contornar uma recusa.

DROP/ALTER não são privilégios independentes revogáveis nas tabelas: sua prevenção depende de ausência de ownership, memberships privilegiadas e atributos administrativos. NOINHERIT isoladamente não é suficiente; memberships também precisam estar ausentes. Privilégios de PUBLIC são aditivos e não admitem um DENY individual que anule a herança. [Privilégios PostgreSQL16](https://www.postgresql.org/docs/16/ddl-priv.html), [GRANT](https://www.postgresql.org/docs/16/sql-grant.html).

O escopo de ACL preparado é neondb. A role PostgreSQL pertence à branch e pode herdar CONNECT/TEMP de PUBLIC em outros bancos; não se afirma isolamento de todos os bancos da branch. Inventariar esse alcance no preflight administrativo antes da aprovação e interromper caso seja exigida remoção em bancos administrativos; nenhuma mudança fora de neondb está proposta. Defaults de novos objetos não são alterados: cada migração futura exige revisão das ACLs e revalidação antes de uso HTTP.

## Adaptação local Java preparada

- `DB_TARGET_ID=neon-original` seleciona somente o host acima, neondb e emr_financas_app. Identidade ausente mantém a política anterior de Task5, que não aceita neondb. Seleção desconhecida é recusada. Banco/host/role fixados no código impedem que editar DB_EXPECTED_HOST aprove outro destino.
- `ProductionConfigGuard` e `DatabaseRuntimeVerifier` usam a mesma seleção. Continuam exigindo Flyway=false, ddl-auto=validate, schema public, SSL verify-full/DefaultJavaSSLFactory, perfis exclusivos e ausência de credenciais de migração. O pool real é conferido antes da abertura da conexão; ACLs, ownership e identidade são conferidos antes de Hibernate usar a DataSource.
- Nenhum default de conexão/credencial original foi inserido em application-prod.properties. A alteração nesse arquivo é somente comentário da seleção explícita. O segredo permanece separado da URL.
- `DatabaseMigration` não foi liberado para neondb. V001–V006 já têm checksums correspondentes; não há necessidade de criar uma role migradora ou transferir ownership para essa reutilização. Uma eventual migração futura será outra intervenção, com role de manutenção apropriada, privilégios avaliados e autorização própria. Não usar neondb_owner no HTTP.
- `DatabaseRuntimePreflight` é uma entrada Java independente, sem Spring/HTTP/JPA/Flyway; conecta com default_transaction_read_only=on, confirma transaction_read_only, reaproveita a política de destino/identidade/ACL e encerra com rollback. Não lê conteúdo de linhas, não chama nextval e não faz testes negativos de DDL contra o original.

## Comandos de validação — preparados, não executados remotamente

Pré-requisitos: cliente psql16+ para as etapas administrativas; CA raiz confiável revisada para libpq; credencial existente fornecida no prompt oculto. TLS libpq é independente do truststore Java. Não reduzir verify-full em caso de falha. Executar do diretório raiz do clone e não usar opção de eco de comandos secretos.

```powershell
# 1. SOMENTE LEITURA; nenhum valor de senha no comando.
$env:PGSSLMODE = 'verify-full'
$env:PGSSLROOTCERT = '<caminho-absoluto-da-CA-confiavel-revisada>'
psql -X -W -h ep-icy-cherry-acw6ysuv.sa-east-1.aws.neon.tech -p 5432 -d neondb -U neondb_owner -f deploy/neon-original/00-inspect-readonly.sql
```

Os comandos de escrita abaixo são apenas a proposta para aprovação posterior. NÃO executar nesta fase. Os flags não substituem autorização humana.

```powershell
# 2. Provisionamento isolado da role emr_financas_app (sem alterar PUBLIC):
psql -X -W -h ep-icy-cherry-acw6ysuv.sa-east-1.aws.neon.tech -p 5432 -d neondb -U neondb_owner -v approve_original_app_provision=yes -f deploy/neon-original/10-provision-app.sql

# 3. Revogação explícita e separada de TEMPORARY de PUBLIC em neondb:
psql -X -W -h ep-icy-cherry-acw6ysuv.sa-east-1.aws.neon.tech -p 5432 -d neondb -U neondb_owner -v approve_revoke_public_temp=yes -f deploy/neon-original/20-revoke-public-temp.sql

# 4. Definição interativa da primeira senha (hash SCRAM-SHA-256 no cliente, sem histórico):
psql -X -W -h ep-icy-cherry-acw6ysuv.sa-east-1.aws.neon.tech -p 5432 -d neondb -U neondb_owner -v HISTFILE=none -c "\password emr_financas_app"
```

Depois da autorização de criação e definição segura da primeira senha pelo operador, validar a role HTTP com o JDK17 aprovado e o hash do NOVO JAR registrado em VALIDATION.json. O launcher pede somente a senha existente de emr_financas_app, injeta-a apenas no ambiente do processo filho e filtra mensagens para não expor detalhes de conexão. Não é o wrapper antigo de testes nem o migrador.

```powershell
.\tools\Invoke-OriginalNeonPreflight.ps1 -JavaHome '<JDK17-aprovado>' -JarSha256 '<sha256-do-JAR-em-VALIDATION.json>'
```

Resultado aceito: exit0 e `preflight=PASS authenticated=true tls=verify-full identity=approved privileges=restricted readOnly=true`. SQLState28xxx indica falha de autenticação; outras recusas exigem diagnóstico sem relaxar TLS/ACLs. Handshake TLS Python anterior não substitui esse teste. Um PASS somente leitura não comprova CRUD funcional; login/navegação/persistência virão no ensaio Render autorizado.

Configuração HTTP futura: prod; DB_TARGET_ID=neon-original; DB_EXPECTED_HOST exato; DB_USERNAME=emr_financas_app; DB_URL construído a partir do host e neondb, sem parâmetros/segredos; DB_PASSWORD em secret seguro; DB_SSL_MODE=verify-full; máximo3 conexões, mínimo1 ou valor revisado. Preservar demais requisitos JWT/CORS/SMTP/proxy. Não aplicar esses valores na Render durante esta preparação.

## Verificação e parecer

Resultados locais em VALIDATION.json. A evidência A–C de 255 testes únicos e PostgreSQL real permanece herdada, sem reexecução extensa. Esta entrega executa apenas os testes focados e package, a recusa do novo preflight antes de conectar quando não há configuração e a análise sintática do launcher. SQL preparado passa por revisão estática; **não foi executado nem validado por PostgreSQL** nesta fase. Não se alega autenticação JDBC remota ou aprovação de runtime publicado.

**GO para revisão/aprovação do pacote local. NO-GO para execução remota agora:** confirmar a role real da Render e aceitar o impacto PUBLIC TEMP sobre eventuais clientes, aprovar o SQL exato e a definição inicial segura da credencial. Os dois default ACLs foram examinados e não exigem mudança. Não há bloqueador estrutural de migração identificado; permanecem os gates de segurança e publicação.

Antes de autorizar, considerar que PUBLIC TEMP afeta novas operações em sessões existentes que dependam dessa herança, não somente reconexões. O owner observado mantém seu grant explícito; não há promessa de impacto zero para clientes não inventariados. Erro anterior a COMMIT desfaz a transação. Depois de COMMIT, recuperação de ACL requer nova autorização; não remover role, encerrar sessões ou restaurar PUBLIC TEMP automaticamente.

Nenhuma alteração Neon/Render/Vercel, uso do projeto de testes, comando Docker, acesso ao volume histórico, commit, push, merge ou deploy nesta preparação.

## Evidência remota focal desta preparação

Consulta pelo SQL Editor confirmou neondb/neondb_owner e transaction_read_only=on; encerramento ROLLBACK observado. Default ACLs: cloud_admin em public, tipo r para neon_superuser com a*r*w*d*D*x*t*, tipo S com r*w*U*. A sessão app futura não terá membership nessa role. Catálogo retornou neondb com PUBLIC TEMP/CONNECT e owner com CREATE/TEMP/CONNECT, além de postgres/cloud_admin com ACL nula. Essa ACL nula usa os defaults PostgreSQL, inclusive PUBLIC CONNECT/TEMP; não é ausência de privilégios. Portanto a role proposta pode herdar acesso a postgres, fora do alvo HTTP fixado; nenhuma revogação em postgres foi preparada. Se isolamento de toda a branch for requisito, interromper e aprovar solução específica antes de uso.

Na amostra de pg_stat_activity havia apenas uma sessão neondb_owner/neon-internal-sql-editor ativa. Isso não identifica a role configurada na Render nem prova ausência de clientes intermitentes, pois o serviço pode estar hibernando. Não foi acessada a URL HTTP nem iniciado/reiniciado qualquer serviço Render.
