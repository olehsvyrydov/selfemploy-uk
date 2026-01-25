# Privacy Notice

**Version 1.0** | Effective: 1 January 2026

## Your Privacy Matters

This privacy notice explains how your data is handled when you use UK Self-Employment Manager.

---

## 1. Who We Are

This application is developed and maintained as open-source software under the Apache License 2.0.

**Important:** Because all your data is stored locally on your device, **YOU are the data controller** for your own information. We (the software developers) do not have access to your data.

---

## 2. Data We Process

### 2.1 Financial and Tax Data (Stored Locally)

- **Income records:** Dates, amounts, descriptions, sources
- **Expense records:** Dates, amounts, categories, descriptions
- **Bank transactions:** Imported CSV data from your bank statements
- **Tax calculations:** Computed tax liabilities based on your data
- **Receipt attachments:** Images or PDFs you attach to transactions
- **Unique Taxpayer Reference (UTR):** Your HMRC identifier

**Where stored:** Encrypted database on your local device only
**Retention:** You control retention; HMRC requires 5 years

### 2.2 HMRC Connection Data (When You Connect)

When you connect to HMRC for Making Tax Digital submissions, the following data is collected and transmitted:

- **Device information:** Unique device ID (UUID), operating system, screen configuration
- **Network information:** Local IP addresses, MAC addresses, timezone
- **User information:** Operating system username
- **Authentication tokens:** Access and refresh tokens for HMRC API

**Why:** This is legally required by HMRC under the Finance Act 2022 and Transaction Monitoring Regulations. We cannot submit your tax returns without this data.

---

## 3. How We Use Your Data

| Purpose | Legal Basis |
|---------|-------------|
| Calculate your tax liability | Contract performance |
| Submit returns to HMRC | Contract performance |
| Comply with HMRC fraud prevention | Legal obligation |
| Store HMRC access tokens | Legitimate interests |
| Maintain audit trail | Legitimate interests |

---

## 4. Data Storage and Security

**All your financial data is stored ONLY on your device.**

We implement the following security measures:

- **AES-256-GCM encryption** for the local database
- **Operating system credential storage** for authentication tokens
- **TLS 1.2+ encryption** for all communications with HMRC

We do **NOT**:

- Store your data on any remote servers
- Have access to your financial information
- Share your data with third parties (except HMRC when you initiate a submission)
- Use your data for analytics, advertising, or any other purpose

---

## 5. Data Retention

| Data Type | Retention Period |
|-----------|------------------|
| Financial records | You decide (HMRC requires 5 years minimum) |
| HMRC tokens | Until you disconnect |
| Acknowledgment records | Indefinite (audit trail) |

**Your responsibility:** HMRC requires you to keep records for at least 5 years from the 31 January following the tax year. This application helps you meet this requirement, but the legal responsibility remains yours.

---

## 6. Your Rights Under UK GDPR

| Right | How to Exercise |
|-------|-----------------|
| **Access** | Request a copy of your data (Export feature in Settings) |
| **Rectification** | Correct inaccurate data (Edit records in the application) |
| **Erasure** | Delete your data (Delete records or uninstall application) |
| **Restriction** | Limit how data is processed (Contact us) |
| **Portability** | Receive data in portable format (Export to JSON/CSV) |
| **Object** | Object to processing (Contact us) |
| **Complain** | Lodge complaint with ICO (ico.org.uk) |

**Note:** Because your data is stored locally on your device, you already have full control. You can view, edit, export, or delete any data at any time.

---

## 7. HMRC Fraud Prevention

When you connect to HMRC, certain device information is collected and transmitted as required by law. This includes:

- Device identifiers (UUID)
- Operating system information
- Network information (local IP, MAC addresses)
- Timezone and screen configuration

**This is not optional.** HMRC will reject submissions without this information.

---

## 8. Changes to This Notice

We may update this privacy notice to reflect changes in the application or legal requirements. If we make material changes:

- The privacy notice version number will change
- You will be asked to acknowledge the updated notice
- A summary of changes will be provided

---

## 9. Contact

- **Privacy questions:** [GitHub Issues](https://github.com/selfemploy-uk/self-employment/issues)
- **Data protection complaints:** [Information Commissioner's Office (ICO)](https://ico.org.uk)
- **ICO Helpline:** 0303 123 1113

---

## Legal Basis Summary

| Processing Activity | Legal Basis |
|--------------------|-------------|
| Core functionality | Contract performance (Article 6(1)(b)) |
| HMRC fraud prevention | Legal obligation (Article 6(1)(c)) |
| Token storage | Legitimate interests (Article 6(1)(f)) |

---

*This privacy notice complies with UK GDPR Articles 13 and 14.*
