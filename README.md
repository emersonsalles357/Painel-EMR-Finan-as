# EMR Finanças

Aplicação financeira Full Stack com frontend React e API REST Spring Boot. A versão atual mantém a autenticação acadêmica existente; autenticação real e isolamento de dados por usuário ainda não fazem parte desta etapa.

## Arquitetura e stack

### Frontend

- React 18.3.1 e TypeScript 5.6.3
- Vite 7.3.6
- React Router 7.18.3
- Axios 1.20.0
- Chart.js 4.4.6 com react-chartjs-2 5.2.0
- Bootstrap 5.3.3 e Bootstrap Icons 1.11.3

O código do frontend fica em `FrontEnd/`; os arquivos de build e o diretório público ficam na raiz do projeto.

### Backend

- Java 17
- Spring Boot 3.2.5
- Spring Web, Spring Data JPA e Bean Validation
- PostgreSQL
- Flyway
- Maven Wrapper 3.9.15

O backend fica em `BackEnd/Back End/backend/` e está organizado nos packages `controller`, `service`, `repository`, `model`, `dto`, `config` e `exception`.

## Pré-requisitos

- JDK 17
- Node.js 20.19 ou superior
- PostgreSQL para execução local, ou um projeto PostgreSQL no Neon

## Variáveis de ambiente

Copie `.env.example` apenas como referência e configure as variáveis no terminal ou na plataforma de deploy. O Spring Boot e o Vite não carregam automaticamente esse arquivo compartilhado.

| Variável | Uso |
| --- | --- |
| `DB_URL` | URL JDBC, por exemplo `jdbc:postgresql://host/database?sslmode=require` |
| `DB_USERNAME` | Usuário do PostgreSQL |
| `DB_PASSWORD` | Senha do PostgreSQL |
| `DB_SSL_MODE` | `require` no Neon; em desenvolvimento local o padrão é `disable` |
| `FRONTEND_URL` | Origem exata autorizada pelo CORS; aceita lista separada por vírgulas |
| `SPRING_PROFILES_ACTIVE` | `dev` ou `prod` |
| `VITE_API_BASE_URL` | URL pública da API terminada em `/api` |
| `VITE_USE_MOCK_API` | `true` para usar apenas os dados mockados do frontend |

Não versione arquivos `.env` reais. O `.gitignore` protege `.env` e suas variações, preservando somente `.env.example`.

### Usuário opcional de desenvolvimento

O backend não cria mais um usuário conhecido automaticamente. Para criar um usuário somente no profile `dev` e somente quando a tabela estiver vazia, defina:

```text
DEV_SEED_USER_ENABLED=true
DEV_SEED_USER_NAME=...
DEV_SEED_USER_EMAIL=...
DEV_SEED_USER_PASSWORD=...
```

Esse mecanismo nunca é carregado no profile `prod`. A senha continua em texto puro porque a modernização da autenticação foi explicitamente deixada para a próxima fase; use o seed somente com dados descartáveis.

## Execução local

### Frontend

Na raiz do projeto:

```bash
npm install
npm run dev
```

Build de produção:

```bash
npm run build
```

### Backend

Entre em `BackEnd/Back End/backend/`, exporte as variáveis de banco e execute:

```bash
./mvnw spring-boot:run
```

No Windows, use `mvnw.cmd`.

Testes e pacote:

```bash
./mvnw test
./mvnw clean package
```

Os testes usam H2 em memória, no modo de compatibilidade PostgreSQL, e validam que a migration e o mapeamento JPA são compatíveis.

## Profiles e banco

- `dev`: PostgreSQL local por padrão em `jdbc:postgresql://localhost:5432/emr_financas`; senha sem valor padrão e SSL desabilitado por padrão.
- `prod`: exige `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` e `FRONTEND_URL`; SSL usa `require` por padrão.
- `test`: banco H2 temporário e isolado, usado apenas pelos testes automatizados.

Flyway executa `db/migration/V001__initial_schema.sql`. Hibernate usa `ddl-auto=validate` e não cria nem altera tabelas silenciosamente.

### Adoção de um banco PostgreSQL existente

`baseline-on-migrate=true` com baseline na versão 1 permite registrar um schema preexistente sem executar a migration inicial sobre suas tabelas. Antes do primeiro deploy:

1. Faça backup verificável do banco atual.
2. Compare as quatro tabelas, sequences, chaves primárias, chave única de e-mail e três chaves estrangeiras com `V001__initial_schema.sql`.
3. Confirme contagens e integridade dos relacionamentos em uma cópia ou janela de manutenção.
4. Só então inicie a aplicação com Flyway. O `ddl-auto=validate` interromperá a inicialização se o schema não corresponder às entidades.

Nenhuma cópia ou exclusão de dados é feita automaticamente.

## CORS e erros

Há uma única configuração global de CORS para `/api/**`. Desenvolvimento permite `http://localhost:5173` por padrão; produção exige `FRONTEND_URL`. Erros inesperados são registrados no backend, mas a API retorna uma mensagem genérica sem SQL, stack trace ou detalhes internos.

## Deploy

O `Dockerfile` da raiz gera e executa somente o backend em Java 17. O frontend deve ser compilado com `npm run build` e publicado a partir de `dist/`, configurando `VITE_API_BASE_URL` durante o build.

## Limitações conhecidas desta fase

- A autenticação ainda compara senha em texto puro e devolve token acadêmico fixo.
- Os endpoints ainda não têm autorização e não isolam dados por usuário.
- Cadastro, recuperação de senha, JWT, rate limiting e nova política de senhas não foram implementados.
- A primeira adoção do Flyway em banco existente exige backup e conferência manual do schema.
