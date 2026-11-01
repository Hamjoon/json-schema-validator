package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.PathType;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.ValidationContext;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.JsonMetaSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.JsonNodePath;
import com.networknt.schema.AbsoluteIri;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getId_Test {

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    AbsoluteIri absoluteIri;

    private ValidationContext validationContext;

    private ObjectMapper objectMapper;

    private JsonNodePath evaluationPath;

    @BeforeEach
    void setupBeforeEach() {
        // Use the V7 meta‑schema and a default factory for the ValidationContext
        validationContext = new ValidationContext(JsonMetaSchema.getV7(), JsonSchemaFactory.getInstance(VersionFlag.V7), new SchemaValidatorsConfig());
        objectMapper = new ObjectMapper();
        evaluationPath = new JsonNodePath(PathType.SCHEMA);
        // Common stubbing for the SchemaLocation used by JsonSchema
        when(schemaLocation.resolve(anyString())).thenReturn("resolved");
        when(schemaLocation.toString()).thenReturn("schemaLocation");
    }

    /**
     * Provides a stream of arguments for the edge‑case {@code getId()} tests.
     * Each argument consists of:
     * <ul>
     *   <li>expected identifier value (may be {@code null})</li>
     *   <li>human‑readable description (used as the test name)</li>
     * </ul>
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> idEdgeCases() {
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of("urn:example:schema", "Non‑null identifier supplied during construction"), org.junit.jupiter.params.provider.Arguments.of(null, "Identifier field is null when no $id and no absolute IRI"), org.junit.jupiter.params.provider.Arguments.of("", "Empty string identifier"), org.junit.jupiter.params.provider.Arguments.of("   ", "Identifier consisting only of whitespace"), org.junit.jupiter.params.provider.Arguments.of("http://example.com/schema.json?query=param%20value", "Identifier containing URL‑encoded characters (no decoding)"), org.junit.jupiter.params.provider.Arguments.of("urn:example:schema#fragment", "Identifier that includes a fragment component"), org.junit.jupiter.params.provider.Arguments.of("ПримерСхемы", "Unicode identifier (Cyrillic characters)"), org.junit.jupiter.params.provider.Arguments.of("schema\nwith\nnewlines", "Identifier containing newline characters"));
    }

    @Test
    void testGetIdWhenIdIsPresent() {
        // Schema node containing a $id property
        ObjectNode schemaNode = objectMapper.createObjectNode();
        schemaNode.put("$id", "http://example.com/schema-id");
        // No fragment – hasNoFragment will be true
        when(schemaLocation.getFragment()).thenReturn(null);
        // not used when $id is present
        when(schemaLocation.getAbsoluteIri()).thenReturn(null);
        JsonSchema jsonSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
        assertEquals("http://example.com/schema-id", jsonSchema.getId(), "The id returned by getId() should be the $id value from the schema");
    }

    @Test
    void testGetIdWhenNoIdAndNoFragment() {
        // Schema node without a $id property
        ObjectNode schemaNode = objectMapper.createObjectNode();
        // No fragment – triggers the “no $id but schema resource” branch
        when(schemaLocation.getFragment()).thenReturn(null);
        // Provide a non‑null absolute IRI – this value becomes the id
        when(schemaLocation.getAbsoluteIri()).thenReturn(absoluteIri);
        when(absoluteIri.toString()).thenReturn("http://example.com/abs");
        JsonSchema jsonSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
        assertEquals("http://example.com/abs", jsonSchema.getId(), "When $id is missing and there is no fragment, getId() should return the absolute IRI");
    }

    @Test
    void testGetIdWhenNoIdAndFragmentPresent() {
        // Schema node without a $id property
        ObjectNode schemaNode = objectMapper.createObjectNode();
        // Simulate a non‑empty fragment – hasNoFragment will be false
        JsonNodePath fragmentPath = mock(JsonNodePath.class);
        when(schemaLocation.getFragment()).thenReturn(fragmentPath);
        // irrelevant in this branch
        when(schemaLocation.getAbsoluteIri()).thenReturn(null);
        JsonSchema jsonSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
        assertNull(jsonSchema.getId(), "When $id is missing and a fragment is present, getId() should be null");
    }

    /**
     * Parameterised test that verifies {@link JsonSchema#getId()} returns exactly
     * the value supplied via the {@code $id} property (or {@code null} when absent).
     *
     * @param expectedId  the identifier we expect {@code getId()} to return
     * @param description description of the scenario (used in the test display name)
     */
    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("idEdgeCases")
    void testGetIdEdgeCases(String expectedId, String description) {
        // Build a schema node – add $id only when we have a non‑null expected value
        ObjectNode schemaNode = objectMapper.createObjectNode();
        if (expectedId != null) {
            schemaNode.put("$id", expectedId);
        }
        // For all edge‑case scenarios we want to ensure that no fragment interferes
        when(schemaLocation.getFragment()).thenReturn(null);
        // Absolute IRI is irrelevant when $id is present; set to null for clarity
        when(schemaLocation.getAbsoluteIri()).thenReturn(null);
        JsonSchema jsonSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
        assertEquals(expectedId, jsonSchema.getId(), () -> "getId() should return the expected identifier for case: " + description);
    }
}
