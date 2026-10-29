# Biblioteca

Este projeto é uma aplicação web de **Biblioteca** construída com **Spring Boot**, com duas formas de acesso:

- **Aplicação Web (Thymeleaf)**: interface HTML com autenticação via **login por formulário**.
- **API REST (`/api/**`)**: endpoints REST protegidos com **JWT (Bearer Token)**.

## Integração Contínua (Trabalho 5)

[![CI](https://github.com/Renan0204/biblioteca/actions/workflows/ci.yml/badge.svg)](https://github.com/Renan0204/biblioteca/actions/workflows/ci.yml)

Pipeline de CI mínimo e reprodutível com **GitHub Actions**, definido em [`.github/workflows/ci.yml`](.github/workflows/ci.yml).

### Como reproduzir localmente

Pré-requisito: **JDK 21**. Não é preciso instalar o Maven: o Maven Wrapper (`mvnw`) baixa a versão fixada (3.9.15).

```bash
./mvnw -B clean verify        # Linux, macOS ou Git Bash
.\mvnw.cmd -B clean verify    # PowerShell ou CMD
```

É o **mesmo comando** que o pipeline executa. Ele compila, roda os testes (relatórios em `target/surefire-reports/`) e gera `target/biblioteca-0.0.1-SNAPSHOT.jar`. O build não precisa de nenhuma variável de ambiente.

### Gatilhos

| Evento | O que acontece |
|---|---|
| `push` na `master` | Build, testes e publicação do JAR como artefato |
| `pull_request` para a `master` | Build e testes; se falhar, o merge fica bloqueado |
| `workflow_dispatch` (manual) | Reexecução para diagnóstico |

### Etapas do job `build-test`

1. **Obter o código** (`actions/checkout`)
2. **Configurar Java 21** (Temurin) com cache do Maven (`actions/setup-java`)
3. **Compilar, testar e empacotar** com `./mvnw -B clean verify`
4. **Armazenar relatórios de teste**: sempre (`if: always()`), inclusive na falha, para diagnóstico
5. **Armazenar pacote gerado**: só se tudo passou (`if: success()`) e só na `master`

### Artefato e rastreabilidade

- O JAR é publicado como `biblioteca-<SHA do commit>`. O SHA liga o pacote ao commit, à execução e ao resultado dos testes.
- Em PR, o GitHub testa um commit de merge temporário (`refs/pull/N/merge`) que não existe no histórico. Por isso o pacote oficial só é publicado a partir da `master`.
- **Cache não é artefato**: o cache do Maven (`~/.m2`) só acelera as próximas execuções; o artefato é a saída verificada do build.

### Tratamento de falhas

- Qualquer comando com código de saída diferente de zero interrompe o job e marca a execução como falha.
- Com falha, o JAR **não** é publicado; os relatórios do Surefire são.
- A `master` é protegida: o check `build-test` é obrigatório, então um PR vermelho não pode ser mergeado, nem por administradores.
- Para diagnosticar: abra a execução, vá até a primeira etapa vermelha, leia a primeira mensagem de erro, baixe `relatorios-<SHA>` e reproduza localmente com `./mvnw -B clean verify`.

### Proteção de segredos

- O segredo que assina os tokens JWT estava escrito no `application.properties`. Agora ele vem da variável de ambiente `JWT_SECRET`, sem valor padrão: se ela faltar, a aplicação não sobe e mostra o motivo.
- O valor antigo continua no histórico do Git (e no repositório original), então é considerado **comprometido** e não deve ser reutilizado.
- Os testes usam uma chave falsa declarada no próprio teste. Assim o pipeline **não precisa de nenhum secret** e funciona até em forks.
- O workflow tem só permissão de leitura (`permissions: contents: read`) e usa apenas actions oficiais do GitHub (`actions/*`), fixadas por versão maior. São versões mais novas que as `@v4` dos slides; fixar pelo SHA do commit seria a opção mais rígida.
- Se um job futuro (por exemplo, o deploy da Aula 11) precisar do segredo, ele ficará em **Settings → Secrets and variables → Actions** e será lido como `${{ secrets.JWT_SECRET }}`. O GitHub mascara esse valor nos logs.
- Os usuários `admin/admin` e `user/user` são dados de demonstração do banco H2 em memória, criados para uso local.

Para rodar a aplicação localmente, gere um segredo de pelo menos 32 caracteres:

```bash
export JWT_SECRET="$(openssl rand -base64 32)"    # Git Bash, Linux ou macOS
./mvnw spring-boot:run
```

```powershell
$b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b)
$env:JWT_SECRET = [Convert]::ToBase64String($b)
.\mvnw.cmd spring-boot:run
```

### Problemas encontrados no diagnóstico local

| Problema | Efeito no CI | Correção |
|---|---|---|
| `mvnw` sem permissão de execução no Git (modo `100644`) | `Permission denied` no runner Linux | `git update-index --chmod=+x mvnw` |
| O projeto usa Java 21 (o exemplo dos slides usa 17) | `release version 21 not supported` | `java-version: "21"` |
| A branch principal é `master` (o exemplo usa `main`) | O pipeline não dispararia no push | `branches: [master]` |
| Segredo JWT no código | Credencial exposta no repositório | Variável de ambiente `JWT_SECRET` |
| Só existia o teste `contextLoads` | Falha controlada pouco realista | Novo `UsuarioServiceTest` (senha salva com BCrypt) |
| Push recusado: o token do `gh` não tinha o escopo `workflow` | O GitHub não aceita criar `.github/workflows/*` sem esse escopo | `gh auth refresh -h github.com -s workflow` |

### Evidências

| Execução | Evento | Commit | Resultado | O que mostra |
|---|---|---|---|---|
| [#1](https://github.com/Renan0204/biblioteca/actions/runs/38657109664) | push na `master` | [`32df1db`](https://github.com/Renan0204/biblioteca/commit/32df1db7b2c55009e87a5245992becf815bb0f48) | ✅ sucesso | 2 testes aprovados; artefatos `biblioteca-32df1db…` (JAR) e `relatorios-32df1db…` |
| [#2](https://github.com/Renan0204/biblioteca/actions/runs/38657253390) | PR [#1](https://github.com/Renan0204/biblioteca/pull/1) | [`4f6bb8d`](https://github.com/Renan0204/biblioteca/commit/4f6bb8d5016971b45f10f2e43e7928ee70d240e7) | ❌ falha | Falha controlada: `UsuarioServiceTest` reprova com "a senha não pode ser salva em texto puro"; JAR não publicado; merge bloqueado |
| [#3](https://github.com/Renan0204/biblioteca/actions/runs/38657338571) | PR [#1](https://github.com/Renan0204/biblioteca/pull/1) | [`66e4a5e`](https://github.com/Renan0204/biblioteca/commit/66e4a5e028f723a8712b1d9fffa6a6b88bfcbec9) | ✅ sucesso | Correção: 2 testes aprovados; merge liberado |

Depois do merge do PR #1, a execução da `master` publica o JAR do commit de merge. O histórico completo está em [Actions → CI](https://github.com/Renan0204/biblioteca/actions/workflows/ci.yml). Logs e artefatos ficam disponíveis por 90 dias.

## Observabilidade, alta disponibilidade e DR (Trabalho 7)

A proposta completa está em [`docs/T7-observabilidade-ha-dr.md`](docs/T7-observabilidade-ha-dr.md). Ela cobre a arquitetura em EKS multi-AZ, SLIs e SLOs, painel, alertas, RPO e RTO, a estratégia de DR, o runbook e os ajustes que a aplicação precisa para rodar com várias réplicas.

## Arquitetura

A aplicação segue uma arquitetura em camadas (MVC + Service + Repository), com separação por pacotes:

- **`com.unialfa.biblioteca.controller`**
  - Controllers MVC (retornam páginas/`templates` Thymeleaf).
  - Ex.: `LivroController`, `AutorController`, `UsuarioController`, `LoginController`.

- **`com.unialfa.biblioteca.api`**
  - Controllers REST (retornam JSON), expostos em `/api/**`.
  - Ex.: `AuthApi` (login e geração do token), `LivroApi`, `AutorApi`.

- **`com.unialfa.biblioteca.service`**
  - Regras de negócio e serviços transversais.
  - Ex.: `LivroService`, `AutorService`, `UsuarioService` e `TokenService`.

- **`com.unialfa.biblioteca.repository`**
  - Repositórios Spring Data (acesso a dados).
  - Ex.: `LivroRepository`, `AutorRepository`, `UsuarioRepository`.

- **`com.unialfa.biblioteca.model`**
  - Entidades JPA (`@Entity`) e modelo de domínio.
  - Ex.: `Usuario`, `Livro`, `Autor`.

- **`com.unialfa.biblioteca.config`**
  - Configurações da aplicação.
  - Ex.: `SecurityConfiguration` (segurança e autenticação).

### Diagrama da Arquitetura

- **Diagrama .mmd**: [`docs/arquitetura.mmd`](docs/arquitetura.mmd)
- **Diagrama .drawio**: [`docs/arquitetura.drawio`](docs/arquitetura.drawio)

![Arquitetura](docs/biblioteca-arquitetura.drawio.png)


### Diagrama do fluxo de autenticação

- **Fonte (Web / formLogin)**: [`docs/auth-web.mmd`](docs/auth-web.mmd)

![Arquitetura](docs/auth-web.png)

- **Fonte (API / JWT)**: [`docs/auth-api-jwt.mmd`](docs/auth-api-jwt.mmd)
  
![Arquitetura](docs/auth-api-jwt.png)

### Fluxo de dados (visão geral)

- **Web (Thymeleaf)**
  - Browser -> `controller/*` -> `service/*` -> `repository/*` -> H2
  - As views ficam em `src/main/resources/templates/**`.

- **API (`/api/**`)**
  - Cliente REST -> `api/*` -> `service/*` -> `repository/*` -> H2
  - Proteção por token JWT (exceto login).

## Frameworks e bibliotecas

Conforme o `pom.xml`:

- **Spring Boot 4.0.6**
  - Base do projeto, autoconfiguração e inicialização.

- **Spring Web MVC (`spring-boot-starter-webmvc`)**
  - Controllers, rotas, validações e camada web.

- **Thymeleaf (`spring-boot-starter-thymeleaf`)**
  - Renderização de HTML no servidor (templates).
  - Integração com segurança via `thymeleaf-extras-springsecurity6`.

- **Spring Data JPA (`spring-boot-starter-data-jpa`)**
  - Persistência com JPA/Hibernate e repositórios.

- **H2 Database**
  - Banco em memória (configurado em `application.properties`).
  - Console habilitado em `/h2-console`.

- **Spring Security (`spring-boot-starter-security`)**
  - Autenticação/Autorização.
  - Dois modos: form-login (web) e resource server JWT (API).

- **Spring OAuth2 Resource Server (`spring-boot-starter-oauth2-resource-server`)**
  - Validação de token JWT no modo Bearer Token para `/api/**`.

## Configurações (application.properties)

- **H2**: `spring.datasource.url=jdbc:h2:mem:banco`
- **Console H2**: `spring.h2.console.path=/h2-console`
- **Segredo JWT**: `api.security.token.secret=${JWT_SECRET}`
  - Este valor é usado para assinar e validar tokens (HMAC SHA-256).
  - Vem da variável de ambiente `JWT_SECRET` (veja [Proteção de segredos](#proteção-de-segredos)).

## Autenticação e autorização (detalhamento)

A segurança está centralizada em `com.unialfa.biblioteca.config.SecurityConfiguration`.

O projeto define **duas cadeias de filtros** (`SecurityFilterChain`) separadas:

- **`apiFilterChain` (Order 1)**: vale somente para rotas `/api/**` e usa JWT.
- **`securityFilterChain` (Order 2)**: vale para o restante do site (Thymeleaf) e usa form-login.

### 1) `@Configuration` e `@EnableWebSecurity`

- **`@Configuration`**
  - Informa ao Spring que a classe declara beans (`@Bean`).
- **`@EnableWebSecurity`**
  - Habilita a configuração de segurança web do Spring Security.

### 2) Propriedade `secret` (`@Value("${api.security.token.secret}")`)

- Lê o segredo definido em `application.properties`, que o obtém da variável de ambiente `JWT_SECRET`.
- É usado para criar as chaves de **assinatura** e **validação** do JWT (algoritmo HS256).

### 3) Cadeia da API: `apiFilterChain(HttpSecurity http)`

Trecho principal:

- **`.securityMatcher("/api/**")`**
  - Diz que esta cadeia **só** se aplica a endpoints que começam com `/api/`.
  - Isso permite ter um modo de segurança para a API e outro para as páginas web.

- **`.authorizeHttpRequests(...)`**
  - Define as regras de autorização.
  - **`/api/auth/login`** está como `permitAll()` porque é o endpoint onde você obtém o token.
  - **Qualquer outra rota** `/api/**` exige usuário autenticado: `.anyRequest().authenticated()`.

- **`.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))`**
  - Ativa o modo *Resource Server*.
  - Isso faz o Spring procurar o token no header:
    - `Authorization: Bearer <token>`
  - A validação do token usa o bean `JwtDecoder` definido na própria classe.

- **`.csrf(AbstractHttpConfigurer::disable)`**
  - Desabilita CSRF para a API.
  - Motivo: API REST stateless normalmente não usa cookie de sessão; o token no header já é a credencial.

- **`.sessionManagement(...SessionCreationPolicy.STATELESS)`**
  - Garante que não haverá sessão HTTP.
  - Cada request precisa enviar o token (stateless).

### 4) Cadeia do site: `securityFilterChain(HttpSecurity http)`

Trecho principal:

- **`.authorizeHttpRequests(...)`**
  - Regras para as páginas web.
  - **`/h2-console/**` e `/usuarios/**`**: somente `hasRole("ADMIN")`.
    - Observação: `hasRole("ADMIN")` procura a authority `ROLE_ADMIN`.
  - **Recursos estáticos** (`/css/**`, `/js/**`, `/images/**`, `/webjars/**`): `permitAll()`.
  - Demais páginas: autenticadas.

- **`.formLogin(form -> ...)`**
  - Ativa login por formulário.
  - **`.loginPage("/login")`**: usa uma página customizada servida por `LoginController`.
  - **`.defaultSuccessUrl("/", true)`**: após login bem-sucedido, redireciona para `/`.
  - **`.permitAll()`**: libera acesso à página de login e ao endpoint de submissão.

- **`.logout(logout -> ...)`**
  - Configura o logout.
  - **`.logoutUrl("/logout")`**: endpoint para encerrar a sessão.
  - **`.logoutSuccessUrl("/login?logout")`**: redireciona após logout.
  - **`.permitAll()`**: logout pode ser acessado por usuários autenticados sem bloqueio extra.

- **`.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))`**
  - Mantém CSRF ligado no site (bom padrão para form-login), mas **ignora** o H2 console.
  - O H2 console normalmente usa frames e posts internos que conflitam com CSRF.

- **`.headers(headers -> headers.frameOptions( ... sameOrigin))`**
  - Ajusta `X-Frame-Options` para permitir que o H2 console funcione.
  - `sameOrigin` permite frames somente do mesmo domínio.

### 5) `PasswordEncoder` (`BCryptPasswordEncoder`)

- Bean `passwordEncoder()` retorna `BCryptPasswordEncoder`.
- Serve para:
  - Armazenar senha com hash seguro.
  - Validar senha no login.
- O `UsuarioService` usa este encoder para salvar senhas.

### 6) `AuthenticationManager`

- Bean `authenticationManager(AuthenticationConfiguration config)`.
- É o componente que executa a autenticação.
- No projeto, ele é usado no endpoint REST:
  - `AuthApi.login()` chama `authenticationManager.authenticate(...)`.

### 7) JWT: `JwtDecoder` e `JwtEncoder`

- **`jwtDecoder()`**
  - Constrói um `SecretKeySpec` com o `secret` e algoritmo `HmacSHA256`.
  - Cria um `NimbusJwtDecoder` para **validar** tokens recebidos na API.

- **`jwtEncoder()`**
  - Usa o mesmo segredo para **assinar** tokens.
  - `ImmutableSecret` + `NimbusJwtEncoder` geram o JWT HS256.

### 8) Como o login da API gera o token

Arquivo: `com.unialfa.biblioteca.api.AuthApi`

- Recebe JSON com `username` e `password` em `/api/auth/login`.
- Autentica com `AuthenticationManager`.
- Se der certo, chama `TokenService.gerar(auth)`.

Arquivo: `com.unialfa.biblioteca.service.TokenService`

- Monta `JwtClaimsSet` com:
  - **`issuer`**: "biblioteca"
  - **`subject`**: `auth.getName()` (login)
  - **`roles`**: authorities do usuário
  - **`expiresAt`**: 24h
- Assina com HS256 e retorna o token.

## Usuários padrão

Em `BibliotecaApplication`, existe um `CommandLineRunner` que cria dois usuários no start:

- `admin` / `admin` com perfil `ADMIN`
- `user` / `user` com perfil `USER`

## Como executar

- Executar a aplicação (antes, defina `JWT_SECRET`, como em [Proteção de segredos](#proteção-de-segredos)):
  - `./mvnw spring-boot:run`
- Acessos:
  - **Site**: `http://localhost:8080/`
  - **Login**: `http://localhost:8080/login`
  - **H2 Console**: `http://localhost:8080/h2-console`

## Como testar a API (exemplo)

- Fazer login e obter token (POST):
  - `http://localhost:8080/api/auth/login`
  - Body JSON:
    - `{ "username": "admin", "password": "admin" }`
- Usar token nas demais rotas:
  - Header: `Authorization: Bearer <token>`

