```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link JsonSchemaFactory#normalizeMetaSchemaUri(String)}.
 *
 * The method is {@code protected static}, therefore it can be accessed directly
 * from this test class because the test resides in the same package.
 *
 * The test cases are derived from the supplied JSON description and cover:
 * <ul>
 *   <li>Ids that are already known {@link SpecVersion.VersionFlag}s – they must be returned unchanged.</li>
 *   <li>Un‑normalized ids that contain the generic “json‑schema.org/draft” pattern – they must be mapped to the corresponding {@link SchemaId} constants.</li>
 *   <li>Ids that do not match any rule – they must be returned unchanged.</li>
 *   <li>Edge‑cases such as null, empty string, multiple draft substrings, overlapping patterns, case‑sensitivity, etc.</li>
 * </ul>
 */
class JsonSchemaFactory_normalizeMetaSchemaUri_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these pure‑function tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Nothing to initialise before each test.
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup needed.
    }

    /* ---------------------------------------------------------------------- */
    /*  Known VersionFlag ids – should be returned unchanged                  */
    /* ---------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("knownVersionFlagProvider")
    void whenIdIsVersionFlag_thenReturnedUnchanged(String input, String expected) {
        String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, actual, "A known VersionFlag id should be returned unchanged");
    }

    static Stream<Arguments> knownVersionFlagProvider() {
        return Stream.of(
                Arguments.of("http://json-schema.org/draft-07/schema#", "http://json-schema.org/draft-07/schema#"),
                Arguments.of("http://json-schema.org/draft-04/schema#", "http://json-schema.org/draft-04/schema#")
        );
    }

    /* ---------------------------------------------------------------------- */
    /*  Un‑normalized draft URIs that contain the json‑schema.org marker     */
    /* ---------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("unnormalizedDraftUriProvider")
    void whenIdIsUnnormalized_thenMappedToSchemaId(String input, String expected) {
        String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, actual,
                "Un‑normalized id containing the json-schema.org marker should be mapped to the corresponding SchemaId");
    }

    static Stream<Arguments> unnormalizedDraftUriProvider() {
        return Stream.of(
                // "/draft-07/" wins over later checks
                Arguments.of("http://json-schema.org/draft-07/schema#", SchemaId.V7),
                // "/draft/2019-09/" maps to V201909
                Arguments.of("https://json-schema.org/draft/2019-09/schema#", SchemaId.V201909),
                // "/draft/2020-12/" maps to V202012
                Arguments.of("http://json-schema.org/draft/2020-12/schema#", SchemaId.V202012),
                // "/draft-04/" maps to V4
                Arguments.of("http://json-schema.org/draft-04/schema#", SchemaId.V4),
                // "/draft-06/" maps to V6
                Arguments.of("http://json-schema.org/draft-06/schema#", SchemaId.V6)
        );
    }

    /* ---------------------------------------------------------------------- */
    /*  Ids that do not match any rule – should be returned unchanged         */
    /* ---------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("noNormalizationProvider")
    void whenIdDoesNotMatchAnyRule_thenReturnedUnchanged(String input) {
        String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(input, actual, "Id that does not match any rule should be returned unchanged");
    }

    static Stream<Arguments> noNormalizationProvider() {
        return Stream.of(
                Arguments.of("http://example.com/custom-schema"),
                Arguments.of("urn:uuid:1234-5678-90ab-cdef"),
                Arguments.of("https://json-schema.org/unknown/draft/schema"),
                Arguments.of("random-string")
        );
    }

    /* ---------------------------------------------------------------------- */
    /*  Null input – expect NullPointerException                              */
    /* ---------------------------------------------------------------------- */
    @Test
    void whenIdIsNull_thenThrowNullPointerException() {
        Executable exec = () -> JsonSchemaFactory.normalizeMetaSchemaUri(null);
        assertThrows(NullPointerException.class, exec,
                "Passing null should result in a NullPointerException");
    }

    /* ---------------------------------------------------------------------- */
    /*  Empty string – should be returned unchanged                           */
    /* ---------------------------------------------------------------------- */
    @Test
    void whenIdIsEmptyString_thenReturnedUnchanged() {
        String actual = JsonSchemaFactory.normalizeMetaSchemaUri("");
        assertEquals("", actual, "Empty string should be returned unchanged");
    }

    /* ---------------------------------------------------------------------- */
    /*  Multiple draft substrings – first matching branch wins               */
    /* ---------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("multipleDraftSubstringsProvider")
    void whenIdContainsMultipleDraftSubstrings_thenFirstMatchWins(String input, String expected) {
        String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, actual,
                "When multiple draft substrings are present, the first matching branch should win");
    }

    static Stream<Arguments> multipleDraftSubstringsProvider() {
        return Stream.of(
                // contains both /draft-07/ and /draft-04/, /draft-07/ is checked first
                Arguments.of("http://json-schema.org/draft-07/draft-04/schema#", SchemaId.V7)
        );
    }

    /* ---------------------------------------------------------------------- */
    /*  Overlapping draft patterns – order of checks matters                  */
    /* ---------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("overlappingDraftPatternsProvider")
    void whenIdHasOverlappingDraftPatterns_thenCorrectBranchChosen(String input, String expected) {
        String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, actual,
                "Overlapping patterns should be resolved according to the order of checks in the method");
    }

    static Stream<Arguments> overlappingDraftPatternsProvider() {
        return Stream.of(
                // "/draft/2019-09/" is checked before "/draft-04/"
                Arguments.of("http://json-schema.org/draft/2019-09/draft-04/schema#", SchemaId.V201909),
                // "/draft-07/" is checked before "/draft/2020-12/"
                Arguments.of("http://json-schema.org/draft/2020-12/draft-07/schema#", SchemaId.V7),
                // "/draft-04/" is checked before "/draft-06/"
                Arguments.of("http://json-schema.org/draft-04/draft-06/schema#", SchemaId.V4)
        );
    }

    /* ---------------------------------------------------------------------- */
    /*  Non‑matching draft patterns – missing required slashes               */
    /* ---------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("nonMatchingDraftPatternProvider")
    void whenIdDoesNotContainRequiredSlashPattern_thenReturnedUnchanged(String input) {
        String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(input, actual,
                "If the required '/draft-XX/' segment is missing, the id should stay unchanged");
    }

    static Stream<Arguments> nonMatchingDraftPatternProvider() {
        return Stream.of(
                Arguments.of("http://json-schema.org/draft07/schema#"),
                Arguments.of("http://json-schema.org/draft-07schema#")
        );
    }

    /* ---------------------------------------------------------------------- */
    /*  Case‑sensitivity – the marker check is case‑sensitive                */
    /* ---------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("caseSensitivityProvider")
    void whenIdHasDifferentCase_thenReturnedUnchanged(String input) {
        String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(input, actual,
                "Case‑sensitive check for the marker should prevent normalization when case differs");
    }

    static Stream<Arguments> caseSensitivityProvider() {
        return Stream.of(
                Arguments.of("http://JSON-SCHEMA.ORG/draft-07/schema#"),
                Arguments.of("HTTP://json-schema.org/draft-07/schema#")
        );
    }
}
```