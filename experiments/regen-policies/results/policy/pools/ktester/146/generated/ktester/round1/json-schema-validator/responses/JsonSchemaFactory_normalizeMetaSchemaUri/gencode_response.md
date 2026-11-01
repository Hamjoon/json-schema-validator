```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#normalizeMetaSchemaUri(String)}.
 *
 * The method is {@code protected static}, but because this test class resides in the same
 * package ({@code com.networknt.schema}) it can be accessed directly.
 *
 * The test suite covers:
 * <ul>
 *   <li>Ids that already match a {@link SpecVersion.VersionFlag} – they must be returned unchanged.</li>
 *   <li>Various “unnormalized” $schema URIs that contain a draft path and must be mapped to the
 *       canonical {@link SchemaId} constants (V4, V6, V7, V201909, V202012).</li>
 *   <li>Fallback cases where the input does not match any rule and should be returned unchanged.</li>
 *   <li>Edge‑cases such as {@code null} input, case‑sensitivity, missing trailing slash, etc.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_normalizeMetaSchemaUri_Test {

    /* --------------------------------------------------------------------- */
    /*                     Lifecycle hooks (currently empty)               */
    /* --------------------------------------------------------------------- */

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these pure‑function tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* --------------------------------------------------------------------- */
    /*                     Test data providers                               */
    /* --------------------------------------------------------------------- */

    /** All {@link SpecVersion.VersionFlag} ids – they must be returned unchanged. */
    static Stream<Arguments> knownVersionFlagIds() {
        return Stream.of(SpecVersion.VersionFlag.values())
                .map(flag -> Arguments.of(flag.getId()));
    }

    /** Unnormalized URIs together with the expected canonical {@link SchemaId} value. */
    static Stream<Arguments> unnormalizedUris() {
        return Stream.of(
                // draft‑07
                Arguments.of("https://json-schema.org/draft-07/schema#", SchemaId.V7),
                Arguments.of("http://json-schema.org/draft-07/schema#", SchemaId.V7),

                // draft‑2019‑09
                Arguments.of("https://json-schema.org/draft/2019-09/schema#", SchemaId.V201909),
                Arguments.of("http://json-schema.org/draft/2019-09/schema#", SchemaId.V201909),

                // draft‑2020‑12
                Arguments.of("https://json-schema.org/draft/2020-12/schema#", SchemaId.V202012),
                Arguments.of("http://json-schema.org/draft/2020-12/schema#", SchemaId.V202012),

                // draft‑04
                Arguments.of("https://json-schema.org/draft-04/schema#", SchemaId.V4),
                Arguments.of("http://json-schema.org/draft-04/schema#", SchemaId.V4),

                // draft‑06
                Arguments.of("https://json-schema.org/draft-06/schema#", SchemaId.V6),
                Arguments.of("http://json-schema.org/draft-06/schema#", SchemaId.V6),

                // a value that does not need normalisation
                Arguments.of("https://example.com/custom-schema", "https://example.com/custom-schema")
        );
    }

    /** URIs that contain multiple draft substrings – the first matching branch wins. */
    static Stream<Arguments> precedenceUris() {
        return Stream.of(
                Arguments.of(
                        "https://example.com/draft-07/path/draft-06/schema.json",
                        SchemaId.V7),
                Arguments.of(
                        "https://example.com/draft/2019-09/path/draft-07/schema",
                        SchemaId.V7)
        );
    }

    /** Case‑sensitive checks – upper‑case marker must not trigger normalisation. */
    static Stream<Arguments> caseSensitivityUris() {
        return Stream.of(
                Arguments.of(
                        "HTTPS://json-schema.org/DRAFT-07/schema#",
                        "HTTPS://json-schema.org/DRAFT-07/schema#")
        );
    }

    /** Missing trailing slash after version substring – no normalisation should happen. */
    static Stream<Arguments> missingSlashUris() {
        return Stream.of(
                Arguments.of("https://example.com/draft-07schema", "https://example.com/draft-07schema"),
                Arguments.of("https://example.com/draft-07", "https://example.com/draft-07")
        );
    }

    /** Draft marker without the exact \"://json-schema.org/draft\" prefix. */
    static Stream<Arguments> prefixMissingUris() {
        return Stream.of(
                Arguments.of(
                        "http://example.com/json-schema.org/draft-07/schema",
                        "http://example.com/json-schema.org/draft-07/schema")
        );
    }

    /** Normalisation should work regardless of scheme (e.g., ftp). */
    static Stream<Arguments> ftpSchemeUris() {
        return Stream.of(
                Arguments.of(
                        "ftp://json-schema.org/draft-07/schema",
                        SchemaId.V7)
        );
    }

    /** URIs with query parameters / fragments – the whole string is replaced by the canonical id. */
    static Stream<Arguments> queryFragmentUris() {
        return Stream.of(
                Arguments.of(
                        "http://json-schema.org/draft-07/schema?foo=bar#section",
                        SchemaId.V7)
        );
    }

    /** Already normalized but with an additional fragment – should be collapsed to the canonical form. */
    static Stream<Arguments> extraFragmentUris() {
        return Stream.of(
                Arguments.of(
                        "http://json-schema.org/draft-07/schema#extra",
                        SchemaId.V7)
        );
    }

    /** Empty string that contains the draft marker but no specific version – unchanged. */
    static Stream<Arguments> emptyDraftMarkerUris() {
        return Stream.of(
                Arguments.of(
                        "http://json-schema.org/draft/",
                        "http://json-schema.org/draft/")
        );
    }

    /** Ids that do not contain any draft marker and are not known VersionFlag – unchanged. */
    static Stream<Arguments> fallbackUris() {
        return Stream.of(
                Arguments.of("urn:custom:schema", "urn:custom:schema"),
                Arguments.of("", "")
        );
    }

    /* --------------------------------------------------------------------- */
    /*                     Actual test methods                                 */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest
    @MethodSource("knownVersionFlagIds")
    void shouldReturnIdUnchangedWhenIdMatchesVersionFlag(String id) {
        String result = JsonSchemaFactory.normalizeMetaSchemaUri(id);
        assertEquals(id, result,
                "The method must return the original id when it matches a VersionFlag");
    }

    @ParameterizedTest
    @MethodSource("unnormalizedUris")
    void shouldNormaliseUnnormalisedMetaSchemaUris(String input, String expected) {
        String result = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, result,
                () -> String.format("Input '%s' should be normalised to '%s'", input, expected));
    }

    @ParameterizedTest
    @MethodSource("precedenceUris")
    void shouldRespectPrecedenceWhenMultipleDraftSubstringsPresent(String input, String expected) {
        String result = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, result,
                () -> "When multiple draft substrings are present the first matching branch should win");
    }

    @ParameterizedTest
    @MethodSource("caseSensitivityUris")
    void shouldBeCaseSensitiveOnDraftMarker(String input, String expected) {
        String result = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, result,
                "The method uses case‑sensitive `contains`; upper‑case markers must not be normalised");
    }

    @ParameterizedTest
    @MethodSource("missingSlashUris")
    void shouldNotNormaliseWhenTrailingSlashIsMissing(String input, String expected) {
        String result = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, result,
                "Missing the required \"/\" after the version substring means no normalisation");
    }

    @ParameterizedTest
    @MethodSource("prefixMissingUris")
    void shouldNotNormaliseWhenExactDraftPrefixIsMissing(String input, String expected) {
        String result = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, result,
                "Only the exact \"://json-schema.org/draft\" prefix triggers normalisation");
    }

    @ParameterizedTest
    @MethodSource("ftpSchemeUris")
    void shouldNormaliseRegardlessOfScheme(String input, String expected) {
        String result = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, result,
                "Scheme (http, https, ftp, etc.) does not affect normalisation once the marker is present");
    }

    @ParameterizedTest
    @MethodSource("queryFragmentUris")
    void shouldStripQueryAndFragmentWhenNormalising(String input, String expected) {
        String result = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, result,
                "Any query string or fragment should be discarded in favour of the canonical identifier");
    }

    @ParameterizedTest
    @MethodSource("extraFragmentUris")
    void shouldCollapseExtraFragmentToCanonicalForm(String input, String expected) {
        String result = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, result,
                "Even if the input already contains a fragment, the method should replace it with the canonical one");
    }

    @ParameterizedTest
    @MethodSource("emptyDraftMarkerUris")
    void shouldLeaveIdUnchangedWhenDraftMarkerHasNoSpecificVersion(String input, String expected) {
        String result = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, result,
                "Presence of the generic draft marker without a known version should not modify the id");
    }

    @ParameterizedTest
    @MethodSource("fallbackUris")
    void shouldReturnIdUnchangedWhenNoNormalisationNeeded(String input, String expected) {
        String result = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, result,
                "Ids that neither match a VersionFlag nor contain a recognised draft pattern must be returned unchanged");
    }

    @Test
    void shouldThrowNpeWhenIdIsNull() {
        assertThrows(NullPointerException.class,
                () -> JsonSchemaFactory.normalizeMetaSchemaUri(null),
                "Calling the method with null should raise NullPointerException");
    }
}
```