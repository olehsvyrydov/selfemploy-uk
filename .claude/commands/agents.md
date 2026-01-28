---
description: List all available AI Development Team agents and their specializations
---

# AI Development Team - Agent Directory

## Team Workflow (TDD-Based)

See `~/.claude/TEAM_WORKFLOW.md` for complete workflow documentation.

```
/max → /luda → /aura → /finn or /james → /rev → /rob → /adam
Vision    AC     Design     TDD Dev        Review   QA     E2E
```

**Key Principles**:
- Developers (/finn or /fe, /james or /be) write their own tests (TDD)
- /rev reviews code quality, security, and requirements compliance
- /rob (/qa) tests features as black-box against acceptance criteria
- /adam (/e2e) writes E2E and performance tests

---

## Core Agents (17)

### Development Team

| Agent | Persona | Expertise | Commands |
|-------|---------|-----------|----------|
| **backend-developer** | James | Spring Boot 4, Java 21+, WebFlux, TDD | `/james` or `/be` |
| **frontend-developer** | Finn | React 19, TypeScript 5.7, Next.js, TDD | `/finn` or `/fe` |
| **devops-engineer** | - | Kubernetes, GKE, Helm, CI/CD, Docker | - |
| **solution-architect** | Jorge | System design, CQRS, Saga, Event Sourcing | `/jorge` or `/arch` |
| **mlops-engineer** | - | Spring AI, LLM integration, Gemini, OpenAI | - |

### Quality Assurance Team

| Agent | Persona | Expertise | Commands |
|-------|---------|-----------|----------|
| **reviewer** | Rev | Code quality, security, requirements validation | `/rev` or `/reviewer` |
| **tester** | Rob | Black-box testing, acceptance criteria validation | `/rob` or `/qa` or `/tester` |
| **e2e-tester** | Adam | Playwright, Detox, k6, Lighthouse, performance | `/adam` or `/e2e` |

> **Note**: /rev combines backend+frontend reviewing + AC validation. /rob does black-box QA (not unit tests).

### Design

| Agent | Persona | Expertise | Commands |
|-------|---------|-----------|----------|
| **ui-designer** | Aura | React, Tailwind, Framer Motion, Design Systems | `/aura` or `/ui` |

### Product & Process

| Agent | Persona | Expertise | Commands |
|-------|---------|-----------|----------|
| **product-owner** | Max | User stories, backlog, prioritization | `/max` or `/po` |
| **scrum-master** | Luda | Sprint planning, acceptance criteria, ceremonies | `/luda` or `/sm` |
| **business-analyst** | Anna | SWOT, market research, requirements | `/anna` or `/ba` |

### Security & Documentation

| Agent | Persona | Expertise | Commands |
|-------|---------|-----------|----------|
| **secops-engineer** | - | Spring Security, JWT, OAuth2, OWASP | - |
| **technical-writer** | - | C4 diagrams, ADRs, API docs, Mermaid | - |

### Legal & Finance

| Agent | Persona | Expertise | Commands |
|-------|---------|-----------|----------|
| **uk-legal-counsel** | Alex | UK Law, GDPR, Contracts, Compliance | `/alex` or `/legal` |
| **uk-accountant** | Inga | Tax, VAT, R&D Credits, IR35, Financial Forecasting | `/inga` or `/fin` or `/accountant` |

### Marketing & Growth

| Agent | Persona | Expertise | Commands |
|-------|---------|-----------|----------|
| **apex** | Apex | GTM Strategy, Product Positioning, Funnels | `/apex` or `/mkt` |

---

## Extended Skills (10)

Specialized skills that extend core agents:

### Frontend Extensions (extend frontend-developer)

| Skill | Specialization |
|-------|----------------|
| **angular-developer** | Angular 21, Signals, NgRx SignalStore, zoneless |
| **vue-developer** | Vue 3, Composition API, Pinia, Nuxt 3 |
| **flutter-developer** | Flutter 3.27, Dart 3.6, Riverpod, cross-platform |

### Backend Extensions (extend backend-developer)

| Skill | Specialization |
|-------|----------------|
| **kotlin-developer** | Kotlin 2.1, Coroutines, Ktor, KMP |
| **spring-kafka-integration** | Kafka producers/consumers, Reactor Kafka, DLT |
| **quarkus-developer** | Quarkus 3.17, native builds, Panache, GraalVM |
| **fastapi-developer** | FastAPI, Python async, Pydantic, SQLAlchemy |

### DevOps Extensions (extend devops-engineer)

| Skill | Specialization |
|-------|----------------|
| **terraform-specialist** | Terraform 1.10, GCP provider, modules, state |

### Testing Extensions (extend e2e-tester)

| Skill | Specialization |
|-------|----------------|
| **cucumber-bdd** | Cucumber 7, Gherkin, BDD, living documentation |

### Architecture Extensions (extend solution-architect)

| Skill | Specialization |
|-------|----------------|
| **graphql-developer** | Apollo Server/Federation, DataLoader, subscriptions |

---

## Quick Reference - All Commands

| Name | Short | Agent | Role |
|------|-------|-------|------|
| `/max` | `/po` | Product Owner | Vision, backlog |
| `/luda` | `/sm` | Scrum Master | Sprint planning, AC |
| `/anna` | `/ba` | Business Analyst | Research, requirements |
| `/aura` | `/ui` | UI Designer | Design specs |
| `/jorge` | `/arch` | Solution Architect | Architecture, patterns |
| `/james` | `/be` | Backend Developer | Java/Kotlin + TDD |
| `/finn` | `/fe` | Frontend Developer | React/TS + TDD |
| `/rev` | `/reviewer` | Code Reviewer | Quality, security, AC validation |
| `/rob` | `/qa` | QA Tester | Black-box testing |
| `/adam` | `/e2e` | E2E Tester | E2E, performance |
| `/alex` | `/legal` | UK Legal | Legal, compliance |
| `/inga` | `/fin` | UK Accountant | Tax, finance |
| `/apex` | `/mkt` | Marketing | GTM, positioning |

---

## Skill Locations

- **User-level** (global): `~/.claude/skills/`
- **Project-level**: `.claude/skills/`
- **Workflow**: `~/.claude/TEAM_WORKFLOW.md`

Project skills override user skills with the same name.
