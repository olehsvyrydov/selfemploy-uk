# Anna - Business Analyst Sprint 6 Retrospective

**Sprint:** 6 - Launch Readiness & Compliance
**Date:** 2026-01-12
**Role:** Senior Business Analyst
**Sprint Duration:** 2026-01-11 to 2026-01-12

---

## Executive Summary

Sprint 6 represented a pivotal moment for the UK Self-Employment Manager project, focusing on launch readiness through legal compliance, financial accuracy, and market presence. The sprint delivered 30 Story Points across 8 P0 tickets, exceeding the average velocity of 25.2 SP. This retrospective evaluates the business value delivered, requirement clarity, and alignment with user needs.

---

## 1. What Went Well?

### 1.1 Exceptional Requirements Clarity

**Strong Stakeholder Collaboration:**

The multi-domain expert involvement created remarkably clear requirements that left minimal room for interpretation:

| Ticket | Domain Expert | Requirements Quality | Notes |
|--------|--------------|---------------------|-------|
| SE-801 | /inga | Excellent | Caught rate error early (3.45 to 3.50); provided calculation logic |
| SE-507, SE-508, SE-509 | /alex | Exceptional | Complete legal text provided; implementation checklist included |
| SE-802 | /inga | Good | UX flow specified; edge cases documented |
| SE-901 | /apex, /aura | Excellent | Messaging, positioning, and visual design aligned |

**Key Success Factors:**
- Domain experts answered developer questions proactively before implementation
- /alex provided APPROVED legal text verbatim, eliminating back-and-forth
- /inga corrected tax rate error BEFORE implementation began, saving rework

### 1.2 High Business Value Delivered

**Compliance Value (Legal Risk Mitigation):**

| Feature | Business Value | Risk Mitigated |
|---------|---------------|----------------|
| Privacy Notice (SE-507) | GDPR compliance | ICO fines up to 17.5M GBP |
| Terms of Service (SE-508) | Legal protection | User claims for tax errors |
| Enhanced Disclaimers (SE-509) | Professional indemnity | Reliance claims |

**Financial Accuracy Value:**

| Feature | Business Value | User Impact |
|---------|---------------|-------------|
| Class 2 NI Calculator (SE-801) | Complete tax picture | Users see full NI liability |
| Bank Import Wizard (SE-802) | Prevents misclassification | Reduces incorrect tax submissions |
| Declaration Timestamp (SE-803) | Audit trail | Evidence for HMRC enquiries |

**Market Presence Value:**

| Feature | Business Value | Reach |
|---------|---------------|-------|
| Landing Page (SE-901) | Product visibility | First impression for all users |
| HMRC Sandbox Tests (SE-703) | API confidence | Production-ready submission |

### 1.3 User Journey Completeness

For the first time, the application provides a complete first-launch experience:

```
User Download -> Privacy Notice (GDPR) -> Terms of Service (Legal)
    -> Onboarding -> Track Income/Expenses -> Bank Import (with wizard)
    -> Tax Summary (with Class 2 NI) -> HMRC Submission (with declaration)
    -> PDF Confirmation (with disclaimer)
```

**Every critical touchpoint now has appropriate legal protection and user guidance.**

### 1.4 Test Coverage Excellence

The QA process delivered outstanding coverage:

| Metric | Value |
|--------|-------|
| Test Cases Designed (/rob) | 271 |
| Automated Tests (/adam) | 540+ |
| Total Tests in CI | 923 |
| Pass Rate | 100% |

This level of test coverage provides high confidence for launch.

---

## 2. What Could Be Improved?

### 2.1 Requirement Gaps Identified

**Class 2 NI Changes from April 2024:**

The original ticket (SE-801) did not account for the April 2024 regulatory change where Class 2 NI became credited (not payable) for those above the Small Profits Threshold. This was caught by /inga during review, but ideally should have been captured in initial requirements.

**Gap:** Regulatory change monitoring should be integrated into sprint planning for tax-related features.

**Voluntary Class 2 User Guidance:**

While the technical implementation is correct, the user-facing guidance for when to opt for voluntary Class 2 contributions (State Pension impact) could be clearer. Users below the Small Profits Threshold need help making this decision.

**Recommendation:** Add an in-app educational component explaining State Pension implications for SE-701 (In-App Help System).

### 2.2 User Journey Issues

**First Launch Experience Length:**

The current first-launch flow requires users to:
1. Read Privacy Notice (2-3 min)
2. Scroll through Terms of Service (3-5 min)
3. Acknowledge Disclaimers (1 min)

**Concern:** Total 6-9 minutes before using the app may cause abandonment.

**Mitigation Implemented:** The legal content cannot be shortened (compliance requirement), but:
- Scroll tracking provides progress feedback
- Clear "Continue" buttons maintain momentum
- Settings access allows later review

**Future Consideration:** User analytics on completion rates once launched.

**Bank Import Ambiguity:**

While the Column Mapping Wizard addresses the major risk of income/expense misclassification, users may still incorrectly categorize business vs. personal transactions.

**Gap:** No guidance on business vs. personal expense separation during import.

**Recommendation:** Add a post-import review step or "mark as personal" functionality.

### 2.3 Missing Features for Full Launch Readiness

Based on retrospective analysis, the following are notable gaps:

| Feature | Impact | Priority |
|---------|--------|----------|
| In-App Help (SE-701) | User onboarding friction | P1 |
| User Onboarding Wizard (SE-702) | First-time user experience | P1 |
| Demo Video (SE-902) | Landing page conversion | P1 |
| User Documentation (SE-903) | Self-service support | P1 |

**Assessment:** These P1 items should be strongly considered for Sprint 7 to reduce support burden at launch.

### 2.4 External Blockers Remain Critical

**HMRC Production Registration:**

The most significant gap is NOT in requirements or implementation - it is the external dependency on HMRC production API access:

| Blocker | Status | Impact |
|---------|--------|--------|
| HMRC Production Registration | NOT STARTED | Cannot submit live returns |
| HMRC Fraud Prevention Certification | NOT STARTED | Required for production |

**Risk:** All development work is complete, but launch is blocked by external process.

**Recommendation:** This requires immediate escalation and dedicated focus.

---

## 3. What Should Change?

### 3.1 Process Improvements

**Regulatory Change Monitoring:**

```
Proposed Process:
1. Before each sprint, /inga reviews recent GOV.UK publications for NI/tax changes
2. Create standing agenda item: "Regulatory Updates" in sprint planning
3. Document any changes in sprint README with impact assessment
```

**Benefit:** Catches regulatory changes before they become implementation surprises.

**Domain Expert Pre-Approval Template:**

Sprint 6 demonstrated the value of domain experts providing complete specifications (e.g., /alex's verbatim legal text). This should become standard practice.

```
Domain Expert Approval Template:
1. Questions answered (table format)
2. Exact content/formulas provided
3. Edge cases documented
4. Implementation checklist
5. Post-implementation verification criteria
```

### 3.2 Requirements Gathering Improvements

**Business Context Documentation:**

For future sprints with regulatory/compliance features, requirements should include:

| Section | Content |
|---------|---------|
| Regulatory Source | Link to legislation/GOV.UK guidance |
| Effective Date | When rule applies |
| User Impact | Who is affected and how |
| Edge Cases | Special situations to handle |
| Audit Requirements | What records must be kept |

**User Persona Alignment:**

Sprint 6 delivered critical infrastructure, but user-facing value could be articulated more clearly. For example:

| Before | After (Recommended) |
|--------|---------------------|
| "Add Class 2 NI Calculator" | "Help users understand their complete NI liability, especially the pension implications of voluntary contributions" |
| "Add Privacy Notice UI" | "Build user trust by transparently explaining local-only data storage and HMRC requirements" |

### 3.3 Launch Readiness Checklist

Based on Sprint 6 learnings, propose a formal Launch Readiness Checklist:

**Technical Readiness:**
- [ ] All P0 features implemented and tested
- [ ] CI/CD passing with 100% test success
- [ ] No P0 bugs open
- [ ] Performance acceptable

**Legal Readiness:**
- [x] Privacy Notice approved by /alex
- [x] Terms of Service approved by /alex
- [x] Disclaimers approved by /alex
- [ ] Human solicitor review (recommended)

**Financial Accuracy:**
- [x] Tax calculations verified by /inga
- [x] NI calculations verified by /inga
- [x] Rates current for tax year

**Market Readiness:**
- [x] Landing page complete
- [ ] Domain configured (selfemploy.uk)
- [ ] App screenshots captured
- [ ] Download links active

**External Dependencies:**
- [ ] HMRC Production Registration **<-- BLOCKER**
- [ ] HMRC Fraud Prevention Certification **<-- BLOCKER**

---

## 4. Business Value Summary

### Sprint 6 Value Scorecard

| Dimension | Score (1-5) | Notes |
|-----------|-------------|-------|
| User Value Delivered | 4 | Complete compliance, but P1 help system missing |
| Requirements Clarity | 5 | Domain experts provided exceptional detail |
| Stakeholder Alignment | 5 | All gates (architecture, finance, legal, UI) approved |
| Risk Mitigation | 5 | All identified legal/financial risks addressed |
| Launch Readiness | 3 | External blockers prevent actual launch |

**Overall Business Value:** HIGH - Sprint 6 transformed the application from technically complete to legally compliant and market-ready. The external HMRC blockers are the sole impediment to launch.

### Value Metrics

| Metric | Sprint 6 | Impact |
|--------|----------|--------|
| Story Points | 30 | 120% of velocity target |
| P0 Tickets | 8/8 (100%) | All critical items complete |
| Legal Documents | 3 | Privacy, ToS, Disclaimers |
| Tax Features | 2 | Class 2 NI, Declaration |
| User Features | 2 | Bank Import Wizard, Landing Page |
| Test Coverage | 923 tests | High confidence for launch |

---

## 5. Recommendations for Sprint 7

### Priority 1 - User Experience
| Ticket | Rationale |
|--------|-----------|
| SE-701: In-App Help | Reduce support burden; improve user self-service |
| SE-702: Onboarding Wizard | Guide first-time users through setup |
| SE-902: Demo Video | Increase landing page conversion |

### Priority 2 - HMRC Integration
| Action | Owner |
|--------|-------|
| Escalate HMRC Production Registration | Team Lead |
| Prepare Fraud Prevention Documentation | /james |
| Test Production Credentials | After registration |

### Priority 3 - Polish
| Ticket | Rationale |
|--------|-----------|
| SE-705: Accessibility Audit | WCAG 2.1 AA compliance |
| SE-706: Error Message Improvements | User-friendly error handling |

---

## 6. Stakeholder Communication

### Key Messages for Product Owner (/max)

1. **Sprint 6 achieved launch readiness from a development perspective.** All P0 compliance and financial features are complete.

2. **External blockers are the sole impediment to launch.** HMRC production registration must be escalated immediately.

3. **P1 user experience features (help, onboarding) should be prioritized** to reduce support burden at launch.

4. **The landing page is ready for deployment** once hosting and domain are configured.

### Metrics for Stakeholder Reporting

| Metric | Value |
|--------|-------|
| Sprint Velocity | 30 SP (120% of target) |
| P0 Completion | 100% |
| Test Coverage | 923 tests |
| Code Review Pass Rate | 100% |
| External Blockers | 2 (HMRC registration, certification) |

---

## 7. Conclusion

Sprint 6 was a highly successful sprint from a business analysis perspective. The team delivered all critical launch readiness features with exceptional requirements clarity driven by strong domain expert collaboration. The primary concern is the external HMRC registration blockers which are outside the development team's direct control.

**Key Takeaways:**
1. Domain expert pre-approval with complete specifications dramatically reduces implementation ambiguity
2. Regulatory change monitoring should be formalized for tax-related features
3. User experience features (help, onboarding) are important for launch success
4. External dependencies need proactive escalation and tracking

---

**Signed:** Anna (Business Analyst)
**Date:** 2026-01-12
**Sprint:** 6 - Launch Readiness & Compliance
