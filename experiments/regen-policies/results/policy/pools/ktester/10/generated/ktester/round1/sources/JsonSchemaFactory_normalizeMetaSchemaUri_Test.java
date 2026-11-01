package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class JsonSchemaFactory_normalizeMetaSchemaUri_Test {

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
        // missing trailing slash
        builder.add(Arguments.of("http://json-schema.org/draft-07schema#"));
        // upper‑case domain (case‑sensitive check)
        builder.add(Arguments.of("http://JSON-SCHEMA.ORG/draft-07/schema#"));
        // empty string
        builder.add(Arguments.of(""));
        return builder.build();
    }

    static Stream<Arguments> normalisationProvider() {
        return Stream.of(// plain draft‑07 (already canonical – still passes through unchanged)
        Arguments.of("http://json-schema.org/draft-07/schema#", SchemaId.V7), // unnormalised draft‑07 with extra prefix before the generic marker
        Arguments.of("http://example.com/any/path://json-schema.org/draft/draft-07/schema#", SchemaId.V7), // plain draft‑04 (canonical)
        Arguments.of("http://json-schema.org/draft-04/schema#", SchemaId.V4), // input containing both draft‑07 and draft‑04 – the method checks for /draft-07/ first
        Arguments.of("http://example.com/xyz://json-schema.org/draft/draft-04/schema#?ref=//json-schema.org/draft-07/schema#", SchemaId.V7), // plain draft‑06 (canonical)
        Arguments.of("http://json-schema.org/draft-06/schema#", SchemaId.V6), // unnormalised draft‑06 with a non‑http scheme (ftp)
        Arguments.of("ftp://json-schema.org/draft/draft-06/schema#", SchemaId.V6), // 2019‑09 draft
        Arguments.of("https://json-schema.org/draft/2019-09/schema#", SchemaId.V201909), // 2020‑12 draft
        Arguments.of("https://json-schema.org/draft/2020-12/schema#", SchemaId.V202012));
    }

    /* --------------------------------------------------------------------- *
     *  1. When the supplied id matches a known VersionFlag id the method     *
     *     must return the id unchanged.                                    *
     * --------------------------------------------------------------------- */
    @Test
    void shouldReturnIdUnchangedWhenIdMatchesVersionFlag() {
        // iterate over all defined VersionFlag values – each flag provides a
        // canonical id via getId().  The method is expected to recognise these
        // ids and leave them untouched.
        for (VersionFlag flag : VersionFlag.values()) {
            String original = flag.getId();
            String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(original);
            assertEquals(original, normalized, "normalizeMetaSchemaUri should not modify a known VersionFlag id");
        }
    }

    /* --------------------------------------------------------------------- *
     *  2. Normalisation of draft‑07 URIs                                      *
     * --------------------------------------------------------------------- */
    @Test
    void shouldNormalizeDraft07Uri() {
        String input = "http://json-schema.org/draft-07/schema#";
        // constant defined in SchemaId
        String expected = SchemaId.V7;
        assertEquals(expected, JsonSchemaFactory.normalizeMetaSchemaUri(input));
    }

    /* --------------------------------------------------------------------- *
     *  3. Normalisation of draft‑04 URIs                                      *
     * --------------------------------------------------------------------- */
    @Test
    void shouldNormalizeDraft04Uri() {
        String input = "http://json-schema.org/draft-04/schema#";
        String expected = SchemaId.V4;
        assertEquals(expected, JsonSchemaFactory.normalizeMetaSchemaUri(input));
    }

    /* --------------------------------------------------------------------- *
     *  4. Normalisation of draft‑06 URIs                                      *
     * --------------------------------------------------------------------- */
    @Test
    void shouldNormalizeDraft06Uri() {
        String input = "http://json-schema.org/draft-06/schema#";
        String expected = SchemaId.V6;
        assertEquals(expected, JsonSchemaFactory.normalizeMetaSchemaUri(input));
    }

    /* --------------------------------------------------------------------- *
     *  5. Normalisation of 2019‑09 draft URIs                                 *
     * --------------------------------------------------------------------- */
    @Test
    void shouldNormalizeDraft2019_09Uri() {
        String input = "https://json-schema.org/draft/2019-09/schema#";
        String expected = SchemaId.V201909;
        assertEquals(expected, JsonSchemaFactory.normalizeMetaSchemaUri(input));
    }

    /* --------------------------------------------------------------------- *
     *  6. Normalisation of 2020‑12 draft URIs                                 *
     * --------------------------------------------------------------------- */
    @Test
    void shouldNormalizeDraft2020_12Uri() {
        String input = "https://json-schema.org/draft/2020-12/schema#";
        String expected = SchemaId.V202012;
        assertEquals(expected, JsonSchemaFactory.normalizeMetaSchemaUri(input));
    }

    /* --------------------------------------------------------------------- *
     *  7. URIs that do not contain the json‑schema.org draft marker must   *
     *     be returned unchanged.                                            *
     * --------------------------------------------------------------------- */
    @Test
    void shouldReturnUnchangedWhenNotAJsonSchemaDraftUri() {
        String input = "https://example.com/custom-schema";
        assertEquals(input, JsonSchemaFactory.normalizeMetaSchemaUri(input));
    }

    /* --------------------------------------------------------------------- *
     *  8. Null or empty strings are not expected by the method (it is used   *
     *     only after validation in the factory constructor).  Nevertheless, *
     *     we verify that a null argument does not cause a NPE – the method    *
     *     simply returns null.                                              *
     * --------------------------------------------------------------------- */
    @Test
    void shouldHandleNullInputGracefully() {
        assertNull(JsonSchemaFactory.normalizeMetaSchemaUri(null));
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

    /* --------------------------------------------------------------------- *
     *  2. IDs that should be normalised to one of the {@link SchemaId}      *
     *     constants. The method replaces the whole string with the constant *
     *     when the generic marker and a recognised sub‑path are present.   *
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] normalise \"{0}\" → \"{1}\"")
    @MethodSource("normalisationProvider")
    void shouldNormaliseDraftUris(String input, String expected) {
        String result = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, result, "The method should replace the input with the canonical SchemaId");
    }

    /* --------------------------------------------------------------------- *
     *  3. Null input – the implementation dereferences the argument, so a   *
     *     {@link NullPointerException} is expected.                         *
     * --------------------------------------------------------------------- */
    @Test
    void shouldThrowNpeWhenIdIsNull() {
        assertThrows(NullPointerException.class, () -> JsonSchemaFactory.normalizeMetaSchemaUri(null), "Passing null should result in a NullPointerException");
    }
}
