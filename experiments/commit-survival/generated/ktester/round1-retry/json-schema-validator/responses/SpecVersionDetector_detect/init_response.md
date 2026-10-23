```java
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
    @ParameterizedTest
    @CsvSource({
            "draft4,       V4",
            "draft6,       V6",
            "draft7,       V7",
            "draft2019-09, V201909",
            "draft2020-12, V202012"
    })
    void detectVersion_whenSchemaTagIsRecognised(String resourceDirectory,
                                                  SpecVersion.VersionFlag expectedFlag) throws IOException {
        // given: a JSON schema file that contains a known $schema tag
        InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resourceDirectory + "/schemaTag.json");
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
    @CsvSource({
            // unrecognised schema URI
            "data/schemaTag.json,        'http://json-schema.org/draft-03/schema#' is unrecognizable schema",
            // $schema tag completely missing
            "data/schemaTagMissing.json, '$schema' tag is not present"
    })
    void detectVersion_whenSchemaTagIsInvalidOrMissing(String schemaPath,
                                                       String expectedMessage) throws IOException {
        // given: a JSON schema file that either lacks $schema or contains an unknown URI
        InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(schemaPath);
        assertNotNull(in, "Test resource not found: " + schemaPath);
        JsonNode node = mapper.readTree(in);

        // when / then: detection throws JsonSchemaException with the expected message
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> SpecVersionDetector.detect(node));
        assertEquals(expectedMessage, ex.getMessage());
    }

    /* --------------------------------------------------------------
     *  Defensive test – ensure detectOptionalVersion behaves as documented.
     *  (Not strictly required for detect(JsonNode) but useful for completeness.)
     * -------------------------------------------------------------- */
    @Test
    void detectOptionalVersion_returnsEmptyWhenTagMissing() throws IOException {
        // given: a schema without a $schema tag
        InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("data/schemaTagMissing.json");
        assertNotNull(in, "Test resource not found: data/schemaTagMissing.json");
        JsonNode node = mapper.readTree(in);

        // when: optional detection is performed
        Optional<SpecVersion.VersionFlag> result = SpecVersionDetector.detectOptionalVersion(node);

        // then: the result is empty
        assertEquals(Optional.empty(), result);
    }
}
```