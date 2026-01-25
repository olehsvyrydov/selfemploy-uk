# Roadmap

This document outlines the planned development phases for the UK Self-Employment Manager.

## Vision

A free, privacy-first desktop application that makes tax management effortless for UK self-employed individuals.

---

## Phase 1: MVP (Q1 2025)

**Target Users:** Self-employed individuals (sole traders, freelancers, contractors)

### Core Features

| Feature | Status | Description |
|---------|--------|-------------|
| Income Tracking | Planned | Record invoices and payments received |
| Expense Tracking | Planned | Categorize expenses (SA103 aligned) |
| Tax Calculator | Planned | Income Tax + NI Class 4 estimates |
| Dashboard | Planned | Summary view with key metrics |
| Local Storage | Planned | Encrypted H2 database (AES-256) |
| Backup/Restore | Planned | Export and import data |

### Technical Foundation

- [x] Project structure (Quarkus + JavaFX)
- [ ] Domain entities (Income, Expense, TaxYear)
- [ ] Tax calculation engine
- [ ] Basic UI with navigation
- [ ] Unit tests (>80% coverage)

---

## Phase 2: HMRC Integration (Q2 2025)

**Goal:** One-click submission to HMRC via Making Tax Digital APIs

### Features

| Feature | Status | Description |
|---------|--------|-------------|
| HMRC OAuth2 | Planned | Government Gateway authentication |
| MTD Submission | Planned | Submit annual Self Assessment |
| Fraud Headers | Planned | Required HMRC security headers |
| Submission History | Planned | Track past submissions |

### Technical

- [ ] HMRC Developer Hub registration
- [ ] Sandbox testing
- [ ] Production API access
- [ ] Token secure storage (OS keychain)

---

## Phase 3: Enhanced Features (Q3 2025)

### Features

| Feature | Status | Description |
|---------|--------|-------------|
| Quarterly Summaries | Planned | MTD quarterly reporting |
| Receipt Attachments | Planned | Attach photos/PDFs to expenses |
| Mileage Tracking | Planned | Vehicle expense calculator |
| Multiple Tax Years | Planned | Historical data support |
| Reports & Charts | Planned | Visual income/expense analysis |

---

## Phase 4: Cloud & Export (Q4 2025)

### Features

| Feature | Status | Description |
|---------|--------|-------------|
| Google Drive Backup | Planned | Optional cloud backup |
| PDF Export | Planned | Generate tax summaries |
| Excel Export | Planned | Spreadsheet export |
| Bank Import | Planned | CSV/OFX statement import |

---

## Phase 5: Plugin Ecosystem (2026)

**Goal:** Enable third-party extensibility through a secure plugin marketplace

### Phase 5.1: Plugin API Foundation (Q1 2026) - COMPLETE

| Feature | Status | Description |
|---------|--------|-------------|
| Plugin API | Done | Core plugin interfaces and lifecycle |
| Extension Points | Done | 9 extension types for different functionality |
| Permission System | Done | 10 permissions across 3 sensitivity levels |
| Plugin Runtime | Done | ClassLoader isolation and lifecycle management |
| Developer Docs | Done | Comprehensive documentation and examples |

### Phase 5.2: Plugin Marketplace (Q2 2026)

| Feature | Status | Description |
|---------|--------|-------------|
| Plugin Discovery | Planned | Browse and search available plugins |
| Plugin Installation | Planned | One-click install from marketplace |
| Version Management | Planned | Automatic updates and compatibility checks |
| Publisher Verification | Planned | Trusted publisher program |
| Community Reviews | Planned | Ratings and reviews system |

### Phase 5.3: Sample Plugins (Q2-Q3 2026)

| Feature | Status | Description |
|---------|--------|-------------|
| Bank Import Plugins | Planned | Import from major UK banks |
| Report Generators | Planned | PDF/Excel custom reports |
| Cloud Backup Plugins | Planned | Google Drive, Dropbox, OneDrive |
| Accounting Exports | Planned | Sage, QuickBooks, Xero formats |

---

## Phase 6: Business Types (2026-2027)

### Partnerships

| Feature | Status | Description |
|---------|--------|-------------|
| Partnership Entities | Planned | Multiple partner support |
| Profit Sharing | Planned | Split calculations |
| SA104 Form | Planned | Partnership Self Assessment |

### Limited Companies

| Feature | Status | Description |
|---------|--------|-------------|
| Corporation Tax | Planned | CT600 calculations |
| Salary/Dividend | Planned | Tax-efficient extraction |
| Director Loans | Planned | S455 tax tracking |

---

## Non-Goals (Out of Scope)

- **VAT Management** - May add later based on demand
- **Payroll** - Complex, better served by specialists
- **Multi-currency** - UK-focused only
- **Mobile App** - Desktop-first approach
- **Cloud-only Version** - Privacy-first means local-first

---

## How to Influence the Roadmap

1. **Vote on Issues** - Add reactions to feature requests
2. **Open Discussions** - Share your use cases
3. **Contribute** - PRs welcome for any planned feature

---

## MTD Compliance Timeline

This roadmap aligns with HMRC's Making Tax Digital requirements:

| Date | Requirement | Our Target |
|------|-------------|------------|
| April 2026 | Income > £50,000 must use MTD | Phase 2 ready |
| April 2027 | Income > £30,000 must use MTD | Phase 3 ready |
| April 2028 | Income > £20,000 must use MTD | Full feature set |

---

*Last updated: January 2026*
