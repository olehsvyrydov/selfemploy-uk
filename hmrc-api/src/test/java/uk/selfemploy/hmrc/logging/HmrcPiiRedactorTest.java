package uk.selfemploy.hmrc.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HmrcPiiRedactor")
class HmrcPiiRedactorTest {

    static Stream<Arguments> redactionCases() {
        return Stream.of(
            Arguments.of("plain NINO",
                "Submission failed for AB123456C",
                "Submission failed for <NINO_REDACTED>"),
            Arguments.of("NINO with paperwork spacing",
                "User AB 12 34 56 C tried again",
                "User <NINO_REDACTED> tried again"),
            Arguments.of("UTR 10 digits",
                "Unique Tax Reference 1234567890 not recognised",
                "Unique Tax Reference <UTR_REDACTED> not recognised"),
            Arguments.of("two UTRs in one line",
                "primary=1111111111 secondary=2222222222",
                "primary=<UTR_REDACTED> secondary=<UTR_REDACTED>"),
            Arguments.of("Bearer token",
                "Authorization: Bearer eyJhbGc.payload.sig",
                "Authorization: Bearer <REDACTED>"),
            Arguments.of("Bearer case-insensitive",
                "Authorization: bearer abc-DEF_123.456",
                "Authorization: Bearer <REDACTED>"),
            Arguments.of("UUID calculation id",
                "calculationId=4e9c8e6e-1234-4abc-9def-0123456789ab failed",
                "calculationId=<ID_REDACTED> failed"),
            Arguments.of("mixed payload",
                "NINO=AB123456C UTR=1234567890 calc=4e9c8e6e-1234-4abc-9def-0123456789ab",
                "NINO=<NINO_REDACTED> UTR=<UTR_REDACTED> calc=<ID_REDACTED>"),
            Arguments.of("null safe",
                null,
                "null"),
            Arguments.of("benign 9-digit number is left alone",
                "code 123456789 is not a UTR",
                "code 123456789 is not a UTR"),
            Arguments.of("benign 11-digit number is left alone",
                "phone 12345678901 stays",
                "phone 12345678901 stays")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("redactionCases")
    @DisplayName("redacts UK tax PII patterns")
    void redactsPiiPatterns(String label, String input, String expected) {
        assertThat(HmrcPiiRedactor.redact(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("never leaks a NINO into the output even when surrounded by punctuation")
    void neverLeaksNinoWithPunctuation() {
        String redacted = HmrcPiiRedactor.redact("[AB123456C]");
        assertThat(redacted).doesNotContain("AB123456C");
        assertThat(redacted).contains("<NINO_REDACTED>");
    }

    @Test
    @DisplayName("preserves non-PII context so logs remain useful")
    void preservesNonPiiContext() {
        String redacted = HmrcPiiRedactor.redact(
            "HTTP 400 from /individuals/business/self-employment for AB123456C — INVALID_NINO");
        assertThat(redacted)
            .contains("HTTP 400")
            .contains("INVALID_NINO")
            .contains("<NINO_REDACTED>")
            .doesNotContain("AB123456C");
    }

    @Test
    @DisplayName("lowercase NINO is deliberately NOT redacted — HMRC NINO format is uppercase only")
    void lowercaseNinoIsNotRedacted() {
        // Documents a deliberate non-coverage: HMRC's NINO format is strictly uppercase
        // (statutory definition; case is part of the format). A lowercase string that
        // looks like a NINO is by definition not a NINO and is not redacted. If a
        // hostile / badly-formatted body ever needs this redacted, normalise to upper
        // before passing to the redactor.
        String input = "user input ab123456c (lowercase, not a real NINO format)";
        assertThat(HmrcPiiRedactor.redact(input))
            .as("lowercase NINO-shaped string survives redaction")
            .isEqualTo(input);
    }

    @Test
    @DisplayName("10-digit Unix epoch timestamp is treated as a UTR — over-eager by design")
    void tenDigitUnixEpochIsRedactedAsUtr() {
        // Documents the deliberate over-redaction trade-off: \b\d{10}\b matches any
        // 10-digit token including Unix-epoch-seconds (~year 2001-2286). The redactor
        // prefers false positives over false negatives for ERROR/WARN-level log output.
        // Callers logging numeric IDs should structure them so they don't collide
        // (e.g. ISO-8601 timestamps instead of epoch-seconds).
        String input = "event at 1716508800 (epoch seconds, not a UTR)";
        assertThat(HmrcPiiRedactor.redact(input))
            .as("10-digit number is redacted regardless of true semantics")
            .isEqualTo("event at <UTR_REDACTED> (epoch seconds, not a UTR)");
    }
}
