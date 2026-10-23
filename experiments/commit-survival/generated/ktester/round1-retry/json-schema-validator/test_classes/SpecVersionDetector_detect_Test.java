package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SpecVersionDetector_detect_Test {

    /**
     * ObjectMapper shared by all test methods.
     */
    private static ObjectMapper mapper;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise the mapper once for the whole test suite.
        mapper = new ObjectMapper();
    }

    private static Stream<Arguments> provideValidSchemaJson() {
        return Stream.of(Arguments.of("{ \"$schema\": \"http://json-schema.org/draft-04/schema#\" }", VersionFlag.DRAFT_04), Arguments.of("{ \"$schema\": \"http://json-schema.org/draft-06/schema#\" }", VersionFlag.DRAFT_06), Arguments.of("{ \"$schema\": \"http://json-schema.org/draft-07/schema#\" }", VersionFlag.DRAFT_07), Arguments.of("{ \"$schema\": \"http://json-schema.org/draft-2019-09/schema#\" }", VersionFlag.DRAFT_2019_09), Arguments.of("{ \"$schema\": \"http://json-schema.org/draft-2019-09/schema\" }", VersionFlag.DRAFT_2019_09), Arguments.of("{ \"$schema\": \"https://json-schema.org/draft/2019-09/schema\" }", VersionFlag.DRAFT_2019_09), Arguments.of("{ \"$schema\": \"https://json-schema.org/draft/2020-12/schema\" }", VersionFlag.DRAFT_2020_12));
    }

    private static Stream<Arguments> provideInvalidOrMissingSchemaJson() {
        String missingMessage = "'$schema' tag is not present";
        return Stream.of(// missing $schema field
        Arguments.of("{ \"title\": \"No schema tag here\" }", missingMessage), // unknown URI
        Arguments.of("{ \"$schema\": \"http://example.com/unknown-schema\" }", missingMessage), // non‑text values
        Arguments.of("{ \"$schema\": 12345 }", missingMessage), Arguments.of("{ \"$schema\": [\"http://json-schema.org/draft-07/schema#\"] }", missingMessage), Arguments.of("{ \"$schema\": null }", missingMessage), // whitespace / case variations (exact match required)
        Arguments.of("{ \"$schema\": \"  http://json-schema.org/draft-07/schema#  \" }", missingMessage), Arguments.of("{ \"$schema\": \"HTTP://JSON-SCHEMA.ORG/DRAFT-07/SCHEMA#\" }", missingMessage), // empty string
        Arguments.of("{ \"$schema\": \"\" }", missingMessage), // fragment / query parameters break the match
        Arguments.of("{ \"$schema\": \"http://json-schema.org/draft-07/schema#.extra\" }", missingMessage), Arguments.of("{ \"$schema\": \"http://json-schema.org/draft-07/schema?#param=1\" }", missingMessage), // root node not an object
        Arguments.of("[]", missingMessage), Arguments.of("123", missingMessage), Arguments.of("true", missingMessage), Arguments.of("\"just a string\"", missingMessage));
    }

    /* --------------------------------------------------------------
     *  Happy‑path tests – the $schema tag is present and recognised.
     * -------------------------------------------------------------- */
    @ParameterizedTest
    @CsvSource({ "draft4,       V4", "draft6,       V6", "draft7,       V7", "draft2019-09, V201909", "draft2020-12, V202012" })
    void detectVersion_whenSchemaTagIsRecognised(String resourceDirectory, SpecVersion.VersionFlag expectedFlag) throws IOException {
        // given: a JSON schema file that contains a known $schema tag
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceDirectory + "/schemaTag.json");
        assertNotNull(in, "Test resource not found: " + resourceDirectory + "/schemaTag.json");
        JsonNode node = mapper.readTree(in);
        // when: the detector is asked to detect the version
        SpecVersion.VersionFlag actual = SpecVersionDetector.detect(node);
        // then: the detected flag matches the expectation
        assertEquals(expectedFlag, actual);
    }

    /* --------------------------------------------------------------
     *  Failure tests – the $schema tag is missing or unrecognised.
     * -------------------------------------------------------------- */
    @ParameterizedTest
    @CsvSource({ // unrecognised schema URI
    "data/schemaTag.json,        'http://json-schema.org/draft-03/schema#' is unrecognizable schema", // $schema tag completely missing
    "data/schemaTagMissing.json, '$schema' tag is not present" })
    void detectVersion_whenSchemaTagIsInvalidOrMissing(String schemaPath, String expectedMessage) throws IOException {
        // given: a JSON schema file that either lacks $schema or contains an unknown URI
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(schemaPath);
        assertNotNull(in, "Test resource not found: " + schemaPath);
        JsonNode node = mapper.readTree(in);
        // when / then: detection throws JsonSchemaException with the expected message
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> SpecVersionDetector.detect(node));
        assertEquals(expectedMessage, ex.getMessage());
    }

    /* --------------------------------------------------------------
     *  Defensive test – ensure detectOptionalVersion behaves as documented.
     *  (Not strictly required for detect(JsonNode) but useful for completeness.)
     * -------------------------------------------------------------- */
    @Test
    void detectOptionalVersion_returnsEmptyWhenTagMissing() throws IOException {
        // given: a schema without a $schema tag
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("data/schemaTagMissing.json");
        assertNotNull(in, "Test resource not found: data/schemaTagMissing.json");
        JsonNode node = mapper.readTree(in);
        // when: optional detection is performed
        Optional<SpecVersion.VersionFlag> result = SpecVersionDetector.detectOptionalVersion(node);
        // then: the result is empty
        assertEquals(Optional.empty(), result);
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
}
