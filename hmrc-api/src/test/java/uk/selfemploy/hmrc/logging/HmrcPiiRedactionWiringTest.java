package uk.selfemploy.hmrc.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wiring contract for {@link HmrcPiiRedactor}: every {@code log.error} / {@code log.warn}
 * statement in the listed production sites that handles an exception message must
 * either reference {@code HmrcPiiRedactor.redact} on the same call <em>or</em> log the
 * exception object as a separate argument (Logback / SLF4J convention — does not
 * substitute the message into the formatted line).
 *
 * <p>Source-grep style invariant rather than runtime log capture, because runtime
 * SLF4J binding under the Quarkus test classpath is non-deterministic and the
 * regression we care about is structural: a future contributor replacing
 * {@code HmrcPiiRedactor.redact(e.getMessage())} with bare {@code e.getMessage()}
 * would compile and ship silently while leaking NINOs into production logs.
 * This test fails the build at refactor time.
 *
 * <p>If a new ERROR/WARN log site needs to handle exception messages, add the
 * source file path to {@link #REDACTOR_AWARE_SOURCE_FILES} and either redact
 * inline or use the throwable-argument convention.
 */
@DisplayName("HmrcPiiRedactor — wiring contract at production log sites")
class HmrcPiiRedactionWiringTest {

    /**
     * Production source files whose ERROR/WARN log statements MUST either invoke
     * {@code HmrcPiiRedactor.redact} when interpolating an exception message
     * into the log template, or pass the throwable as a separate trailing argument
     * (SLF4J/Logback convention — the message is not substituted).
     */
    private static final List<String> REDACTOR_AWARE_SOURCE_FILES = List.of(
        "server/src/main/java/uk/selfemploy/hmrc/resilience/HmrcResilienceDecorator.java",
        "server/src/main/java/uk/selfemploy/hmrc/client/HmrcHeaderFactory.java",
        "hmrc-api/src/main/java/uk/selfemploy/hmrc/oauth/DefaultTokenExchangeClient.java",
        "hmrc-api/src/main/java/uk/selfemploy/hmrc/fraud/FraudPreventionService.java"
    );

    /** Matches a log.error(...) or log.warn(...) call up to the first newline-terminated statement. */
    private static final Pattern LOG_ERROR_OR_WARN =
        Pattern.compile("log\\.(error|warn)\\([^;]*\\)");

    @Test
    @DisplayName("every ERROR/WARN log statement that interpolates an exception message uses the redactor")
    void everyExceptionInterpolatingLogIsRedacted() throws IOException {
        Path repoRoot = findRepoRoot();
        List<String> violations = new ArrayList<>();

        for (String relativePath : REDACTOR_AWARE_SOURCE_FILES) {
            Path sourceFile = repoRoot.resolve(relativePath);
            assertThat(sourceFile)
                .as("listed source file must exist — has it been moved or renamed?")
                .exists();

            String src = Files.readString(sourceFile);

            LOG_ERROR_OR_WARN.matcher(src).results().forEach(match -> {
                String stmt = match.group();
                boolean interpolatesExceptionMessage =
                    stmt.contains(".getMessage()") || stmt.contains("e.getCause()");

                if (!interpolatesExceptionMessage) {
                    return;  // log.error("plain literal", e) is fine; the exception is logged as a Throwable argument
                }

                boolean redacted = stmt.contains("HmrcPiiRedactor.redact(");
                if (!redacted) {
                    violations.add(relativePath + " :: " + collapse(stmt));
                }
            });
        }

        assertThat(violations)
            .as("Each ERROR/WARN log that interpolates an exception message into the template "
                + "must wrap that message in HmrcPiiRedactor.redact(...). If the listed offenders are "
                + "intentional (e.g. the exception message cannot carry HMRC PII), add a brief justification "
                + "comment AND list-exempt the file by removing it from REDACTOR_AWARE_SOURCE_FILES.")
            .isEmpty();
    }

    private static Path findRepoRoot() {
        Path cwd = Paths.get("").toAbsolutePath();
        while (cwd != null && !Files.exists(cwd.resolve(".git"))) {
            cwd = cwd.getParent();
        }
        if (cwd == null) {
            throw new IllegalStateException("could not locate repo root from CWD");
        }
        return cwd;
    }

    private static String collapse(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }
}
