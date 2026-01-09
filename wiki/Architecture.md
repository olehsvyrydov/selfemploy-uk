# Architecture

**Author**: Jorge (Principal Solution Architect)
**Status**: ✅ Approved
**Date**: 2026-01-09

---

## Overview

The UK Self-Employment Manager is a desktop application built with **Quarkus + JavaFX**, designed for privacy-first local data storage with optional cloud backup.

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     UK Self-Employment Manager                          │
│                        Desktop Application                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                      PRESENTATION LAYER                          │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │   │
│  │  │    FXML     │  │  ViewModels │  │    JavaFX Controllers   │  │   │
│  │  │   Views     │  │   (MVVM)    │  │    (Event Handlers)     │  │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                    │                                    │
│                                    ▼                                    │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                      APPLICATION LAYER                           │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │   │
│  │  │   Income    │  │   Expense   │  │     Tax Calculator      │  │   │
│  │  │   Service   │  │   Service   │  │       Service           │  │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                    │                                    │
│                    ┌───────────────┼───────────────┐                   │
│                    ▼               ▼               ▼                   │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐     │
│  │   PERSISTENCE    │  │    HMRC API      │  │   CLOUD BACKUP   │     │
│  │  (H2 Encrypted)  │  │   (MTD APIs)     │  │   (Google Drive) │     │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘     │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Module Structure

```
selfemploy-uk/
├── common/          # Domain entities, DTOs, enums
├── persistence/     # H2 database, Panache repositories, Flyway
├── hmrc-api/        # HMRC MTD REST clients, OAuth2, fraud headers
├── core/            # Business logic, tax calculator, services
├── ui/              # JavaFX controllers, FXML layouts, CSS
└── app/             # Application launcher, packaging
```

### Module Dependencies

```
common ◄─── persistence ◄─── core ◄─── ui ◄─── app
   ▲            ▲              ▲
   │            │              │
   └────────────┴── hmrc-api ──┘
```

---

## Technology Stack

| Layer | Technology | Version |
|-------|------------|---------|
| DI Framework | Quarkus | 3.17.x |
| UI Framework | JavaFX | 21.x |
| Database | H2 (encrypted) | 2.3.x |
| ORM | Hibernate + Panache | 6.x |
| Migrations | Flyway | 10.x |
| HTTP Client | Quarkus REST Client | 3.x |
| Build | Maven | 3.9+ |
| Native | GraalVM | 21+ |

---

## Design Patterns

### MVVM (Model-View-ViewModel)

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│      VIEW       │     │   VIEW MODEL    │     │     MODEL       │
│     (FXML)      │◄───►│   (Observable)  │◄───►│   (Services)    │
│                 │     │                 │     │                 │
│  - Bindings     │     │  - Properties   │     │  - Business     │
│  - Event refs   │     │  - Commands     │     │    Logic        │
│  - No logic     │     │  - Validation   │     │  - Data Access  │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

### Repository Pattern

```java
@ApplicationScoped
public class IncomeRepository implements PanacheRepository<IncomeEntity> {

    public List<IncomeEntity> findByTaxYear(TaxYear taxYear) {
        return find("date >= ?1 and date <= ?2",
            taxYear.startDate(), taxYear.endDate()).list();
    }
}
```

### Service Layer

```java
@ApplicationScoped
public class TaxCalculationService {

    @Inject IncomeRepository incomeRepo;
    @Inject ExpenseRepository expenseRepo;
    @Inject TaxRatesProvider taxRates;

    public TaxCalculation calculate(TaxYear taxYear) {
        var totalIncome = incomeRepo.sumByTaxYear(taxYear);
        var totalExpenses = expenseRepo.sumDeductibleByTaxYear(taxYear);
        var taxableProfit = totalIncome.subtract(totalExpenses);

        var rates = taxRates.forYear(taxYear);
        var incomeTax = rates.calculateIncomeTax(taxableProfit);
        var ni = rates.calculateNI(taxableProfit);

        return new TaxCalculation(taxableProfit, incomeTax, ni);
    }
}
```

---

## Security Architecture

### Database Encryption

```
User Master Password
        │
        ▼
┌───────────────┐
│   PBKDF2      │  (100,000 iterations, SHA-256)
│   Key Derive  │
└───────┬───────┘
        │
        ▼
┌───────────────┐
│  256-bit Key  │  (Database Encryption Key)
└───────┬───────┘
        │
        ▼
┌───────────────────────────────────────┐
│            H2 Database                 │
│  jdbc:h2:file:./data/tax;CIPHER=AES   │
└───────────────────────────────────────┘
```

### OAuth Token Storage

| Platform | Storage Method |
|----------|---------------|
| Windows | Windows Credential Manager |
| macOS | Keychain |
| Linux | Secret Service API / Encrypted file |

---

## Architecture Decision Records (ADRs)

### ADR-001: Quarkus over Spring Boot

**Decision**: Use Quarkus 3.x instead of Spring Boot.

**Rationale**:
- Faster startup time (sub-second vs 3-5 seconds)
- Lower memory footprint (~50MB vs ~200MB)
- Better GraalVM native image support

### ADR-002: H2 with File Encryption

**Decision**: Use H2 database with built-in AES encryption.

**Rationale**:
- Native AES-256 encryption support
- Better Java integration than SQLite
- Flyway migration support

### ADR-003: MVVM Pattern for JavaFX

**Decision**: Implement MVVM (Model-View-ViewModel) pattern.

**Rationale**:
- ViewModels are testable without UI
- JavaFX Properties provide natural binding
- Clear separation of concerns

### ADR-004: OS Keychain for Token Storage

**Decision**: Use platform-native secure storage for HMRC tokens.

**Rationale**:
- OS-managed security is more robust
- Tokens protected even if app data is compromised
- Standard practice for desktop applications

### ADR-005: Six-Module Maven Structure

**Decision**: Split project into common, persistence, hmrc-api, core, ui, app modules.

**Rationale**:
- Clear dependency hierarchy
- Enables parallel development
- Each module has single responsibility

---

## Threading Model

JavaFX requires UI updates on the Application Thread:

```java
@ApplicationScoped
public class UIExecutor {

    private final ExecutorService backgroundExecutor =
        Executors.newFixedThreadPool(4);

    public <T> void executeAsync(
            Supplier<T> backgroundTask,
            Consumer<T> uiUpdate,
            Consumer<Throwable> onError) {

        backgroundExecutor.submit(() -> {
            try {
                T result = backgroundTask.get();
                Platform.runLater(() -> uiUpdate.accept(result));
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e));
            }
        });
    }
}
```

---

## Native Image (GraalVM)

For native compilation, use Gluon's GraalVM plugin:

```xml
<plugin>
    <groupId>com.gluonhq</groupId>
    <artifactId>gluonfx-maven-plugin</artifactId>
    <version>1.0.22</version>
    <configuration>
        <target>host</target>
        <mainClass>uk.selfemploy.app.Launcher</mainClass>
    </configuration>
</plugin>
```

### Required Configuration

- `reflect-config.json` - Classes accessed via reflection
- `resource-config.json` - FXML, CSS, images, SQL migrations
