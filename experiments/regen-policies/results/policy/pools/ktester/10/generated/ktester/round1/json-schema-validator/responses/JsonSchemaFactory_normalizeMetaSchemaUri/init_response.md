```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class JsonSchemaFactory_normalizeMetaSchemaUri_Test {

    /* --------------------------------------------------------------------- *
     *  No external resources or collaborators are required for the static   *
     *  method under test, therefore the lifecycle methods remain empty.      *
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
            assertEquals(original, normalized,
                    "normalizeMetaSchemaUri should not modify a known VersionFlag id");
        }
    }

    /* --------------------------------------------------------------------- *
     *  2. Normalisation of draft‑07 URIs                                      *
     * --------------------------------------------------------------------- */
    @Test
    void shouldNormalizeDraft07Uri() {
        String input = "http://json-schema.org/draft-07/schema#";
        String expected = SchemaId.V7; // constant defined in SchemaId
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
}
```