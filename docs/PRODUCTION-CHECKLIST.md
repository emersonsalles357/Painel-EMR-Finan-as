# Produção — checklist e roteiro

Estado desta entrega: preparação local; não autoriza publicação, push ou migração de dados reais. Registre operador, data, ambiente, revisão Git e evidências sem secrets para cada item.

## Antes de acessar banco real

- [ ] Confirmar endpoint Neon, região, versão PostgreSQL, banco, usuário e se é homologação/produção. Não inferir que banco desconhecido está vazio.
- [ ] Identificar banco anterior, responsável pelos dados e necessidade de migração. Sem acesso, estado permanece desconhecido.
- [ ] Configurar DB_* em secret manager, sem valores na linha de comando, Git ou logs.
- [ ] Usar endpoint direto Neon para aplicação controlada de migrations; pool Hikari pequeno (máximo 5, mínimo 0). Avaliar pooled endpoint somente após validação própria; o roteiro inicial usa direto.
- [ ] DB_URL deve conter só protocolo JDBC/host/porta/banco. Remover os parâmetros da string fornecida pelo console; sslmode e sslfactory são definidos separadamente. Não usar NonValidatingFactory, `sslmode=require` como substituto de validação, nem desabilitar certificados.

O driver usa `verify-full` e `org.postgresql.ssl.DefaultJavaSSLFactory`, com CAs confiáveis no Java. Falha de certificado deve ser diagnosticada (cadeia, hostname, relógio, truststore), nunca contornada. [Documentação pgJDBC](https://jdbc.postgresql.org/documentation/ssl/) e [segurança Neon](https://neon.com/docs/security/security-overview).

### Preflight JDBC sem migrations

Com DB_URL, DB_USERNAME, DB_PASSWORD e DB_SSL_MODE já exportados de maneira segura, execute na raiz (PowerShell; ajuste o caminho de cache caso necessário):

```powershell
$pgJar = Get-ChildItem "$env:USERPROFILE/.m2/repository/org/postgresql/postgresql/*/postgresql-*.jar" |
  Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName
java --class-path "$pgJar" tools/NeonPreflight.java
```

Nesta estação o cache Maven utilizado fica em `.m2/repository` na raiz do projeto. O utilitário não carrega Spring/Flyway: define sessão somente leitura, executa SELECT 1, consulta TLS negociado, usuário/banco, permissões, tabelas, colunas, constraints, índices, sequences e contagens das tabelas conhecidas; encerra com rollback. Falhas mostram somente classe/SQLState. O relatório de schema é operacionalmente sensível: armazenar fora do Git com acesso restrito.

- [ ] SELECT 1 aprovado; conexão demonstra DNS/conectividade; confirmar `ssl=true`, protocolo/cipher e usuário/banco esperados.
- [ ] Conferir schema public e **todos os outros schemas** retornados. Tabelas ausentes não provam ausência de dados em outro schema.
- [ ] Revisar permissões por objeto: SELECT/INSERT/UPDATE/DELETE nas tabelas e USAGE nas sequences para runtime; proprietário/DDL para migrations. As permissões de schema impressas pelo preflight não substituem essa revisão.
- [ ] Se V001–V005 já existem, executar `tools/audit-integrity.sql` com psql em sessão autenticada e verificar totais, órfãos, nulos, duplicados e roles. Não executar se o schema divergir sem adaptar consulta de leitura.

### Banco novo autorizado

Somente após inventário confirmar vazio e decisão de iniciar vazio: iniciar uma única instância do artifact com profile prod e variáveis completas, em homologação. Flyway aplica V001 a V005, valida checksums e Hibernate valida mapeamentos. Conferir:

```sql
SELECT version, description, checksum, success
FROM flyway_schema_history ORDER BY installed_rank;
```

Não habilitar baseline automático. Nenhuma migration V006 foi criada nesta fase.

### Banco existente e backup

Não iniciar a aplicação contra schema não revisado: o startup pode aplicar migrations pendentes. Antes de qualquer mudança:

1. Identificar origem e janela de consistência; interromper escritas na janela aprovada.
2. Configurar PGHOST, PGPORT, PGDATABASE, PGUSER, PGSSLMODE=verify-full e CA correta para libpq. Usar arquivo de senha protegido ou secret manager. Não colocar senha no comando.
3. Criar backup em local seguro **fora do repositório**:

```powershell
pg_dump --format=custom --file="$backupPath"
if ($LASTEXITCODE -ne 0) { throw 'Backup falhou' }
pg_restore --list "$backupPath"
Get-Item -LiteralPath $backupPath | Select-Object Length,LastWriteTimeUtc
Get-FileHash -LiteralPath $backupPath -Algorithm SHA256
```

`$backupPath` deve ser um caminho seguro definido pelo operador. Registrar origem (sem senha), data UTC, bytes, SHA-256, versão pg_dump, método e local protegido. Lista de conteúdo e checksum **não comprovam restauração**.

4. Restaurar em banco separado vazio, previamente autorizado, usando `pg_restore --exit-on-error --no-owner --no-acl --dbname="$restoreDatabase" "$backupPath"`. Nunca usar `--clean` contra produção.
5. Comparar contagens e integridade na cópia, tipos, nullability, constraints, índices, sequences (evitar colisão de IDs), JPA e migrations. Inventariar usuário, gastos, recebimentos, investimentos e tokens relevantes, sem exportar hashes/tokens para relatório público.
6. Produzir plano e pedir autorização antes de mover dados reais ou adotar histórico Flyway. Não executar baseline/repair automaticamente; schema legado não equivale necessariamente à V001.
7. Após migração autorizada, repetir inventário e comparar antes/depois. Qualquer divergência interrompe a promoção.

FK financeiras nullable: auditar `cd_usuario IS NULL` e órfãos nas três tabelas. Só avaliar V006 após evidência real, backup e solução de registros sem proprietário. Não atribuir usuários arbitrários nem excluir dados.

## SMTP e fluxo de recuperação

- [ ] SMTP autenticado com STARTTLS obrigatório e verificação de hostname; MAIL_FROM autorizado. Este roteiro suporta STARTTLS; provedor somente SSL implícito/465 exige adaptação validada antes de publicar.
- [ ] Timeout de conexão/leitura/escrita: 5 segundos. Logs não contêm mensagem SMTP completa nem token.
- [ ] Configurar SPF, DKIM, DMARC e limites de envio conforme provedor e domínio.
- [ ] Usar endereço de teste controlado: cadastro → solicitar recuperação → verificar chegada, spam, remetente e link `${FRONTEND_URL}/redefinir-senha?token=...` → abrir link → redefinir → login novo → token reutilizado rejeitado.
- [ ] Falha de envio preserva resposta pública genérica; observar alerta `Falha na entrega SMTP`, sem detalhes sensíveis. Sucesso de API não comprova entrega na caixa postal.
- [ ] Não registrar query strings de reset no frontend, proxy, analytics ou monitoramento; enviar `Referrer-Policy: no-referrer` no servidor estático.

## Frontend, proxy e segurança

- [ ] Definir domínio e DNS, emitir HTTPS, redirecionar HTTP no gateway e restringir acesso direto ao backend conforme arquitetura escolhida.
- [ ] Mesma origem: reverse proxy encaminha `/api` sem remover prefixo. Frontend `VITE_API_BASE_URL=/api`.
- [ ] Origens diferentes: build com URL HTTPS explícita terminada em /api e CORS restrito. Não incluir barra final nas origens.
- [ ] Servidor estático oferece fallback para index.html nas rotas React (`/login`, `/cadastro`, `/esqueci-senha`, `/redefinir-senha`, páginas autenticadas). Não aplicar fallback às chamadas API. Confirmar nomes no App.tsx antes de configurar regras específicas.
- [ ] CORS permite GET/POST/PUT/PATCH/DELETE/OPTIONS; credenciais só com origens exatas. Validar preflight PATCH aceito no domínio oficial e rejeitado em domínio estranho.
- [ ] O aplicativo usa RemoteAddr e `server.forward-headers-strategy=none`; ignora X-Forwarded-For/Forwarded. Não há proxy confiável identificado nesta entrega. Atrás de gateway, clientes podem compartilhar o mesmo limite de IP. Definir rate limiting no gateway e política de IP antes de uso público. Só adotar RemoteIpValve/resolver após documentar IPs/CIDRs confiáveis, bloqueio de acesso direto e sanitização da cadeia de headers.
- [ ] Uma instância ativa. Reinício limpa buckets. Múltiplas instâncias exigem limite no gateway/storage compartilhado; Redis é evolução, não implementado.
- [ ] JWT_SECRET aleatório e exclusivo por ambiente; validade padrão 60 minutos. Tokens antigos sobrevivem à troca/reset; revogação é risco residual aceito explicitamente antes da promoção.
- [ ] Não combinar prod/dev/test, habilitar seeds, H2, mocks, DEBUG, SQL/bind logging ou captura de Authorization/body.
- [ ] Revisar dependências backend e imagens com scanner de vulnerabilidades da plataforma. npm audit cobre apenas frontend; Spring Boot 3.2.5/Flyway 9.22.3 são versões herdadas, não há declaração de ausência de CVEs backend nesta entrega.

## Build, deploy e verificação

- [ ] Executar builds/testes do README; registrar revisão e SHA-256 do JAR. Testar em Java 17 e PostgreSQL da versão escolhida no Neon.
- [ ] Flyway 9.22.3 alerta que sua versão suportada/testada vai até PostgreSQL 15; os testes locais em 17.10 passaram. Para produção, validar versão selecionada e decidir atualização de dependências em mudança separada ou homologar formalmente a combinação, sem assumir suporte oficial.
- [ ] Rodar todos os 16 fluxos solicitados em homologação Neon/SMTP real. Testes locais não substituem essa aprovação.
- [ ] Após autorização explícita de deploy, configurar secrets na plataforma, publicar backend (`java -jar ...` ou imagem Docker) e frontend `dist/` com API definida durante build.
- [ ] Smoke test HTTPS: login, perfil, dados persistidos, dashboard, isolamento, recuperação, bloqueio e navegação direta nas rotas.
- [ ] Não há Actuator novo. Probe TCP confirma processo/porta, não prontidão do banco. Uma chamada sem token a `/api/auth/me` deve retornar 401 genérico; monitor autenticado com conta de teste pode validar dependências. Não interpretar `/` 404 como falha automática.
- [ ] Monitorar 5xx, latência, reinícios, conexões Neon, falhas SMTP, 429 e capacidade. Definir responsável e destino de alertas; nunca coletar tokens ou senhas.

## Rollback (somente plano)

Aplicação: guardar commit, JAR/imagem e bundle frontend anteriores com checksum. Se schema for compatível, reimplantar artifact anterior e variáveis da mesma versão, após autorização. Não usar reset --hard/force push.

Banco: não editar migrations aplicadas nem executar down migration improvisada. Restaurar backup **em banco separado**, comparar integridade e confirmar ponto de recuperação/perda potencial de escritas posteriores. Só mudar conexão/cutover após aprovação explícita; preservar banco original para investigação. Migrações futuras devem prever compatibilidade com versão anterior. Nenhum rollback foi executado.
