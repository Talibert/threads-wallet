# ⚡ Threads Wallet - Simulador de Risco de Portfólios com Concorrência Híbrida

![Java 21](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![Spring Boot 3.5](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen?style=flat-square&logo=springboot)
![Virtual Threads](https://img.shields.io/badge/Virtual%20Threads-Project%20Loom-blue?style=flat-square)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square&logo=postgresql)
![H2 Database](https://img.shields.io/badge/H2-In--Memory%20(Testes)-lightblue?style=flat-square)
![ArchUnit](https://img.shields.io/badge/ArchUnit-1.3-yellow?style=flat-square)
![OpenAPI 3](https://img.shields.io/badge/OpenAPI-3.0%20%2F%20Swagger-green?style=flat-square&logo=swagger)

Projeto desenvolvido em **Java 21** e **Spring Boot 3** focado em demonstrar o uso avançado de concorrência com **Virtual Threads** e **Threads Tradicionais de Plataforma**, garantindo o isolamento estrito entre cargas de trabalho de **I/O** e **CPU**.
Utiliza **PostgreSQL 16** via Docker em runtime e **H2 em memória** exclusivamente para a suíte de testes automatizados.

O domínio do sistema é um **Simulador de Risco de Portfólios** simplificado, utilizando arquitetura orientada ao padrão **Strategy** para suportar múltiplos métodos de avaliação de risco:
- **Monte Carlo (`MONTE_CARLO`):** Simulação estocástica de choques com 100.000 iterações por carteira geradas via `ThreadLocalRandom.current().nextGaussian()`.
- **VaR Paramétrico (`VAR_PARAMETRICO`):** Cálculo analítico de Value at Risk com nível de confiança de 95% ($z = 1.645 \times \sqrt{\sum w_i^2 \sigma_i^2}$).

---

## 🎯 Arquitetura de Concorrência

> **Princípio Central:** O sistema **não mistura** processamento pesado na mesma thread que faz chamadas de rede ou banco de dados.

```
                              [ Carga de 1.000 Carteiras ]
                                           │
                                           ▼
               ┌────────────────────────────────────────────────────────┐
               │    GESTÃO DE I/O: Executor de Virtual Threads (Loom)   │
               │         1 Virtual Thread exclusiva por carteira        │
               └───────────────────────────┬────────────────────────────┘
                                           │
                        ┌──────────────────┴──────────────────┐
                        │                                     │
                 (1) Leitura I/O                       (4) Escrita I/O
                        │                                     ▲
                        ▼                                     │
               ┌─────────────────┐                   ┌────────────────┐
               │ Consulta Ativos │                   │ Atualiza Risco │
               │     no H2       │                   │    no H2       │
               └────────┬────────┘                   └────────┬───────┘
                        │                                     │
                        │ (2) Submete dados à Estratégia      │ (3) Retorna risco
                        ▼                                     │
        ┌─────────────────────────────────────────────────────┴──────────────────┐
        │        GESTÃO DE CPU: Pool Fixo de Threads Tradicionais (Platform)     │
        │             Tamanho = Núcleos da máquina - 2 (Reservadas p/ SO/JVM)    │
        │       Executa Estratégia Selecionada (Monte Carlo ou VaR Paramétrico)  │
        └────────────────────────────────────────────────────────────────────────┘
```

### 1. Gestão de I/O (Virtual Threads)
- Para cada uma das 1.000 carteiras processadas, uma **Virtual Thread sob demanda** é disparada.
- A Virtual Thread realiza a consulta no banco para buscar os ativos associados à carteira.
- Em seguida, despacha a tarefa de computação pesada para o Pool de CPU e aguarda o resultado (`Future.get()`).
- Durante essa espera, a Virtual Thread é **suspensa (*unmounted*)** de sua *Carrier Thread*, liberando o núcleo do sistema operacional para continuar processando outras tarefas.
- Quando o cálculo é finalizado, a Virtual Thread é **retomada (*remounted*)** e atualiza o banco de dados com o risco calculado.

### 2. Gestão de CPU (Pool Fixo de Threads Nativas & Strategy Pattern)
- Pool de threads tradicionais configurado com tamanho fixo igual ao número de núcleos disponíveis na máquina:
  ```java
  int cores = Runtime.getRuntime().availableProcessors();
  Executors.newFixedThreadPool(cores, Thread.ofPlatform().name("cpu-worker-", 1).factory());
  ```
- **Padrão Strategy Dinâmico:** Todas as implementações de `CalculadoraRisco` são injetadas pelo Spring (`List<CalculadoraRisco>`) e roteadas dinamicamente:
  - **Monte Carlo (`MonteCarloCalculadoraRiscoImpl`):** Executa um laço longo de **100.000 iterações** matemáticas de choque gaussiano saturando a CPU com simulações estatísticas.
  - **VaR Paramétrico (`VarParametricoCalculadoraRiscoImpl`):** Executa a resolução analítica de risco de forma ultra veloz (~0.1 ms por carteira).
- Garante saturação máxima dos núcleos físicos de processamento, **sem incorrer no custo excessivo de troca de contexto (*context switching*)** de criar milhares de threads nativas do SO.

---

## 🗄️ Modelagem de Dados (PostgreSQL / H2 nos Testes)

Estrutura relacional com entidades independentes e normalizadas:

```
┌──────────────────────────────────────┐
│               CARTEIRA               │
├──────────────────────────────────────┤
│ id: BIGINT [PK, Auto Increment]      │
│ nome_cliente: VARCHAR NOT NULL       │
└─────────┬──────────────────┬─────────┘
          │ 1                │ 1
          │                  │
          │ N                │ N
┌─────────▼────────────┐  ┌──▼───────────────────────────────────┐
│        ATIVO         │  │           RISCO_CALCULADO            │
├──────────────────────┤  ├──────────────────────────────────────┤
│ id: BIGINT [PK]      │  │ id: BIGINT [PK, Auto Increment]      │
│ carteira_id: FK      │  │ carteira_id: BIGINT [FK -> CARTEIRA] │
│ ticker: VARCHAR      │  │ valor: DOUBLE NOT NULL               │
│ valor_atual: DOUBLE  │  │ tipo: VARCHAR NOT NULL               │
│ taxa_volatilidade: D │  └──────────────────────────────────────┘
└──────────────────────┘
```

---

## 📁 Estrutura do Projeto (Clean Architecture)

```
com.example.threadswallet/
├── Application.java                             # Inicialização Spring Boot
├── domain/                                      # Regras de Negócio e Domínio Puro
│   ├── carteira/
│   │   ├── Carteira.java                        # Entidade / Agregado
│   │   ├── Ativo.java                           # Entidade de Ativo
│   │   ├── RiscoCalculado.java                  # Entidade de Risco Calculado
│   │   ├── CarteiraRepository.java              # Interface de Repositório Carteira (DIP)
│   │   ├── AtivoRepository.java                 # Interface de Repositório Ativo (DIP)
│   │   ├── RiscoCalculadoRepository.java        # Interface de Repositório Risco (DIP)
│   │   ├── CalculadoraRisco.java                # Interface de Cálculo de Risco (DIP)
│   │   ├── AmostraRisco.java                    # Record de agregação de amostras Map-Reduce
│   │   └── MetodoCalculo.java                   # Enum de Métodos de Cálculo (MONTE_CARLO, VAR_PARAMETRICO)
│   └── exception/
│       └── DomainException.java                 # Exceções de Domínio
├── application/                                 # Casos de Uso e Orquestração
│   ├── dto/
│   │   ├── SimulacaoResult.java                 # Métricas da simulação em lote
│   │   ├── CarteiraIndividualResult.java        # Métricas de cálculo individual
│   │   ├── CarteiraDTO.java                     # DTO de leitura de carteira
│   │   └── RiscoCalculadoDTO.java               # DTO de risco calculado
│   └── usecase/
│       ├── ProcessarMultiplasCarteirasUseCase.java # Disparo em lote das 1.000 Virtual Threads
│       ├── ProcessarCarteiraIndividualUseCase.java # Cálculo de carteira única com particionamento de CPU
│       ├── ProcessarCarteiraUseCase.java        # Orquestração I/O -> CPU -> I/O no lote
│       ├── GerarMassaDadosUseCase.java          # Inserção em lote no banco
│       └── ListarCarteirasUseCase.java          # Consulta de carteiras
└── infra/                                       # Adaptadores Tecnológicos
    ├── config/
    │   ├── ConcurrencyConfig.java               # Configuração dos Executores (VT e CPU)
    │   └── OpenApiConfig.java                   # Configuração Swagger
    ├── calculation/
    │   ├── MonteCarloCalculadoraRiscoImpl.java  # Motor de Monte Carlo (Map-Reduce ou sequencial)
    │   └── VarParametricoCalculadoraRiscoImpl.java # Motor de VaR Paramétrico (Analítico 95%)
    ├── persistence/
    │   ├── CarteiraJpaEntity.java               # Entidade JPA Carteira
    │   ├── AtivoJpaEntity.java                  # Entidade JPA Ativo
    │   ├── RiscoCalculadoJpaEntity.java         # Entidade JPA Risco Calculado
    │   ├── CarteiraJpaRepository.java           # Spring Data JPA Carteira
    │   ├── AtivoJpaRepository.java              # Spring Data JPA Ativo
    │   ├── RiscoCalculadoJpaRepository.java     # Spring Data JPA Risco Calculado
    │   ├── CarteiraRepositoryImpl.java          # Implementação de CarteiraRepository
    │   ├── AtivoRepositoryImpl.java             # Implementação de AtivoRepository
    │   └── RiscoCalculadoRepositoryImpl.java    # Implementação de RiscoCalculadoRepository
    ├── controller/
    │   ├── SimuladorController.java             # Endpoints REST (em lote e individual)
    │   ├── UploadAtivoController.java           # Endpoint REST de upload de ativos
    │   └── dto/
    │       ├── SimulacaoResponse.java           # DTO de resposta da simulação em lote
    │       ├── CarteiraIndividualResponse.java  # DTO de resposta do cálculo individual
    │       ├── CarteiraResponse.java            # DTO de carteira
    │       └── RiscoCalculadoResponse.java      # DTO de risco calculado
    ├── tools/
    │   └── DBInstall.java                       # Gerador autônomo de DDL para Migrations Flyway
    └── exception/
        ├── GlobalExceptionHandler.java          # Tratamento de exceções
        └── ErrorResponse.java                   # Payload de erro
```

---

## 🗄️ Migrations com Flyway e Utilitário DBInstall

O banco de dados de produção (PostgreSQL) e os testes automatizados utilizam o **Flyway** para versionamento de schema:
- **Localização dos scripts:** [`src/main/resources/db/migration/`](file:///src/main/resources/db/migration/)
- **Migration inicial:** [`V1__create_carteira_and_ativo_tables.sql`](file:///src/main/resources/db/migration/V1__create_carteira_and_ativo_tables.sql)
- **Validação de Schema:** `spring.jpa.hibernate.ddl-auto=validate` garante sincronismo estrito entre entidades JPA e as tabelas reais.

### Utilitário `DBInstall`:
O projeto possui o utilitário autônomo [`DBInstall`](file:///src/main/java/com/example/threadsWallet/infra/tools/DBInstall.java), que inspeciona as entidades JPA via reflexão e gera os comandos DDL formatados para PostgreSQL:
```bash
./mvnw compile exec:java -Dexec.mainClass="com.example.threadswallet.infra.tools.DBInstall"
```

---

## ⚡ Como Executar

### Pré-requisitos:
- **Java 21** ou superior instalado.
- **Docker** e **Docker Compose** instalados.

### 1. Subir o PostgreSQL via Docker Compose:
```bash
# Cria a rede docker caso não exista
docker network create wallet-network

# Sobe o container wallet-db com o PostgreSQL 16
docker-compose up -d
```

### 2. Iniciar a Aplicação:
```bash
./mvnw spring-boot:run
```

A aplicação inicializa conectando ao PostgreSQL `walletDb` em `localhost:5432`:
- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Credenciais do PostgreSQL:** Definidas no `docker-compose.yml`:
  - **URL:** `jdbc:postgresql://localhost:5432/walletDb`
  - **Usuário:** `usuario`
  - **Senha:** `senha_forte`

---

## 🧪 Cenário de Teste de Carga Massiva

O projeto inclui um teste automatizado ponta a ponta ([`SimuladorConcorrenciaIntegrationTest`](file:///src/test/java/com/example/threadswallet/integration/SimuladorConcorrenciaIntegrationTest.java)):

1. Insere **1.000 carteiras** no banco H2 (com **3 a 5 ativos** cada, totalizando entre 3.000 e 5.000 ativos).
2. Dispara o cronômetro.
3. Submete as **1.000 carteiras simultaneamente** ao executor de Virtual Threads em um laço.
4. O Pool de CPU processa as simulações matemáticas (**100.000 iterações** de Monte Carlo por carteira $\times$ 1.000 carteiras = **100.000.000 iterações** no total).
5. As Virtual Threads gravam os riscos calculados no H2.
6. Imprime o tempo total gasto e valida se todas as 1.000 carteiras possuem risco calculado maior que zero.

### Executando os Testes via Terminal:
```bash
./mvnw test
```

### Exemplo de Saída no Console:
```
================================================================================
 🚀 SIMULAÇÃO DE CARGA CONCLUÍDA COM SUCESSO!
================================================================================
 📊 Total de Carteiras Processadas: 1000
 🏷️  Método de Cálculo: VAR_PARAMETRICO
 ⏱️  Tempo Total Decorrido: 129 ms (0.13 s)
 ⚡ Tempo Médio por Carteira: 0.13 ms
 🧠 Núcleos de CPU (Pool Fixo): 10
 🎲 Iterações Parametrizadas: 100000
 🧵 Gestão de I/O: 1000 Virtual Threads disparadas concorrentemente
================================================================================

================================================================================
 🚀 SIMULAÇÃO DE CARGA CONCLUÍDA COM SUCESSO!
================================================================================
 📊 Total de Carteiras Processadas: 1000
 🏷️  Método de Cálculo: MONTE_CARLO
 ⏱️  Tempo Total Decorrido: 17428 ms (17.43 s)
 ⚡ Tempo Médio por Carteira: 17.43 ms
 🧠 Núcleos de CPU (Pool Fixo): 10
 🎲 Iterações Parametrizadas: 100000
 🧵 Gestão de I/O: 1000 Virtual Threads disparadas concorrentemente
================================================================================
```

### 🧬 Taxonomia e Hierarquia de Testes

Para garantir testes rápidos e sem acoplamentos desnecessários, o projeto define classes abstratas dedicadas:

| Categoria | Classe Base | Arquivo de Propriedades | Escopo Carregado |
|---|---|---|---|
| **Unitários** | [`UnitAbstractTests`](file:///src/test/java/com/example/threadswallet/UnitAbstractTests.java) | `application-test-unit.properties` | **Zero Spring Context, zero banco**. Apenas JUnit 5 e Mockito. |
| **Repositório** | [`RepositoryAbstractTests`](file:///src/test/java/com/example/threadswallet/RepositoryAbstractTests.java) | `application-test-repository.properties` | `@DataJpaTest`: Apenas banco H2 + JPA + Migrations Flyway. |
| **Integração** | [`IntegrationAbstractTests`](file:///src/test/java/com/example/threadswallet/IntegrationAbstractTests.java) | `application-test-integration.properties` | `@SpringBootTest`: Contexto completo, pools de concorrência e banco. |
| **Controllers** | [`ControllerAbstractTests`](file:///src/test/java/com/example/threadswallet/ControllerAbstractTests.java) | `application-test-integration.properties` | Herda integração e disponibiliza `MockMvc` para chamadas HTTP. |

---

## 🌐 Endpoints REST

| Método | Endpoint | Parâmetros | Descrição |
|---|---|---|---|
| `POST` | `/api/simulador/massa-dados` | `totalCarteiras` (padrão: 1000)<br>`limparAntes` (padrão: true) | **Passo 1:** Gera a base de carteiras com 3 a 5 ativos cada no banco. |
| `POST` | `/api/simulador/executar` | `metodo` (obrigatório, opções: `MONTE_CARLO`, `VAR_PARAMETRICO`)<br>`limite` (opcional)<br>`iteracoes` (padrão: 100000) | **Passo 2:** Dispara o cálculo concorrente em lote (Throughput) com Virtual Threads e CPU pool para as carteiras cadastradas usando a estratégia selecionada. Retorna erro 400 se o método não for informado ou se a base estiver vazia. |
| `POST` | `/api/simulador/carteiras/{carteiraId}/executar` | `carteiraId` (Path, ID da carteira)<br>`metodo` (obrigatório: `MONTE_CARLO`, `VAR_PARAMETRICO`)<br>`iteracoes` (opcional, padrão: 100000) | **Cálculo de Carteira Individual (Latency):** Calcula o risco de uma carteira sob demanda. Para `MONTE_CARLO`, particiona as iterações entre todas as threads de CPU (Map-Reduce). Para `VAR_PARAMETRICO`, executa direto em 1 thread de CPU. |
| `GET` | `/api/simulador/carteiras` | - | **Passo 3:** Consulta as carteiras e seus riscos calculados. |
| `POST` | `/api/carteiras/{carteiraId}/ativos/upload` | `carteiraId` (Path, ID da carteira)<br>`arquivo` (Multipart, `.csv` ou `.txt`) | **Ingestão de Ativos por Carteira:** Recebe arquivo de ativos e associa todos à carteira indicada no path, salvando em lotes atômicos. |

### Exemplo de Disparo sob Demanda via cURL:

```bash
# 1. Gerar a massa de 1.000 carteiras no banco
curl -X POST "http://localhost:8080/api/simulador/massa-dados?totalCarteiras=1000&limparAntes=true"

# 2. Disparar a simulação e o cálculo concorrente com VaR Paramétrico
curl -X POST "http://localhost:8080/api/simulador/executar?metodo=VAR_PARAMETRICO"

# Ou disparar a simulação com Monte Carlo (100.000 iterações)
curl -X POST "http://localhost:8080/api/simulador/executar?metodo=MONTE_CARLO&iteracoes=100000"

# 3. Consultar as carteiras e riscos resultantes
curl -X GET "http://localhost:8080/api/simulador/carteiras"
```

### Exemplo de Resposta JSON:
```json
{
  "totalCarteirasProcessadas": 1000,
  "metodoCalculo": "VAR_PARAMETRICO",
  "tempoTotalMs": 129,
  "tempoTotalSegundos": 0.13,
  "tempoMedioPorCarteiraMs": 0.13,
  "nucleosCpuDisponiveis": 10,
  "iteracoesParametrizadas": 100000,
  "mensagem": "Simulação massiva de concorrência concluída com sucesso."
}
```

---

## 🏛️ Guardrails com ArchUnit

O projeto possui 10 regras arquiteturais verificadas continuamente via [`CleanArchitectureTest`](file:///src/test/java/com/example/threadswallet/architecture/CleanArchitectureTest.java), garantindo:
- Pureza absoluta do `domain` (zero dependências de Spring, JPA ou HTTP).
- Isolamento da camada `application`.
- Injeção obrigatória por construtor (sem field injection com `@Autowired`).
- Inversão de dependência (DIP) com interfaces de repositório no domínio.

```bash
./mvnw test -Dtest=CleanArchitectureTest
```