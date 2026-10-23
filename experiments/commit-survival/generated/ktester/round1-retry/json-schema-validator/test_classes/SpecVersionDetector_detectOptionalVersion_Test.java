package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SpecVersionDetector_detectOptionalVersion_Test {

    /**
     * Shared ObjectMapper for creating JSON nodes
     */
    private static ObjectMapper mapper;

    @BeforeAll
    static void setupBeforeAll() {
        // initialise the mapper once for the whole test suite
        mapper = new ObjectMapper();
    }

    private static Stream<Arguments> provideSuccessfulCases() {
        return Stream.of(// No $schema property → empty Optional
        Arguments.of("{}", null, "Empty object, no $schema => empty Optional"), // Full meta‑schema URIs
        Arguments.of("{\"$schema\":\"http://json-schema.org/draft-07/schema#\"}", SpecVersion.VersionFlag.DRAFT_07, "Full draft‑07 URI"), Arguments.of("{\"$schema\":\"http://json-schema.org/draft-04/schema#\"}", SpecVersion.VersionFlag.DRAFT_04, "Full draft‑04 URI"), Arguments.of("{\"$schema\":\"https://json-schema.org/draft/2019-09/schema\"}", SpecVersion.VersionFlag.DRAFT_2019_09, "2019‑09 meta‑schema URI"), Arguments.of("{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"}", SpecVersion.VersionFlag.DRAFT_2020_12, "2020‑12 meta‑schema URI"), // Short‑form identifiers that need normalization
        Arguments.of("{\"$schema\":\"draft-07\"}", SpecVersion.VersionFlag.DRAFT_07, "Short form 'draft-07' → DRAFT_07"), Arguments.of("{\"$schema\":\"draft-06\"}", SpecVersion.VersionFlag.DRAFT_06, "Short form 'draft-06' → DRAFT_06"), // Root node is an array → get(\"$schema\") returns null → empty Optional
        Arguments.of("[{\"$schema\":\"http://json-schema.org/draft-07/schema#\"}]", null, "Root array node – should return empty Optional"));
    }

    private static Stream<Arguments> provideExceptionCases() {
        return Stream.of(// Unknown URI
        Arguments.of("{\"$schema\":\"http://example.com/unknown-schema\"}", "'http://example.com/unknown-schema' is unrecognizable schema", "Unrecognizable full URI"), // Empty string
        Arguments.of("{\"$schema\":\"\"}", "'' is unrecognizable schema", "Empty string as $schema"), // Numeric value
        Arguments.of("{\"$schema\":123}", "'123' is unrecognizable schema", "Numeric $schema value"), // Explicit null
        Arguments.of("{\"$schema\":null}", "'null' is unrecognizable schema", "Explicit null $schema value"), // Boolean true
        Arguments.of("{\"$schema\":true}", "'true' is unrecognizable schema", "Boolean true $schema value"), // Boolean false
        Arguments.of("{\"$schema\":false}", "'false' is unrecognizable schema", "Boolean false $schema value"), // Array value
        Arguments.of("{\"$schema\":[\"http://json-schema.org/draft-07/schema#\"]}", "'[\"http://json-schema.org/draft-07/schema#\"]' is unrecognizable schema", "Array $schema value"), // Whitespace only
        Arguments.of("{\"$schema\":\"   \"}", "'   ' is unrecognizable schema", "Whitespace only string"));
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
        assertEquals(SpecVersion.VersionFlag.DRAFT_07, result.get(), "The detected version flag should correspond to draft‑07");
    }

    @Test
    void detectOptionalVersion_whenSchemaTagPresentButUnrecognizable_throwsException() {
        // given
        ObjectNode node = mapper.createObjectNode();
        String unknownUri = "http://example.com/unknown/schema#";
        node.put("$schema", unknownUri);
        // when / then
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> SpecVersionDetector.detectOptionalVersion(node));
        assertEquals("'" + unknownUri + "' is unrecognizable schema", ex.getMessage(), "Exception message should indicate the unrecognizable schema URI");
    }

    @Test
    void detectOptionalVersion_whenSchemaTagMissing_returnsEmptyOptional() {
        // given
        // no $schema field
        ObjectNode node = mapper.createObjectNode();
        // when
        Optional<SpecVersion.VersionFlag> result = SpecVersionDetector.detectOptionalVersion(node);
        // then
        assertEquals(Optional.empty(), result, "When $schema tag is missing the method should return Optional.empty()");
    }

    /**
     * Successful detection cases – the method should return an Optional
     * (either empty or containing the expected {@link VersionFlag}).
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideSuccessfulCases")
    void detectOptionalVersion_successfulCases(String json, SpecVersion.VersionFlag expectedFlag, String description) throws Exception {
        JsonNode node = mapper.readTree(json);
        Optional<SpecVersion.VersionFlag> result = SpecVersionDetector.detectOptionalVersion(node);
        if (expectedFlag == null) {
            // Expect an empty Optional
            assertTrue(result.isEmpty(), description);
        } else {
            // Expect a present flag equal to the expected one
            assertTrue(result.isPresent(), description);
            assertEquals(expectedFlag, result.get(), description);
        }
    }

    /**
     * Cases where the method must throw {@link JsonSchemaException} with a
     * specific message because the supplied $schema value cannot be mapped
     * to a known {@link VersionFlag}.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideExceptionCases")
    void detectOptionalVersion_exceptionCases(String json, String expectedMessage, String description) throws Exception {
        JsonNode node = mapper.readTree(json);
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> SpecVersionDetector.detectOptionalVersion(node), description);
        assertEquals(expectedMessage, ex.getMessage(), description);
    }

    /**
     * Null argument handling – the implementation dereferences the argument
     * without a null‑check, therefore a {@link NullPointerException} is expected.
     */
    @Test
    void detectOptionalVersion_nullJsonNode_throwsNPE() {
        assertThrows(NullPointerException.class, () -> SpecVersionDetector.detectOptionalVersion((JsonNode) null), "Passing null JsonNode should throw NullPointerException");
    }
}
