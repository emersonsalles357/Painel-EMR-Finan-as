# Relatório de Release Final — EMR Finanças V2

Data da conclusão operacional: 2026-09-09 (America/Sao_Paulo).

## 1. Estado inicial

- Branch homologada: `feature/emr-financas-v2`.
- SHA da feature local e remota: `9521bf3ed5c6d7761c5ad92bba8a12aaf1fd1559`.
- SHA da `origin/main` anterior: `fecdec4196cfed7c6cd070ad2d7c95c7ab39ea28`.
- Working tree inicialmente limpa e referências remotas sincronizadas.
- Os dois commits pertenciam a históricos Git desconectados, sem merge-base.

## 2. Feature validada

A árvore homologada da feature foi mantida integralmente. Antes da integração, o frontend compilou, a auditoria NPM não encontrou vulnerabilidades e o backend concluiu 82 de 82 testes, além do empacotamento.

## 3. Comparação com main

- O commit antigo da `main` era um commit raiz de histórico independente.
- Tree SHA da `main` antiga (`fecdec4`): `8d5374ef4695ce3b7344fe3bf23fc6679746cab7`.
- Tree SHA do commit-base da feature (`21a6c02`): `8d5374ef4695ce3b7344fe3bf23fc6679746cab7`.
- `git diff --exit-code fecdec4 21a6c02`: nenhuma diferença.

Assim, a divergência era exclusivamente de histórico, e não de conteúdo.

## 4. Merge

A integração excepcional foi executada na branch temporária `integration/main-v2-history-link`, criada a partir da `origin/main` antiga, usando `--allow-unrelated-histories`. Os conflitos `add/add` esperados foram resolvidos de forma determinística pela substituição do index e do worktree pela árvore exata de `origin/feature/emr-financas-v2`, preservando o estado de merge.

Antes do commit:

- `git ls-files -u`: nenhuma entrada não resolvida.
- Diff do index contra a feature: zero.
- Diff do worktree contra a feature: zero.

## 5. Commit de merge

- Commit: `123105b7aa871924f46b25145eae6a45b9eac1a7`.
- Mensagem: `merge: link main history and release EMR Finanças V2`.
- Primeiro pai: `fecdec4196cfed7c6cd070ad2d7c95c7ab39ea28`.
- Segundo pai: `9521bf3ed5c6d7761c5ad92bba8a12aaf1fd1559`.
- Tree SHA do merge: `b2313365e202067aaedee25bfff6b75d61ef094a`.
- Tree SHA da feature: `b2313365e202067aaedee25bfff6b75d61ef094a`.
- Diff entre o merge e a feature: zero.

O commit possui dois pais e a árvore final é exatamente a árvore homologada da feature.

## 6. Push main

A `main` local avançou por fast-forward até o merge e foi enviada à origem com push normal, sem force. Após o push operacional, `HEAD` e `origin/main` apontavam para `123105b7aa871924f46b25145eae6a45b9eac1a7`.

## 7. Vercel

- Projeto: `painel-emr-finan-as`.
- Production Branch: `main`.
- Ambiente: Production.
- Deployment operacional: `A1DDB75migvoXYswAjDXAa2NRadf`.
- Commit implantado: `123105b7aa871924f46b25145eae6a45b9eac1a7`.
- Status observado: `Ready` e `Latest`.
- Domínio oficial preservado: `https://painel-emr-finan-as.vercel.app`.
- Root Directory preservado na raiz do repositório.
- Build Command preservado: `npm run build`.
- Output Directory preservado: `dist`.
- Install Command preservado: `npm install`.
- `VITE_API_BASE_URL` preservada como `https://painel-emr-financas.onrender.com/api`.

## 8. Render

- Serviço: `Painel-EMR-Finanças`.
- Branch: `main`.
- Deploy operacional: `dep-dagkl0ajnfac73e0on7g`.
- Commit implantado: `123105b7aa871924f46b25145eae6a45b9eac1a7`.
- Status observado: `Deploy succeeded` e `Live`.
- Runtime Docker, Root Directory `BackEnd/Back End/backend`, Dockerfile e configurações existentes foram preservados.
- O log confirmou Java 17, perfil `prod`, Tomcat na porta 10000 e Flyway com seis migrations válidas, schema na versão `006` e nenhuma migration pendente.

## 9. Smoke test

Todas as rotas abaixo responderam HTTP 200 no domínio oficial:

- `/`
- `/login`
- `/cadastro`
- `/esqueci-senha`
- `/redefinir-senha`
- `/dashboard`

Também foram validados por navegação direta e refresh:

- `/login`: formulário de acesso renderizado.
- `/cadastro`: formulário de criação de conta renderizado.
- `/dashboard`: sem sessão, redirecionamento esperado para `/login`, sem 404.

As páginas de recuperação e redefinição também renderizaram; sem token, `/redefinir-senha` apresentou corretamente o estado de link inválido. Nenhuma rota apresentou 404 da Vercel.

Backend:

- `GET /health`: HTTP 200 com `{"status":"UP"}`.
- `GET /api/auth/me` sem JWT: HTTP 401, conforme esperado.

## 10. CORS

- Origem oficial `https://painel-emr-finan-as.vercel.app`: permitida, com origem exata e credenciais habilitadas.
- Origem inválida `https://example.invalid`: rejeitada com HTTP 403.
- Nenhum wildcard foi observado.

## 11. Testes

- Backend: 82/82 testes aprovados.
- `mvnw clean test`: aprovado.
- `mvnw clean package`: aprovado.
- Os testes foram repetidos formalmente após a criação do merge.

## 12. Build

- `npm install`: aprovado.
- `npm run build`: aprovado antes e depois da integração.
- `npm audit --audit-level=high`: 0 vulnerabilidades.
- Package Java 17: aprovado.

## 13. Git

- A `main` resultante contém o histórico antigo: sim.
- A `main` resultante contém o histórico completo da feature: sim.
- Branch local de segurança preservada: `backup/main-before-v2-history-link`.
- Branch local temporária preservada: `integration/main-v2-history-link`.
- Branch `feature/emr-financas-v2` preservada local e remotamente.
- Nenhuma tag criada, nenhum branch apagado e nenhum force push executado.
- O SHA final da `main`, após o commit documental deste relatório, deve ser consultado no próprio commit que adiciona este arquivo; ele é posterior ao merge operacional e não altera código da aplicação.

## 14. Pendências não bloqueantes

- Brevo E2E não executado nesta etapa.
- Inspeção direta do Neon/Flyway não executada; o estado do Flyway foi confirmado pelos logs do deploy.
- Conta técnica ID 3 permanece.
- Homologação visual autenticada completa não foi repetida após o E2E previamente aprovado.

Esses itens já eram conhecidos e não bloquearam a release.

## 15. URLs finais

- Frontend: `https://painel-emr-finan-as.vercel.app`.
- Backend: `https://painel-emr-financas.onrender.com`.
- API: `https://painel-emr-financas.onrender.com/api`.
- Health: `https://painel-emr-financas.onrender.com/health`.

## 16. Status da release

- Históricos desconectados confirmados: SIM.
- Tree da `main` antiga igual à tree base da feature: SIM.
- Merge com `--allow-unrelated-histories`: APROVADO.
- Merge com dois pais: SIM.
- Tree do merge igual à tree homologada da feature: SIM.
- Push da `main`: APROVADO.
- Vercel acompanhando `main`: SIM.
- Render acompanhando `main`: SIM.
- SPA/refresh pós-merge: APROVADO.
- CORS pós-merge: APROVADO.
- Health: APROVADO.
- Feature preservada: SIM.
- Release EMR Finanças V2: CONCLUÍDA.
