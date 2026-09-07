# EMR Finanças V2 — fases 8 e 9

Implementação local em 07/09/2026. Nenhum push, alteração de produção ou acesso a banco externo.

## 1. Diagnóstico inicial
Branch feature/emr-financas-v2, HEAD inicial c6708f8. A árvore NÃO estava limpa: PasswordPolicyValidator já possuía uma alteração local para o máximo de 72 caracteres e UpdateProfileRequest.java já existia sem rastreamento. Ambos foram preservados e integrados. React 18, TypeScript, Vite, Bootstrap/Icons e Chart.js existentes foram mantidos. O frontend compilava. O dashboard tinha séries e percentual fixos, elementos de acesso redundantes, botão Exportar inoperante e perfil demonstrativo. O contexto financeiro não carregava automaticamente os dados reais. CSS dividido entre estilos globais e módulo da sidebar, com cores e efeitos inconsistentes. O teste backend inicial esbarrou em permissões do cache Maven; portanto não há baseline backend executado com sucesso anterior às alterações desta sessão.

## 2–5. Design system, paleta, tipografia e logo
Tokens semânticos centralizados em FrontEnd/styles/variables.css; aplicação global em FrontEnd/styles/emr.css. Fundo #00131f, fundo secundário #001b2a, superfície #032131, bordas #164257, ciano #00d3ec, positivo #00debf, negativo #ff6470, azul #329cff, texto #f1f7fc e secundário #a9c8db. Cards escuros, bordas finas, raios de 8–14px, brilho discreto no item ativo e foco ciano. Tipografia: pilha Inter / Segoe UI / system-ui; sem download de fonte externa, usando a fonte disponível no dispositivo. Logo oficial copiada integralmente para FrontEnd/assets/emr-logo.png, sem redesenho, recorte ou deformação, reutilizada pelo componente Brand. Seu fundo original foi preservado.

## 6–7. Sidebar e topbar
Sidebar com logo, Dashboard, Recebimentos, Gastos, Investimentos, Minha conta e Sair. Destaque ativo ciano; drawer abaixo de 992px, fechamento por navegação, clique no fundo e Escape. Conteúdo fora do drawer fica inerte quando aberto e sidebar fechada fica fora da navegação por teclado. Topbar mostra contexto e usuário real com link para perfil. Nome longo é abreviado visualmente para evitar overflow. Exportar fictício removido.

## 8–10. Dashboard, gráficos e dados reais
Quatro cards: saldo consolidado, receitas, despesas e investimentos. Dados das APIs autenticadas /api/gastos, /api/recebimentos e /api/investimentos. Chart.js existente calcula receitas/despesas por mês efetivamente presente nos registros e distribuição por categoria. Nenhuma série, comparação percentual ou evolução de patrimônio inventada. Sem registros, estados vazios com links operacionais. Movimentações recentes consolidam os três tipos, ordenados por data. Ações rápidas abrem os formulários existentes via ?novo=1.

A regra do backend foi preservada: saldo = recebimentos − gastos + investimentos. Esse valor NÃO é tratado como saldo disponível; rótulo e explicação visível indicam a composição. A fórmula pode superestimar disponibilidade e demanda decisão de produto futura. Os totais incluem todos os registros, como no backend, sem filtro novo por status. O adaptador legado contém status sintéticos para gastos (Pago) e investimentos (Ativo), pois esses campos não existem no contrato correspondente. Esses status não foram usados para inventar indicadores de caixa. Não houve alteração silenciosa de regra financeira.

Serviços financeiros passam a usar a API real; sementes foram removidas do contexto. O arquivo mockApi legado permanece no repositório, mas o fluxo financeiro não o utiliza. Dados são carregados ao autenticar e limpos ao trocar sessão; respostas de carregamento de outro token são descartadas. Usuário é consultado em /api/auth/me; falhas de login liberam novamente o botão e expiração de sessão usa o contexto React.

## 11. Elementos da referência não implementados
Busca global, notificações, relatórios/exportação, objetivos, comparações percentuais, filtros de período e curva de valorização da carteira não foram adicionados: faltam fluxos ou dados reais que os sustentem. Busca e filtros locais das tabelas já existentes foram preservados. Não foi adicionada IA ou integração bancária.

## 12–15. Telas públicas e gestão financeira
Login, cadastro, recuperação e reset usam logo oficial, superfícies escuras, botões ciano e formulários consistentes. Cadastro, reset e perfil compartilham indicadores de senha. Gastos, Recebimentos e Investimentos mantêm criação, edição, exclusão, busca, filtros e paginação existentes; ações rápidas abrem os modais. Tabelas, formulários e modais recebem o tema global, estados de carregamento/erro e novas opções de tentativa. Falhas de gravação em modal agora têm mensagem visível.

## 16–17. Perfil e senha
/perfil exibe nome, e-mail e tipo real de conta. Somente nome é editável. PATCH /api/users/me resolve identidade via AuthenticatedUserService e SecurityContext; DTO não permite alterar id, role, e-mail nem atributos internos. Nome é validado e normalizado com trim.

POST /api/users/me/password recebe somente senhaAtual e novaSenha. Valida senha atual com PasswordEncoder.matches, reutiliza PasswordPolicyValidator, grava BCrypt e responde 204 vazio. DTO limita comprimentos; frontend confirma nova senha e limpa os campos após sucesso. Não há log de senha ou retorno de hash. JWT, filtro de segurança, BCrypt, roles, proteção administrativa, rate limiting, bloqueio e recuperação existentes foram preservados. A mudança não revoga JWTs já emitidos; mantêm a expiração existente. Política: 10–72 caracteres, maiúscula, minúscula, número e especial. PATCH foi acrescentado ao CORS para as origens já permitidas; nenhuma origem foi ampliada.

## 18–19. Responsividade e acessibilidade
Inspeção no navegador em desktop 1280px, tablet 768px e smartphone 390px, com revisão de login, cadastro, recuperação/reset, dashboard, gestão financeira e perfil. Dashboard verificado vazio e com três lançamentos enviados à API local H2. Total conferido: 2.500 − 450 + 600 = 2.650; esses valores existiram apenas no banco de teste descartável, não no código da aplicação. Ação rápida de despesa abriu o modal real. Edição de nome pelo navegador foi confirmada com mensagem de sucesso e atualização da topbar. CORS PATCH bloqueado foi detectado visualmente e corrigido antes da conclusão. Drawer mobile e tabela com scroll horizontal verificados.

Layouts reorganizam cards e perfil, tabelas preservam scroll, formulários têm labels, modais têm identificação acessível, Escape, foco inicial/retorno e contenção de Tab; foco visível e preferência por movimento reduzido são respeitados. Estados usam texto e ícones além de cores. Gráficos têm nomes acessíveis. A inspeção não substitui uma auditoria completa com leitores de tela e todos os dispositivos físicos.

## 20–22. Inventário de arquivos
A lista completa de arquivos criados e alterados segue abaixo, gerada a partir do diff da implementação. Nenhum arquivo existente foi removido. Logs temporários de execução foram descartados, sem entrar nos commits.

## 23–25. Testes, build e audit
- npm install: concluído, dependências sem mudanças de versão.
- npm run build: aprovado (TypeScript e Vite).
- npm audit: 0 vulnerabilidades.
- mvnw clean test: 55 testes, 0 falhas, 0 erros, 0 ignorados.
- mvnw clean package: aprovado, incluindo os mesmos 55 testes.
- Maven usou o cache local autorizado em ../.m2/repository; execução fora da restrição do sandbox foi necessária por AccessDeniedException ao ler JARs. Todos os testes usam perfil test com H2 em memória.
- Novos testes: atualização do próprio perfil, nome inválido, autenticação obrigatória, mass assignment de id/role/e-mail ineficaz, isolamento A/B, senha atual incorreta, cada violação de política, alteração de hash BCrypt, login antigo rejeitado, login novo aceito e CORS PATCH com origem permitida/rejeitada.
- Migrations antigas intactas. Nenhum teste anterior removido.

## 26. Pendências e limites
Não há bloqueio de compilação ou testes nestas fases. Antes da produção, revisar semântica do saldo e status não persistidos, definir endpoint de API e origens CORS, configurar entrega SMTP, credenciais e infraestrutura da próxima fase. Revogação de sessões após troca de senha continua sendo evolução separada da política JWT atual. O dashboard agrega os registros disponíveis, sem paginação de backend ou histórico de preços da carteira. A logo original pesa cerca de 1,27MB e foi preservada integralmente.

## 27. Git
Branch feature/emr-financas-v2. Alterações divididas em commits locais de backend/perfil, frontend/design e documentação. As duas alterações locais iniciais foram incorporadas explicitamente ao trabalho, sem descarte. Nenhum push, force push ou alteração de remoto. Lista dos commits pode ser obtida com git log -3 --oneline.

## 28. Fase 10
Pronto para iniciar a fase 10 de banco Neon e preparação de produção, com as decisões e configurações acima. Isso não significa aplicação já implantada ou banco Neon já configurado. Nenhum banco externo foi acessado ou modificado nesta entrega.

## Execução local
Frontend: npm run dev. /api é encaminhado para localhost:8080 pelo proxy Vite; VITE_API_BASE_URL permite configuração explícita. Em build, o padrão é /api na mesma origem, evitando conexão automática ao backend remoto antigo.
Para a inspeção, o backend foi iniciado com spring-boot:run, useTestClasspath=true, profiles=test e spring.config.additional-location=file:./src/test/resources/application-test.properties, escutando somente 127.0.0.1. Essa configuração é exclusivamente de validação local e não deve ser promovida para produção.

### Alterados

- BackEnd/Back End/backend/src/main/java/br/com/emr/emrfinancas/config/CorsConfig.java
- BackEnd/Back End/backend/src/main/java/br/com/emr/emrfinancas/service/PasswordPolicyValidator.java
- FrontEnd/App.tsx
- FrontEnd/components/CRUDTable.tsx
- FrontEnd/components/DashboardChart.tsx
- FrontEnd/components/Modal.tsx
- FrontEnd/components/Navbar.tsx
- FrontEnd/components/Sidebar.module.css
- FrontEnd/components/Sidebar.tsx
- FrontEnd/contexts/AuthContext.tsx
- FrontEnd/contexts/FinancasContext.tsx
- FrontEnd/pages/DashboardPage.tsx
- FrontEnd/pages/ForgotPasswordPage.tsx
- FrontEnd/pages/GastosPage.tsx
- FrontEnd/pages/InvestimentosPage.tsx
- FrontEnd/pages/LoginPage.tsx
- FrontEnd/pages/PerfilPage.tsx
- FrontEnd/pages/RecebimentosPage.tsx
- FrontEnd/pages/RegisterPage.tsx
- FrontEnd/pages/ResetPasswordPage.tsx
- FrontEnd/services/_apiClient.ts
- FrontEnd/services/api.ts
- FrontEnd/services/realApi.ts
- FrontEnd/styles/variables.css
- vite.config.ts

### Criados ou integrados do estado inicial

- BackEnd/Back End/backend/src/main/java/br/com/emr/emrfinancas/controller/ProfileController.java
- BackEnd/Back End/backend/src/main/java/br/com/emr/emrfinancas/dto/ChangePasswordRequest.java
- BackEnd/Back End/backend/src/main/java/br/com/emr/emrfinancas/dto/UpdateProfileRequest.java
- BackEnd/Back End/backend/src/main/java/br/com/emr/emrfinancas/service/ProfileService.java
- BackEnd/Back End/backend/src/test/java/br/com/emr/emrfinancas/ProfileIntegrationTests.java
- FrontEnd/assets/emr-logo.png
- FrontEnd/components/Brand.tsx
- FrontEnd/components/FinancialStatus.tsx
- FrontEnd/styles/emr.css
- FrontEnd/utils/password.ts
- docs/FASES-8-9-RELATORIO.md
