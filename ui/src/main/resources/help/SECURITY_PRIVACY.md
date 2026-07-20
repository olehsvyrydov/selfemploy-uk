---
id: SECURITY_PRIVACY
title: Security & Privacy
category: User Guide
---

Your data privacy is the priority.

**Local-only storage**

- All data stays on **your** computer.
- The app has no access to your data.
- No cloud servers store your information.

**Encryption**

- Your HMRC API credentials and National Insurance number are encrypted (AES-256-GCM).
- Your financial data is stored in a local database on your device. You can protect it with a passphrase, which encrypts the whole database at rest (SQLCipher, AES-256). Without a passphrase the database is not encrypted. If you set a passphrase, keep it and your recovery code safe — there is no way to reset them.

**What is sent to HMRC**

- Your tax figures (income, expenses, profit).
- Fraud-prevention headers (required by law): device ID, OS, app version, timezone.

**No third-party sharing** — nothing is shared with advertisers, analytics, or tracking companies. Only HMRC receives data, and only when you submit.
