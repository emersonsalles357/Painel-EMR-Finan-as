# Task 5 — operação experimental pendente de autorização

Documento histórico preparatório. Para migração/runtime e validação local, prevalece [TASK5_LOCAL_INTEGRATION.md](TASK5_LOCAL_INTEGRATION.md). O clone está no commit de referência 6be16f0; este roteiro não comprova o estado atual dos provedores.

## Revisão e origem

- Repositório: `emersonsalles357/Painel-EMR-Finan-as`.
- Base: `cb2520b54d88a40027297744be6e1c62a9832257`.
- Branch proposta: `task5/render-free-20261016`, sem merge em `main`.
- Dos 190 arquivos de aplicação, 182 permanecem byte a byte iguais à candidata Task 4; oito fontes de teste diferem exclusivamente pela remoção de uma linha vazia excedente no EOF, conforme correção local de formatação. O manifesto registra os hashes efetivos normalizados. Onze arquivos já rastreados no HEAD (documentação, exemplos vazios e ferramentas) foram preservados. Não importar os relatórios/evidências privados ou o working tree histórico inteiro.
- O manifesto publicável com caminhos/hashes e a varredura ficam fora do clone, em `.task5-20261016/` na raiz externa. Reexecutar a conferência antes de stage/commit/push.
- A configuração antiga em documentos preservados não substitui os requisitos efetivos abaixo.

## Gate 1 — Git

Pedir autorização específica para criar o commit e enviar a nova branch. Nenhum comando abaixo foi executado:

```powershell
git add --pathspec-from-file=../../../.task5-20261016/stage-paths.txt
git diff --cached --check
git diff --cached --stat
git commit -m "chore: prepare validated EMR V2 candidate for experimental Render deployment"
git push --set-upstream origin task5/render-free-20261016
```

Executar somente dentro deste clone. O arquivo `stage-paths.txt` é uma allowlist gerada a partir das alterações efetivamente revisadas; não usar `git add .` no workspace externo. Confirmar o SHA produzido e o SHA remoto após o push.

Push pode acionar integrações externas desconhecidas. A branch é nova e `main` não será alterada; não há workflow GitHub Actions nos arquivos presentes. Serviços e integrações de conta continuam sem inspeção. Se for encontrado serviço apontando para essa branch, interromper antes de push/deploy e pedir autorização específica. Não mudar auto-deploy de serviços existentes.

## Gate 2 — contas e recursos

Entrar nas contas Render/Neon pelo navegador, sem enviar senhas ou tokens no chat. Inspecionar primeiro serviços, projetos, planos, quotas, regiões e auto-deploy. A existência de um banco não autoriza seu uso: comprovar isolamento e finalidade sintética.

Antes de criar ou alterar recursos, apresentar IDs/nomes/região/destino, plano efetivamente gratuito, revisão Git aprovada e eventual cobrança por quotas. Não adicionar cartão, plano pago, domínio ou serviço de produção. Criar somente um Web Service experimental e um banco/projeto Neon isolado, se necessários e aprovados. Reutilizar frontend existente apenas após identificar domínio, projeto, branch e controle pelo responsável.

## Banco Neon e TLS

- PostgreSQL 16 preferido; confirmar versão real no plano/conta. Banco exclusivo, vazio e sintético. Nunca importar backups/dados do histórico.
- Usar endpoint **direto** (sem `-pooler`). Migração e runtime usam processos e roles separados; Hikari limita o runtime a três conexões por padrão.
- `DB_URL` deve ser `jdbc:postgresql://<host>.neon.tech:5432/<database>`, sem `?`, fragmento, usuário ou senha embutidos.
- `DB_SSL_MODE=verify-full`; `DefaultJavaSSLFactory` usa a confiança do Java e o driver valida o hostname. Não baixar a segurança para contornar falhas.
- Confirmar handshake real com a imagem Java final e a conta aprovada: conectividade, cadeia/hostname, `pg_stat_ssl.ssl=true` e versão do servidor. Compatibilidade documental/local não prova conexão remota.
- Papel de aplicação dedicado: `emr_task5_app`, LOGIN sem atributos administrativos, CONNECT/USAGE/DML/sequences, sem CREATE, ownership ou acesso ao histórico Flyway. A role migradora dedicada possui os objetos e só é usada no processo de migração. SQL revisável em `task5-runtime-privileges.sql`; qualquer execução Neon exige autorização separada.
- Inspeção JDBC de leitura já disponível em `tools/NeonPreflight.java`; carregar variáveis por mecanismo seguro de processo, não por argumentos com credenciais. Executar no banco identificado, nunca em produção. Saída contém metadados do banco e deve permanecer local/sanitizada.
- Após autorização explícita, o migrador standalone aplica `V001`–`V006` e valida checksums/histórico. Só então configurar ACLs aprovadas e iniciar `prod`, que mantém Flyway desabilitado.
- `baseline-on-migrate=false`, `clean-disabled=true`, `ddl-auto=validate`. Não criar limpeza automática ou executar testes de integração locais contra Neon.

## Serviço Render

Configuração e estratégia de deploy controlado na Render:
- **Estratégia de Deploy sem contaminação da `main`:** A Render acompanha a branch `main`. Para validar e testar a Task 5 experimentalmente antes de qualquer merge na `main`, o deploy será realizado exclusivamente via **Manual Deploy → Deploy a specific commit**, informando o SHA publicado na branch isolada `task5/render-free-20261016`. Isso impede a publicação prematura ou acidental da `main`.
- **Configuração do serviço (após autorização expressa):**

| Campo | Valor |
|---|---|
| Tipo/runtime | Web Service / Docker |
| Nome proposto | `emr-v2-task5-api` (confirmar disponibilidade) |
| Plano | Free, confirmar no painel antes de criar |
| Branch | `task5/render-free-20261016`, SHA aprovado |
| Modo de Deploy | **Manual Deploy → Deploy a specific commit** (informando o commit SHA isolado) |
| Root Directory | raiz do repositório |
| Dockerfile | `./Dockerfile` |
| Docker Context | `.` |
| Docker Command | vazio: manter ENTRYPOINT Java |
| Health Check | `/health` |
| Auto-Deploy | Off no **novo** serviço; nenhuma mudança em serviços existentes |
| Porta/bind | `server.port=${PORT:8080}`, bind padrão em todas as interfaces; verificar no runtime |
| Região | escolher próxima ao Neon após confirmar opções gratuitas |

Dockerfile raiz: build Maven/Temurin 17, JAR `app.jar`, runtime `eclipse-temurin:17-jre`, usuário `emr`. Não usar o Dockerfile do subdiretório com o contexto da raiz. Tags base são móveis: registrar digests efetivos do build remoto; não chamar builds de byte a byte reproduzíveis sem fixação/verificação dos digests.

| Variável | Configuração |
|---|---|
| SPRING_PROFILES_ACTIVE | `prod` |
| DB_URL | JDBC Neon direto, sem query ou credenciais |
| DB_USERNAME / DB_PASSWORD | credenciais novas da role isolada, painel seguro |
| DB_SSL_MODE | `verify-full` |
| DB_POOL_MAX_SIZE / DB_POOL_MIN_IDLE | `3` / `1` |
| JWT_SECRET | aleatório de alta entropia, >=32 bytes e >=16 caracteres distintos; painel seguro |
| JWT_EXPIRATION | `3600000` milissegundos |
| FRONTEND_URL | origem HTTPS real, sem barra final/path |
| CORS_ALLOWED_ORIGINS | origens HTTPS exatas separadas por vírgula; sem wildcard |
| PORT | usar a variável atribuída pela Render |
| MAIL_HOST / MAIL_USERNAME / MAIL_PASSWORD / MAIL_FROM | SMTP de testes válido; obrigatório no código prod |
| MAIL_PORT | porta permitida pela Render e comprovada pelo relay, por exemplo 2525 se suportada |
| MAIL_AUTH / MAIL_STARTTLS | `true` / `true` |

Nunca colocar segredos em Dockerfile, build args, VITE, YAML versionado ou relatórios. Não configurar `SPRING_MAIL_TEST_CONNECTION=true` para forçar conectividade em cada boot. O código mantém STARTTLS obrigatório e validação de identidade SMTP.

Render Free bloqueia saída nas portas 25/465/587; o default SMTP 587 da candidata não funcionará para envio nessa infraestrutura. Um relay compatível em 2525 pode preservar o código e TLS, mas deve ter acesso/plano verificados. Não criar conta paga nem migrar para API de e-mail nesta preparação. Se não houver relay disponível, registrar recuperação de senha como BLOCKED; não usar credenciais fictícias para passar a guarda.

Free tem memória limitada (512 MB conforme página de preços consultada), disco efêmero e pode reiniciar. Registrar RSS/OOM/restarts quando disponíveis, sem pressupor que o heap corresponde ao total de memória. Não ajustar JVM sem evidência de necessidade; não usar keep-alive.

## Frontend

Identificar hospedagem existente antes de publicar. O build local desta preparação usa `/api` e não é o artefato desacoplado final. Rebuild aprovado com `VITE_API_BASE_URL=https://<api-real>/api`; clientes usam caminhos `/auth/...`, `/dashboard/...` etc., sem acrescentar outro `/api`. Verificar no navegador a URL efetiva de cada chamada.

Publicar na hospedagem identificada exige autorização específica para esse projeto. `vercel.json` mantém rewrite para `/index.html`. Se a hospedagem existente for outra, conferir sua regra SPA e validar refresh de rotas. CORS exige a origem final no backend; a troca exige aprovação externa.

## Verificação funcional e métricas (NÃO EXECUTADAS REMOTAMENTE)

1. Registrar serviço, URL, SHA/digest, logs sanitizados, versão Java/Boot, tempo Spring até READY, Flyway e `/health`. `/health` por si só não prova consulta ao banco: confirmar também perfil, dashboard e CRUD.
2. Em navegador: cadastro sintético, login, dashboard utilizável, gastos/recebimentos/investimentos, logout, expiração/401, CORS, falha de rede/recuperação e refresh SPA. Recuperação de senha somente com caixa/relay de testes autorizado. Não gravar tokens, senhas ou dados de contas reais nas evidências.
3. Warm: uma requisição por vez, pausa de pelo menos 2 segundos. Começar com n=3 para health/login válido/perfil/dashboard; n=1 para login inválido e cada operação CRUD/consulta financeira. Respeitar o rate limiter e o bloqueio por falhas. Não repetir erro de login em série.
4. Registrar cada duração em relógio monotônico e status. Separar DNS/TCP/TLS/TTFB/total quando disponíveis. Não gravar corpos de login ou headers Authorization. Para n suficiente, mínimo/mediana/p95 nearest-rank/máximo; indicar n. Em n=3 o p95 coincide com o máximo e tem pouca precisão. Não estimar métricas ausentes.
5. Cold: fechar abas da aplicação e interromper clientes/pings/monitoramentos externos; anotar última requisição conhecida em UTC/BRT. Aguardar pelo menos 16–20 minutos sem tráfego próprio e obter evidência de hibernação no painel/logs antes da primeira chamada. Checks internos do provedor não são pings manuais. Registrar tráfego desconhecido como limitação.
6. Medir primeira resposta **útil** (HTTP 200 com payload esperado; página de loading do provedor não conta), readiness, boot Spring e conexão/migrations separadamente. Observar primeiro acesso pela SPA com DevTools e sem chamar health previamente. Registrar login→dashboard utilizável e comportamento do loading.
7. Repetir segunda janela se houver tempo; sem evidência de hibernação, classificar UNVERIFIED. Sem teste remoto, INCONCLUSIVE. Não comparar os 7,6s locais diretamente ao cold start total.

## Rollback e preservação

Interromper testes em caso de cobrança inesperada, exposição de segredo, acesso a dados não sintéticos, migrations em alvo incorreto, OOM/restarts recorrentes, falha TLS, CORS ou autenticação. Preservar evidências sanitizadas. Solicitar autorização para suspender o novo serviço ou reverter a revisão do frontend experimental; não excluir banco nem executar migrations reversas automaticamente. Reverter código não reverte schema. Não tocar em serviços de produção.

Nenhum Docker/volume histórico é necessário. D3/Q permanecem fora do escopo. Não iniciar Docker Desktop automaticamente: containers com política de restart podem iniciar volumes históricos. O daemon estava indisponível na preparação e o estado atual dos volumes não foi reinspecionado; nenhuma operação de montagem/start foi executada.

## Fontes oficiais consultadas em 2026-10-16

- [Render Free: hibernação, quotas, SMTP e limites](https://render.com/docs/free)
- [Render: auto-deploy e deploy manual](https://render.com/docs/deploys)
- [Render: health checks](https://render.com/docs/health-checks)
- [Render: preços e memória](https://render.com/pricing)
- [pgJDBC: SSL e DefaultJavaSSLFactory](https://jdbc.postgresql.org/documentation/ssl/)
- [Neon: pooled/direct](https://neon.com/docs/connect/connection-pooling)
- [Twilio: SMTP 2525 com TLS, quando disponível na conta](https://www.twilio.com/docs/sendgrid/for-developers/sending-email/integrating-with-the-smtp-api)

Os documentos públicos não confirmam planos, quotas ou permissões da conta do responsável.
