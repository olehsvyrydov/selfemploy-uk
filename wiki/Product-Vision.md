# Product Vision

**Author**: Max (Product Owner)
**Status**: ✅ Approved
**Version**: 1.1

---

## Vision Statement

> **For** UK self-employed individuals and small business owners **who** struggle with annual tax compliance and HMRC submissions, **the** UK Self-Employment Manager **is a** free, open-source desktop application **that** simplifies year-round financial record-keeping and enables one-click annual tax return submission to HMRC. **Unlike** expensive subscription-based accounting software or complex spreadsheets, **our product** offers privacy-first local storage with optional cloud backup, cross-platform support, and full Making Tax Digital compliance without ongoing costs.

### Elevator Pitch

*"Stop dreading tax season. Track your income and expenses throughout the year, see your tax liability in real-time, and submit your Self Assessment to HMRC with a single click - all for free, all on your own computer, with optional cloud backup for peace of mind."*

---

## Target User Personas

### Persona 1: Sarah the Sole Trader (MVP Primary)

| Attribute | Details |
|-----------|---------|
| **Role** | Freelance Graphic Designer |
| **Age** | 28-45 |
| **Income** | £25,000 - £85,000/year |
| **Tech Savviness** | Moderate |
| **Current Pain** | Uses spreadsheets, dreads January deadline |
| **Goals** | Spend less time on admin, avoid penalties |

**Jobs to be Done:**
- Record income when I invoice a client
- Log expenses with receipt photos
- Know how much tax I'll owe before January
- Submit my Self Assessment without hiring an accountant

### Persona 2: Mike the Partnership Partner (Phase 2)

| Attribute | Details |
|-----------|---------|
| **Role** | Co-owner of small consultancy |
| **Income** | £40,000 - £120,000/year (partnership share) |
| **Current Pain** | Partnership accounting is confusing |
| **Goals** | Track partnership income/expenses, understand profit share |

### Persona 3: Emma the Ltd Director (Phase 3)

| Attribute | Details |
|-----------|---------|
| **Role** | Director of single-person Ltd company |
| **Income** | £50,000 - £150,000/year (company revenue) |
| **Current Pain** | Corporation tax, dividends, salary split complexity |
| **Goals** | Optimize tax between salary and dividends |

---

## Feature Prioritization (MoSCoW)

### Must Have (P0) - MVP

| Feature | Description |
|---------|-------------|
| **Income Recording** | Record invoices, payments, client details |
| **Expense Tracking** | Log expenses with HMRC-aligned categories |
| **Tax Year Management** | Work within UK tax year (6 Apr - 5 Apr) |
| **Tax Calculation Preview** | Real-time estimated tax liability |
| **HMRC Authentication** | OAuth2 connection to HMRC Gateway |
| **Annual Return Submission** | One-click Self Assessment submission |
| **Local Data Storage** | Encrypted H2 database |
| **Backup/Restore** | Export and import data files |

### Should Have (P1)

| Feature | Description |
|---------|-------------|
| **Quarterly Summaries** | Generate MTD quarterly updates |
| **Deadline Notifications** | Alert before key HMRC dates |
| **Document Attachments** | Attach receipt images/PDFs |
| **Multi-Year Support** | View previous tax years |
| **PDF Export** | Export tax summary to PDF |

### Could Have (P2)

| Feature | Description |
|---------|-------------|
| **Google Drive Backup** | Optional cloud backup |
| **Excel/Word Export** | Export to multiple formats |
| **Bank Statement Import** | CSV import from banks |
| **Mileage Tracking** | Business travel calculator |
| **Invoice Generation** | Create basic invoices |

### Won't Have (v1.x)

- Real-time cloud sync / multi-device access
- Mobile application
- Full bookkeeping (double-entry)
- Payroll for employees
- Corporation Tax filing

---

## Success Metrics (OKRs)

### Objective 1: Deliver a Usable MVP

| Key Result | Target |
|------------|--------|
| MVP features complete | 100% P0 features |
| Cross-platform builds | 3 platforms |
| Test coverage | >80% unit |

### Objective 2: Achieve HMRC Compliance

| Key Result | Target |
|------------|--------|
| Sandbox API integration | 100% test scenarios pass |
| Production API access | Approved by HMRC |
| Tax calculation accuracy | 100% match HMRC examples |

### Objective 3: Build Open Source Community

| Key Result | Target |
|------------|--------|
| GitHub stars | 500+ in first 6 months |
| Contributors | 10+ external |
| Documentation coverage | All features documented |

---

## Product Roadmap

```
Phase 1 (MVP): Solo Self-Employed
├── Sprint 0: Foundation & Setup ✅
├── Sprint 1: Core Domain (Income, Expenses)
├── Sprint 2: Tax Calculation & Dashboard
├── Sprint 3: HMRC Integration
└── Sprint 4: Polish & Release v1.0

Phase 2: Enhanced Features
├── PDF/Excel/Word export
├── Google Drive backup
├── Quarterly MTD submissions

Phase 3: Partnerships
├── Partnership profit splits
├── Partnership return filing

Phase 4: Limited Companies
├── Corporation Tax basics
├── Dividend tracking
```
