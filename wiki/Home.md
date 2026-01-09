# UK Self-Employment Manager

Welcome to the **UK Self-Employment Manager** wiki - the comprehensive documentation for our free, open-source desktop application helping UK self-employed individuals manage their accounting and submit annual reports to HMRC.

## Quick Links

| Section | Description |
|---------|-------------|
| [Getting Started](Getting-Started) | Installation and first steps |
| [Product Vision](Product-Vision) | Project goals and roadmap |
| [Architecture](Architecture) | Technical design and patterns |
| [Tax Reference](Tax-Reference) | UK tax rates, categories, and calculations |
| [Legal & Compliance](Legal-Compliance) | GDPR, disclaimers, and regulatory requirements |
| [Contributing](Contributing) | How to contribute to the project |

---

## What Is This Project?

A **free, privacy-first desktop application** for UK self-employed individuals to:

- **Track income and expenses** throughout the year
- **Calculate tax estimates** (Income Tax + National Insurance Class 4)
- **Submit annual Self Assessment** to HMRC via Making Tax Digital (MTD) APIs
- **Store data locally** with AES-256 encryption

### Why This Project?

| Feature | selfemploy-uk | QuickBooks | FreeAgent |
|---------|---------------|------------|-----------|
| **Price** | FREE | £12-32/mo | £14.50/mo |
| **Data Location** | Local (your computer) | Cloud (US servers) | Cloud (UK servers) |
| **Open Source** | Yes (Apache 2.0) | No | No |
| **MTD Compatible** | Yes | Yes | Yes |

---

## Target Users

### Phase 1 (MVP): Self-Employed Individuals
- Freelancers, contractors, sole traders
- Income £12,570 - £150,000+
- Filing Self Assessment (SA100 + SA103)

### Phase 2: Partnerships
- Business partners sharing profits
- Partnership tax returns (SA104)

### Phase 3: Limited Companies
- Single-director companies
- Salary/dividend optimization
- Corporation Tax (CT600)

---

## Technology Stack

| Component | Technology |
|-----------|------------|
| Backend | Quarkus 3.x |
| UI | JavaFX 21+ |
| Database | H2 (AES-256 encrypted) |
| Build | Maven + jpackage |
| Platforms | Windows, macOS, Linux |

---

## MTD Timeline

The UK Government is mandating Making Tax Digital for self-employed:

| Date | Requirement |
|------|-------------|
| **April 2026** | Income > £50,000 must use MTD |
| **April 2027** | Income > £30,000 must use MTD |
| **April 2028** | Income > £20,000 must use MTD |

This application will be MTD-compliant, helping you meet these requirements for free.

---

## Documentation Status

| Document | Status |
|----------|--------|
| Product Vision | ✅ Approved |
| Architecture | ✅ Approved |
| Accounting Requirements | ✅ Approved |
| Legal Compliance | ✅ Conditionally Approved |
| Marketing Strategy | ✅ Approved |

---

## Support

- **Issues**: [GitHub Issues](https://github.com/olehsvyrydov/selfemploy-uk/issues)
- **Discussions**: [GitHub Discussions](https://github.com/olehsvyrydov/selfemploy-uk/discussions)
- **Contributing**: See [CONTRIBUTING.md](https://github.com/olehsvyrydov/selfemploy-uk/blob/main/CONTRIBUTING.md)

---

## Disclaimer

This software is provided for informational purposes only and does not constitute professional tax or financial advice. Always consult with a qualified accountant for significant financial decisions.
