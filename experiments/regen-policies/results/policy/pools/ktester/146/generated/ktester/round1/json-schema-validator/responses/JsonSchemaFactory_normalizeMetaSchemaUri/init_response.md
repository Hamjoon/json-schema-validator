```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
 * The tests cover:
 * <ul>
 *   <li>Ids that are already known {@link SpecVersion.VersionFlag} values – they must be returned unchanged.</li>
 *   <li>Various “unnormalized” $schema URIs that contain a draft path and must be mapped to the
 *       canonical {@link SchemaId} constants (V4, V6, V7, V201909, V202012).</li>
 *   <li>A fallback case where the input does not match any rule and should be returned unchanged.</li>
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

    /* --------------------------------------------------------------------- */
    /*                     Actual test methods                                 */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest
    @MethodSource("knownVersionFlagIds")
    void shouldReturnIdUnchangedWhenIdMatchesVersionFlag(String id) {
        String result = JsonSchemaFactory.normalizeMetaSchemaUri(id);
        assertEquals(id, result, "The method must return the original id when it matches a VersionFlag");
    }

    @ParameterizedTest
    @MethodSource("unnormalizedUris")
    void shouldNormaliseUnnormalisedMetaSchemaUris(String input, String expected) {
        String result = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, result,
                () -> String.format("Input '%s' should be normalised to '%s'", input, expected));
    }
}
```