package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.stream.Stream;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JsonSchemaFactory_normalizeMetaSchemaUri_Test {

    static Stream<Arguments> knownVersionFlagProvider() {
        return Stream.of(Arguments.of("http://json-schema.org/draft-07/schema#", "http://json-schema.org/draft-07/schema#"), Arguments.of("http://json-schema.org/draft-04/schema#", "http://json-schema.org/draft-04/schema#"));
    }

    static Stream<Arguments> unnormalizedDraftUriProvider() {
        return Stream.of(// "/draft-07/" wins over later checks
        Arguments.of("http://json-schema.org/draft-07/schema#", SchemaId.V7), // "/draft/2019-09/" maps to V201909
        Arguments.of("https://json-schema.org/draft/2019-09/schema#", SchemaId.V201909), // "/draft/2020-12/" maps to V202012
        Arguments.of("http://json-schema.org/draft/2020-12/schema#", SchemaId.V202012), // "/draft-04/" maps to V4
        Arguments.of("http://json-schema.org/draft-04/schema#", SchemaId.V4), // "/draft-06/" maps to V6
        Arguments.of("http://json-schema.org/draft-06/schema#", SchemaId.V6));
    }

    static Stream<Arguments> noNormalizationProvider() {
        return Stream.of(Arguments.of("http://example.com/custom-schema"), Arguments.of("urn:uuid:1234-5678-90ab-cdef"), Arguments.of("https://json-schema.org/unknown/draft/schema"), Arguments.of("random-string"));
    }

    static Stream<Arguments> multipleDraftSubstringsProvider() {
        return Stream.of(// contains both /draft-07/ and /draft-04/, /draft-07/ is checked first
        Arguments.of("http://json-schema.org/draft-07/draft-04/schema#", SchemaId.V7));
    }

    static Stream<Arguments> overlappingDraftPatternsProvider() {
        return Stream.of(// "/draft/2019-09/" is checked before "/draft-04/"
        Arguments.of("http://json-schema.org/draft/2019-09/draft-04/schema#", SchemaId.V201909), // "/draft-07/" is checked before "/draft/2020-12/"
        Arguments.of("http://json-schema.org/draft/2020-12/draft-07/schema#", SchemaId.V7), // "/draft-04/" is checked before "/draft-06/"
        Arguments.of("http://json-schema.org/draft-04/draft-06/schema#", SchemaId.V4));
    }

    static Stream<Arguments> nonMatchingDraftPatternProvider() {
        return Stream.of(Arguments.of("http://json-schema.org/draft07/schema#"), Arguments.of("http://json-schema.org/draft-07schema#"));
    }

    static Stream<Arguments> caseSensitivityProvider() {
        return Stream.of(Arguments.of("http://JSON-SCHEMA.ORG/draft-07/schema#"), Arguments.of("HTTP://json-schema.org/draft-07/schema#"));
    }

    /**
     * When the supplied id matches one of the {@link SpecVersion.VersionFlag} ids,
     * the method must return the original value unchanged.
     */
    @ParameterizedTest
    @CsvSource({ // The concrete ids are obtained from the enum values.
    // They are written explicitly to keep the test independent from the enum implementation.
    "'http://json-schema.org/draft-04/schema', http://json-schema.org/draft-04/schema", "'http://json-schema.org/draft-06/schema', http://json-schema.org/draft-06/schema", "'http://json-schema.org/draft-07/schema', http://json-schema.org/draft-07/schema", "'https://json-schema.org/draft/2019-09/schema', https://json-schema.org/draft/2019-09/schema", "'https://json-schema.org/draft/2020-12/schema', https://json-schema.org/draft/2020-12/schema" })
    void whenIdIsVersionFlag_thenReturnedUnchanged(String input, String expected) {
        String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, actual, "A known VersionFlag id should be returned unchanged");
    }

    /**
     * Un‑normalized ids that contain the generic “json‑schema.org/draft” pattern
     * must be normalised to the appropriate {@link SchemaId} constant.
     */
    @ParameterizedTest
    @CsvSource({ // format: unnormalizedId, expectedNormalizedId
    "'http://example.com/some/path/draft-07/schema.json', " + SchemaId.V7, "'https://example.org/any/draft/2019-09/whatever', " + SchemaId.V201909, "'https://foo.bar/draft/2020-12/bar.json', " + SchemaId.V202012, "'http://myhost/draft-04/schema', " + SchemaId.V4, "'https://host/draft-06/schema', " + SchemaId.V6 })
    void whenIdIsUnnormalized_thenMappedToSchemaId(String input, String expected) {
        String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, actual, "Un‑normalized id containing the json-schema.org marker should be mapped to the corresponding SchemaId");
    }

    /**
     * Ids that do not match any rule must be returned unchanged.
     */
    @Test
    void whenIdDoesNotMatchAnyRule_thenReturnedUnchanged() {
        String[] inputs = { "http://example.com/custom-schema", "urn:uuid:1234-5678-90ab-cdef", "https://json-schema.org/unknown/draft/schema", "random-string" };
        for (String input : inputs) {
            String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
            assertEquals(input, actual, "Id that does not match any rule should be returned unchanged");
        }
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void whenIdIsNull_thenThrowNullPointerException() {
        Executable exec = () -> JsonSchemaFactory.normalizeMetaSchemaUri(null);
        assertThrows(NullPointerException.class, exec, "Passing null should result in a NullPointerException");
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void whenIdIsEmptyString_thenReturnedUnchanged() {
        String actual = JsonSchemaFactory.normalizeMetaSchemaUri("");
        assertEquals("", actual, "Empty string should be returned unchanged");
    }

    /* ---------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("multipleDraftSubstringsProvider")
    void whenIdContainsMultipleDraftSubstrings_thenFirstMatchWins(String input, String expected) {
        String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, actual, "When multiple draft substrings are present, the first matching branch should win");
    }

    /* ---------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("overlappingDraftPatternsProvider")
    void whenIdHasOverlappingDraftPatterns_thenCorrectBranchChosen(String input, String expected) {
        String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, actual, "Overlapping patterns should be resolved according to the order of checks in the method");
    }

    /* ---------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("nonMatchingDraftPatternProvider")
    void whenIdDoesNotContainRequiredSlashPattern_thenReturnedUnchanged(String input) {
        String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(input, actual, "If the required '/draft-XX/' segment is missing, the id should stay unchanged");
    }

    /* ---------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("caseSensitivityProvider")
    void whenIdHasDifferentCase_thenReturnedUnchanged(String input) {
        String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(input, actual, "Case‑sensitive check for the marker should prevent normalization when case differs");
    }
}
