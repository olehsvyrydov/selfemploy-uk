# Sprint 6 Retrospective - /rob (Senior QA Engineer)

**Sprint:** 6 - Launch Readiness & Compliance
**Period:** 2026-01-11 to 2026-01-12
**Agent:** /rob (Rob - Senior QA Engineer)
**Date:** 2026-01-12

---

## Summary

Sprint 6 was exceptionally productive for QA. I designed **271 test cases** across 8 P0 tickets covering Class 2 NI Calculator, Legal Compliance UI (Privacy Notice, Terms of Service, Enhanced Disclaimers), Bank Import Column Mapping Wizard, Declaration Timestamp Persistence, HMRC Sandbox Integration Tests, and the Landing Page.

### Test Case Breakdown by Ticket

| Ticket Batch | Tickets | Total Cases | P0 | P1 | P2 |
|--------------|---------|-------------|-------|-------|------|
| SE-801 + SE-507 | Class 2 NI + Privacy Notice | 56 | 25 | 22 | 9 |
| SE-508 + SE-803 | Terms of Service + Declaration Timestamp | 42 | 14 | 21 | 7 |
| SE-509 + SE-802 + SE-703 | Disclaimers + Column Mapping + HMRC Tests | 88 | 38 | 35 | 15 |
| SE-901 | Landing Page | 85 | 41 | 32 | 12 |
| **TOTAL** | **8 P0 Tickets** | **271** | **118** | **110** | **43** |

### Key Activities
- Designed comprehensive black-box test cases from acceptance criteria
- Manual browser testing of SE-901 Landing Page using Browser MCP
- Mobile responsiveness verification across multiple viewport breakpoints
- Cross-browser testing recommendations (Chrome, Firefox, Safari, Edge, Mobile Safari, Chrome Android)
- Created detailed automation recommendations for /adam

---

## 1. What Went Well?

### Comprehensive Test Coverage
- **43% P0 Coverage:** 118 P0 (Critical) test cases ensure no launch blockers escape testing
- **Boundary Condition Testing:** Class 2 NI calculator tests include threshold boundary cases (exactly at GBP 6,845, just above, just below)
- **Multi-Platform Coverage:** Landing page tests cover 6 browser/platform combinations and 5 viewport breakpoints

### Strong Collaboration with Developers

| Developer | Collaboration Highlight |
|-----------|------------------------|
| /james | Class 2 NI implementation notes helped identify edge cases (voluntary vs mandatory, effective rate calculation) |
| /finn | Bank Import Wizard MVVM design enabled clear test data planning (3 amount interpretation modes) |
| Both | Implementation notes in `implementation/*.md` files provided excellent context for test design |

### Efficient Test Case Design Process
- **Acceptance Criteria-Driven:** Every test case maps directly to specific ACs
- **Structured Reports:** Consistent format across all QA reports with clear preconditions, steps, expected results, and automation notes
- **Prioritization:** Clear P0/P1/P2 classification enables /adam to focus automation efforts

### Legal Compliance Coverage
- SE-507 Privacy Notice: 31 test cases covering GDPR requirements (data collection disclosure, retention, rights)
- SE-508 Terms of Service: 24 test cases including scroll-to-accept verification
- SE-509 Disclaimers: 28 test cases verifying non-dismissible legal text in 3 locations

### Browser MCP Integration for Manual Testing
- Successfully used Browser MCP tools for SE-901 Landing Page verification
- Verified responsive design across viewports (320px, 375px, 768px, 1024px, 1440px)
- Confirmed FAQ accordion functionality, smooth scrolling, and mobile navigation toggle

---

## 2. What Could Be Improved?

### Test Environment Gaps

| Gap | Impact | Recommendation |
|-----|--------|----------------|
| No dedicated QA test server | Manual testing relies on local dev environment | Set up staging environment for consistent testing |
| Missing test data fixtures | Each test run requires manual data setup | Create shared test data scripts/fixtures |
| JavaFX E2E testing complexity | TestFX requires Xvfb in CI, adding overhead | Investigate headless JavaFX alternatives or accept CI Xvfb |

### Tooling Limitations
- **PDF Content Verification:** No automated way to verify PDF disclaimer content (TC-509-010, TC-509-011) - requires PDFBox integration
- **Screen Reader Testing:** TC-509-016 accessibility test requires manual verification with NVDA/VoiceOver
- **Large CSV Performance Testing:** TC-802-038 needs 10,000+ row test files that don't exist yet

### Test Data Management
- **SE-802 Bank Import Wizard:** Needs standardized test CSV files for:
  - Standard format (positive income, negative expense)
  - Inverted format (banks that reverse sign convention)
  - Separate columns format (Debit/Credit columns)
  - Edge cases (special characters, missing values, large files)
- **Recommendation:** Create `test-resources/csv/` folder with curated test data

### Documentation Gaps
- Some test cases reference "implementation dependent" behavior without clear specification
- TC-803-009 boundary behavior (exactly 24 hours) needs implementation clarification
- SE-802 preference persistence matching criteria need specification

### Test Execution Tracking
- No mechanism to track which test cases have been executed manually vs automated
- Recommendation: Add execution status column to QA reports or use test management tool

---

## 3. What Should Change?

### Process Improvements

#### A. Pre-Sprint Test Data Preparation
**Proposal:** Before implementation begins, create test data fixtures based on acceptance criteria.

```
Sprint N Planning:
1. /luda defines ACs
2. /rob reviews ACs and identifies test data needs
3. /rob creates test data fixtures BEFORE /james or /finn start implementation
4. Developers can use same fixtures for their unit tests
```

**Benefit:** Faster QA turnaround, developers and QA aligned on edge cases.

#### B. AC Clarification Checklist for QA
**Proposal:** Add mandatory clarification points to each AC:

| Clarification Point | Example |
|---------------------|---------|
| Boundary behavior | "At threshold X, is the condition >= or >?" |
| Error messages | "What exact error text for validation failure?" |
| Persistence scope | "Is preference per-user, per-installation, or per-bank?" |
| UI state recovery | "After error, does form preserve input or reset?" |

#### C. Automation Priority Guidelines for /adam
**Proposal:** Formalize automation criteria:

| Priority | Automation Requirement | Timeline |
|----------|----------------------|----------|
| P0 | **MANDATORY** - Must be automated before release | Same sprint |
| P1 | **REQUIRED** - Automate before next sprint | Within 1 sprint |
| P2 | **OPTIONAL** - Automate if time permits | Backlog |

#### D. Cross-Browser Test Matrix in CI
**Proposal:** Add browser matrix to GitHub Actions:

```yaml
strategy:
  matrix:
    browser: [chromium, firefox, webkit]
    viewport: [desktop, mobile]
```

**Benefit:** Catch browser-specific issues before manual QA.

### Tool Improvements

#### E. Test Data Management System
**Proposal:** Implement structured test data approach:

```
test-resources/
  csv/
    bank-statements/
      barclays-standard.csv
      hsbc-inverted.csv
      nationwide-separate-columns.csv
    edge-cases/
      empty-file.csv
      large-10000-rows.csv
      special-characters.csv
  json/
    tax-calculations/
      basic-income.json
      high-income.json
      loss-scenario.json
```

#### F. PDF Testing Integration
**Proposal:** Add PDFBox to test dependencies for automated PDF content verification:

```xml
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.x</version>
    <scope>test</scope>
</dependency>
```

**Benefit:** Automate TC-509-010, TC-509-011, TC-803-014, TC-803-015.

### Collaboration Improvements

#### G. QA Involvement in Design Review
**Proposal:** Include /rob in /aura design reviews for testability feedback:

```
Current:  /aura -> /finn/james -> /rev -> /rob
Proposed: /aura (+ /rob testability review) -> /finn/james -> /rev -> /rob
```

**Benefit:** Identify test automation challenges early (e.g., scroll-to-accept UX).

#### H. Joint Test Case Review with /adam
**Proposal:** Before /adam implements automation, brief review session:
- Clarify automation notes
- Identify tricky locators
- Agree on test data approach
- Estimate automation effort

---

## Metrics

### Sprint 6 QA Velocity

| Metric | Value | Notes |
|--------|-------|-------|
| Test Cases Designed | 271 | 8 P0 tickets |
| Test Cases per Day | ~135 | 2-day sprint |
| P0 Coverage | 43.5% | 118/271 cases |
| Documentation Pages | 4 | One per ticket batch |
| Browser Viewports Tested | 5 | 320px to 1440px |
| Browser Matrix Defined | 6 | Chrome, FF, Safari, Edge, Mobile Safari, Chrome Android |

### Test Case Distribution by Type

| Category | Count | Percentage |
|----------|-------|------------|
| Unit/Integration (for /adam) | 112 | 41% |
| E2E UI Tests | 124 | 46% |
| Performance | 5 | 2% |
| Accessibility | 12 | 4% |
| Cross-Browser | 18 | 7% |

### Key Quality Indicators

| Indicator | Status | Notes |
|-----------|--------|-------|
| All ACs have test coverage | YES | 100% AC coverage |
| Boundary tests defined | YES | Class 2 NI, timestamp validation, scroll percentage |
| Error scenario tests | YES | Null handling, invalid format, service failure |
| Happy path tests | YES | All critical user journeys covered |
| Edge case tests | YES | Empty CSV, large files, special characters |

---

## Recommendations Summary

### Immediate (Sprint 7)

1. **Create test data fixtures** for SE-802 Bank Import (CSV files)
2. **Add PDFBox dependency** for PDF content verification
3. **Document boundary behaviors** for ambiguous test cases
4. **Establish QA-/adam handoff session** before automation begins

### Medium-Term (Next 2-3 Sprints)

5. **Set up staging environment** for consistent QA testing
6. **Add browser matrix to CI** (Playwright multi-browser)
7. **Create test execution tracking** (spreadsheet or test management tool)
8. **Include /rob in /aura design reviews** for testability input

### Long-Term (Quarterly)

9. **Develop accessibility testing strategy** (automated + manual with screen readers)
10. **Build performance test suite** (Lighthouse CI for landing page)
11. **Create end-to-end smoke test suite** for regression

---

## Sprint 6 QA Assessment

| Aspect | Rating | Notes |
|--------|--------|-------|
| Test Coverage | Excellent | 271 cases, all ACs covered |
| Test Quality | Excellent | Clear structure, automation-ready |
| Collaboration | Good | Strong dev collaboration, more /aura involvement needed |
| Tooling | Adequate | PDF testing and screen reader gaps |
| Process | Good | Efficient design, handoff to /adam clear |

**Overall Sprint 6 QA Grade: A-**

The sprint was highly successful with comprehensive test coverage and well-structured test cases. Improvements needed in test data management, PDF verification tooling, and earlier QA involvement in UI design reviews.

---

## Sign-Off

**Retrospective Completed By:** /rob (Rob - Senior QA Engineer)
**Date:** 2026-01-12
**Status:** COMPLETE

---

*"Quality is never an accident; it is always the result of intelligent effort."* - John Ruskin
