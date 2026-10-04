# 🤖 Diretrizes para Agentes de IA (AGENTS.md)

Este documento estabelece as diretrizes arquiteturais, padrões de concorrência, modelagem de dados e regras de desenvolvimento para qualquer agente de IA ou desenvolvedor atuando neste repositório.

O **Threads Wallet** é um projeto em **Java 21** e **Spring Boot 3.5** projetado para demonstrar e treinar conceitos avançados de **Concorrência Moderna**:
- **Cargas de I/O (Banco de Dados / Rede):** Isoladas em **Virtual Threads** sob demanda (1 Virtual Thread por carteira processada).
- **Cargas de Processamento (CPU-bound):** Isoladas em um **Pool Fixo de Threads Tradicionais de Plataforma**, dimensionado a partir dos núcleos da máquina (`Runtime.getRuntime().availableProcessors()`) reservando 2 núcleos para o Sistema Operacional, JVM (GC/JIT) e Carrier Threads (`Math.max(1, cores - 2)`).
- **Domínio:** **Simulador de Risco de Portfólios Simplificado** com suporte a múltiplas estratégias via **Strategy Pattern**:
  - **Monte Carlo (`MONTE_CARLO`):** Simulação estocástica de choques com 100.000 iterações por carteira.
  - **VaR Paramétrico (`VAR_PARAMETRICO`):** Cálculo analítico de Value at Risk com nível de confiança de 95% ($z = 1.645 \times \sqrt{\sum w_i^2 \sigma_i^2}$).
- **Persistência:** Spring Data JPA / Hibernate com **PostgreSQL 16** em runtime (configurado via `docker-compose.yml`) e **H2 em memória** exclusivamente para testes automatizados.
- **Arquitetura:** Clean Architecture com guardrails automatizados via **ArchUnit**.

---

## 🧭 1. Princípios de Concorrência Não-Negociáveis

> ⚠️ **Regra de Ouro:** **NUNCA** misturar processamento pesado de CPU na mesma thread que realiza chamadas de banco de dados ou rede.

### 1.1. Gestão de I/O (Virtual Threads)
1. **Executor:** Configurado em [`ConcurrencyConfig`](file:///src/main/java/com/example/threadswallet/infra/config/ConcurrencyConfig.java) como `virtualThreadExecutor` utilizando `Executors.newThreadPerTaskExecutor(...)` com Virtual Threads nomeadas.
2. **Ciclo de Vida por Carteira:**
   Para cada carteira processada, uma **Virtual Thread exclusiva** é disparada no [`ExecutarSimulacaoCargaUseCase`](file:///src/main/java/com/example/threadswallet/application/usecase/ExecutarSimulacaoCargaUseCase.java) e orquestrada no [`ProcessarCarteiraUseCase`](file:///src/main/java/com/example/threadswallet/application/usecase/ProcessarCarteiraUseCase.java):
   - **Passo 1 (I/O - Leitura):** Consulta os ativos da carteira no banco de dados (`findAtivosByCarteiraId`).
   - **Passo 2 (Offload de CPU):** Submete a execução matemática da estratégia de risco selecionada para o pool fixo de CPU (`cpuThreadPool.submit(...)`).
   - **Passo 3 (Espera sem Bloqueio de SO):** Chama `future.get()`. A Virtual Thread é suspensa (*unmounted*) da *Carrier Thread*, liberando o núcleo do sistema operacional para outras tarefas.
   - **Passo 4 (I/O - Escrita):** Após a conclusão da matemática, a Virtual Thread é retomada (*remounted*) e atualiza o risco calculado no banco (`atualizarRisco`).

### 1.2. Gestão de CPU (Pool Fixo de Threads Tradicionais)
1. **Executor:** Configurado em [`ConcurrencyConfig`](file:///src/main/java/com/example/threadswallet/infra/config/ConcurrencyConfig.java) como `cpuThreadPool` utilizando `Executors.newFixedThreadPool(poolSize)`.
2. **Dimensionamento Responsivo:** O tamanho do pool é dimensionado como `Math.max(1, cores - threadsReservadas)` (padrão de 2 threads reservadas via `simulador.cpu-pool.threads-reservadas`), garantindo que o Sistema Operacional, a JVM (Garbage Collector e JIT) e as Carrier Threads de I/O mantenham responsividade contínua mesmo sob saturação de simulações matemáticas.
3. **Estratégias de Cálculo (Stateless - Strategy Pattern):**
   - Ambas as implementações de [`CalculadoraRisco`](file:///src/main/java/com/example/threadswallet/domain/carteira/CalculadoraRisco.java) são **completamente stateless (não guardam estado interno)**.
   - **Monte Carlo ([`MonteCarloCalculadoraRiscoImpl`](file:///src/main/java/com/example/threadswallet/infra/calculation/MonteCarloCalculadoraRiscoImpl.java)):** Executa o laço gerando choques gaussianos aleatórios via `ThreadLocalRandom.current()` para simular volatilidade e esgotar a CPU de forma controlada.
   - **VaR Paramétrico ([`VarParametricoCalculadoraRiscoImpl`](file:///src/main/java/com/example/threadswallet/infra/calculation/VarParametricoCalculadoraRiscoImpl.java)):** Executa a fórmula analítica de variância-covariância sob distribuição normal para VaR 95%.
   - **Injeção de Estratégias via Spring:** O Spring injeta automaticamente todas as implementações (`List<CalculadoraRisco>`) no [`ProcessarCarteiraUseCase`](file:///src/main/java/com/example/threadswallet/application/usecase/ProcessarCarteiraUseCase.java), que indexa as estratégias em um mapa imutável por [`MetodoCalculo`](file:///src/main/java/com/example/threadswallet/domain/carteira/MetodoCalculo.java).

---

## 🗄️ 2. Modelagem de Dados (PostgreSQL / H2 nos Testes)

O banco de dados é propositalmente enxuto, estruturado em duas tabelas com relacionamento 1:N:

```
┌─────────────────────────────────┐
│            CARTEIRA             │
├─────────────────────────────────┤
│ id: BIGINT [PK]                 │
│ nome_cliente: VARCHAR NOT NULL  │
│ risco_calculado: DOUBLE NULL    │
└────────────────┬────────────────┘
                 │ 1
                 │
                 │ N
┌────────────────▼────────────────┐
│              ATIVO              │
├─────────────────────────────────┤
│ id: BIGINT [PK]                 │
│ carteira_id: BIGINT [FK]        │
│ ticker: VARCHAR NOT NULL        │
│ valor_atual: DOUBLE NOT NULL    │
│ taxa_volatilidade: DOUBLE NOT   │
└─────────────────────────────────┘
```

- **Carteira:**
  - `id`: `Long` (Chave Primária, gerada automaticamente)
  - `nome_cliente`: `String` (Nome do titular da carteira)
  - `risco_calculado`: `Double` (Permite nulo; preenchido após o cálculo de Monte Carlo)
- **Ativo:**
  - `id`: `Long` (Chave Primária, gerada automaticamente)
  - `carteira_id`: `Long` (Chave Estrangeira apontando para Carteira)
  - `ticker`: `String` (Código do papel, ex: PETR4, VALE3)
  - `valor_atual`: `Double` (Preço de mercado do ativo)
  - `taxa_volatilidade`: `Double` (Desvio padrão histórico diário, ex: 0.15)

---

## 🏛️ 3. Guardrails Arquiteturais do ArchUnit

O projeto aplica Clean Architecture estrita validada por [`CleanArchitectureTest`](file:///src/test/java/com/example/threadswallet/architecture/CleanArchitectureTest.java):

| Regra | Descrição |
|---|---|
| **1. Pureza do Domínio** | Classes em `..domain..` **não podem** importar `..application..` nem `..infra..`. |
| **2. Independência de Framework** | Classes em `..domain..` **não podem** importar pacotes do Spring (`org.springframework..`). |
| **3. Zero JPA no Domínio** | Classes em `..domain..` **não podem** usar anotações do Jakarta Persistence (`jakarta.persistence..`). |
| **4. Isolamento da Aplicação** | Classes em `..application..` **não podem** importar classes de `..infra..`. |
| **5. Desacoplamento Web** | Classes em `..application..` **não podem** importar o Spring Web (`org.springframework.web..`). |
| **6. Injeção por Construtor** | Proibido `@Autowired` ou `@Value` em propriedades privadas. Injeção **apenas por construtor**. |
| **7. Camadas Estritas** | `Domain` $\leftarrow$ `Application` $\leftarrow$ `Infra`. |
| **8. Localização de Controllers** | Classes `*Controller` devem residir em `..infra.controller..`. |
| **9. Localização de UseCases** | Classes `*UseCase` devem residir em `..application..usecase..`. |
| **10. DIP em Repositórios** | Classes com sufixo `Repository` no `domain` devem ser obrigatoriamente **interfaces**. |

> ⚠️ **Pacote Raiz:** O pacote base de todo o projeto é `com.example.threadswallet`.

---

## 📁 4. Estrutura do Código-Fonte

```
com.example.threadswallet/
├── Application.java                             # Ponto de entrada Spring Boot
├── domain/                                      # Core Agnóstico de Negócio
│   ├── carteira/
│   │   ├── Carteira.java                        # Agregado da carteira
│   │   ├── Ativo.java                           # Entidade do ativo
│   │   ├── CarteiraRepository.java              # Interface de persistência da carteira (DIP)
│   │   ├── AtivoRepository.java                 # Interface de persistência do ativo (DIP)
│   │   ├── CalculadoraRisco.java                # Interface para o cálculo de risco (DIP)
│   │   └── MetodoCalculo.java                   # Enum de métodos de cálculo (MONTE_CARLO, VAR_PARAMETRICO)
│   └── exception/
│       └── DomainException.java                 # Exceção pura de negócio
├── application/                                 # Orquestração de Casos de Uso
│   ├── dto/
│   │   ├── SimulacaoResult.java                 # Resultado e métricas da simulação
│   │   ├── CarteiraDTO.java                     # DTO de leitura de carteira
│   │   └── ProcessamentoAtivosResult.java       # Resultado do processamento de ativos
│   └── usecase/
│       ├── ProcessarCarteiraUseCase.java        # Fluxo I/O -> CPU -> I/O da carteira (com Strategy)
│       ├── ExecutarSimulacaoCargaUseCase.java   # Disparo concorrente de 1.000 Virtual Threads
│       ├── GerarMassaDadosUseCase.java          # Geração em lote de 1.000 carteiras no H2
│       ├── ListarCarteirasUseCase.java          # Consulta de carteiras e riscos
│       └── ProcessarArquivoAtivosUseCase.java   # Ingestão e gravação de ativos em lotes via Virtual Thread
└── infra/                                       # Adaptadores Tecnológicos
    ├── config/
    │   ├── ConcurrencyConfig.java               # Configuração dos pools de threads
    │   └── OpenApiConfig.java                   # Documentação Swagger
    ├── calculation/
    │   ├── MonteCarloCalculadoraRiscoImpl.java  # Motor CPU Monte Carlo (100k iterações)
    │   └── VarParametricoCalculadoraRiscoImpl.java # Motor CPU VaR Paramétrico (Analítico 95%)
    ├── persistence/
    │   ├── CarteiraJpaEntity.java               # Entidade JPA da tabela carteira
    │   ├── AtivoJpaEntity.java                  # Entidade JPA da tabela ativo
    │   ├── CarteiraJpaRepository.java           # Spring Data JPA da carteira
    │   ├── AtivoJpaRepository.java              # Spring Data JPA do ativo
    │   ├── CarteiraRepositoryImpl.java          # Implementação de CarteiraRepository
    │   └── AtivoRepositoryImpl.java             # Implementação de AtivoRepository
    ├── controller/
    │   ├── SimuladorController.java             # Endpoints REST para teste e consulta
    │   ├── UploadAtivoController.java           # Endpoint REST de ingestão de ativos por carteira
    │   └── dto/
    │       ├── SimulacaoResponse.java           # Envelope de resposta HTTP
    │       ├── CarteiraResponse.java            # DTO de resposta de carteira
    │       ├── ArquivoUpload.java               # Encapsulamento e validação de upload multipart
    │       └── UploadArquivoResponse.java       # Resposta HTTP de upload
    ├── tools/
    │   └── DBInstall.java                       # Gerador autônomo de DDL para Migrations Flyway
    └── exception/
        ├── GlobalExceptionHandler.java          # Handler centralizado de erros
        └── ErrorResponse.java                   # Envelope padronizado de erro
```

---

## 🧪 5. Cenário de Carga e Validação

O sistema foi preparado para rodar uma validação massiva com 1.000 carteiras simultâneas:

1. **População da Base:** Insere 1.000 carteiras no banco de dados H2, cada uma contendo de 3 a 5 ativos aleatórios.
2. **Início do Cronômetro:** Marca o instante inicial em milissegundos.
3. **Submissão Massiva:** Submete as 1.000 tarefas concorrentemente ao executor de Virtual Threads de uma única vez em laço.
4. **Execução Híbrida Concorrente:**
   - 1.000 Virtual Threads realizam consultas simultâneas no H2.
   - Cada Virtual Thread envia o cálculo ao Pool de CPU ($N$ threads nativas).
   - O Pool de CPU processa as simulações matemáticas (100.000 iterações cada).
   - As Virtual Threads aguardam sem travar as carrier threads do SO.
   - Conforme cada cálculo conclui, a Virtual Thread grava o risco no H2.
5. **Aferição e Métricas:** O cronômetro é encerrado e são impressos o tempo total, tempo médio por carteira, núcleos utilizados e total de tarefas processadas.

### Como Executar os Testes:
```bash
# Executa todos os testes (Unitários com Mockito/Spy, ArchUnit e Teste Massivo de Integração)
./mvnw test

# Executa apenas a validação arquitetural
./mvnw test -Dtest=CleanArchitectureTest

# Executa os testes unitários de UseCase com Mockito e Spy
./mvnw test -Dtest=ProcessarCarteiraUseCaseTest

# Executa exclusivamente o cenário de carga massivo com 1.000 carteiras
./mvnw test -Dtest=SimuladorConcorrenciaIntegrationTest
```

### 5.1. Padrões de Testes com Mockito e Spy
- **Isolamento de Use Cases:** Utilizar `@ExtendWith(MockitoExtension.class)`.
- **Uso de `@Spy`:** A calculadora [`MonteCarloCalculadoraRiscoImpl`](file:///src/main/java/com/example/threadswallet/infra/calculation/MonteCarloCalculadoraRiscoImpl.java) deve ser espionada via `@Spy` (ou `@MockitoSpyBean` no contexto Spring) para verificar chamadas reais preservando o comportamento matemático ou permitindo substituição parcial via `doReturn(...).when(...)`.
- **Captura de Argumentos:** Empregar `ArgumentCaptor<T>` para validar os valores exatos passados entre a Virtual Thread, a calculadora e a persistência.
- **Validação de Invocação:** Usar `verify(spy, times(...)).calcularRisco(...)` e `verifyNoInteractions(...)` para garantir que o fluxo de concorrência e tratamento de erros respeitem os caminhos esperados.

### 5.2. Hierarquia Estrita de Classes Abstratas de Teste
O projeto adota uma taxonomia estrita para tempo de execução e isolamento de testes:
- **Unitários ([`UnitAbstractTests`](file:///src/test/java/com/example/threadswallet/UnitAbstractTests.java)):**
  - Configuração: [`application-test-unit.properties`](file:///src/test/resources/application-test-unit.properties).
  - Escopo: **Zero Spring Context e zero banco de dados**. Execução instantânea via JUnit 5 e Mockito.
  - Implementações: [`CarteiraTest`](file:///src/test/java/com/example/threadswallet/domain/carteira/CarteiraTest.java), [`MonteCarloCalculadoraRiscoTest`](file:///src/test/java/com/example/threadswallet/infra/calculation/MonteCarloCalculadoraRiscoTest.java), [`ProcessarCarteiraUseCaseTest`](file:///src/test/java/com/example/threadswallet/application/usecase/ProcessarCarteiraUseCaseTest.java), [`ExecutarSimulacaoCargaUseCaseTest`](file:///src/test/java/com/example/threadswallet/application/usecase/ExecutarSimulacaoCargaUseCaseTest.java), [`ProcessarArquivoAtivosUseCaseTest`](file:///src/test/java/com/example/threadswallet/application/usecase/ProcessarArquivoAtivosUseCaseTest.java), [`ArquivoUploadTest`](file:///src/test/java/com/example/threadswallet/infra/controller/dto/ArquivoUploadTest.java), [`DBInstallTest`](file:///src/test/java/com/example/threadswallet/infra/tools/DBInstallTest.java).
- **Repositório ([`RepositoryAbstractTests`](file:///src/test/java/com/example/threadswallet/RepositoryAbstractTests.java)):**
  - Configuração: [`application-test-repository.properties`](file:///src/test/resources/application-test-repository.properties).
  - Escopo: `@DataJpaTest`, carrega **exclusivamente a camada de persistência** (JPA/Hibernate) e o banco de dados H2 com migrations via Flyway.
  - Implementações: [`CarteiraRepositoryTest`](file:///src/test/java/com/example/threadswallet/infra/persistence/CarteiraRepositoryTest.java), [`AtivoRepositoryTest`](file:///src/test/java/com/example/threadswallet/infra/persistence/AtivoRepositoryTest.java), [`FlywayMigrationTest`](file:///src/test/java/com/example/threadswallet/infra/persistence/FlywayMigrationTest.java).
- **Integração ([`IntegrationAbstractTests`](file:///src/test/java/com/example/threadswallet/IntegrationAbstractTests.java)):**
  - Configuração: [`application-test-integration.properties`](file:///src/test/resources/application-test-integration.properties).
  - Escopo: `@SpringBootTest`, **sobe o contexto completo** da aplicação (pools de threads nativas e virtuais, banco e beans).
  - Implementações: [`SimuladorConcorrenciaIntegrationTest`](file:///src/test/java/com/example/threadswallet/integration/SimuladorConcorrenciaIntegrationTest.java).
- **Controllers ([`ControllerAbstractTests`](file:///src/test/java/com/example/threadswallet/ControllerAbstractTests.java)):**
  - Herda de `IntegrationAbstractTests` e configura `@AutoConfigureMockMvc` para simulação HTTP sem subir porta de rede.
  - Implementações: [`SimuladorControllerTest`](file:///src/test/java/com/example/threadswallet/infra/controller/SimuladorControllerTest.java), [`UploadAtivoControllerTest`](file:///src/test/java/com/example/threadswallet/infra/controller/UploadAtivoControllerTest.java).

---

## 🌐 6. Endpoints da API REST

Com a aplicação rodando (`./mvnw spring-boot:run` com o PostgreSQL do `docker-compose up -d` ativo):
- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Banco de Dados (Runtime):** PostgreSQL (`jdbc:postgresql://localhost:5432/walletDb`, Usuário: `usuario`, Senha: `senha_forte`)

| Método | Endpoint | Parâmetros | Descrição |
|---|---|---|---|
| `POST` | `/api/simulador/massa-dados` | `totalCarteiras` (padrão: 1000)<br>`limparAntes` (padrão: true) | **Passo 1:** Gera a massa de carteiras com 3 a 5 ativos cada no banco. |
| `POST` | `/api/simulador/executar` | `metodo` (obrigatório, opções: `MONTE_CARLO`, `VAR_PARAMETRICO`)<br>`limite` (opcional)<br>`iteracoes` (padrão: 100000) | **Passo 2:** Dispara o cálculo concorrente com Virtual Threads e CPU pool para as carteiras cadastradas usando a estratégia selecionada. Retorna erro 400 se o método não for informado ou se a base estiver vazia. |
| `GET` | `/api/simulador/carteiras` | - | **Passo 3:** Consulta as carteiras cadastradas e seus riscos calculados. |
| `POST` | `/api/carteiras/{carteiraId}/ativos/upload` | `carteiraId` (Path, ID da carteira)<br>`arquivo` (Multipart, `.csv` ou `.txt`) | **Ingestão de Ativos por Carteira:** Recebe arquivo de ativos e associa todos à carteira indicada no path, salvando em lotes atômicos. Rejeita arquivos vazios, formatos inválidos ou carteiras inexistentes (400). |

---

## 📋 7. Checklist para Implementações Futuras

Ao criar novas regras, endpoints ou fluxos de concorrência:
- [ ] Tarefas de I/O (consultas JDBC/JPA, HTTP externo, arquivos) **devem** ser submetidas ao `virtualThreadExecutor`.
- [ ] Cálculos matemáticos pesados ou loops intensivos de CPU **devem** ser submetidos ao `cpuThreadPool`.
- [ ] Nunca chamar `monteCarloCalculadora.calcularRisco(...)` diretamente na Virtual Thread sem despachar para o pool de CPU.
- [ ] Manter construtores privados e fábricas estáticas em entidades de domínio.
- [ ] Rodar `./mvnw test` e verificar se os 10 guardrails do ArchUnit continuam 100% íntegros.
