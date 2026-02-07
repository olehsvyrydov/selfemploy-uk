package uk.selfemploy.core.bankimport;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.domain.ImportAudit;
import uk.selfemploy.common.enums.ImportAuditType;
import uk.selfemploy.persistence.repository.BankTransactionRepository;
import uk.selfemploy.persistence.repository.ImportAuditRepository;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

/**
 * Service for importing bank statements into the staging review workflow.
 *
 * <p>Unlike the legacy {@link CsvImportService} which imports directly to Income/Expense,
 * this service stages transactions as {@link BankTransaction} records with PENDING status
 * for manual review and categorization.</p>
 *
 * <p>Import flow:
 * <ol>
 *   <li>Auto-detect bank CSV format</li>
 *   <li>Parse CSV into ImportedTransaction records</li>
 *   <li>Detect duplicates via transaction hash</li>
 *   <li>Create BankTransaction staging records (PENDING)</li>
 *   <li>Create ImportAudit record for audit trail</li>
 * </ol>
 */
@ApplicationScoped
public class BankStatementImportService {

    static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;

    private final BankFormatDetector formatDetector;
    private final BankTransactionRepository bankTransactionRepository;
    private final ImportAuditRepository importAuditRepository;
    private final Clock clock;

    @Inject
    public BankStatementImportService(
            BankFormatDetector formatDetector,
            BankTransactionRepository bankTransactionRepository,
            ImportAuditRepository importAuditRepository,
            Clock clock) {
        this.formatDetector = formatDetector;
        this.bankTransactionRepository = bankTransactionRepository;
        this.importAuditRepository = importAuditRepository;
        this.clock = clock;
    }

    /**
     * Imports a bank statement CSV, staging transactions for review.
     *
     * @param businessId the business to import transactions for
     * @param csvFile    path to the CSV file
     * @param charset    character encoding of the file
     * @return import result with statistics
     * @throws CsvParseException if the file format is unrecognized or cannot be parsed
     */
    @Transactional
    public BankStatementImportResult importBankStatement(
            UUID businessId, Path csvFile, Charset charset) {
        validateFileSize(csvFile);

        BankCsvParser parser = formatDetector.detectFormat(csvFile, charset)
            .orElseThrow(() -> new CsvParseException(
                "Unknown CSV format. Please check the file format or use manual column mapping."));

        String bankName = parser.getBankName();
        String sourceFormatId = toSourceFormatId(bankName);
        Instant now = clock.instant();

        List<ImportedTransaction> allTransactions = parser.parse(csvFile, charset);

        // Detect duplicates against existing staged transactions and within the batch
        Set<String> seenHashes = new HashSet<>();
        List<ImportedTransaction> uniqueTransactions = new ArrayList<>();
        int duplicateCount = 0;

        for (ImportedTransaction tx : allTransactions) {
            String hash = tx.transactionHash();
            if (bankTransactionRepository.existsByHash(businessId, hash) || seenHashes.contains(hash)) {
                duplicateCount++;
            } else {
                uniqueTransactions.add(tx);
                seenHashes.add(hash);
            }
        }

        // Create ImportAudit first to get the audit ID
        String fileName = csvFile.getFileName().toString();
        String fileHash = computeFileHash(csvFile);
        ImportAudit audit = importAuditRepository.save(ImportAudit.create(
            businessId, now, fileName, fileHash,
            ImportAuditType.BANK_CSV,
            allTransactions.size(),
            uniqueTransactions.size(),
            duplicateCount,
            List.of()
        ));

        // Stage unique transactions as PENDING BankTransactions
        List<UUID> transactionIds = new ArrayList<>();
        for (ImportedTransaction tx : uniqueTransactions) {
            BankTransaction bankTx = BankTransaction.create(
                businessId,
                audit.id(),
                sourceFormatId,
                tx.date(),
                tx.amount(),
                tx.description(),
                null,  // accountLastFour - not available from CSV
                tx.reference(),
                tx.transactionHash(),
                now
            );
            bankTransactionRepository.save(bankTx);
            transactionIds.add(bankTx.id());
        }

        return new BankStatementImportResult(
            audit.id(),
            bankName,
            allTransactions.size(),
            uniqueTransactions.size(),
            duplicateCount,
            0
        );
    }

    /**
     * Converts a bank display name to a source format ID.
     * e.g., "Barclays" → "csv-barclays"
     */
    static String toSourceFormatId(String bankName) {
        return "csv-" + bankName.toLowerCase().replaceAll("\\s+", "-");
    }

    private void validateFileSize(Path csvFile) {
        try {
            long size = Files.size(csvFile);
            if (size > MAX_FILE_SIZE_BYTES) {
                throw new CsvParseException(String.format(
                    "File size (%d bytes) exceeds maximum allowed size (%d bytes)",
                    size, MAX_FILE_SIZE_BYTES));
            }
        } catch (IOException e) {
            throw new CsvParseException("Unable to read file: " + csvFile.getFileName(), e);
        }
    }

    private String computeFileHash(Path csvFile) {
        try {
            byte[] content = Files.readAllBytes(csvFile);
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            return bytesToHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        } catch (IOException e) {
            return null;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
