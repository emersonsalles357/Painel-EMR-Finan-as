# EMR Finanças V2

Aplicação React + TypeScript com API Java 17 / Spring Boot 3.2.5, Spring Security stateless, JWT, BCrypt, roles USER/ADMIN, isolamento por usuário, perfil, recuperação de senha e dashboard com dados reais. Banco PostgreSQL, com Neon previsto para produção; schema gerenciado por Flyway V001–V005 e Hibernate `validate`.

## Execução local

Requisitos: JDK 17, Node.js compatível com Vite 7 (20.19+ ou 22.12+), PostgreSQL e SMTP de teste. A raiz do código está nesta pasta; backend em `BackEnd/Back End/backend`.

1. Use `.env.example` como referência. O Spring não importa esse arquivo automaticamente. Exporte apenas as variáveis necessárias no processo; não copie credenciais de produção para desenvolvimento.
2. Configure `SPRING_PROFILES_ACTIVE=dev`, `DEV_DB_URL`, `DEV_DB_USERNAME`, `DEV_DB_PASSWORD`, `JWT_SECRET` e `FRONTEND_URL=http://localhost:5173`.
3. Configure SMTP local nas variáveis `MAIL_*`; apenas em dev podem ser usados `MAIL_AUTH=false` e `MAIL_STARTTLS=false`. Seeds são opcionais, somente dev, e usam BCrypt.
4. No backend: `./mvnw spring-boot:run` (Windows: `mvnw.cmd`).
5. Na raiz: `npm install`, depois `npm run dev`. O proxy Vite encaminha `/api` para localhost:8080.

O profile dev usa **DEV_DB_*** e ignora DB_* de produção. `test` usa H2 isolado. Não combine prod com dev/test. O Vite carrega seus arquivos `.env` convencionais; variáveis VITE_* são públicas no bundle e nunca devem conter secrets.

## Validação e artefatos

```sh
npm install
npm run build
npm audit
cd "BackEnd/Back End/backend"
./mvnw clean test
./mvnw clean package
```

Frontend: `dist/`. Backend: `target/emr-financas-0.0.1-SNAPSHOT.jar`. Testes H2 não substituem PostgreSQL real. Consulte o relatório da Fase 10 para os resultados efetivamente executados e as limitações.

## Produção

Use `SPRING_PROFILES_ACTIVE=prod`. Configure no gerenciador de secrets:

| Variável | Requisito |
| --- | --- |
| DB_URL | `jdbc:postgresql://<endpoint>.neon.tech/<database>`, sem query string ou credenciais embutidas |
| DB_USERNAME / DB_PASSWORD | Credenciais do ambiente Neon autorizado |
| DB_SSL_MODE | `verify-full` (padrão); valida certificado e hostname usando truststore Java |
| FRONTEND_URL | Origem HTTPS final, sem barra final, caminho, query ou fragmento; base do link de reset |
| CORS_ALLOWED_ORIGINS | Opcional, lista explícita de origens HTTPS; quando ausente usa FRONTEND_URL; não definir como vazio |
| JWT_SECRET | Gerar com CSPRNG ao menos 32 bytes aleatórios, codificados em Base64; não reutilizar exemplos |
| JWT_EXPIRATION | Milissegundos, padrão e máximo em prod 3600000 (60 minutos) |
| MAIL_HOST / MAIL_PORT | Servidor SMTP do provedor; porta STARTTLS, normalmente 587 |
| MAIL_USERNAME / MAIL_PASSWORD | Credenciais SMTP obrigatórias em prod |
| MAIL_AUTH / MAIL_STARTTLS | `true` em produção; STARTTLS obrigatório, validação de identidade habilitada |
| MAIL_FROM | Endereço remetente validado pelo provedor |
| PASSWORD_RESET_EXPIRATION_MINUTES | 15 por padrão |
| VITE_API_BASE_URL | Build: `/api` na mesma origem, ou `https://<dominio-api>/api` em origens diferentes |
| PORT | Porta do backend, 8080 por padrão |

A validação de produção acontece antes da criação do datasource e recusa defaults inseguros, TLS reduzido, baseline automático, seeds, isenção de rate limit, origens inválidas e segredo JWT curto/repetitivo. A checagem de formato não comprova entropia: a geração criptográfica continua obrigatória.

O Dockerfile gera o backend com testes, usa Java 17 JRE e usuário sem root, ativa prod e não embute secrets. O frontend deve ser publicado separadamente. Não há plataforma final de deploy configurada.

## Banco, segurança e operação

Não altere V001–V005. `baseline-on-migrate=false`, `clean-disabled=true`, `ddl-auto=validate`. Iniciar a aplicação pode aplicar migrations pendentes: **execute o preflight antes de iniciar contra Neon**. Banco existente requer backup restaurado para teste, comparação e autorização antes de migrar. Não foi criada V006 sem evidência do banco real.

O backend usa o IP do socket (`RemoteAddr`) e ignora headers encaminhados; sem arquitetura de proxy validada, todos os clientes de um proxy compartilham buckets. Rate limit em memória pressupõe uma instância; reinício limpa buckets. Não habilite headers globais sem revisar a fronteira de confiança. Veja o roteiro de operação.

JWT anterior à troca/reset permanece válido até expirar. A fórmula preservada é `recebimentos - gastos + investimentos`, apresentada como **valor consolidado**. Status de gastos (`Pago`) e investimentos (`Ativo`) em `FrontEnd/services/realApi.ts` são sintéticos; status de recebimentos existe no backend. Não houve mudança de regra financeira ou persistência de status nesta fase.

- [Roteiro de produção, preflight, backup, SMTP e rollback](docs/PRODUCTION-CHECKLIST.md)
- [Relatório e evidências da Fase 10](docs/FASE-10-RELATORIO.md)
- [Auditoria read-only de integridade](tools/audit-integrity.sql)
- [Preflight JDBC read-only, sem Spring/Flyway](tools/NeonPreflight.java)
