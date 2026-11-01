```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchemaFactory#normalizeMetaSchemaUri(String)}.
 *
 * <p>
 * The method is {@code protected static} and therefore accessible from this
 * test class because it resides in the same package.
 * </p>
 */
class JsonSchemaFactory_normalizeMetaSchemaUri_Test {

    /* --------------------------------------------------------------------- *
     *  Lifecycle – no external resources are required for the static method. *
     * --------------------------------------------------------------------- */
    @BeforeAll
    static void setupBeforeAll() {
        // nothing to initialise globally
    }

    @BeforeEach
    void setupBeforeEach() {
        // nothing to initialise per test
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing to clean up per test
    }

    @AfterAll
    static void teardownAfterAll() {
        // nothing to clean up globally
    }

    /* --------------------------------------------------------------------- *
     *  1. IDs that must be returned unchanged.                              *
     *     – all {@link VersionFlag} ids (they are already canonical)       *
     *     – non‑draft URIs or edge‑cases that do not match the normalisation *
     *       rules.                                                            *
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] unchanged id = \"{0}\"")
    @MethodSource("unchangedIdsProvider")
    void shouldReturnIdUnchanged(String id) {
        String result = JsonSchemaFactory.normalizeMetaSchemaUri(id);
        assertEquals(id, result, "The method should leave the id untouched");
    }

    static Stream<Arguments> unchangedIdsProvider() {
        Stream.Builder<Arguments> builder = Stream.builder();

        // all known VersionFlag ids – the method should recognise them and skip normalisation
        for (VersionFlag flag : VersionFlag.values()) {
            builder.add(Arguments.of(flag.getId()));
        }

        // ids that do not contain the generic draft marker or contain it but no recognised sub‑path
        builder.add(Arguments.of("urn:custom:schema"));
        builder.add(Arguments.of("https://example.com/custom-schema"));
        builder.add(Arguments.of("http://json-schema.org/draft/unknown/schema#"));
        builder.add(Arguments.of("http://json-schema.org/draft-07schema#")); // missing trailing slash
        builder.add(Arguments.of("http://JSON-SCHEMA.ORG/draft-07/schema#")); // upper‑case domain (case‑sensitive check)
        builder.add(Arguments.of("")); // empty string

        return builder.build();
    }

    /* --------------------------------------------------------------------- *
     *  2. IDs that should be normalised to one of the {@link SchemaId}      *
     *     constants. The method replaces the whole string with the constant *
     *     when the generic marker and a recognised sub‑path are present.   *
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] normalise \"{0}\" → \"{1}\"")
    @MethodSource("normalisationProvider")
    void shouldNormaliseDraftUris(String input, String expected) {
        String result = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, result,
                "The method should replace the input with the canonical SchemaId");
    }

    static Stream<Arguments> normalisationProvider() {
        return Stream.of(
                // plain draft‑07 (already canonical – still passes through unchanged)
                Arguments.of("http://json-schema.org/draft-07/schema#", SchemaId.V7),

                // unnormalised draft‑07 with extra prefix before the generic marker
                Arguments.of("http://example.com/any/path://json-schema.org/draft/draft-07/schema#", SchemaId.V7),

                // plain draft‑04 (canonical)
                Arguments.of("http://json-schema.org/draft-04/schema#", SchemaId.V4),

                // input containing both draft‑07 and draft‑04 – the method checks for /draft-07/ first
                Arguments.of(
                        "http://example.com/xyz://json-schema.org/draft/draft-04/schema#?ref=//json-schema.org/draft-07/schema#",
                        SchemaId.V7),

                // plain draft‑06 (canonical)
                Arguments.of("http://json-schema.org/draft-06/schema#", SchemaId.V6),

                // unnormalised draft‑06 with a non‑http scheme (ftp)
                Arguments.of("ftp://json-schema.org/draft/draft-06/schema#", SchemaId.V6),

                // 2019‑09 draft
                Arguments.of("https://json-schema.org/draft/2019-09/schema#", SchemaId.V201909),

                // 2020‑12 draft
                Arguments.of("https://json-schema.org/draft/2020-12/schema#", SchemaId.V202012)
        );
    }

    /* --------------------------------------------------------------------- *
     *  3. Null input – the implementation dereferences the argument, so a   *
     *     {@link NullPointerException} is expected.                         *
     * --------------------------------------------------------------------- */
    @Test
    void shouldThrowNpeWhenIdIsNull() {
        assertThrows(NullPointerException.class,
                () -> JsonSchemaFactory.normalizeMetaSchemaUri(null),
                "Passing null should result in a NullPointerException");
    }
}
```