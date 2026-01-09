# Contributing

Thank you for your interest in contributing to the UK Self-Employment Manager!

---

## Code of Conduct

By participating in this project, you agree to abide by our [Code of Conduct](https://github.com/olehsvyrydov/selfemploy-uk/blob/main/CODE_OF_CONDUCT.md).

---

## How to Contribute

### Reporting Bugs

1. Check [existing issues](https://github.com/olehsvyrydov/selfemploy-uk/issues) first
2. Use the [bug report template](https://github.com/olehsvyrydov/selfemploy-uk/issues/new?template=bug_report.md)
3. Include:
   - Steps to reproduce
   - Expected vs actual behavior
   - Environment details (OS, Java version)
   - Screenshots if applicable

### Suggesting Features

1. Check the [ROADMAP](https://github.com/olehsvyrydov/selfemploy-uk/blob/main/ROADMAP.md)
2. Search existing issues and discussions
3. Use the [feature request template](https://github.com/olehsvyrydov/selfemploy-uk/issues/new?template=feature_request.md)

### Contributing Code

1. **Fork** the repository
2. **Create a branch**: `git checkout -b feature/your-feature`
3. **Follow TDD** - Write tests first
4. **Commit** with clear messages
5. **Push** and create a Pull Request

---

## Development Setup

### Prerequisites

- Java 21+
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

---

## Coding Standards

### Java Style

- Standard Java naming conventions
- Meaningful variable/method names
- Methods < 20 lines preferred
- Classes < 200 lines preferred
- Single responsibility principle

### Testing

**TDD is mandatory:**

1. Write failing test
2. Write minimal code to pass
3. Refactor
4. Commit

**Coverage targets:**
- Unit tests: > 80%
- Integration tests: > 60%

**Frameworks:**
- JUnit 5
- Mockito
- AssertJ
- TestFX (for UI)

### Commit Messages

```
[module] Brief description

- Detail 1
- Detail 2

Fixes #123
```

**Format:**
- Present tense: "Add feature" not "Added feature"
- First line < 50 characters
- Reference issue numbers

---

## Project Structure

```
selfemploy-uk/
├── common/          # Domain entities, DTOs, enums
├── persistence/     # Database layer
├── hmrc-api/        # HMRC API integration
├── core/            # Business logic
├── ui/              # JavaFX UI
└── app/             # Launcher
```

---

## Tax Calculation Contributions

Special requirements for tax-related code:

1. **Reference HMRC documentation** - Cite sources in comments
2. **Include tax year** - Rates change annually
3. **Comprehensive tests** - Edge cases matter
4. **Domain expert review** - Tag @inga for review

---

## Pull Request Process

1. **Update documentation** if needed
2. **Add tests** for new functionality
3. **Run all tests**: `mvn verify`
4. **Request review** from maintainers
5. **Address feedback**

### PR Title Format

```
[module] Brief description

Examples:
[core] Add Income Tax calculator for 2025/26
[ui] Fix expense form validation
[hmrc-api] Implement OAuth2 token refresh
```

---

## Recognition

Contributors are recognized in:
- GitHub contributors list
- Release notes

---

## Questions?

- Open a [Discussion](https://github.com/olehsvyrydov/selfemploy-uk/discussions)
- Check existing issues and discussions first

---

Thank you for helping make tax management easier for UK self-employed!
