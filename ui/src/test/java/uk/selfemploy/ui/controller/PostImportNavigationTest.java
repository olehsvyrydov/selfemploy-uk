package uk.selfemploy.ui.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit tests for post-import navigation flow between controllers.
 *
 * Tests the Java logic (getImportResultMessage, showImportSuccessBanner,
 * setNavigateToTransactionReview) without requiring the JavaFX toolkit.
 */
@DisplayName("Post-Import Navigation Tests")
class PostImportNavigationTest {

    @Nested
    @DisplayName("BankImportWizardController - getImportResultMessage()")
    class ImportResultMessageTests {

        @Test
        @DisplayName("should return null before any import has occurred")
        void shouldReturnNullBeforeImport() {
            BankImportWizardController controller = new BankImportWizardController();
            assertThat(controller.getImportResultMessage()).isNull();
        }
    }

    @Nested
    @DisplayName("TransactionReviewController - showImportSuccessBanner()")
    class ImportSuccessBannerTests {

        @Test
        @DisplayName("should handle null FXML fields without throwing NPE")
        void shouldHandleNullFxmlFieldsWithoutNpe() {
            TransactionReviewController controller = new TransactionReviewController();
            // FXML fields are null since we did not load FXML
            assertThatCode(() -> controller.showImportSuccessBanner("Test message"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should handle null message without throwing NPE")
        void shouldHandleNullMessageWithoutNpe() {
            TransactionReviewController controller = new TransactionReviewController();
            assertThatCode(() -> controller.showImportSuccessBanner(null))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("IncomeController - setNavigateToTransactionReview()")
    class IncomeControllerNavigationTests {

        @Test
        @DisplayName("should accept navigation callback without error")
        void shouldAcceptNavigationCallback() {
            IncomeController controller = new IncomeController();
            Consumer<String> callback = msg -> {};

            assertThatCode(() -> controller.setNavigateToTransactionReview(callback))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should accept null navigation callback")
        void shouldAcceptNullNavigationCallback() {
            IncomeController controller = new IncomeController();

            assertThatCode(() -> controller.setNavigateToTransactionReview(null))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("ExpenseController - setNavigateToTransactionReview()")
    class ExpenseControllerNavigationTests {

        @Test
        @DisplayName("should accept navigation callback without error")
        void shouldAcceptNavigationCallback() {
            ExpenseController controller = new ExpenseController();
            Consumer<String> callback = msg -> {};

            assertThatCode(() -> controller.setNavigateToTransactionReview(callback))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should accept null navigation callback")
        void shouldAcceptNullNavigationCallback() {
            ExpenseController controller = new ExpenseController();

            assertThatCode(() -> controller.setNavigateToTransactionReview(null))
                    .doesNotThrowAnyException();
        }
    }
}
