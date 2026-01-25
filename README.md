# UK Self-Employment Manager

[![CI Build](https://github.com/olehsvyrydov/selfemploy-uk/actions/workflows/ci.yml/badge.svg)](https://github.com/olehsvyrydov/selfemploy-uk/actions/workflows/ci.yml)
[![CodeQL](https://github.com/olehsvyrydov/selfemploy-uk/actions/workflows/codeql.yml/badge.svg)](https://github.com/olehsvyrydov/selfemploy-uk/actions/workflows/codeql.yml)

A free, open-source desktop application for UK self-employed individuals to manage their accounting and submit annual reports to HMRC via Making Tax Digital (MTD) APIs.

## Features

- **Income Tracking**: Record invoices, payments, and income sources
- **Expense Management**: Categorize expenses aligned with SA103 form categories
- **Tax Calculator**: Real-time estimates for Income Tax and National Insurance Class 4
- **HMRC Integration**: One-click annual report submission via MTD APIs
- **Privacy-First**: All data stored locally with AES-256 encryption
- **Cross-Platform**: Windows, macOS, and Linux support

## Why This Project?

- **FREE**: No monthly subscriptions (QuickBooks £12-32/mo, FreeAgent £14.50/mo)
- **Privacy-First**: Your financial data stays on your computer
- **Open Source**: Transparent, community-driven development
- **MTD Ready**: Compliant with HMRC Making Tax Digital requirements

## Quick Start

### Prerequisites

- Java 21 or later
- Maven 3.9+

### Build & Run

```bash
# Clone the repository
git clone https://github.com/olehsvyrydov/selfemploy-uk.git
cd selfemploy-uk

# Build the project
mvn clean install

# Run the application
mvn -pl app javafx:run
```

### Create Native Installer

```bash
# Build native installer for your platform
mvn -pl app -Ppackage jpackage:jpackage
```

## Project Structure

```
self-employment/
├── common/          # Shared domain entities, DTOs, enums
├── persistence/     # Database entities, repositories, Flyway migrations
├── hmrc-api/        # HMRC MTD API clients, OAuth2, fraud prevention
├── core/            # Business logic, tax calculator, services
├── ui/              # JavaFX controllers, FXML layouts, CSS styling
├── app/             # Application launcher and native packaging
└── docs/            # Documentation, sprints, architecture
```

## Technology Stack

| Component | Technology |
|-----------|------------|
| Backend | Quarkus 3.x |
| UI | JavaFX 21+ |
| Database | H2 (encrypted) |
| Migrations | Flyway |
| Build | Maven + jpackage |
| Native | GraalVM Native Image |

## Target Users

1. **Self-Employed Individuals**: Freelancers, contractors, sole traders
2. **Partnerships**: (Phase 2) Business partners sharing profits
3. **Ltd Company Directors**: (Phase 3) Salary/dividend optimization

## Plugin System

The application features an extensible plugin architecture that allows third-party developers to add new functionality without modifying the core codebase.

### Key Features

- **9 Extension Points**: Navigation pages, dashboard widgets, custom reports, data importers/exporters, tax calculation extensions, expense categories, external integrations, and HMRC API extensions
- **Secure by Design**: Permission-based security model with 10 granular permissions across 3 sensitivity levels
- **Isolated Execution**: Each plugin runs in its own classloader with isolated data storage
- **Plugin Lifecycle**: Full lifecycle management (load, enable, disable, unload)

### For Plugin Developers

See the comprehensive [Plugin Developer Documentation](docs/plugin-development/README.md) to get started creating plugins.

```xml
<!-- Add to your pom.xml -->
<dependency>
    <groupId>uk.selfemploy</groupId>
    <artifactId>self-employment-plugin-api</artifactId>
    <version>0.1.0</version>
    <scope>provided</scope>
</dependency>
```

### Extension Points Available

| Extension Point | Purpose |
|-----------------|---------|
| NavigationExtension | Add navigation pages |
| DashboardWidget | Custom dashboard widgets |
| ReportGenerator | Generate custom reports |
| DataImporter | Import data (CSV, bank statements) |
| DataExporter | Export to other formats |
| TaxCalculatorExtension | Custom tax calculations |
| ExpenseCategoryExtension | Industry-specific categories |
| IntegrationExtension | External service integration |
| HmrcApiExtension | HMRC API extensions |

## HMRC Making Tax Digital (MTD) Timeline

| Date | Requirement |
|------|-------------|
| April 2026 | Income > £50,000 must use MTD |
| April 2027 | Income > £30,000 must use MTD |
| April 2028 | Income > £20,000 must use MTD |

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Disclaimer

This software is provided for informational purposes only and does not constitute professional tax or financial advice. Always consult with a qualified accountant for significant financial decisions. The software calculates estimates based on current tax rules and rates, which may change.

---

Built with assistance from the [AI Scrum Team project (Claude Code)](https://github.com/olehsvyrydov/AI-development-team)
