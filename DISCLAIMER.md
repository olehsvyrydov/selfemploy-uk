# Disclaimer

## Important Notice

This software is provided "as is" under the Apache License 2.0, without warranty of any kind, express or implied. **UK Self-Employment Manager is not a substitute for professional tax, legal, or financial advice.**

## Tax Calculations

This software provides tax calculation tools based on published HMRC rates and thresholds. While we strive for accuracy, all calculations are **estimates only** and may not reflect your actual tax liability. You should verify all figures with a qualified accountant or tax advisor before making any financial decisions or submitting to HMRC.

## HMRC Submissions

By using this software to submit data to HMRC via Making Tax Digital (MTD), you confirm that the information provided is accurate and complete to the best of your knowledge. **You are solely responsible for the accuracy of all submitted data.** Submissions to HMRC may have legal and financial consequences.

## Bank Statement Import

The automatic categorisation suggestions provided by this software are for convenience only and do not constitute tax, accounting, or financial advice. You are solely responsible for verifying the accuracy of all categorisations before submitting to HMRC. Incorrect categorisation may result in HMRC penalties.

## Record Keeping

HMRC requires you to retain financial records for at least 6 years from the 31 January following the tax year. Deleting imported data within this period may leave you without adequate records in the event of an HMRC enquiry. Penalties for inadequate records can be up to £3,000 per tax year.

## Consumer Rights Act 2015

Under the Consumer Rights Act 2015, digital content must be of satisfactory quality. As this is free, open-source software provided under the Apache License 2.0, the statutory remedies for defects differ from paid software.

## Recommendations

We strongly recommend:

- Reviewing all calculations before submission
- Keeping your own records to verify figures
- Consulting a qualified accountant for complex situations

## Limitation of Liability

To the maximum extent permitted by applicable law, in no event shall the authors or copyright holders be liable for any claim, damages, or other liability arising from the use of this software. See the Apache License 2.0 for full terms.

---

## HMRC Individual Calculations v8 — Calculation Outputs (SLFEMPUK-35)

Outputs of Individual Calculations v8 (including `transitionProfit`, crypto CGT, BADR multi-asset) are unaudited estimates. User remains the taxpayer under TMA 1970 s.7 and bears sole liability for accuracy under FA 2007 Sch.24 penalties (up to 100% of tax + interest).

This software implements a Pre-Submission Confirmation gate before any final declaration is transmitted to HMRC. Submission cannot proceed until you have explicitly confirmed that the figures are accurate. The confirmation timestamp, your user identifier, a salted SHA-256 hash of your NINO, and a SHA-256 of the submitted tuple are recorded locally in an append-only audit log for evidential purposes (under FA 2009 Sch.55 enquiry conditions). Plaintext NINO and the raw HMRC payload are never persisted in the audit log.

---

By using this software, you acknowledge that tax calculations are estimates and that you bear responsibility for the accuracy of any submissions made to HMRC.
