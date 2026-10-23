```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SpecVersionDetector#detect(JsonNode)}.
 *
 * <p>The test suite covers:
 * <ul>
 *   <li>Successful detection of all known draft URIs.</li>
 *   <li>Behaviour when the {@code $schema} tag is missing, unknown or malformed.</li>
 *   <li>Handling of {@code null} input.</li>
 *   <li>Various non‑object root node scenarios.</li>
 * </ul>
 *
 * <p>All tests use a shared {@link ObjectMapper} instantiated once for the whole test class.
 */
@ExtendWith(MockitoExtension.class)
class SpecVersionDetector_detect_Test {

    /** ObjectMapper shared by all test methods. */
    private static ObjectMapper mapper;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise the mapper once for the whole test suite.
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required at the moment.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
    }

    /* --------------------------------------------------------------
     *  Happy‑path tests – the $schema tag is present and recognised.
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] detect should return {1} for JSON: {0}")
    @MethodSource("provideValidSchemaJson")
    void detectVersion_whenSchemaTagIsRecognised(String json, VersionFlag expectedFlag) throws IOException {
        // given: a JSON node containing a known $schema URI
        JsonNode node = mapper.readTree(json);

        // when: the detector is asked to detect the version
        VersionFlag actual = SpecVersionDetector.detect(node);

        // then: the detected flag matches the expectation
        assertEquals(expectedFlag, actual);
    }

    private static Stream<Arguments> provideValidSchemaJson() {
        return Stream.of(
                Arguments.of("{ \"$schema\": \"http://json-schema.org/draft-04/schema#\" }", VersionFlag.DRAFT_04),
                Arguments.of("{ \"$schema\": \"http://json-schema.org/draft-06/schema#\" }", VersionFlag.DRAFT_06),
                Arguments.of("{ \"$schema\": \"http://json-schema.org/draft-07/schema#\" }", VersionFlag.DRAFT_07),
                Arguments.of("{ \"$schema\": \"http://json-schema.org/draft-2019-09/schema#\" }", VersionFlag.DRAFT_2019_09),
                Arguments.of("{ \"$schema\": \"http://json-schema.org/draft-2019-09/schema\" }", VersionFlag.DRAFT_2019_09),
                Arguments.of("{ \"$schema\": \"https://json-schema.org/draft/2019-09/schema\" }", VersionFlag.DRAFT_2019_09),
                Arguments.of("{ \"$schema\": \"https://json-schema.org/draft/2020-12/schema\" }", VersionFlag.DRAFT_2020_12)
        );
    }

    /* --------------------------------------------------------------
     *  Failure tests – unknown or missing $schema tag.
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] detect should throw JsonSchemaException for JSON: {0}")
    @MethodSource("provideInvalidOrMissingSchemaJson")
    void detectVersion_whenSchemaTagIsInvalidOrMissing(String json, String expectedMessage) throws IOException {
        // given: a JSON node that either lacks $schema or contains an unrecognised value
        JsonNode node = mapper.readTree(json);

        // when / then: detection throws JsonSchemaException with the expected message
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> SpecVersionDetector.detect(node));
        assertEquals(expectedMessage, ex.getMessage());
    }

    private static Stream<Arguments> provideInvalidOrMissingSchemaJson() {
        String missingMessage = "'$schema' tag is not present";
        return Stream.of(
                // missing $schema field
                Arguments.of("{ \"title\": \"No schema tag here\" }", missingMessage),

                // unknown URI
                Arguments.of("{ \"$schema\": \"http://example.com/unknown-schema\" }", missingMessage),

                // non‑text values
                Arguments.of("{ \"$schema\": 12345 }", missingMessage),
                Arguments.of("{ \"$schema\": [\"http://json-schema.org/draft-07/schema#\"] }", missingMessage),
                Arguments.of("{ \"$schema\": null }", missingMessage),

                // whitespace / case variations (exact match required)
                Arguments.of("{ \"$schema\": \"  http://json-schema.org/draft-07/schema#  \" }", missingMessage),
                Arguments.of("{ \"$schema\": \"HTTP://JSON-SCHEMA.ORG/DRAFT-07/SCHEMA#\" }", missingMessage),

                // empty string
                Arguments.of("{ \"$schema\": \"\" }", missingMessage),

                // fragment / query parameters break the match
                Arguments.of("{ \"$schema\": \"http://json-schema.org/draft-07/schema#.extra\" }", missingMessage),
                Arguments.of("{ \"$schema\": \"http://json-schema.org/draft-07/schema?#param=1\" }", missingMessage),

                // root node not an object
                Arguments.of("[]", missingMessage),
                Arguments.of("123", missingMessage),
                Arguments.of("true", missingMessage),
                Arguments.of("\"just a string\"", missingMessage)
        );
    }

    /* --------------------------------------------------------------
     *  Defensive test – null input.
     * -------------------------------------------------------------- */
    @Test
    void detectVersion_whenInputIsNull_throwsNullPointerException() {
        // given: a null JsonNode reference
        JsonNode node = null;

        // when / then: NPE is thrown because the implementation does not guard against null
        assertThrows(NullPointerException.class, () -> SpecVersionDetector.detect(node));
    }

    /* --------------------------------------------------------------
     *  Additional sanity check for the optional helper method.
     * -------------------------------------------------------------- */
    @Test
    void detectOptionalVersion_returnsEmptyWhenTagMissing() throws IOException {
        // given: a schema without a $schema tag
        JsonNode node = mapper.readTree("{ \"title\": \"No tag\" }");

        // when: optional detection is performed
        Optional<VersionFlag> result = SpecVersionDetector.detectOptionalVersion(node);

        // then: the result is empty
        assertEquals(Optional.empty(), result);
    }
}
```