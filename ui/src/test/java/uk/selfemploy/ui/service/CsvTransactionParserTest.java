package uk.selfemploy.ui.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.ui.viewmodel.BankFormat;
import uk.selfemploy.ui.viewmodel.ColumnMapping;
import uk.selfemploy.ui.viewmodel.ImportedTransactionRow;
import uk.selfemploy.ui.viewmodel.TransactionType;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CsvTransactionParser")
class CsvTransactionParserTest {

    @TempDir
    Path tempDir;

    private CsvTransactionParser parser;

    @BeforeEach
    void setUp() {
        parser = new CsvTransactionParser();
    }

    private Path createCsvFile(String content) throws IOException {
        Path file = tempDir.resolve("test.csv");
        Files.writeString(file, content);
        return file;
    }

    @Nested
    @DisplayName("Single amount column")
    class SingleAmountColumn {

        @Test
        @DisplayName("parses income (positive amount)")
        void parsesIncome() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Amount\n" +
                "15/06/2025,Client Payment,1500.00\n"
            );

            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");
            mapping.setAmountColumn("Amount");
            mapping.setDateFormat("dd/MM/yyyy");

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions()).hasSize(1);
            ImportedTransactionRow row = result.transactions().get(0);
            assertThat(row.date()).isEqualTo(LocalDate.of(2025, 6, 15));
            assertThat(row.description()).isEqualTo("Client Payment");
            assertThat(row.amount()).isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(row.type()).isEqualTo(TransactionType.INCOME);
        }

        @Test
        @DisplayName("parses expense (negative amount)")
        void parsesExpense() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Amount\n" +
                "20/06/2025,Office Supplies,-45.99\n"
            );

            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");
            mapping.setAmountColumn("Amount");
            mapping.setDateFormat("dd/MM/yyyy");

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions()).hasSize(1);
            ImportedTransactionRow row = result.transactions().get(0);
            assertThat(row.amount()).isEqualByComparingTo(new BigDecimal("45.99"));
            assertThat(row.type()).isEqualTo(TransactionType.EXPENSE);
        }

        @Test
        @DisplayName("parses multiple rows")
        void parsesMultipleRows() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Amount\n" +
                "15/06/2025,Client Payment,1500.00\n" +
                "16/06/2025,Office Supplies,-45.99\n" +
                "17/06/2025,Consulting Fee,3000.00\n"
            );

            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");
            mapping.setAmountColumn("Amount");
            mapping.setDateFormat("dd/MM/yyyy");

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions()).hasSize(3);
            assertThat(result.warnings()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Separate income/expense columns")
    class SeparateAmountColumns {

        @Test
        @DisplayName("parses income from income column")
        void parsesIncomeColumn() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Money out,Money in,Balance\n" +
                "15/06/2025,Client Payment,,1500.00,2500.00\n"
            );

            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");
            mapping.setSeparateAmountColumns(true);
            mapping.setIncomeColumn("Money in");
            mapping.setExpenseColumn("Money out");
            mapping.setDateFormat("dd/MM/yyyy");

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions()).hasSize(1);
            ImportedTransactionRow row = result.transactions().get(0);
            assertThat(row.amount()).isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(row.type()).isEqualTo(TransactionType.INCOME);
        }

        @Test
        @DisplayName("parses expense from expense column")
        void parsesExpenseColumn() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Money out,Money in,Balance\n" +
                "20/06/2025,Office Depot,45.99,,1454.01\n"
            );

            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");
            mapping.setSeparateAmountColumns(true);
            mapping.setIncomeColumn("Money in");
            mapping.setExpenseColumn("Money out");
            mapping.setDateFormat("dd/MM/yyyy");

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions()).hasSize(1);
            ImportedTransactionRow row = result.transactions().get(0);
            assertThat(row.amount()).isEqualByComparingTo(new BigDecimal("45.99"));
            assertThat(row.type()).isEqualTo(TransactionType.EXPENSE);
        }

        @Test
        @DisplayName("handles mixed income and expense rows")
        void handlesMixedRows() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Money out,Money in,Balance\n" +
                "15/06/2025,Client Payment,,1500.00,2500.00\n" +
                "16/06/2025,Office Depot,45.99,,2454.01\n" +
                "17/06/2025,Refund,,25.00,2479.01\n"
            );

            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");
            mapping.setSeparateAmountColumns(true);
            mapping.setIncomeColumn("Money in");
            mapping.setExpenseColumn("Money out");
            mapping.setDateFormat("dd/MM/yyyy");

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions()).hasSize(3);
            assertThat(result.transactions().get(0).type()).isEqualTo(TransactionType.INCOME);
            assertThat(result.transactions().get(1).type()).isEqualTo(TransactionType.EXPENSE);
            assertThat(result.transactions().get(2).type()).isEqualTo(TransactionType.INCOME);
        }
    }

    @Nested
    @DisplayName("Date format handling")
    class DateFormats {

        @Test
        @DisplayName("parses dd/MM/yyyy format")
        void parsesSlashFormat() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Amount\n" +
                "25/12/2025,Christmas Bonus,500.00\n"
            );

            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");
            mapping.setAmountColumn("Amount");
            mapping.setDateFormat("dd/MM/yyyy");

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions().get(0).date())
                .isEqualTo(LocalDate.of(2025, 12, 25));
        }

        @Test
        @DisplayName("parses yyyy-MM-dd format")
        void parsesIsoFormat() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Amount\n" +
                "2025-06-15,Payment,1000.00\n"
            );

            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");
            mapping.setAmountColumn("Amount");
            mapping.setDateFormat("yyyy-MM-dd");

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions().get(0).date())
                .isEqualTo(LocalDate.of(2025, 6, 15));
        }

        @Test
        @DisplayName("parses d MMM yyyy format")
        void parsesMediumFormat() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Amount\n" +
                "5 Jun 2025,Payment,1000.00\n"
            );

            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");
            mapping.setAmountColumn("Amount");
            mapping.setDateFormat("d MMM yyyy");

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions().get(0).date())
                .isEqualTo(LocalDate.of(2025, 6, 5));
        }

        @Test
        @DisplayName("parses dd-MM-yyyy format")
        void parsesDashFormat() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Amount\n" +
                "15-06-2025,Payment,1000.00\n"
            );

            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");
            mapping.setAmountColumn("Amount");
            mapping.setDateFormat("dd-MM-yyyy");

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions().get(0).date())
                .isEqualTo(LocalDate.of(2025, 6, 15));
        }
    }

    @Nested
    @DisplayName("Error tolerance")
    class ErrorTolerance {

        @Test
        @DisplayName("skips malformed date rows with warning")
        void skipsMalformedDate() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Amount\n" +
                "INVALID,Payment,1000.00\n" +
                "15/06/2025,Valid Payment,500.00\n"
            );

            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");
            mapping.setAmountColumn("Amount");
            mapping.setDateFormat("dd/MM/yyyy");

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions()).hasSize(1);
            assertThat(result.transactions().get(0).description()).isEqualTo("Valid Payment");
            assertThat(result.warnings()).hasSize(1);
            assertThat(result.warnings().get(0)).contains("line 2");
        }

        @Test
        @DisplayName("skips malformed amount rows with warning")
        void skipsMalformedAmount() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Amount\n" +
                "15/06/2025,Bad Amount,NOT_A_NUMBER\n" +
                "16/06/2025,Good Amount,500.00\n"
            );

            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");
            mapping.setAmountColumn("Amount");
            mapping.setDateFormat("dd/MM/yyyy");

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions()).hasSize(1);
            assertThat(result.warnings()).hasSize(1);
        }

        @Test
        @DisplayName("skips blank lines")
        void skipsBlankLines() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Amount\n" +
                "15/06/2025,Payment One,500.00\n" +
                "\n" +
                "16/06/2025,Payment Two,600.00\n" +
                "\n"
            );

            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");
            mapping.setAmountColumn("Amount");
            mapping.setDateFormat("dd/MM/yyyy");

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions()).hasSize(2);
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("warns when required column not found in headers")
        void warnsOnMissingColumn() throws IOException {
            Path csv = createCsvFile(
                "Date,Desc,Amount\n" +
                "15/06/2025,Payment,500.00\n"
            );

            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");  // Does not exist in CSV
            mapping.setAmountColumn("Amount");
            mapping.setDateFormat("dd/MM/yyyy");

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions()).isEmpty();
            assertThat(result.warnings()).isNotEmpty();
            assertThat(result.warnings().get(0)).containsIgnoringCase("Description");
        }

        @Test
        @DisplayName("skips rows where both income and expense columns are empty")
        void skipsRowsWithNoAmount() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Money out,Money in\n" +
                "15/06/2025,Empty Row,,\n" +
                "16/06/2025,Valid Income,,500.00\n"
            );

            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");
            mapping.setSeparateAmountColumns(true);
            mapping.setIncomeColumn("Money in");
            mapping.setExpenseColumn("Money out");
            mapping.setDateFormat("dd/MM/yyyy");

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions()).hasSize(1);
            assertThat(result.warnings()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Amount cleaning")
    class AmountCleaning {

        @Test
        @DisplayName("strips currency symbol")
        void stripsCurrencySymbol() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Amount\n" +
                "15/06/2025,Payment,\u00a31500.00\n"
            );

            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");
            mapping.setAmountColumn("Amount");
            mapping.setDateFormat("dd/MM/yyyy");

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions().get(0).amount())
                .isEqualByComparingTo(new BigDecimal("1500.00"));
        }

        @Test
        @DisplayName("strips thousand separators")
        void stripsThousandSeparators() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Amount\n" +
                "15/06/2025,Big Payment,\"1,500.00\"\n"
            );

            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");
            mapping.setAmountColumn("Amount");
            mapping.setDateFormat("dd/MM/yyyy");

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions().get(0).amount())
                .isEqualByComparingTo(new BigDecimal("1500.00"));
        }

        @Test
        @DisplayName("handles GBP prefix")
        void handlesGbpPrefix() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Amount\n" +
                "15/06/2025,Payment,GBP 500.00\n"
            );

            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");
            mapping.setAmountColumn("Amount");
            mapping.setDateFormat("dd/MM/yyyy");

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions().get(0).amount())
                .isEqualByComparingTo(new BigDecimal("500.00"));
        }
    }

    @Nested
    @DisplayName("Quoted CSV fields")
    class QuotedFields {

        @Test
        @DisplayName("handles quoted description with commas")
        void handlesQuotedDescription() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Amount\n" +
                "15/06/2025,\"ACME Corp, Inc.\",1500.00\n"
            );

            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");
            mapping.setAmountColumn("Amount");
            mapping.setDateFormat("dd/MM/yyyy");

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions().get(0).description())
                .isEqualTo("ACME Corp, Inc.");
        }

        @Test
        @DisplayName("handles escaped quotes in fields")
        void handlesEscapedQuotes() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Amount\n" +
                "15/06/2025,\"Payment for \"\"Project X\"\"\",1500.00\n"
            );

            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");
            mapping.setAmountColumn("Amount");
            mapping.setDateFormat("dd/MM/yyyy");

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions().get(0).description())
                .isEqualTo("Payment for \"Project X\"");
        }
    }

    @Nested
    @DisplayName("Bank format presets")
    class BankFormatPresets {

        @Test
        @DisplayName("Barclays format")
        void barclaysFormat() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Money out,Money in,Balance\n" +
                "15/06/2025,ACME CORP,,,2500.00\n" +
                "16/06/2025,OFFICE DEPOT,45.99,,2454.01\n" +
                "17/06/2025,CLIENT PAYMENT,,3000.00,5454.01\n"
            );

            ColumnMapping mapping = ColumnMapping.forBankFormat(BankFormat.BARCLAYS);

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            // First row has no amount in either column - should be skipped
            assertThat(result.transactions()).hasSize(2);
            assertThat(result.transactions().get(0).type()).isEqualTo(TransactionType.EXPENSE);
            assertThat(result.transactions().get(0).amount()).isEqualByComparingTo("45.99");
            assertThat(result.transactions().get(1).type()).isEqualTo(TransactionType.INCOME);
            assertThat(result.transactions().get(1).amount()).isEqualByComparingTo("3000.00");
        }

        @Test
        @DisplayName("Starling format with single amount column")
        void starlingFormat() throws IOException {
            Path csv = createCsvFile(
                "Date,Counter Party,Reference,Type,Amount (GBP),Balance (GBP)\n" +
                "15/06/2025,Amazon,REF123,CARD,-29.99,970.01\n" +
                "16/06/2025,Employer,SALARY,FPS,2500.00,3470.01\n"
            );

            ColumnMapping mapping = ColumnMapping.forBankFormat(BankFormat.STARLING);

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions()).hasSize(2);
            assertThat(result.transactions().get(0).type()).isEqualTo(TransactionType.EXPENSE);
            assertThat(result.transactions().get(0).description()).isEqualTo("Amazon");
            assertThat(result.transactions().get(1).type()).isEqualTo(TransactionType.INCOME);
        }

        @Test
        @DisplayName("Santander format")
        void santanderFormat() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Amount,Balance\n" +
                "15/06/2025,DIRECT DEBIT,-89.00,911.00\n" +
                "16/06/2025,FASTER PAYMENT,1500.00,2411.00\n"
            );

            ColumnMapping mapping = ColumnMapping.forBankFormat(BankFormat.SANTANDER);

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions()).hasSize(2);
            assertThat(result.transactions().get(0).type()).isEqualTo(TransactionType.EXPENSE);
            assertThat(result.transactions().get(1).type()).isEqualTo(TransactionType.INCOME);
        }

        @Test
        @DisplayName("Lloyds format with separate debit/credit columns")
        void lloydsFormat() throws IOException {
            Path csv = createCsvFile(
                "Transaction Date,Transaction Description,Debit Amount,Credit Amount\n" +
                "15/06/2025,AMAZON.CO.UK,29.99,\n" +
                "16/06/2025,CLIENT TRANSFER,,5000.00\n"
            );

            ColumnMapping mapping = ColumnMapping.forBankFormat(BankFormat.LLOYDS);

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions()).hasSize(2);
            assertThat(result.transactions().get(0).type()).isEqualTo(TransactionType.EXPENSE);
            assertThat(result.transactions().get(0).amount()).isEqualByComparingTo("29.99");
            assertThat(result.transactions().get(1).type()).isEqualTo(TransactionType.INCOME);
            assertThat(result.transactions().get(1).amount()).isEqualByComparingTo("5000.00");
        }

        @Test
        @DisplayName("Monzo format with category")
        void monzoFormat() throws IOException {
            Path csv = createCsvFile(
                "Transaction ID,Date,Time,Type,Name,Emoji,Category,Amount\n" +
                "tx_001,15/06/2025,10:30:00,Card,Amazon,,Shopping,-29.99\n" +
                "tx_002,16/06/2025,09:00:00,FPS,Employer,,Income,2500.00\n"
            );

            ColumnMapping mapping = ColumnMapping.forBankFormat(BankFormat.MONZO);

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions()).hasSize(2);
            assertThat(result.transactions().get(0).description()).isEqualTo("Amazon");
            assertThat(result.transactions().get(0).type()).isEqualTo(TransactionType.EXPENSE);
            assertThat(result.transactions().get(1).type()).isEqualTo(TransactionType.INCOME);
        }

        @Test
        @DisplayName("HSBC format")
        void hsbcFormat() throws IOException {
            Path csv = createCsvFile(
                "Date,Type,Paid out,Paid in,Balance\n" +
                "15/06/2025,DD,89.00,,911.00\n" +
                "16/06/2025,FPS,,1500.00,2411.00\n"
            );

            ColumnMapping mapping = ColumnMapping.forBankFormat(BankFormat.HSBC);

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions()).hasSize(2);
            assertThat(result.transactions().get(0).type()).isEqualTo(TransactionType.EXPENSE);
            assertThat(result.transactions().get(1).type()).isEqualTo(TransactionType.INCOME);
        }

        @Test
        @DisplayName("Metro Bank format")
        void metroBankFormat() throws IOException {
            Path csv = createCsvFile(
                "Date,Transaction type,Description,Money out,Money in,Balance\n" +
                "15/06/2025,DD,Sky Broadband,42.50,,957.50\n" +
                "16/06/2025,FPS,Client A,,2000.00,2957.50\n"
            );

            ColumnMapping mapping = ColumnMapping.forBankFormat(BankFormat.METRO_BANK);

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions()).hasSize(2);
            assertThat(result.transactions().get(0).description()).isEqualTo("Sky Broadband");
            assertThat(result.transactions().get(0).type()).isEqualTo(TransactionType.EXPENSE);
            assertThat(result.transactions().get(1).type()).isEqualTo(TransactionType.INCOME);
        }
    }

    @Nested
    @DisplayName("Transaction row defaults")
    class TransactionRowDefaults {

        @Test
        @DisplayName("sets null category for uncategorized transactions")
        void uncategorizedByDefault() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Amount\n" +
                "15/06/2025,Payment,500.00\n"
            );

            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");
            mapping.setAmountColumn("Amount");
            mapping.setDateFormat("dd/MM/yyyy");

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions().get(0).category()).isNull();
            assertThat(result.transactions().get(0).isDuplicate()).isFalse();
            assertThat(result.transactions().get(0).confidence()).isZero();
        }

        @Test
        @DisplayName("generates unique IDs for each row")
        void generatesUniqueIds() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Amount\n" +
                "15/06/2025,Payment One,500.00\n" +
                "16/06/2025,Payment Two,600.00\n"
            );

            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");
            mapping.setAmountColumn("Amount");
            mapping.setDateFormat("dd/MM/yyyy");

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions().get(0).id())
                .isNotEqualTo(result.transactions().get(1).id());
        }
    }

    @Nested
    @DisplayName("Empty and edge cases")
    class EdgeCases {

        @Test
        @DisplayName("returns empty result for header-only file")
        void headerOnlyFile() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Amount\n"
            );

            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");
            mapping.setAmountColumn("Amount");
            mapping.setDateFormat("dd/MM/yyyy");

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions()).isEmpty();
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("handles zero amount as expense")
        void zeroAmountIsExpense() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Amount\n" +
                "15/06/2025,Zero Transaction,0.00\n"
            );

            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");
            mapping.setAmountColumn("Amount");
            mapping.setDateFormat("dd/MM/yyyy");

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions()).hasSize(1);
            // Zero is treated as expense (non-positive)
            assertThat(result.transactions().get(0).type()).isEqualTo(TransactionType.EXPENSE);
        }

        @Test
        @DisplayName("trims whitespace from field values")
        void trimsWhitespace() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Amount\n" +
                " 15/06/2025 , Payment With Spaces , 500.00 \n"
            );

            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");
            mapping.setAmountColumn("Amount");
            mapping.setDateFormat("dd/MM/yyyy");

            CsvTransactionParser.ParseResult result = parser.parse(csv, mapping);

            assertThat(result.transactions().get(0).description()).isEqualTo("Payment With Spaces");
            assertThat(result.transactions().get(0).date()).isEqualTo(LocalDate.of(2025, 6, 15));
        }
    }
}
