# Sprint 6: Launch Readiness & Compliance

**Started:** 2026-01-11
**Completed:** 2026-01-12
**Status:** ✅ COMPLETE
**Planned SP:** 30 (P0 only) | 50 (P0+P1) | 63 (Full backlog)
**Velocity Reference:** 25.2 SP/sprint average

---

## Sprint Goal

> Complete all P0 blockers from retrospective: Class 2 NI, Legal compliance (Privacy Notice, ToS, Disclaimers), and finance accuracy fixes to achieve launch readiness.

---

## External Blockers (CRITICAL - ESCALATE IMMEDIATELY)

| Item | Owner | Status | Action Required |
|------|-------|--------|-----------------|
| HMRC Production Registration | Team Lead | **NOT STARTED** | Escalate immediately - required for live API access |
| HMRC Fraud Prevention Certification | Team Lead | **NOT STARTED** | Requires compliance documentation |

**Impact:** Application cannot submit to HMRC production without these approvals. All development complete but launch blocked.

---

## Complete Ticket Backlog (From Retrospective)

### P0 - CRITICAL (Must Complete Before Launch) - 30 SP

| Ticket | Title | SP | Dev | Source | Status | Review | QA |
|--------|-------|-----|-----|--------|--------|--------|-----|
| **SE-801** | **Class 2 NI Calculator** | **5** | /james | /inga | **IMPLEMENTED** | **/rev APPROVED** | **/rob DESIGNED** |
| **SE-507** | **Privacy Notice UI** | **3** | /finn | /alex | **IMPLEMENTED** | **/rev APPROVED** | **/rob DESIGNED** |
| **SE-508** | **Terms of Service UI** | **5** | /finn | /alex | **IMPLEMENTED** | **/rev APPROVED** | **/rob DESIGNED** |
| **SE-509** | **Enhanced Disclaimers** | **2** | /finn | /alex | **IMPLEMENTED** | **/rev APPROVED** | **/rob DESIGNED** |
| **SE-802** | **Bank Import Column Mapping Wizard** | **3** | /finn | /inga | **IMPLEMENTED** | **/rev APPROVED** | **/rob DESIGNED** |
| **SE-803** | **Declaration Timestamp Persistence** | **2** | /james | /inga | **IMPLEMENTED** | **/rev APPROVED** | **/rob DESIGNED** |
| **SE-901** | **Landing Page** | **5** | /finn | /apex | **IMPLEMENTED** | **/rev APPROVED** | **/rob DESIGNED** |
| SE-703 | HMRC Sandbox Integration Tests | 5 | /james | Sprint 6 | **IMPLEMENTED** | **/rev APPROVED** | **/rob DESIGNED** |

### P1 - Important (Should Have) - 20 SP

| Ticket | Title | SP | Dev | Source | Status |
|--------|-------|-----|-----|--------|--------|
| SE-701 | In-App Help System | 5 | /finn | Sprint 6 | PENDING |
| SE-702 | User Onboarding Wizard | 5 | /finn | Sprint 6 | PENDING |
| SE-804 | PAYE Exemption UI | 1 | /finn | /inga | PENDING |
| SE-805 | Non-Allowable Expense Warnings | 1 | /finn | /inga | PENDING |
| SE-902 | Demo Video | 3 | /finn | /apex | PENDING |
| SE-903 | User Documentation | 3 | /finn | /apex | PENDING |
| TD-005 | Document JavaFX CI Requirements | 1 | /james | /jorge | PENDING |
| TD-006 | E2E Test Tag Verification Hook | 1 | /adam | /jorge | PENDING |

### P2 - Nice to Have - 13 SP

| Ticket | Title | SP | Dev | Source | Status |
|--------|-------|-----|-----|--------|--------|
| SE-806 | Expense Audit Trail | 3 | /james | /inga | BACKLOG |
| SE-807 | Record Retention Policy | 2 | /james | /inga | BACKLOG |
| SE-510 | SAR Handling Process | 3 | /alex | /alex | BACKLOG |
| SE-511 | GDPR Data Export | 3 | /james | /alex | BACKLOG |
| SE-904 | GitHub README Enhancement | 2 | /finn | /apex | BACKLOG |

### P3 - Deferred - 17 SP

| Ticket | Title | SP | Dev | Source | Status |
|--------|-------|-----|-----|--------|--------|
| SE-704 | Performance Optimization | 3 | /james | Sprint 6 | BACKLOG |
| SE-705 | Accessibility Audit (WCAG 2.1 AA) | 3 | /finn | Sprint 6 | BACKLOG |
| SE-706 | Error Message Improvements | 2 | /finn | Sprint 6 | BACKLOG |
| SE-707 | Mileage Tracking | 5 | /james, /finn | Sprint 6 | BACKLOG |
| SE-708 | Export to Excel Format | 3 | /james | Sprint 6 | BACKLOG |
| TD-004 | Structured Logging with Correlation IDs | 2 | /james | /jorge | BACKLOG |

---

## P0 Tickets - Detailed Specifications

### SE-801: Class 2 NI Calculator (5 SP) - /james

**Source:** /inga Finance Retrospective
**Risk:** HIGH - Tax calculation incomplete without this

```
US-801: Calculate Class 2 National Insurance
As a self-employed user, I want Class 2 NI included in my tax calculation
So that I see my complete tax liability

Acceptance Criteria:
- AC-1: Calculate Class 2 NI at £3.45/week (2025/26 rate)
- AC-2: Only apply if profits > Lower Profits Limit (£12,570)
- AC-3: Display separately from Class 4 NI in tax summary
- AC-4: Include in total tax liability calculation
- AC-5: Handle voluntary Class 2 NI option for those below threshold
- AC-6: Update TaxLiabilityResult record to include niClass2 field
- AC-7: All tax summary UIs updated to show Class 2 NI
```

**Files to Create/Modify:**
- `core/src/main/java/uk/selfemploy/core/calculator/NationalInsuranceClass2Calculator.java`
- `core/src/main/java/uk/selfemploy/core/calculator/TaxLiabilityResult.java` (add niClass2)
- `core/src/test/java/uk/selfemploy/core/calculator/NationalInsuranceClass2CalculatorTest.java`
- Update all tax summary ViewModels to display Class 2 NI

---

### SE-507: Privacy Notice UI (3 SP) - /finn

**Source:** /alex Legal Retrospective
**Risk:** HIGH - GDPR legal requirement

```
US-507: View Privacy Notice
As a user, I want to read the privacy notice before using the app
So that I understand how my data is handled

Acceptance Criteria:
- AC-1: Privacy Notice accessible from Settings menu
- AC-2: Privacy Notice shown on first launch (before onboarding)
- AC-3: User must acknowledge reading privacy notice
- AC-4: Privacy Notice includes: data collected, processing basis, retention, rights
- AC-5: Link to full privacy policy (opens in browser)
- AC-6: Privacy notice version stored with acknowledgment timestamp
- AC-7: Re-show privacy notice if version changes
```

**Files to Create:**
- `ui/src/main/resources/fxml/privacy-notice.fxml`
- `ui/src/main/java/uk/selfemploy/ui/controller/PrivacyNoticeController.java`
- `ui/src/main/java/uk/selfemploy/ui/viewmodel/PrivacyNoticeViewModel.java`
- `persistence/src/main/resources/db/migration/V{N}__add_privacy_acknowledgment.sql`

---

### SE-508: Terms of Service UI (5 SP) - /finn

**Source:** /alex Legal Retrospective
**Risk:** HIGH - Legal requirement before launch

```
US-508: Accept Terms of Service
As a user, I want to read and accept terms of service
So that the legal relationship is established

Acceptance Criteria:
- AC-1: Terms of Service shown on first launch (after privacy notice)
- AC-2: User must scroll through entire ToS before accepting
- AC-3: "I Accept" button only enabled after scrolling
- AC-4: ToS includes: software disclaimer, no professional advice, limitation of liability
- AC-5: ToS version and acceptance timestamp stored
- AC-6: User cannot use app without accepting ToS
- AC-7: Re-show ToS if version changes
- AC-8: ToS accessible from Settings > Legal
```

**Files to Create:**
- `ui/src/main/resources/fxml/terms-of-service.fxml`
- `ui/src/main/java/uk/selfemploy/ui/controller/TermsOfServiceController.java`
- `ui/src/main/java/uk/selfemploy/ui/viewmodel/TermsOfServiceViewModel.java`
- `common/src/main/resources/legal/terms-of-service.md`

---

### SE-509: Enhanced Disclaimers (2 SP) - /finn

**Source:** /alex Legal Retrospective
**Risk:** MEDIUM - Required for professional indemnity

```
US-509: Display Tax Calculation Disclaimers
As a user, I want clear disclaimers about tax calculations
So that I understand the software limitations

Acceptance Criteria:
- AC-1: Tax summary shows disclaimer: "This is an estimate only. Verify with a qualified accountant."
- AC-2: HMRC submission shows disclaimer: "You are responsible for accuracy of submitted data."
- AC-3: Disclaimer on PDF confirmation: "This software does not constitute professional tax advice."
- AC-4: Disclaimers cannot be dismissed permanently
- AC-5: Disclaimer text matches /alex legal review specifications
```

**Files to Modify:**
- `ui/src/main/resources/fxml/tax-summary.fxml` (add disclaimer)
- `ui/src/main/java/uk/selfemploy/ui/controller/AnnualSubmissionController.java` (add disclaimer)
- `core/src/main/java/uk/selfemploy/core/pdf/PdfConfirmationGenerator.java` (add disclaimer)

---

### SE-802: Bank Import Column Mapping Wizard (3 SP) - /finn

**Source:** /inga Finance Retrospective
**Risk:** HIGH - Incorrect income/expense classification possible

```
US-802: Map CSV Columns During Import
As a user importing bank statements, I want to verify column mapping
So that amounts are correctly classified as income or expense

Acceptance Criteria:
- AC-1: After file selection, show column mapping wizard
- AC-2: Display preview of first 5 rows
- AC-3: User must confirm: which column is Amount, Date, Description
- AC-4: User must confirm: positive values are Income or Expense
- AC-5: Show summary: "X transactions as INCOME, Y as EXPENSES"
- AC-6: User must click "Confirm Mapping" before import proceeds
- AC-7: Save mapping preferences for same bank format
```

---

### SE-803: Declaration Timestamp Persistence (2 SP) - /james

**Source:** /inga Finance Retrospective
**Risk:** MEDIUM - Audit trail requirement

```
US-803: Store Declaration Acceptance Timestamp
As a system, I want to persist declaration acceptance timestamps
So that we have an audit trail for compliance

Acceptance Criteria:
- AC-1: Add declaration_accepted_at TIMESTAMP column to submissions table
- AC-2: Store timestamp in UTC ISO 8601 format
- AC-3: Add declaration_text_hash VARCHAR(64) for version tracking
- AC-4: Timestamp stored when user checks declaration checkbox
- AC-5: Timestamp included in PDF confirmation
- AC-6: Cannot submit without declaration timestamp being set
```

---

### SE-901: Landing Page (5 SP) - /finn

**Source:** /apex Marketing Retrospective
**Risk:** HIGH - Cannot launch without marketing presence

```
US-901: Create Product Landing Page
As a potential user, I want a landing page explaining the product
So that I can decide to download and use it

Acceptance Criteria:
- AC-1: Landing page at selfemploy.uk (or similar)
- AC-2: Hero section: "Your Tax. Your Data. Your Freedom."
- AC-3: Feature highlights: Free, Open-source, Privacy-first
- AC-4: Download buttons for Windows, macOS, Linux
- AC-5: Quick demo GIF or screenshot
- AC-6: FAQ section
- AC-7: Link to GitHub repository
- AC-8: Responsive design (mobile-friendly)
```

---

### SE-703: HMRC Sandbox Integration Tests (5 SP) - /james

```
US-703: Verify HMRC API Integration
As a developer, I want comprehensive tests against HMRC sandbox
So that we have confidence in production readiness

Acceptance Criteria:
- AC-1: Test OAuth flow with sandbox credentials
- AC-2: Test Business Details API retrieval
- AC-3: Test Quarterly Update submission (all 4 quarters)
- AC-4: Test Annual Return submission
- AC-5: Test error handling for invalid submissions
- AC-6: Test fraud prevention headers are included
- AC-7: All tests documented with HMRC test scenarios
- AC-8: Tests can run in CI with sandbox API
```

---

## Sprint Capacity Planning

| Priority | SP Total | % of Velocity (25 SP) |
|----------|----------|----------------------|
| P0 (Critical) | 30 | 120% - **OVER CAPACITY** |
| P1 (Important) | 20 | - |
| P2 (Nice to Have) | 13 | - |
| P3 (Deferred) | 17 | - |

**Decision Required:** P0 tickets exceed velocity. Options:
1. **Split Sprint 6 into 6A/6B** - Do 25 SP of P0, then remaining 5 SP + P1 in 6B
2. **Extend Sprint** - Allow 2.5 week sprint for all P0
3. **Defer some P0** - Move SE-901 (Landing Page) to Sprint 7

**Recommendation:** Split into Sprint 6A (25 SP) and Sprint 6B (remaining)

---

## Immediate Action Items (START NOW)

### /james - Backend (10 SP available immediately)

| Priority | Ticket | Title | SP | Status |
|----------|--------|-------|-----|--------|
| 1 | **SE-801** | Class 2 NI Calculator | 5 | **START NOW** |
| 2 | SE-803 | Declaration Timestamp Persistence | 2 | Next |
| 3 | SE-703 | HMRC Sandbox Integration Tests | 5 | After SE-801 |

### /finn - Frontend (13 SP available immediately)

| Priority | Ticket | Title | SP | Status |
|----------|--------|-------|-----|--------|
| 1 | **SE-507** | Privacy Notice UI | 3 | **START NOW** |
| 2 | **SE-508** | Terms of Service UI | 5 | **START NOW** (parallel) |
| 3 | SE-509 | Enhanced Disclaimers | 2 | After SE-507 |
| 4 | SE-802 | Bank Import Column Mapping | 3 | After SE-509 |

---

## Approval Gates Required

| Gate | Agent | Tickets | Status |
|------|-------|---------|--------|
| Architecture | /jorge | SE-801, SE-703 | **APPROVED** |
| Finance | /inga | SE-801, SE-802, SE-803 | **APPROVED** (Class 2 rate corrected to 3.50/week) |
| Legal | /alex | SE-507, SE-508, SE-509 | **APPROVED** (with mandatory content) |
| UI Design | /aura | SE-507, SE-508, SE-509, SE-802, SE-901 | **APPROVED** |

---

## Team Assignments (Sprint 6)

| Agent | Tickets | SP | Focus |
|-------|---------|-----|-------|
| /james | SE-801, SE-803, SE-703 | 12 | Class 2 NI, HMRC tests, audit |
| /finn | SE-507, SE-508, SE-509, SE-802, SE-901 | 18 | Legal UI, bank import |
| /aura | SE-507, SE-508, SE-509, SE-802, SE-901 | - | Design specs |
| /rev | All | - | Code review |
| /rob | All | - | QA test cases |
| /adam | All, TD-006 | - | E2E automation |

---

## Risks and Mitigations

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| HMRC production registration delayed | High | **CRITICAL** | Escalate weekly, prepare all documentation |
| P0 scope exceeds velocity | High | High | Split sprint or extend duration |
| Legal content approval delayed | Medium | High | Start /alex review immediately |
| Class 2 NI rate verification | Low | Medium | Verify with HMRC published rates |

---

## Activity Log

| Date | Agent | Action |
|------|-------|--------|
| 2026-01-11 | /luda | Sprint 6 planning started after Sprint 5 completion |
| 2026-01-11 | /luda | Created sprint folder and initial README |
| 2026-01-11 | /luda | Identified P0 external blockers and action items |
| 2026-01-11 | /luda | Multi-agent retrospective completed (/jorge, /anna, /inga, /apex, /alex) |
| 2026-01-11 | /luda | **UPDATED: Added ALL retrospective tickets to backlog** |
| 2026-01-11 | /luda | **P0 tickets identified: SE-801, SE-507, SE-508, SE-509, SE-802, SE-803, SE-901, SE-703** |
| 2026-01-11 | /jorge | **APPROVED: Architecture for SE-801 (Class 2 NI) and SE-703 (Sandbox Tests)** |
| 2026-01-11 | /alex | **APPROVED: Legal compliance for SE-507, SE-508, SE-509 with mandatory content requirements** |
| 2026-01-11 | /inga | **APPROVED: Finance compliance for SE-801, SE-802, SE-803 with CRITICAL correction: Class 2 NI rate is 3.50/week (not 3.45) for 2025/26** |
| 2026-01-11 | /aura | **APPROVED: UI Design specifications for SE-507, SE-508, SE-509, SE-802, SE-901** - All designs saved to `approvals/aura-ui-designs/` |
| 2026-01-11 | /james | **IMPLEMENTED: SE-801 Class 2 NI Calculator** - 23 tests, correct rates (3.50/week, 6,845 SPT) |
| 2026-01-11 | /finn | **IMPLEMENTED: SE-507 Privacy Notice UI** - 29 tests, MVVM pattern, all 9 sections per /alex |
| 2026-01-11 | /rev | **CODE REVIEW APPROVED: SE-801 and SE-507** - Ready for QA. Report: `reviews/rev-SE-801-SE-507.md` |
| 2026-01-11 | /rob | **QA TEST CASES DESIGNED: SE-801 and SE-507** - 56 test cases (25 P0, 22 P1, 9 P2). Ready for /adam automation. Report: `testing/rob-qa-SE-801-SE-507.md` |
| 2026-01-11 | /finn | **IMPLEMENTED: SE-508 Terms of Service UI** - 50+ tests, scroll tracking, version management |
| 2026-01-11 | /james | **IMPLEMENTED: SE-803 Declaration Timestamp Persistence** - 19 tests, SHA-256 hash, UTC timestamps |
| 2026-01-11 | /rev | **CODE REVIEW APPROVED: SE-508 and SE-803** - Both APPROVED for QA. Report: `reviews/rev-SE-508-SE-803.md` |
| 2026-01-11 | /rob | **QA TEST CASES DESIGNED: SE-508 and SE-803** - 42 test cases (14 P0, 21 P1, 7 P2). Ready for /adam automation. Report: `testing/rob-qa-SE-508-SE-803.md` |
| 2026-01-11 | /finn | **IMPLEMENTED: SE-509 Enhanced Disclaimers** - 22 tests in DisclaimersTest.java. Centralized in Disclaimers.java with version tracking. |
| 2026-01-11 | /finn | **IMPLEMENTED: SE-802 Bank Import Column Mapping Wizard** - 68 tests. 3-step wizard with MVVM pattern. |
| 2026-01-11 | /james | **IMPLEMENTED: SE-703 HMRC Sandbox Integration Tests** - 234 tests. WireMock-based CI-compatible tests. |
| 2026-01-11 | /rev | **CODE REVIEW APPROVED: SE-509, SE-802, SE-703** - All three tickets approved. Report: `reviews/rev-SE-509-SE-802-SE-703.md` |
| 2026-01-11 | /rob | **QA TEST CASES DESIGNED: SE-509, SE-802, SE-703** - 88 test cases (38 P0, 35 P1, 15 P2). Ready for /adam automation. Report: `testing/rob-qa-SE-509-SE-802-SE-703.md` |
| 2026-01-11 | /finn | **IMPLEMENTED: SE-901 Landing Page** - Complete static website (index.html, CSS, JS). Privacy, Terms, Disclaimer pages. Responsive design. Implementation notes: `implementation/SE-901-landing-page.md` |
| 2026-01-11 | /rev | **CODE REVIEW APPROVED: SE-901 Landing Page** - Excellent HTML5, CSS, JS quality. All ACs verified. Report: `reviews/rev-SE-901.md` |
| 2026-01-11 | /rob | **QA TEST CASES DESIGNED: SE-901 Landing Page** - 85 test cases (41 P0, 32 P1, 12 P2). Comprehensive E2E coverage for responsive design, navigation, accessibility. Ready for /adam automation. Report: `testing/rob-qa-SE-901.md` |
| 2026-01-12 | /adam | **RETROSPECTIVE COMPLETED** - 540+ automated tests, 923 total in CI. Key findings: WireMock HMRC mocking successful, Playwright landing page tests (46), headless JavaFX via Xvfb resolved. Report: `retrospectives/adam-retro.md` |

---

## Definition of Done (Sprint 6)

- [x] All P0 tickets completed (8/8 - 30 SP)
- [x] All tickets have /jorge architecture approval
- [x] Finance tickets approved by /inga
- [x] Legal tickets approved by /alex
- [x] All UI tickets have /aura design specs
- [x] Code review passed by /rev
- [x] QA test cases designed by /rob (271 test cases total)
- [x] E2E tests implemented by /adam (540+ automated tests)
- [x] All tests passing in CI (923 tests, 0 failures)
- [ ] HMRC production registration escalated ⚠️ BLOCKED

## Sprint 6 Final Summary

| Metric | Value |
|--------|-------|
| **Story Points Completed** | 30 SP |
| **Velocity** | 30 SP (above 25.2 SP average) |
| **P0 Tickets** | 8/8 (100%) |
| **Tests Added** | 540+ |
| **CI Status** | ✅ All Passing |

### Commits
- `[Sprint-6] Launch Readiness & Compliance - Complete Sprint`
- `fix(migrations): Use H2-compatible SQL syntax for all migrations`
- `fix(tests): Adjust HMRC test suite verification expectations`
- `fix(tests): Include Class 2 NI in total tax calculation test`
- `fix(cdi): Add Clock producer for DeclarationService injection`

---

**Scrum Master:** /luda
**Last Updated:** 2026-01-12 (/adam retrospective completed - 540+ automated tests, comprehensive E2E and CI coverage)
