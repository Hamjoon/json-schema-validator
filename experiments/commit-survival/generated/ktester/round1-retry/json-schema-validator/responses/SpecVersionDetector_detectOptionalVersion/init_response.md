```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SpecVersionDetector_detectOptionalVersion_Test {

    /** Shared ObjectMapper for creating JSON nodes */
    private static ObjectMapper mapper;

    @BeforeAll
    static void setupBeforeAll() {
        // initialise the mapper once for the whole test suite
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state required at the moment
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally
    }

    @Test
    void detectOptionalVersion_whenSchemaTagPresentAndValid_returnsVersionFlag() {
        // given
        ObjectNode node = mapper.createObjectNode();
        node.put("$schema", "http://json-schema.org/draft-07/schema#");

        // when
        Optional<SpecVersion.VersionFlag> result = SpecVersionDetector.detectOptionalVersion(node);

        // then
        assertTrue(result.isPresent(), "Expected a version flag to be present");
        assertEquals(SpecVersion.VersionFlag.V7, result.get(),
                "The detected version flag should correspond to draft‑07");
    }

    @Test
    void detectOptionalVersion_whenSchemaTagPresentButUnrecognizable_throwsException() {
        // given
        ObjectNode node = mapper.createObjectNode();
        String unknownUri = "http://example.com/unknown/schema#";
        node.put("$schema", unknownUri);

        // when / then
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> SpecVersionDetector.detectOptionalVersion(node));
        assertEquals("'" + unknownUri + "' is unrecognizable schema", ex.getMessage(),
                "Exception message should indicate the unrecognizable schema URI");
    }

    @Test
    void detectOptionalVersion_whenSchemaTagMissing_returnsEmptyOptional() {
        // given
        ObjectNode node = mapper.createObjectNode(); // no $schema field

        // when
        Optional<SpecVersion.VersionFlag> result = SpecVersionDetector.detectOptionalVersion(node);

        // then
        assertEquals(Optional.empty(), result,
                "When $schema tag is missing the method should return Optional.empty()");
    }
}
```