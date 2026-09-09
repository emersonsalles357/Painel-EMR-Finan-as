# Fase 10 — relatório de preparação de produção

Data: 07/09/2026. Escopo: infraestrutura e hardening; nenhuma feature de produto, push, deploy ou migração de dados reais.

## 1. Estado inicial

Raiz Git encontrada na subpasta `Painel-EMR-Finan-as-main`, dentro do workspace. Branch `feature/emr-financas-v2`, HEAD `a196478` (relatório das Fases 8–9). Histórico recente conferido: a196478, bc00f00, d52a3f1, c6708f8, 4c1066d, c9c5940, 93a61fe, f67f417, d7cfbae, dac7085. Árvore inicialmente com alteração do usuário em `FrontEnd/pages/LoginPage.tsx`, preservada e excluída dos commits desta fase.

Baseline: frontend build aprovado, npm audit 0; backend 55 testes, 0 falhas/erros/ignorados. Antes disso, Maven falhou por cache padrão sem permissão e leitura de JARs bloqueada no sandbox; diagnóstico resolvido usando cache local e execução autorizada. Baseline executado no Java instalado 25.0.2, com alvo de compilação 17. Validação final refeita com JDK 17 real.

## 2. Neon

Configuração preparada: DB_URL/DB_USERNAME/DB_PASSWORD externos, DB_SSL_MODE=verify-full, factory TLS padrão Java. Produção exige hostname Neon, JDBC sem query/credenciais embutidas; isso impede que parâmetros na URL sobreponham validação TLS. Pool máximo 5, mínimo ocioso 0.

Credenciais Neon não estavam disponíveis nas variáveis da sessão. DNS, conexão, TLS negociado, SELECT 1, estado vazio/existente e migrations **no Neon: não executados**. Não há alegação de banco Neon já criado/configurado. Utilitário `tools/NeonPreflight.java` preparado, compilado em modo source-file Java 17 e verificado quanto à rejeição de credenciais ausentes; conexão externa não testada.

Para validação real local foi criado cluster descartável PostgreSQL 17.10 em `.phase10/pgdata`, bind 127.0.0.1:55432. SELECT 1 aprovado e schema public inicialmente com zero tabelas. Banco `postgres` usado exclusivamente pela suíte e banco novo `phase10_e2e` pelo fluxo local. TLS local não ativado; isso não constitui teste de TLS Neon. Nenhum serviço/banco antigo foi usado.

## 3. Schema: JPA, V001–V005 e PostgreSQL

| Migration | Revisão |
| --- | --- |
| V001 | Quatro entidades/tabelas, quatro sequences com allocationSize/incremento 1; BIGINT, VARCHAR com limites, NUMERIC(12,2), NUMERIC(8,2), DATE; PK, e-mail UNIQUE e três FKs financeiras nullable coerentes com JPA |
| V002 | Índices por cd_usuario nas três tabelas financeiras |
| V003 | Role VARCHAR(20), default USER, NOT NULL e CHECK USER/ADMIN; EnumType.STRING |
| V004 | Token SHA-256 VARCHAR(64) único, sequence incremento 1, FK obrigatória e índices; Instant nas entidades e TIMESTAMP nas migrations |
| V005 | Contador INT não nulo/default 0 e bloqueio TIMESTAMP, mapeados em Usuario |

Flyway + Hibernate validate e os testes de autenticação/reset/perfil/isolamento passaram em PostgreSQL 17.10. O mapeamento original de Instant também passou em verificação específica; não houve alteração de entidades/tipos/migrations. `hibernate.jdbc.time_zone=UTC` explicita conversão uniforme. Dados legados exigem verificar convenção de timezone antes de adoção. O índice explícito de token hash se sobrepõe ao índice da constraint UNIQUE; mantido para preservar migrations.

## 4. Dados

Banco antigo: não identificado/acessado. Relevância e contagens reais desconhecidas. Backup real: não realizado, pois não houve acesso a origem real. Migração de dados reais executada: **NÃO**. Não se decidiu iniciar Neon vazio nem descartar dados antigos. O checklist exige backup verificável, restauração em cópia e autorização antes de qualquer migração.

No banco descartável do E2E, auditoria confirmou quatro usuários de teste, zero grupos de e-mail duplicado, zero roles inválidas, zero proprietários nulos/órfãos e um token de reset já utilizado. Contagens finais após HTTP + navegador: 2 gastos, 2 recebimentos e 2 investimentos, todos com proprietário válido. Esses números representam exclusivamente dados descartáveis de teste.

## 5. Flyway

V001, V002, V003, V004 e V005: aplicadas com success=true no PostgreSQL local; verificadas no flyway_schema_history. Reexecução da suíte validou histórico existente sem reaplicar migrations. V001–V005 intactas no Git. Prod usa Flyway, validate-on-migrate=true, baseline-on-migrate=false, clean-disabled=true e Hibernate validate.

Aviso observado: Flyway 9.22.3 declara compatibilidade testada até PostgreSQL 15 e recomenda atualização para 17.10. Os testes reais locais passaram; isso não equivale a suporte oficial. Homologar a versão escolhida no Neon e decidir atualização de dependências separadamente antes da promoção.

## 6. V006

Criada: **NÃO**. Não há auditoria de dados reais que justifique NOT NULL nas FKs financeiras. Ausência de órfãos no banco de teste não comprova segurança de alterar produção. Consulta read-only de auditoria incluída.

## 7. SMTP

Configuração prod externa/provider-neutral, autenticação e STARTTLS obrigatórios, validação de identidade, timeout 5s e remetente obrigatório. Erros registram só a classe, evitando vazamento de corpo/link/token por mensagens de exceção. Link de recuperação usa FRONTEND_URL único; CORS possui configuração separada opcional.

SMTP externo: **não testado**, sem credenciais/destinatário externo controlado. SMTP local: envio real pelo SmtpEmailService para servidor de teste em 127.0.0.1:2525, conteúdo mantido em memória; remetente e link conferidos, token usado no reset, reutilização rejeitada e login com nova senha aprovado. Não comprova entrega externa, SPF/DKIM/DMARC, reputação ou TLS SMTP real. Homologação externa é bloqueio de promoção.

## 8. CORS

Uma configuração global /api/**, origens HTTPS exatas no prod: CORS_ALLOWED_ORIGINS ou FRONTEND_URL quando variável opcional ausente. Wildcards, origens com caminho/query e HTTP rejeitados no guard. Métodos GET/POST/PUT/PATCH/DELETE/OPTIONS; allowCredentials=true somente nas origens explícitas. Testes existentes de PATCH permitido/rejeitado preservados.

## 9. Proxy/IP

Removida leitura direta de X-Forwarded-For no AuthController. RemoteAddr identifica cliente, server.forward-headers-strategy=none impede adaptação global de headers sem confiança. Teste novo comprova que variar X-Forwarded-For/Forwarded não evita 429. Nenhum proxy/gateway de produção foi identificado; atrás de proxy os clientes compartilham bucket. Antes do deploy definir arquitetura, bloqueio de acesso direto e política do gateway; não habilitar confiança global apenas para recuperar IP.

## 10. JWT

Secret externo sem default; mínimo 32 bytes e diversidade mínima de caracteres no prod, geração criptográfica obrigatória. Validade em milissegundos, padrão 3600000 = 60 minutos, máximo prod 60 minutos. JWT antigo continua válido até expirar depois de troca/reset. Sem blacklist/refresh/revogação nesta fase.

## 11. Rate limiting

Em memória; uma instância ativa, reinício limpa buckets, sem sincronização entre instâncias. Isenção local desabilitada/recusada em prod. Redis não adicionado. Evolução: gateway/storage compartilhado para escala. Bloqueio persistido de conta continua 3 falhas/15 minutos e foi validado.

## 12. Frontend

npm install, TypeScript/Vite build e npm audit aprovados (0 vulnerabilidades conhecidas pelo npm). API padrão /api, URL explícita build-time para origem diferente; nenhum endpoint remoto antigo ativo no cliente. Mocks não ativados na API de produção. Preview ajustado para o mesmo configLoader runner dos demais comandos após falha de acesso do carregador padrão no sandbox.

Logo original preservada, 1.273,73 kB no build; não otimizada (opcional). Rotas do build servidas localmente com proxy /api; roteamento/fallback no host final ainda exige validação. Status sintéticos em realApi.ts: gastos=Pago, investimentos=Ativo; recebimentos usa status persistido. Filtros/edições de status sintético podem sugerir persistência inexistente: documentado como decisão futura, sem mudança de contrato. Regra consolidada preservada: recebimentos - gastos + investimentos; não é saldo disponível.

## 13. Backend

Java 17 verificado: Eclipse Temurin 17.0.20.1+1 baixado da API oficial Adoptium com SHA-256 validado, usado somente na pasta temporária ignorada. Spring Boot 3.2.5 preservado. Prod exige configurações, Neon, SMTP, Flyway e validate, recusa profiles dev/test misturados, seeds, isenção local e TLS reduzido. Falha sem configurações testada: exit=1, nenhum início de datasource/migration. Startup com configuração sintética e TLS reduzido (require) também foi rejeitado pelo guard antes de qualquer conexão.

Dockerfile: caminho backend explícito, build com testes, runtime Java 17 JRE e usuário não root, prod padrão. Docker build não executado: daemon indisponível nesta estação. JAR gerado: `BackEnd/Back End/backend/target/emr-financas-0.0.1-SNAPSHOT.jar`.
SHA-256: `EF96681AE43E91A7A0E61ED96666CE9F6A7C5341045CBF36E7690E6B8BB53707`.

## 14. Testes

| Execução | Testes | Falhas | Erros | Ignorados |
| --- | ---: | ---: | ---: | ---: |
| Baseline H2 | 55 | 0 | 0 | 0 |
| Após hardening H2 | 74 | 0 | 0 | 0 |
| PostgreSQL 17.10 | 74 | 0 | 0 | 0 |
| clean package Java 17 + H2 | 74 | 0 | 0 | 0 |
| Java 17 + PostgreSQL 17.10 final | 74 | 0 | 0 | 0 |

19 casos adicionais: 18 configurações/perfis/isolamento dev e 1 integração de spoofing de IP. Nenhum teste removido; quatro testes de rate limit agora definem IP do socket em vez de header não confiável. Preflight Java compila/rejeita ausência de variáveis. SQL de auditoria executado no PostgreSQL descartável. git diff --check aprovado. Logs temporários ficam em .phase10 (não versionados).

## 15. E2E

Fluxo HTTP local contra JAR Java 17 + PostgreSQL + SMTP capturador aprovado: cadastro, login, leitura/edição de perfil, troca autenticada de senha, senha anterior rejeitada, recebimento/gasto/investimento, dashboard 1000 - 100 + 200 = 1100, usuário B sem dados de A, IDOR rejeitado nos três recursos, chamada sem token 401, três falhas/bloqueio, solicitação de recuperação, remetente/link recebidos no SMTP local, reset, token de uso único e login novo. Esse fluxo é automatizado por HTTP, não um teste manual integral das 16 telas.

Inspeção interativa do build no navegador: cadastro, login, dashboard, perfil, nome salvo, criação e persistência dos três tipos de lançamento (gasto 25, receita 100 e investimento 50), dashboard visual com consolidado 125 e logout retornando ao login. Reset/troca/bloqueio foram validados via HTTP e suíte, sem caixa postal externa. E2E Neon e SMTP real permanece pendente; não declarar validação manual integral em produção.

## 16. Segurança

- **CRÍTICO:** nenhuma exposição real de segredo ou perda de dados identificada na inspeção; ausência de achado não certifica todo histórico externo.
- **ALTO:** publicação bloqueada por configuração/homologação Neon/SMTP/domínios/proxy ainda não disponíveis. Confiança cega em X-Forwarded-For, baseline automático e SMTP sem TLS no prod foram corrigidos.
- **MÉDIO:** JWT sobrevive a reset, rate limit em memória/proxy compartilhado, revisão de dependências backend e compatibilidade Flyway/PG 17 pendentes. Autenticação por token em localStorage mantém risco residual em caso de XSS; não refatorada nesta fase.
- **BAIXO:** logo grande, status sintéticos, índice redundante de hash, ausência de readiness dedicada. Health check Actuator não adicionado automaticamente.

## 17. Secrets e logs

Revisados arquivos de configuração atuais e histórico Git acessível com padrões de conexão com credenciais, chaves privadas, defaults de senhas e secrets. Único candidato encontrado nesse scan: secret explicitamente fictício do profile de testes em application-test.properties. Nenhuma credencial real encontrada; scan heurístico não é prova de inexistência de secrets arbitrários ou removidos fora do histórico acessível. Não houve necessidade comprovada de rotação; qualquer credencial real futuramente encontrada exige revogação/rotação no provedor, mesmo que removida do arquivo atual.

Senha, JWT completo, hash BCrypt, token reset e payload SMTP não foram impressos no relatório. Exceções de API/SMTP agora registram somente tipo, respostas públicas permanecem genéricas. Dumps/certificados/secrets/logs/temporários protegidos no Git e contexto Docker. Não há dump real no Git.

## 18. Arquivos criados

- BackEnd/Back End/backend/src/main/java/br/com/emr/emrfinancas/config/ProductionConfigGuard.java
- BackEnd/Back End/backend/src/test/java/br/com/emr/emrfinancas/ProductionConfigurationTests.java
- tools/NeonPreflight.java
- tools/audit-integrity.sql
- docs/PRODUCTION-CHECKLIST.md
- docs/FASE-10-RELATORIO.md

## 19. Arquivos alterados nesta fase

- .dockerignore, .env.example, .gitignore, Dockerfile, README.md, package.json
- BackEnd/Back End/backend/src/main/resources/application.properties
- BackEnd/Back End/backend/src/main/resources/application-dev.properties
- BackEnd/Back End/backend/src/main/resources/application-prod.properties
- BackEnd/Back End/backend/src/main/java/br/com/emr/emrfinancas/controller/AuthController.java
- BackEnd/Back End/backend/src/main/java/br/com/emr/emrfinancas/exception/GlobalExceptionHandler.java
- BackEnd/Back End/backend/src/main/java/br/com/emr/emrfinancas/service/SmtpEmailService.java
- BackEnd/Back End/backend/src/test/java/br/com/emr/emrfinancas/AuthenticationLockoutAndRateLimitTests.java

LoginPage.tsx é modificação preexistente, preservada. package-lock.json não sofreu alteração de conteúdo. Artefatos ignorados (.phase10, dist, target) não são arquivos fonte da entrega.

## 20. Arquivos removidos

Nenhum arquivo versionado removido; nenhuma migration antiga modificada. Nenhum banco/schema/tabela real excluído.

## 21. Git

Branch feature/emr-financas-v2. Commits locais: `52a4429` (configuração) e `e204552` (testes); documentação/preflight no commit `docs: add neon preflight and phase 10 production report`, consultável em git log. Árvore não limpa por alteração preexistente do usuário em LoginPage.tsx. Push: NÃO. Force push: NÃO. Reset destrutivo: NÃO. Nenhum remoto alterado.

## 22. Produção

**Pronto para deploy: NÃO.** Preparação técnica local implementada e validada; a Fase 10 não deve ser considerada encerrada integralmente enquanto faltarem validações externas e decisão de dados.

Bloqueios: credenciais e ambiente Neon autorizado; identificar banco anterior/backup se aplicável; SELECT 1/TLS/schema/Flyway no Neon; SMTP real com conta de teste e confirmação de entrega/reset; domínio/DNS/HTTPS/CORS/API final; arquitetura de proxy/IP; homologação da versão PostgreSQL/Flyway e revisão de dependências backend/imagem; build Docker se esse for o artifact escolhido; autorização explícita de publicação.

## 23. Próximo passo exato

1. Definir homologação Neon, banco anterior e domínios, sem enviar secrets pelo chat; configurar variáveis no secret manager/ambiente seguro.
2. Executar tools/NeonPreflight.java (somente leitura), revisar todos os schemas e tools/audit-integrity.sql quando compatível.
3. Havendo dados reais, seguir backup/restore de teste e plano do checklist; obter autorização para migração. Não decidir início vazio automaticamente.
4. Em homologação autorizada, aplicar Flyway controladamente pelo startup de uma única instância, conferir V001–V005 e Hibernate validate, repetir E2E com Neon/SMTP real.
5. Resolver DNS/HTTPS/proxy, validar CORS/API e versões/dependências; executar builds finais e guardar artifacts/checksums anteriores.
6. Solicitar autorização de deploy com configuração e resultados concretos. Somente após aprovação publicar backend e dist, executar smoke e monitorar. Rollback documentado no checklist; não executado.

Serviços temporários de backend, preview, SMTP e PostgreSQL encerrados ao fim da validação; dados locais descartáveis e evidências permaneceram na pasta ignorada .phase10, sem exclusão de banco.
