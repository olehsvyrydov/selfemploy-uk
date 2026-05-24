package uk.selfemploy.core.audit;

import uk.selfemploy.common.legal.SubmissionConfirmation;

import java.io.IOException;

/**
 * Append-only sink for Pre-Submission Confirmation audit records.
 *
 * <p>For every final declaration submitted
 * to HMRC, one line is written here containing the confirmation timestamp,
 * the user identifier, a salted SHA-256 hash of the NINO, and a SHA-256 of the
 * canonical submission tuple. Plaintext NINO and the raw HMRC payload are
 * never persisted.
 *
 * <p>Implementations must:
 * <ul>
 *   <li>Be append-only (no rewrites, no deletes) — line-appended JSONL is the
 *       canonical format used by {@link FileSystemDeclarationAuditLog}.</li>
 *   <li>Hash the NINO before writing (never accept or expose plaintext NINO
 *       on the wire format).</li>
 *   <li>Use ISO-8601 / RFC 3339 UTC timestamps.</li>
 * </ul>
 */
public interface DeclarationAuditLog {

    /**
     * Appends an audit record for a confirmed submission.
     *
     * @param confirmation        user confirmation captured at the gate
     * @param plaintextNino       plaintext NINO (will be hashed before write;
     *                            never persisted in plaintext)
     * @param taxYearLabel        tax year label, e.g. "2024-25"
     * @param calculationId       HMRC calculation identifier being declared
     * @throws IOException if the audit line cannot be written
     */
    void recordConfirmedSubmission(
            SubmissionConfirmation confirmation,
            String plaintextNino,
            String taxYearLabel,
            String calculationId
    ) throws IOException;
}
