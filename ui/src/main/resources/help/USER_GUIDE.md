---
id: USER_GUIDE
title: User Guide
category: User Guide
---

## What this app does

UK Self-Employment Manager is a free, open-source desktop application for UK self-employed individuals. Use it to:

- **Track income** — record all business income with dates and descriptions
- **Track expenses** — log expenses categorised to match HMRC's SA103 form
- **Calculate tax** — real-time estimates of Income Tax and National Insurance
- **Submit to HMRC** — annual Self Assessment through the Making Tax Digital APIs

What makes it different: completely free, privacy-first (data stays on your computer), open source, and MTD-ready.

## Getting started

1. Complete the setup wizard with your name and UTR.
2. Select your tax year (it runs 6 April to 5 April).
3. Choose your business type (freelancer, sole trader, and so on).

After setup you can add income from the Dashboard or Income page, record expenses from the Expenses page, and view tax estimates on the Tax Summary page.

## Daily usage

- **Adding income** — Income → Add Income → enter date, amount, description and source.
- **Adding expenses** — Expenses → Add Expense → select the SA103 category → enter details.

Expense categories map to HMRC's SA103F boxes:

| Category | SA103F box |
|----------|------------|
| Cost of sales | Box 17 |
| Staff costs | Box 19 |
| Travel | Box 20 |
| Premises | Box 21 |
| Office costs | Box 23 |
| Professional fees | Box 28 |
| Other expenses | Box 30 |

## Bank statement import

Save time by importing transactions directly from your bank's CSV export instead of typing them in.

**Supported banks** — Barclays, HSBC, Lloyds, Nationwide, Starling, Monzo, Revolut, Santander, Metro Bank, and any other bank via manual column mapping.

**How to import**

1. Download a CSV statement from your online banking.
2. Open the Import Wizard from the Income or Expenses page.
3. Drag and drop your CSV file (or click Browse).
4. The app auto-detects your bank and maps the columns.
5. Preview transactions and assign or confirm categories.
6. Click Import to add them to your records.

Duplicate detection, category suggestions, business-vs-personal filtering and a full import history with undo are all built in. Always download the CSV format (not PDF or OFX) and review the suggested categories before confirming.

## Understanding the HMRC connection

This app uses OAuth 2.0 — a secure, industry-standard protocol:

1. Click **Connect & Verify** in Settings.
2. Your browser opens HMRC's official Government Gateway.
3. You log in with **your** credentials on HMRC's website.
4. HMRC asks whether to grant this app access.
5. You choose **Grant authority** and return to the app.

Your password is never shared with the app — you enter it only on HMRC's website, and the app receives only a limited-access token that HMRC controls.

## Security and privacy

**Local-only storage** — all data stays on your computer; there are no cloud servers and the app has no access to your data.

**Encryption** — your HMRC API credentials and National Insurance number are encrypted (AES-256-GCM). You can also protect the whole database with a passphrase, which encrypts all of your financial data at rest (SQLCipher, AES-256); without a passphrase the database is not encrypted.

When you submit, only your tax figures and the fraud-prevention headers HMRC requires are sent.

## Key deadlines

- Tax year ends: **5 April**
- Online return deadline: **31 January**
- Tax payment deadline: **31 January**
- Second payment on account: **31 July**

**Late-filing penalties**

- 1 day late: £100
- 3 months late: £10/day (up to £900)
- 6 months late: £300 or 5% of the tax owed
- 12 months late: £300 or up to 100% of the tax owed

---

This software is a record-keeping tool, not a substitute for professional advice. Tax calculations are estimates only — always verify with a qualified accountant.
