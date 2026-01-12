# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.

## Project Overview

**UK Self-Employment Manager** - A free, open-source desktop application for UK self-employed individuals to manage their accounting and submit annual reports to HMRC via Making Tax Digital (MTD) APIs.

### Key Features
- Income/expense tracking with SA103 form categories
- Real-time tax calculation (Income Tax + NI Class 4)
- One-click HMRC submission via MTD APIs
- Privacy-first: local encrypted storage (H2 + AES-256)
- Cross-platform: Windows, macOS, Linux

### Target Users
1. **Self-Employed** (MVP): Freelancers, contractors, sole traders
2. **Partnerships** (Phase 2): Business partners sharing profits
3. **Ltd Company Directors** (Phase 3): Salary/dividend optimization

## Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Backend | Quarkus | 3.17.x |
| UI | JavaFX | 21.x |
| Database | H2 (encrypted) | 2.3.x |
| Migrations | Flyway | 10.x |
| Build | Maven | 3.9+ |
| Java | OpenJDK | 21 LTS |
| Native | GraalVM | 21+ |
| Packaging | jpackage | JDK 21 |

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
    ├── sprints/     # Sprint definitions and working folders
    ├── architecture/# ADRs, C4 diagrams
    └── requirements/# Product requirements
```

## Development Commands

```bash
# Build all modules
mvn clean install

# Run the application
mvn -pl app javafx:run

# Run tests
mvn test

# Run integration tests
mvn verify

# Run single test class
mvn test -Dtest=TaxCalculatorTest

# Run single test method
mvn test -Dtest=TaxCalculatorTest#shouldCalculateIncomeTax

# Create native installer
mvn -pl app -Ppackage jpackage:jpackage

# Check for dependency updates
mvn versions:display-dependency-updates
```

## Development Methodology

This project follows **strict TDD (Test-Driven Development)** and the **AI Development Team Workflow**.

### TDD Cycle (Mandatory)
1. Write failing tests first (RED)
2. Write minimal code to pass (GREEN)
3. Refactor while keeping tests green
4. Commit after successful test run

### Team Workflow Sequence
```
/max → /luda → /jorge → [/inga] → [/alex] → [/aura] → /james|/finn → /rev + [/aura verify] → /rob + /adam
Vision   AC    Arch.    Finance   Legal    Design   TDD Dev          Review              Testing

[ ] = Conditional based on feature type
```

**All features require `/jorge` architecture approval before implementation.**

---

## Context Preservation System (CRITICAL)

**Purpose**: All approvals, decisions, and reports MUST be saved to files to preserve context across conversations. This is mandatory for team continuity.

### Sprint Folder Structure

Every sprint gets a dedicated working folder:

```
docs/sprints/
├── sprint-{N}-{name}.md              # Sprint definition (tickets, AC)
│
└── sprint-{N}/                        # Sprint working folder
    ├── README.md                      # Sprint overview + live status
    ├── DECISION_LOG.md                # All key decisions with rationale (REQUIRED)
    │
    ├── approvals/                     # Gate approvals (REQUIRED)
    │   ├── jorge-architecture.md      # /jorge decisions
    │   ├── inga-finance.md            # /inga (if needed)
    │   ├── alex-legal.md              # /alex (if needed)
    │   └── aura-ui-designs/           # /aura designs
    │       └── {ticket-id}-{name}.md
    │
    ├── implementation/                # Dev notes per ticket
    │   ├── {ticket-id}-{name}.md
    │   └── TECH-XXX-{description}.md  # Technical debt tickets from /rev
    │
    ├── reviews/                       # Code review reports
    │   └── rev-{ticket-id}.md
    │
    └── testing/                       # QA & E2E reports
        ├── rob-qa-{ticket-id}.md
        └── adam-e2e-{ticket-id}.md
```

### Agent File Conventions

| Agent | Writes To | When | Triggers /luda |
|-------|-----------|------|----------------|
| /max | README.md (goals section) | Sprint planning | Yes |
| /luda | README.md, SPRINT-STATUS.md | After each approval, status change | N/A |
| /jorge | `approvals/jorge-architecture.md` | Architecture decisions | **YES** |
| /inga | `approvals/inga-finance.md` | Finance/tax approvals | **YES** |
| /alex | `approvals/alex-legal.md` | Legal/compliance approvals | **YES** |
| /aura | `approvals/aura-ui-designs/{ticket}.md` | UI specifications | **YES** |
| /james | `implementation/{ticket}.md` | Backend implementation notes | Yes (on complete) |
| /finn | `implementation/{ticket}.md` | Frontend implementation notes | Yes (on complete) |
| /rev | `reviews/rev-{ticket}.md` | Code review reports | Yes |
| /rob | `testing/rob-qa-{ticket}.md` | QA test reports | Yes |
| /adam | `testing/adam-e2e-{ticket}.md` | E2E test reports | Yes |

### Auto-Save Rules (MANDATORY)

**Rule 1: Every Approval Must Be Saved**
```
After ANY approval gate completes:
1. Agent saves decision to their designated file
2. Agent explicitly triggers: "/luda - please update sprint status"
3. /luda updates README.md with approval status
```

**Rule 2: Implementation Notes Required**
```
When starting implementation:
1. Developer creates implementation/{ticket}.md
2. Notes include: approach, key decisions, blockers
3. On completion: update file with results + trigger /luda
```

**Rule 3: Reports Are Persistent**
```
All reports (review, QA, E2E) MUST:
1. Be saved to the designated file
2. Include date, status, and findings
3. Trigger /luda to update sprint status
```

---

## Agent Quick Reference

| Command | Name | Role | Extended Skills |
|---------|------|------|-----------------|
| `/max` | Max | Product Owner - vision, backlog | - |
| `/luda` | Luda | Scrum Master - AC, sprint status | - |
| `/jorge` | Jorge | Solution Architect - architecture | **MANDATORY** |
| `/inga` | Inga | UK Accountant - finance approval | `uk-self-employment` |
| `/alex` | Alex | UK Legal - legal approval | - |
| `/aura` | Aura | UI Designer - design + verify | `javafx-designer` |
| `/james` | James | Backend Dev - Java/Quarkus + TDD | `javafx-developer`, `hmrc-api-specialist` |
| `/finn` | Finn | Frontend Dev - JavaFX + TDD | `javafx-developer` |
| `/rev` | Rev | Code Reviewer - quality, security | - |
| `/rob` | Rob | QA - test case design, reproduction tests | - |
| `/adam` | Adam | Test Automation - E2E, integration | - |

## Approval Gates

| Gate | Agent | When Required |
|------|-------|---------------|
| Architecture | /jorge | **ALWAYS** - all features |
| Finance | /inga | Tax calculations, HMRC submission, SA103 mapping |
| Legal | /alex | GDPR, data handling, disclaimers |
| UI Design | /aura | JavaFX UI components and layouts |

## Workflow Rules

1. **Architecture Approval Required**: All features MUST be approved by /jorge before implementation
2. **No Feature Without Acceptance Criteria**: Features cannot proceed without documented AC from /luda
3. **Developers Own Their Tests**: Unit and integration tests are written BY developers (TDD), not QA
4. **Security is Non-Negotiable**: /rev must run security scans on every code review
5. **Design QA for Frontend**: Frontend features require /aura to verify UI implementation
6. **Domain Expert Approval**: Finance → /inga, Legal → /alex
7. **Reports Close the Loop**: Every phase produces a report/status update that triggers the next phase

## Key Domain Concepts

### SA103 Form Categories
Expenses align with HMRC Self Assessment SA103F form boxes:
- Box 10: Cost of goods
- Box 12: Staff costs
- Box 13: Travel
- Box 14: Premises
- Box 16: Office costs
- Box 21: Professional fees
- Box 23: Other expenses

### Tax Calculation (2024/25)

**Income Tax Bands:**
- Personal Allowance: £12,570 (0%)
- Basic Rate: £12,571 - £50,270 (20%)
- Higher Rate: £50,271 - £125,140 (40%)
- Additional Rate: Over £125,140 (45%)

**National Insurance Class 4:**
- Lower Profits Limit: £12,570
- Upper Profits Limit: £50,270
- Main Rate: 6% (£12,570 - £50,270)
- Additional Rate: 2% (above £50,270)

### HMRC MTD APIs
- Base URL (Sandbox): `https://test-api.service.hmrc.gov.uk`
- Base URL (Production): `https://api.service.hmrc.gov.uk`
- OAuth2 via Government Gateway
- Fraud prevention headers required

## Testing Strategy

### Unit Tests (Developers)
- Location: `src/test/java/`
- Framework: JUnit 5 + Mockito + AssertJ
- Coverage target: >80%

### UI Tests
- Framework: TestFX
- Location: `ui/src/test/java/`

### Integration Tests
- Framework: Quarkus Test + Testcontainers
- Suffix: `*IT.java`

## Sprint Workflow

Before implementing any feature:
1. Create sprint definition in `docs/sprints/sprint-{N}-{name}.md`
2. Create working folder `docs/sprints/sprint-{N}/`
3. Create required subfolders: `approvals/`, `implementation/`, `reviews/`, `testing/`
4. Get required approvals and save to `approvals/` subfolder
5. Track status in sprint `README.md`
6. Create `DECISION_LOG.md` for key decisions

### Sprint README.md Template

```markdown
# Sprint {N}: {Feature Name}

**Started**: YYYY-MM-DD
**Status**: 🟡 In Progress | 🟢 Complete | 🔴 Blocked

## Approval Status

| Gate | Agent | Status | File | Date |
|------|-------|--------|------|------|
| Architecture | /jorge | ✅ Approved | [Link](approvals/jorge-architecture.md) | YYYY-MM-DD |
| Finance | /inga | ⏳ Pending | - | - |

## Tickets

| Ticket | Description | Dev | Status | Review | QA | E2E |
|--------|-------------|-----|--------|--------|-----|-----|
| SE-XXX | Feature X | /james | ✅ Done | ✅ | ⏳ | ⏳ |

## Activity Log

| Date | Agent | Action |
|------|-------|--------|
| YYYY-MM-DD | /agent | Description of action |
```

---

## Continuous Improvement

### Sprint Retrospective Process (v6.0 - Full Team)

After each sprint completion, run a **multi-agent retrospective** for comprehensive review:

#### Retrospective Participants

**Core Team (Always Required):**

| Agent | Role | Focus Area |
|-------|------|------------|
| /luda | Facilitator | Consolidation, process updates |
| /max | Product Owner | Product vision, priorities, user value |
| /jorge | Architecture | Technical decisions, patterns |
| /anna | Business Analysis | Requirements, user value |
| /james | Backend Development | Implementation, TDD, backend challenges |
| /finn | Frontend Development | UI/UX, JavaFX, frontend challenges |
| /rev | Code Review | Quality, security, review process |
| /rob | QA Engineering | Test design, coverage, QA process |
| /adam | E2E Automation | CI/CD, automation, test infrastructure |

**Domain Experts (Conditional):**

| Agent | When Required |
|-------|---------------|
| /inga | Finance features (tax, accounting, MTD) |
| /alex | Legal features (GDPR, compliance, terms) |
| /apex | Launch/marketing features |

#### Three Questions Framework

Each agent answers:
1. **What went well?** - Successes, achievements, practices to continue
2. **What could be improved?** - Pain points, inefficiencies, gaps
3. **What should change?** - Process changes, workflow updates

#### Retrospective Workflow

```
Sprint Complete → /luda triggers retro → Run parallel agent reviews
     │
     │ CORE TEAM (Always):
     ├── /max: Product owner review
     ├── /jorge: Architecture review
     ├── /anna: Business analysis review
     ├── /james: Backend development review
     ├── /finn: Frontend development review
     ├── /rev: Code review process review
     ├── /rob: QA process review
     ├── /adam: E2E/CI automation review
     │
     │ DOMAIN EXPERTS (Conditional):
     ├── /inga: Finance/MTD review (if finance features)
     ├── /alex: Legal/GDPR review (if legal features)
     └── /apex: Marketing/GTM review (if launch-related)
           │
           ▼
     /luda consolidates → Tech debt tickets + Process updates + Next sprint planning
```

#### Output Artifacts

| Artifact | Location | Created By |
|----------|----------|------------|
| Agent reports | `docs/sprints/sprint-{N}/retrospectives/{agent}-retro.md` | Each agent |
| Consolidated report | `docs/sprints/sprint-{N}/consolidated-retrospective.md` | /luda |
| Tech debt tickets | Sprint backlog | /luda |
| Process updates | This CLAUDE.md or TEAM_WORKFLOW.md | /luda |

**Template:** `~/Documents/my/ai-dev-team/claude/templates/retrospective-template.md`

#### Legacy Format (Simple)

For minor sprints, use the **Start/Stop/Continue** format:

| Category | Purpose |
|----------|---------|
| **✅ START** | New practices to adopt |
| **🛑 STOP** | Practices causing friction |
| **🔄 CONTINUE** | Practices working well |

Save retrospective to `docs/sprints/sprint-{N}/retrospective.md`.

### Velocity Benchmarks

Based on project history:

| Metric | Value | Notes |
|--------|-------|-------|
| **Average Velocity** | ~29 SP | Per sprint (2-week) |
| **Recommended Capacity** | 30-35 SP | Per sprint |
| **Test Growth Rate** | ~40-60 tests | Per sprint |

### Key Process Learnings (Sprint 3)

**What Works Well:**
- **TDD Methodology**: Catches bugs early, high confidence in code quality
- **Full Pipeline Flow**: `/impl → /rev → /rob → /adam` ensures quality at each stage
- **Architecture Approval Gate**: Prevents rework, ensures consistency
- **Domain Expert Reviews**: `/inga` and `/alex` validate compliance early
- **Nested Test Classes**: Well-organized tests by feature/scenario

**Improvements Identified:**
- **CI/CD Pipeline**: Automate builds and tests on every push (SE-401)
- **Smaller Commits**: Avoid large monolithic commits; prefer incremental changes
- **Headless E2E Testing**: Investigate TestFX monocle for CI-compatible E2E tests
- **Security Scanning in CI**: Add Trivy/CodeQL to automated pipeline

### Key Process Learnings (Sprint 4) - NEW

**What Worked Well:**
- **Saga Pattern**: Exemplary implementation with resume capability and state persistence
- **Resilience4j Integration**: Production-ready circuit breaker and retry with metrics
- **CI/CD Pipeline**: Comprehensive GitHub Actions with CodeQL security scanning
- **Module Separation**: Clean 6-module architecture maintained throughout
- **Exception Hierarchy**: Correctly distinguishes retryable vs non-retryable errors

**Process Improvements Implemented (v4.1):**

1. **Sprint Start Checklist** - Run full `mvn compile` at sprint start to catch cross-module issues
2. **Architecture Conditions** - Add explicit checkboxes to /jorge approvals for /rev verification
3. **Temporary Types Policy** - Create consolidation ticket immediately when creating temp types
4. **Auth Infrastructure First** - Implement authentication before building API clients
5. **WireMock Stub Repository** - Save reusable API stubs to `src/test/resources/wiremock/`
6. **Feature Demo Recording** - Record 30-second GIFs for major features at sprint end

**Definition of Done Updates:**
- [ ] All modules compile successfully
- [ ] No temporary types without consolidation ticket (TD-XXX)
- [ ] Architecture conditions verified with explicit checkboxes in code review
- [ ] Auth integration verified if API calls present

**Technical Debt Identified (Sprint 5):**
- TD-001: OAuth Token Integration (3 SP) - Blocking real API calls
- TD-002: Consolidate UI Types with Common Module (2 SP)
- TD-003: Fix Quarkus CDI Configuration (3 SP)
- TD-004: Add Declaration Timestamp Column (1 SP)

See: `docs/sprints/sprint-4/RETROSPECTIVE.md` for full retrospective details.

---

## Bug / Issue Workflow

Use the `/bug` or `/issue` command with a description:

```
/bug I see internal server error in /approval page when I move from dashboard
/bug Tax calculation is wrong for income over £100,000
```

### Bug Workflow

```
/bug [description]
       │
       ▼
┌──────────────────────────┐
│ Creates BUG-XXX report   │
└────────────┬─────────────┘
             │
             ▼
┌──────────────────────────┐
│ /luda creates ticket     │
│ • Sets priority (P0-P3)  │
│ • Assigns investigator   │
│ • Schedules in sprint    │
└────────────┬─────────────┘
             │
             ▼
┌──────────────────────────┐
│ INVESTIGATION PHASE      │
│ • Identify component     │
│ • Reproduce issue        │
│ • Find root cause        │
└────────────┬─────────────┘
             │
     ┌───────┴───────┐
     │               │
     ▼               ▼
┌────────────┐  ┌───────────────────┐
│ REPRODUCED │  │ CANNOT REPRODUCE  │
└─────┬──────┘  │ → Close OR        │
      │         │ → Request info OR │
      │         │ → Mark for monitor│
      │         └───────────────────┘
      ▼
┌──────────────────────────┐
│ /rob writes failing      │
│ reproduction test        │
└────────────┬─────────────┘
             │
             ▼
┌──────────────────────────┐
│ FIX PHASE (TDD)          │
│ • Write unit tests (RED) │
│ • Implement fix (GREEN)  │
│ • Refactor, tests pass   │
└────────────┬─────────────┘
             │
             ▼
┌──────────────────────────┐
│ /rev reviews fix         │
│ /adam runs automated     │
│ tests (verifies fix)     │
│ /luda closes ticket      │
└──────────────────────────┘
```

### Bug Priority Levels

| Priority | Criteria | Response |
|----------|----------|----------|
| **P0** | System down, data loss, security breach | Immediate |
| **P1** | Major feature broken, no workaround | Same day |
| **P2** | Feature impaired, workaround exists | Current sprint |
| **P3** | Minor issue, cosmetic | Backlog |

### Bug Investigation Report Template

```markdown
# Bug Investigation Report: BUG-XXX

**Reported**: YYYY-MM-DD
**Investigated By**: [agent]
**Priority**: P0/P1/P2/P3
**Component**: Core / Persistence / UI / HMRC-API

## Summary
[Brief description of the bug]

## Root Cause Analysis
[Technical explanation]

## Affected Files
- `path/to/file.java` - [description]

## Reproduction Steps
1. Step 1
2. Step 2
3. Expected: [what should happen]
4. Actual: [what happens]

## Proposed Fix
[Description of how to fix]
```

## Security Considerations

- All financial data encrypted at rest (AES-256)
- HMRC OAuth tokens stored securely in OS keychain
- No data sent to external servers (local-only)
- GDPR compliant data handling

## Key Deadlines (HMRC)

| Filing | Deadline |
|--------|----------|
| Self Assessment Online | 31 January |
| Payment on Account 1 | 31 January |
| Payment on Account 2 | 31 July |

## MTD Timeline

| Date | Requirement |
|------|-------------|
| April 2026 | Income > £50,000 must use MTD |
| April 2027 | Income > £30,000 must use MTD |
| April 2028 | Income > £20,000 must use MTD |
