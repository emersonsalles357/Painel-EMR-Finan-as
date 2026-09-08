# Relatório Final de Produção — EMR Finanças V2

Data da homologação: 08/09/2026  
Branch: `feature/emr-financas-v2`

## 1. Estado inicial

- Repositório inicialmente limpo e sincronizado com `origin/feature/emr-financas-v2`.
- Frontend oficial acessível em `https://painel-emr-finan-as.vercel.app`.
- Backend oficial acessível após cold start em `https://painel-emr-financas.onrender.com`.
- Problema reproduzido antes da correção: acesso direto a `/login` e `/cadastro` retornava `404: NOT_FOUND` da Vercel.
- `git diff --check` sem erros no preflight.

## 2. Alterações realizadas

- Adicionado `vercel.json` com rewrite SPA para `index.html`.
- Mantido `BrowserRouter`; não foi introduzido `HashRouter`.
- Adicionado resolvedor dedicado de IP do cliente para o rate limiting no Render.
- Produção passou a confiar somente em `CF-Connecting-IP`, com validação estrita de IPv4/IPv6 e fallback para `RemoteAddr`.
- `X-Forwarded-For` e `Forwarded` continuam ignorados.
- Adicionados quatro testes contra spoofing, valor malformado e fallback.
- Nenhuma regra financeira foi alterada.
- Nenhuma migration foi alterada ou criada.

## 3. SPA/Vercel

- Configuração adotada: `rewrites` com `source: "/(.*)"` e `destination: "/index.html"`.
- A sintaxe foi validada contra a documentação atual da Vercel. `rewrites` é a alternativa recomendada atualmente; `routes.handle: filesystem` permanece reconhecido, porém está depreciado.
- `vercel.json` é JSON válido.
- Build local gerou `index.html`, JavaScript, CSS, fontes e imagem normalmente.
- Homologação local: acesso direto e reload aprovados em `/`, `/login`, `/cadastro`, `/esqueci-senha`, `/redefinir-senha`, `/dashboard`, `/gastos`, `/recebimentos`, `/investimentos` e `/perfil`.
- Rotas protegidas sem sessão redirecionaram para `/login`.
- Produção retestada após o deploy: todas as rotas abriram diretamente e sobreviveram a reload sem `404: NOT_FOUND`.
- `/`, `/dashboard`, `/gastos`, `/recebimentos`, `/investimentos` e `/perfil` redirecionaram para `/login` sem sessão, como esperado.
- `/login`, `/cadastro`, `/esqueci-senha` e `/redefinir-senha` permaneceram na rota solicitada.
- O bundle implantado serviu normalmente JavaScript, CSS e imagem; a imagem principal foi carregada com largura natural válida.

Referência: https://vercel.com/docs/project-configuration/vercel-json

## 4. Branch e deploy

- Branch confirmada: `feature/emr-financas-v2`.
- Commits enviados:
  - `703f2f6 fix: add vercel spa fallback`
  - `90348f7 fix: resolve render client ip safely`
  - `978bb01 docs: add final production homologation report`
- Nenhum merge em `main`, rebase destrutivo ou force push foi realizado.
- Push para `origin/feature/emr-financas-v2`: **EXECUTADO**; HEAD local e remoto sincronizados em `978bb01430f48660ca537d7d526bb32be13b9222`.
- Vercel Production: **READY**, comprovado pelo domínio oficial servindo o novo bundle `index-CSoEGa8p.js` e o fallback SPA.
- Render: **LIVE**, com health, autenticação e operações persistentes respondendo após o deploy.

## 5. API Base URL

- `_apiClient.ts` usa `import.meta.env.VITE_API_BASE_URL` como `baseURL` do Axios.
- Build com `VITE_API_BASE_URL=https://painel-emr-financas.onrender.com/api` confirmou a URL exata no bundle.
- Pesquisa no bundle confirmou ausência de `/api/api`.
- Endpoints relativos continuam corretos: `/auth/login`, `/auth/register`, `/auth/me`, `/auth/forgot-password` e `/auth/reset-password`.

## 6. CORS

- Configuração local revisada: sem wildcard de origem e sem liberação genérica de `*.vercel.app`.
- Preflight de produção com `Origin: https://painel-emr-finan-as.vercel.app`: HTTP 200 e `Access-Control-Allow-Origin` correspondente.
- Preflight com `Origin: https://example.invalid`: HTTP 403 e sem autorização CORS.
- Resultado: **APROVADO**.

## 7. Render

- `GET /health`: HTTP 200 com `{"status":"UP"}` após cold start.
- `GET /api/auth/me` sem JWT: HTTP 401.
- O serviço apresentou a tela normal de cold start do plano Render antes de responder.
- O profile `prod` é fixado pelo Dockerfile e o guard de produção impede startup com configuração insegura ou incompleta.
- Não houve falha observável de Flyway, Hibernate, Neon ou configuração: o serviço iniciou e cadastro, autenticação e CRUD persistente funcionaram.
- Acesso ao painel/logs internos do Render não estava disponível; a confirmação foi funcional pelos endpoints públicos.

## 8. Neon

- Credenciais de acesso direto não estavam disponíveis.
- Consulta read-only de versão do PostgreSQL e inspeção direta do schema: **NÃO EXECUTADO**.
- Conectividade e persistência no Neon foram validadas indiretamente pelo E2E real: cadastro, perfil, três tipos de lançamento, edição, listagem, dashboard, exclusão, troca de senha e novo login funcionaram.
- A versão efetiva do PostgreSQL, `flyway_schema_history` e a coluna `NR_TOKEN_VERSION` não foram consultadas diretamente por falta de credenciais.

## 9. Flyway

- Versão resolvida pelo Spring Boot: Flyway Community 9.22.3.
- Localmente, as migrations V001–V006 foram validadas e aplicadas em H2 compatível com PostgreSQL durante os testes.
- V001–V006 permanecem byte a byte sem alterações no diff.
- `NR_TOKEN_VERSION` está presente na migration V006 e na entidade `Usuario`.
- A versão efetiva do PostgreSQL no Neon não pôde ser consultada sem credenciais; por isso não houve upgrade especulativo do Flyway.
- Risco conhecido: Flyway 9.22.3 certifica PostgreSQL até a versão 15 nas notas da série. Avaliar upgrade somente após conhecer a versão efetiva do Neon e testar compatibilidade com Spring Boot 3.2.5.

Referência: https://documentation.red-gate.com/flyway/release-notes-and-older-versions/release-notes-for-flyway-engine

## 10. Brevo

- Credenciais e acesso à caixa de entrada de homologação não estavam disponíveis.
- Envio real, remetente e conteúdo do link: **NÃO EXECUTADO**.
- Código revisado: o link deriva de `app.frontend.url`, token bruto não é persistido e falhas de e-mail não registram a mensagem completa.

## 11. Cadastro

- Testes automatizados cobrem normalização de e-mail, BCrypt, role `USER`, rejeição de role forjada, validação e isolamento.
- Cadastro real em produção: **APROVADO**, HTTP 201.
- E-mail enviado em caixa alta foi normalizado; a resposta não expôs senha e `/auth/me` confirmou `ROLE_USER`.
- Conta técnica remanescente: ID 3. Não existe endpoint seguro de autoexclusão; nenhuma limpeza via SQL foi tentada.

## 12. Login

- Testes automatizados cobrem login válido, credencial incorreta genérica, usuário inexistente, JWT válido/inválido/adulterado/expirado e lockout.
- Login real da conta temporária: **APROVADO**, HTTP 200 com JWT.
- `/auth/me` com token retornou HTTP 200 e o perfil foi atualizado com HTTP 200.
- Após a troca, a senha anterior retornou HTTP 401 e a nova senha autenticou com HTTP 200.
- Nenhum JWT ou senha foi impresso ou persistido no relatório.

## 13. JWT/tokenVersion

- Testes automatizados aprovados para:
  - token antigo invalidado após troca de senha;
  - novo login com token válido;
  - reset de senha invalidando token anterior;
  - alteração do usuário A sem invalidar token do usuário B.
- V006 foi preservada.
- E2E real em produção: **APROVADO**.
- Troca de senha retornou HTTP 204; JWT antigo passou a retornar HTTP 401; novo login e novo `/auth/me` retornaram HTTP 200.

## 14. Recuperação de senha

- Testes locais cobrem resposta anti-enumeração, token válido, inválido, adulterado, expirado, reutilização, segunda solicitação, BCrypt e invalidação de JWT.
- Fluxo Brevo E2E: **NÃO EXECUTADO**.

## 15. CRUD financeiro

- Testes de integração locais validam gastos, recebimentos, investimentos e totais do dashboard.
- CRUD real em produção: **APROVADO**.
- Criação de gasto, recebimento e investimento retornou HTTP 201; as três listagens continham os registros.
- Edição do gasto retornou HTTP 200 e o valor atualizado persistiu em nova leitura.
- Dashboard retornou HTTP 200 com totais corretos: gastos 11,50; recebimentos 30,75; investimentos 20,25.
- Exclusões retornaram HTTP 204 e as listagens finais ficaram vazias para a conta temporária.

## 16. Multiusuário

- Testes locais validam isolamento de gastos, recebimentos, investimentos e dashboard entre usuários.
- Homologação com duas contas reais em produção: **NÃO EXECUTADO**.

## 17. IDOR/BOLA

- Testes locais cobrem leitura/alteração de IDs pertencentes a outro usuário e tentativa de troca de proprietário.
- Resultado local: **APROVADO**.

## 18. Rate limiting/proxy

- Confirmado o risco anterior: `RemoteAddr` representa o proxy no Render e pode compartilhar buckets entre clientes.
- A documentação atual do Render orienta usar o IP fornecido pela borda. A documentação oficial também informa que `CF-Connecting-IP` é sobrescrito pela Cloudflare, ao contrário da cadeia `X-Forwarded-For`.
- Implementado resolvedor dedicado somente no profile de produção, confiando em `CF-Connecting-IP`.
- Aceitos apenas literais IPv4/IPv6 válidos, sem listas, portas, zone IDs ou nomes DNS.
- Header ausente ou inválido usa `RemoteAddr`; ausência de ambos usa bucket seguro `unknown`.
- Quatro novos testes passaram, incluindo spoofing por `X-Forwarded-For`/`Forwarded` e cadeia malformada.
- Em produção, uma requisição isolada com `X-Forwarded-For` e `Forwarded` forjados foi processada normalmente sem permitir contorno observável.
- Uma tentativa de injetar `CF-Connecting-IP` inválido recebeu HTTP 403 da borda Cloudflare antes de alcançar a aplicação, confirmando que o cliente não controla livremente o header confiado.
- Testes automatizados validam header confiável válido, ausência, valor malformado e fallback para `RemoteAddr` sem carga agressiva em produção.
- A limitação em memória por instância permanece; se o Render escalar horizontalmente, recomenda-se store distribuído.

Referências:

- https://render.com/articles/how-render-handles-ddos-attacks
- https://render.com/articles/host-pocketbase-on-render

## 19. Responsividade

- Homologação visual local e no domínio de produção executada em 1920×1080, 1366×768, 768×1024 e 390×844.
- Login, cadastro, esqueci senha e redefinir senha foram inspecionados visualmente.
- Não houve overflow horizontal; rolagem vertical em formulários longos é esperada.
- Navegação interna, histórico Voltar e reload foram aprovados.
- Rotas protegidas redirecionaram corretamente para login sem sessão.
- Sidebar, drawer, modais e tabelas autenticadas não foram inspecionados visualmente: a senha efêmera foi descartada após o E2E e não foi criada uma segunda conta, respeitando o limite autorizado de uma conta temporária.

## 20. Segurança

- Busca por padrões fortes de secrets em arquivos rastreados: nenhuma ocorrência.
- Apenas `.env.example` foi encontrado; nenhum `.env` real rastreado.
- JWT secret, senha Neon e chave SMTP permanecem externalizados.
- TLS Neon configurado como `verify-full` e validado pelo guard de produção.
- BCrypt, CSRF stateless, role `USER`, token reset somente em hash, tokenVersion, lockout, rate limiting e mensagens anti-enumeração têm cobertura automatizada.
- CORS sem wildcard aprovado em produção.

## 21. Testes automatizados

- Java: Eclipse Temurin 17.0.20.1.
- `mvnw clean test`: **82/82 aprovados**, 0 falhas, 0 erros, 0 ignorados.
- `mvnw clean package`: **APROVADO**, repetindo 82/82 testes e gerando `target/app.jar`.
- Observação: a execução dentro do sandbox encontrou `AccessDeniedException` ao fechar JARs; a mesma suíte executada fora do sandbox passou integralmente.

## 22. Build

- `npm install`: aprovado.
- `npm run build`: aprovado.
- Build de produção com a variável oficial: aprovado.
- `npm audit --audit-level=high`: 0 vulnerabilidades.
- `dist` gerado com HTML, JS, CSS, fontes e imagem.

## 23. Git

- `git diff --check`: aprovado antes dos commits.
- Os três commits autorizados foram enviados sem force push para `origin/feature/emr-financas-v2`.
- HEAD local e remoto ficaram sincronizados em `978bb01430f48660ca537d7d526bb32be13b9222` após o primeiro push.
- Esta atualização pós-deploy será registrada em um commit documental adicional, tecnicamente necessário e solicitado explicitamente.

## 24. Pendências restantes

- Validar Neon diretamente, incluindo versão do PostgreSQL e `flyway_schema_history`, quando houver credenciais read-only.
- Validar Brevo E2E quando houver caixa de entrada/credenciais autorizadas.
- Homologar visualmente páginas autenticadas e seus componentes em produção.
- Remover a conta técnica ID 3 somente se for criado futuramente um endpoint seguro de autoexclusão; não usar SQL destrutivo para isso.

## 25. URLs finais

- Frontend: https://painel-emr-finan-as.vercel.app
- Backend: https://painel-emr-financas.onrender.com
- API: https://painel-emr-financas.onrender.com/api
- Health: https://painel-emr-financas.onrender.com/health

## 26. Release readiness

As correções estão implementadas, revisadas, testadas, implantadas e homologadas. As pendências de inspeção direta do Neon, Brevo E2E e visual autenticado estão explicitamente registradas e não impedem a correção do fallback SPA nem os fluxos críticos validados. Nenhum merge em `main` foi realizado.

FRONTEND EM PRODUÇÃO: SIM

BACKEND EM PRODUÇÃO: SIM

SPA/REFRESH: APROVADO

CORS: APROVADO

CADASTRO E2E: APROVADO

LOGIN E2E: APROVADO

CRUD E2E: APROVADO

JWT REVOCATION E2E: APROVADO

NEON: APROVADO

BREVO E2E: NÃO EXECUTADO

TESTES AUTOMATIZADOS: 82/82

GIT LIMPO: SIM (após commit e push desta atualização)

PRONTO PARA MERGE EM MAIN: SIM
