# Contributing to selfemploy-uk

Thank you for your interest in contributing to the UK Self-Employment Manager! This document provides guidelines and instructions for contributing.

## Code of Conduct

By participating in this project, you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md).

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check existing issues to avoid duplicates.

When creating a bug report, include:
- Clear, descriptive title
- Steps to reproduce the issue
- Expected vs actual behavior
- Screenshots if applicable
- Your environment (OS, Java version)

### Suggesting Features

Feature requests are welcome! Please:
- Check the [ROADMAP.md](ROADMAP.md) first
- Search existing issues for similar requests
- Describe the problem your feature would solve
- Explain your proposed solution

### Contributing Code

1. **Fork the repository**
2. **Create a feature branch** from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Follow TDD** - Write tests first, then implementation
4. **Commit with clear messages**:
   ```
   Add income tracking validation

   - Add validation for positive amounts
   - Add date range validation
   - Add unit tests for validators
   ```
5. **Push and create a Pull Request**

## Development Setup

### Prerequisites

- Java 21 (LTS)
- Maven 3.9+
- Git

### Build & Run

```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/selfemploy-uk.git
cd selfemploy-uk

# Build
mvn clean install

# Run
mvn -pl app javafx:run

# Run tests
mvn test
```

## Coding Standards

### Java Style

- Follow standard Java naming conventions
- Use meaningful variable and method names
- Keep methods short (< 20 lines preferred)
- Classes should have single responsibility

### Testing

- **TDD is mandatory** - Write failing tests first
- Unit test coverage target: > 80%
- Use JUnit 5, Mockito, and AssertJ
- Name tests descriptively: `shouldCalculateTaxWhenIncomeExceedsPersonalAllowance`

### Commits

- Use present tense: "Add feature" not "Added feature"
- Keep first line under 50 characters
- Reference issue numbers: "Fix #123: Correct NI calculation"

## Project Structure

```
selfemploy-uk/
├── common/       # Domain entities, DTOs, enums
├── persistence/  # Database layer (H2, Flyway, Panache)
├── hmrc-api/     # HMRC MTD API integration
├── core/         # Business logic, tax calculator
├── ui/           # JavaFX controllers, FXML, CSS
└── app/          # Application launcher
```

## Tax Calculation Contributions

If contributing to tax calculations:

1. **Reference HMRC documentation** - Cite sources
2. **Include tax year** - Rules change annually
3. **Add comprehensive tests** - Edge cases matter
4. **Get review from /inga** - Domain expert approval required

## Pull Request Process

1. Update documentation if needed
2. Add tests for new functionality
3. Ensure all tests pass: `mvn verify`
4. Request review from maintainers
5. Address review feedback

### PR Title Format

```
[MODULE] Brief description

Examples:
[core] Add Income Tax calculator for 2024/25
[ui] Fix expense form validation
[hmrc-api] Implement OAuth2 token refresh
```

## Recognition

Contributors will be recognized in:
- GitHub contributors list
- Release notes for significant contributions

---

## Contributing Plugins

We welcome plugin contributions that extend the application's functionality. Plugins allow you to add features without modifying the core codebase.

### Getting Started with Plugin Development

See the comprehensive [Plugin Developer Guide](docs/plugin-development/getting-started.md) to build your first plugin in 15 minutes.

### Plugin Contribution Process

1. **Review the Documentation**
   - Read [docs/plugin-development/README.md](docs/plugin-development/README.md) for an overview
   - Understand the 9 available [extension points](docs/plugin-development/extension-points.md)
   - Review the [security requirements](docs/plugin-development/security.md)

2. **Create Your Plugin**
   - Add the Plugin SDK dependency (see getting started guide)
   - Implement the `Plugin` interface
   - Choose appropriate extension points
   - Declare required permissions in your descriptor

3. **Follow Security Guidelines**
   - Request only necessary permissions (principle of least privilege)
   - Sign your JAR if requesting HIGH sensitivity permissions
   - Encrypt any sensitive data stored locally
   - Use HTTPS for all network connections

4. **Testing Requirements**
   - Unit test coverage > 80%
   - Test with minimal permissions to ensure graceful degradation
   - Include integration tests using the provided test utilities
   - Tag JavaFX-dependent tests with `@Tag("e2e")`

5. **Submit for Review**
   - Create a separate repository for your plugin
   - Include comprehensive README with:
     - Feature description
     - Required permissions and justification
     - Installation instructions
     - Screenshots/demos
   - Submit to the Plugin Marketplace (coming Q2 2026)

### Plugin Extension Points

| Extension Point | Use Case |
|-----------------|----------|
| NavigationExtension | Add custom pages to the sidebar |
| DashboardWidget | Create dashboard cards and KPIs |
| ReportGenerator | Generate PDF/Excel reports |
| DataImporter | Import bank statements, CSV files |
| DataExporter | Export to Sage, QuickBooks formats |
| TaxCalculatorExtension | Scottish rates, custom reliefs |
| ExpenseCategoryExtension | Industry-specific expense categories |
| IntegrationExtension | Connect to Stripe, PayPal, banks |
| HmrcApiExtension | VAT returns, partnership submissions |

### Sample Plugin Structure

```
my-plugin/
├── pom.xml
├── README.md
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/myplugin/
│   │   │       ├── MyPlugin.java
│   │   │       └── extensions/
│   │   └── resources/
│   │       └── META-INF/services/
│   │           └── uk.selfemploy.plugin.api.Plugin
│   └── test/
│       └── java/
└── LICENSE
```

### Getting Help

- Review [example plugins](docs/plugin-development/examples/)
- Check the [API reference](docs/plugin-development/api-reference.md)
- Open a Discussion with the "plugin-development" tag

---

## Questions?

- Open a [Discussion](https://github.com/olehsvyrydov/selfemploy-uk/discussions)
- Check existing issues and discussions first

---

Thank you for contributing to making tax management easier for UK self-employed!
