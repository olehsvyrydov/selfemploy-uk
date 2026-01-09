# Legal & Compliance

**Author**: Alex (Senior UK Legal Counsel)
**Status**: ✅ Conditionally Approved
**Date**: 2026-01-09

---

## Disclaimer

⚠️ This documentation provides legal guidance but does not constitute formal legal advice. For significant matters, consult a qualified solicitor.

---

## Executive Summary

The UK Self-Employment Manager presents a **LOW-TO-MEDIUM risk profile**:

### Favourable Factors ✅

- Local-only data storage eliminates most GDPR "controller" obligations
- Open-source model shifts responsibility to users
- No handling of client money (avoids FCA regulation)
- Apache 2.0 license provides robust liability protection

### Areas Requiring Attention ⚠️

- Disclaimer language must meet UCTA "reasonableness" test
- HMRC MTD vendor registration is mandatory
- Google Drive backup triggers additional GDPR considerations

---

## Applicable Legislation

| Act | Relevance |
|-----|-----------|
| **UK GDPR + DPA 2018** | Personal data processing |
| **UCTA 1977** | Limitation of liability |
| **Consumer Rights Act 2015** | Digital content standards |
| **Computer Misuse Act 1990** | Security obligations |
| **Finance Act 2021** | MTD obligations |

---

## GDPR/DPA 2018 Compliance

### Data Controller Status

For local-only mode:
- **User = Data Controller** (not the software provider)
- **Software = Data Processing Tool** (like Excel)
- **Developer = NOT a Data Controller**

### Data Processed

| Data Type | Storage Location | Controller |
|-----------|------------------|------------|
| Business income records | Local device | User |
| Business expense records | Local device | User |
| Tax calculations | Local device | User |
| HMRC submission data | HMRC servers | User/HMRC |
| Receipt attachments | Local device | User |

### Privacy Notice

The application includes a privacy notice explaining:
- What data the app processes
- Where data is stored
- Who can access data
- User rights and responsibilities

---

## Disclaimers

### Main Disclaimer

```
IMPORTANT LEGAL NOTICES

1. NATURE OF THIS SOFTWARE
This software is a RECORD-KEEPING TOOL to assist with
self-employment accounting. It is NOT a substitute for
professional tax advice.

2. NO PROFESSIONAL ADVICE
Nothing in this software constitutes professional tax,
legal, or financial advice. Tax calculations are estimates
based on published HMRC rates.

3. YOUR RESPONSIBILITIES
YOU are legally responsible for:
- The accuracy of all data you enter
- Verifying all calculations before submission to HMRC
- Meeting all filing deadlines
- Maintaining supporting documentation

4. LIMITATION OF LIABILITY
This software is provided "AS IS" without warranty.
We are not liable for HMRC penalties, fines, or errors
in tax calculations.

This limitation does not exclude liability for:
- Death or personal injury caused by negligence
- Fraud or fraudulent misrepresentation
- Any liability that cannot be excluded by law

5. OPEN SOURCE SOFTWARE
This software is provided under the Apache License 2.0.
```

### Pre-Submission Acknowledgment

Before any HMRC submission, users must confirm:

- [ ] I have reviewed all data for accuracy
- [ ] I understand this will be a legal submission to HMRC
- [ ] I accept responsibility for the information submitted
- [ ] I have kept supporting documentation

---

## HMRC MTD Vendor Requirements

### Mandatory Requirements

| Requirement | Status |
|-------------|--------|
| Developer Hub registration | Required before production |
| Sandbox testing | Must pass all test scenarios |
| Production application | Apply for production credentials |
| Fraud Prevention Headers | **Mandatory** for all API calls |
| Privacy Policy | Must be accessible to users |

### Fraud Prevention Headers

HMRC requires specific headers for all API calls:

```
Gov-Client-Connection-Method
Gov-Client-Device-ID
Gov-Client-User-IDs
Gov-Client-Timezone
Gov-Client-Local-IPs
Gov-Vendor-Version
Gov-Vendor-License-IDs
```

### Failure to Comply

| Violation | Consequence |
|-----------|-------------|
| Missing fraud headers | API access revoked |
| Incorrect submissions | HMRC investigation |
| Data breach | ICO enforcement + HMRC action |

---

## Record Retention

| Record Type | Minimum Retention |
|-------------|-------------------|
| Income/Expenses | 5 years from filing deadline |
| Tax submissions | 5 years |
| Supporting documents | 5 years |

The application helps users meet these requirements by:
- Storing data securely with encryption
- Providing backup/restore functionality
- Warning when records are approaching retention limits

---

## Open Source License

### Apache License 2.0

Key provisions:
- Software provided "AS IS" without warranty
- No liability for direct, indirect, or consequential damages
- Users accept risks of using open-source software

### Terms of Service

In addition to Apache 2.0, users accept:
- They are 18+ and UK taxpayers
- They authorise HMRC submissions
- They are responsible for data accuracy
- No guaranteed support or updates

---

## Risk Assessment

| Risk Area | Probability | Impact | Overall | Mitigation |
|-----------|-------------|--------|---------|------------|
| GDPR breach | Low | High | **Medium** | Local storage, encryption |
| UCTA challenge | Low | Medium | **Low** | Reasonable disclaimers |
| HMRC penalties | Low | High | **Medium** | User responsibility |
| FCA enforcement | Very Low | High | **Low** | Not handling money |

---

## Compliance Checklist

### Pre-Launch (Mandatory)

- [ ] Privacy Notice implemented
- [ ] Terms of Service created
- [ ] HMRC vendor registration submitted
- [ ] Fraud prevention headers implemented
- [ ] Enhanced disclaimers in place
- [ ] Pre-submission acknowledgment flow
- [ ] Encryption documentation

### Pre-Launch (Recommended)

- [ ] Third-party security audit
- [ ] Human solicitor review of terms
- [ ] Cyber liability insurance
- [ ] Cookie policy (if using analytics)

### Google Drive Feature (P2)

- [ ] Data transfer documentation
- [ ] Separate opt-in consent
- [ ] Google API compliance
- [ ] Updated privacy notice

---

## Contact for Legal Questions

For legal questions about this application:
- Open a [GitHub Discussion](https://github.com/olehsvyrydov/selfemploy-uk/discussions)
- Tag with "legal" label

For personal tax advice:
- Consult a qualified UK accountant
- Visit [HMRC.gov.uk](https://www.gov.uk/self-assessment-tax-returns)
