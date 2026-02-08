# Privacy Notice

**Version 2.0** | Effective: 8 February 2026

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
- **Tax calculations:** Computed tax liabilities based on your data
- **Receipt attachments:** Images or PDFs you attach to transactions
- **Unique Taxpayer Reference (UTR):** Your HMRC identifier

**Where stored:** Encrypted database on your local device only
**Retention:** You control retention; HMRC requires a minimum of 6 years (see Section 5)

### 2.2 Bank Statement Data (Stored Locally)

When you use the Bank Statement Import feature, the following data is extracted from your CSV bank statement files and stored locally:

**Data We Store:**

| Data Field | Purpose | Legal Justification |
|------------|---------|---------------------|
| Transaction date | Required for tax year allocation and SA103 reporting | Contract performance |
| Transaction amount | Required for income/expense calculation | Contract performance |
| Transaction description | Required for business categorisation | Contract performance |
| Account last 4 digits | Minimal identifier for audit trail purposes | Legitimate interests |

**Data We Do NOT Store:**

| Data Field | Reason for Exclusion |
|------------|---------------------|
| Full account number | Not necessary for tax preparation |
| Sort code | Not necessary for tax preparation |
| Account holder name | Not necessary for tax preparation |
| Running balance | Not necessary for tax preparation |
| Bank name or branch details | Not necessary for tax preparation |

**Data Minimisation:** In accordance with UK GDPR Article 5(1)(c), we only extract and store the minimum data fields necessary for tax preparation. All other data in your bank statement file is discarded during import.

**Original Files:** Your original CSV bank statement files are not retained by the Software after import. Only the extracted data fields listed above are stored in the encrypted local database. You should retain your original bank statements separately as part of your own record-keeping obligations.

**Categorisation Suggestions:** The Software may suggest expense categories and business/personal classifications for imported transactions. These suggestions are generated algorithmically and are not tax advice. You are solely responsible for verifying the accuracy of all categorisations before using them for HMRC submissions.

### 2.3 HMRC Connection Data (When You Connect)

When you connect to HMRC for Making Tax Digital submissions, the following data is collected and transmitted:

- **Device information:** Unique device ID (UUID), operating system, screen configuration
- **Network information:** Local IP addresses, MAC addresses, timezone
- **User information:** Operating system username
- **Authentication tokens:** Access and refresh tokens for HMRC API

**Why:** This is legally required by HMRC under the Finance Act 2022 and Transaction Monitoring Regulations. We cannot submit your tax returns without this data.

---

## 3. How We Use Your Data

| Purpose | Legal Basis (UK GDPR) |
|---------|----------------------|
| Calculate your tax liability | Contract performance (Article 6(1)(b)) |
| Import and categorise bank transactions | Contract performance (Article 6(1)(b)) |
| Submit returns to HMRC | Contract performance (Article 6(1)(b)) |
| Comply with HMRC fraud prevention | Legal obligation (Article 6(1)(c)) |
| Store HMRC access tokens | Legitimate interests (Article 6(1)(f)) |
| Maintain audit trail for imported transactions | Legitimate interests (Article 6(1)(f)) |

**Bank Statement Import Legal Basis:** We process your bank statement data because it is necessary for the performance of a contract with you -- specifically, to provide our accounting and tax preparation services. Importing bank transactions is a core function of the Software that enables you to efficiently record business income and expenses for HMRC Self Assessment purposes.

---

## 4. Data Storage and Security

**All your financial data, including imported bank transactions, is stored ONLY on your device.**

We implement the following security measures:

- **AES-256-GCM encryption** for the local database
- **Operating system credential storage** for authentication tokens
- **TLS 1.2+ encryption** for all communications with HMRC
- **SHA-256 transaction hashing** for duplicate detection during bank imports

We do **NOT**:

- Store your data on any remote servers
- Have access to your financial information
- Share your data with third parties (except HMRC when you initiate a submission)
- Use your data for analytics, advertising, or any other purpose
- Transmit your bank data anywhere other than your local encrypted database

---

## 5. Data Retention

| Data Type | Retention Period | Legal Basis |
|-----------|------------------|-------------|
| Financial records (income, expenses) | You decide (HMRC requires **6 years** minimum) | s.12B Taxes Management Act 1970 |
| Imported bank transactions | You decide (HMRC requires **6 years** minimum) | s.12B Taxes Management Act 1970 |
| HMRC tokens | Until you disconnect | Contract performance |
| Acknowledgment records | Indefinite (audit trail) | Legitimate interests |

**Your responsibility:** HMRC requires you to keep records for at least **6 years from the 31 January following the tax year** to which they relate. This is because HMRC may open a discovery assessment under s.29 Taxes Management Act 1970 for up to 6 years after the end of the relevant tax year (or up to 20 years in cases of deliberate loss under s.36 TMA 1970).

This application helps you meet this retention requirement, but the legal responsibility remains yours.

**Important:** If you delete records within the 6-year retention period, you may be unable to support your Self Assessment return in the event of an HMRC enquiry. Penalties for failure to keep adequate records can be up to **£3,000 per tax year** under s.12B(5) Taxes Management Act 1970.

---

## 6. Your Rights Under UK GDPR

| Right | How to Exercise |
|-------|-----------------|
| **Access** | Request a copy of your data (Export feature in Settings or Transaction Review) |
| **Rectification** | Correct inaccurate data (Edit records in the application) |
| **Erasure** | Delete your data (Delete records or uninstall application) |
| **Restriction** | Limit how data is processed (Contact us) |
| **Portability** | Receive data in portable format (Export to JSON/CSV from Transaction Review) |
| **Object** | Object to processing (Contact us) |
| **Complain** | Lodge complaint with ICO (ico.org.uk) |

**Note:** Because your data is stored locally on your device, you already have full control. You can view, edit, export, or delete any data at any time.

**Right to Erasure and Tax Record Retention:** You have the right to request deletion of your personal data under UK GDPR Article 17. However, UK tax law requires retention of financial records for a minimum of 6 years from the end of the relevant tax year (s.12B Taxes Management Act 1970). If you request deletion of bank transaction data that falls within this retention period, we recommend you retain alternative records to satisfy your HMRC obligations. The Software will delete the data as requested, but you should be aware that this may leave you without adequate records in the event of an HMRC enquiry.

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

**Version 2.0 Changes (February 2026):** Added Section 2.2 covering Bank Statement Import data processing, updated retention period from 5 years to 6 years throughout to align with s.29/s.36 Taxes Management Act 1970 discovery assessment windows, expanded Section 5 with detailed retention obligations and penalty warnings, added bank import legal basis to Section 3, added data export from Transaction Review to Section 6.

---

## 9. Contact

- **Privacy questions:** [GitHub Issues](https://github.com/selfemploy-uk/self-employment/issues)
- **Data protection complaints:** [Information Commissioner's Office (ICO)](https://ico.org.uk)
- **ICO Helpline:** 0303 123 1113

---

## Legal Basis Summary

| Processing Activity | Legal Basis |
|--------------------|-------------|
| Core functionality (tax calculations) | Contract performance (Article 6(1)(b)) |
| Bank statement import and categorisation | Contract performance (Article 6(1)(b)) |
| HMRC fraud prevention | Legal obligation (Article 6(1)(c)) |
| Token storage | Legitimate interests (Article 6(1)(f)) |
| Transaction audit trail | Legitimate interests (Article 6(1)(f)) |

---

*This privacy notice complies with UK GDPR Articles 13 and 14.*
