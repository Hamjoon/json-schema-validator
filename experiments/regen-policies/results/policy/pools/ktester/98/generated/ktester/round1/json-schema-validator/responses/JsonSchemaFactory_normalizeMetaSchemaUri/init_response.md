```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for {@link JsonSchemaFactory#normalizeMetaSchemaUri(String)}.
 *
 * The method is {@code protected static}, therefore it can be accessed directly
 * from a test class that resides in the same package.
 *
 * The tests cover:
 * <ul>
 *   <li>Ids that are already known {@link SpecVersion.VersionFlag}s – they must be returned unchanged.</li>
 *   <li>Un‑normalized ids that contain the generic “json‑schema.org/draft” pattern – they must be mapped to the corresponding {@link SchemaId} constants.</li>
 *   <li>Ids that do not match any rule – they must be returned unchanged.</li>
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

    /**
     * When the supplied id matches one of the {@link SpecVersion.VersionFlag} ids,
     * the method must return the original value unchanged.
     */
    @ParameterizedTest
    @CsvSource({
        // The concrete ids are obtained from the enum values.
        // They are written explicitly to keep the test independent from the enum implementation.
        "'http://json-schema.org/draft-04/schema', http://json-schema.org/draft-04/schema",
        "'http://json-schema.org/draft-06/schema', http://json-schema.org/draft-06/schema",
        "'http://json-schema.org/draft-07/schema', http://json-schema.org/draft-07/schema",
        "'https://json-schema.org/draft/2019-09/schema', https://json-schema.org/draft/2019-09/schema",
        "'https://json-schema.org/draft/2020-12/schema', https://json-schema.org/draft/2020-12/schema"
    })
    void whenIdIsVersionFlag_thenReturnedUnchanged(String input, String expected) {
        String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, actual, "A known VersionFlag id should be returned unchanged");
    }

    /**
     * Un‑normalized ids that contain the generic “json‑schema.org/draft” pattern
     * must be normalised to the appropriate {@link SchemaId} constant.
     */
    @ParameterizedTest
    @CsvSource({
        // format: unnormalizedId, expectedNormalizedId
        "'http://example.com/some/path/draft-07/schema.json', " + SchemaId.V7,
        "'https://example.org/any/draft/2019-09/whatever', " + SchemaId.V201909,
        "'https://foo.bar/draft/2020-12/bar.json', " + SchemaId.V202012,
        "'http://myhost/draft-04/schema', " + SchemaId.V4,
        "'https://host/draft-06/schema', " + SchemaId.V6
    })
    void whenIdIsUnnormalized_thenMappedToSchemaId(String input, String expected) {
        String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, actual, "Un‑normalized id should be mapped to the corresponding SchemaId");
    }

    /**
     * Ids that do not match any rule must be returned unchanged.
     */
    @Test
    void whenIdDoesNotMatchAnyRule_thenReturnedUnchanged() {
        String[] inputs = {
                "http://example.com/custom-schema",
                "urn:uuid:1234-5678-90ab-cdef",
                "https://json-schema.org/unknown/draft/schema",
                "random-string"
        };
        for (String input : inputs) {
            String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
            assertEquals(input, actual, "Id that does not match any rule should be returned unchanged");
        }
    }
}
```