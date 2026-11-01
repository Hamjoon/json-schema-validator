package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.core.JsonParseException;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.IOException;
import java.net.URI;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_5_Test {

    /**
     * The factory under test – a spy so we can stub protected methods.
     */
    private JsonSchemaFactory factorySpy;

    /**
     * A mock {@link JsonSchema} that will be returned by the stubbed method.
     */
    private JsonSchema mockSchema;

    @BeforeEach
    void setupBeforeEach() {
        // Obtain a real factory instance with default configuration.
        JsonSchemaFactory realFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        // Spy on the real instance so that we can stub protected methods.
        factorySpy = Mockito.spy(realFactory);
        // Prepare a mock JsonSchema that will be returned by the stub.
        mockSchema = mock(JsonSchema.class);
        // Stub the protected getMappedSchema method (used internally by getSchema(String)).
        // The method signature is: protected JsonSchema getMappedSchema(SchemaLocation, SchemaValidatorsConfig)
        doReturn(mockSchema).when(factorySpy).getMappedSchema(any(SchemaLocation.class), any(SchemaValidatorsConfig.class));
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the spy after each test to avoid cross‑test interference.
        reset(factorySpy);
    }

    private static Stream<Arguments> validSchemaProvider() {
        return Stream.of(Arguments.of("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}", "Valid JSON schema with a property"), Arguments.of("type: object\nproperties:\n  age:\n    type: integer", "Valid YAML schema"), Arguments.of("{\"$ref\":\"http://json-schema.org/draft-07/schema#\"}", "JSON schema referencing the Draft‑07 meta‑schema"));
    }

    private static Stream<Arguments> edgeCaseSchemaProvider() {
        return Stream.of(Arguments.of("   { \"type\": \"string\" }   ", "JSON schema with leading/trailing whitespace"), Arguments.of("{}", "Empty JSON object (valid schema)"), Arguments.of("   \t\n  ", "String containing only whitespace – should be rejected"));
    }

    @Test
    void getSchema_String_DelegatesToGetMappedSchemaAndReturnsResult() {
        // GIVEN: a minimal JSON schema string.
        String schemaJson = "{\"type\":\"object\"}";
        // WHEN: the public getSchema(String) method is invoked.
        JsonSchema result = factorySpy.getSchema(schemaJson);
        // THEN: the result should be the mock we stubbed, and the internal
        //       getMappedSchema method must have been called exactly once.
        assertSame(mockSchema, result, "The factory should return the stubbed JsonSchema instance.");
        // Verify that the protected method was invoked with non‑null arguments.
        verify(factorySpy, times(1)).getMappedSchema(any(SchemaLocation.class), any(SchemaValidatorsConfig.class));
    }

    @Test
    void getSchema_String_NullArgument_ThrowsNullPointerException() {
        // GIVEN: a null schema string.
        String nullSchema = null;
        // WHEN / THEN: a NullPointerException (or IllegalArgumentException) is expected.
        // The actual implementation throws a NullPointerException when the argument is null.
        assertThrows(NullPointerException.class, () -> factorySpy.getSchema(nullSchema));
    }

    @Test
    void getSchema_String_EmptyString_ThrowsIllegalArgumentException() {
        // GIVEN: an empty schema string.
        String emptySchema = "";
        // WHEN / THEN: an IllegalArgumentException is expected because the method
        // ultimately tries to parse the string as a URI or JSON content.
        assertThrows(IllegalArgumentException.class, () -> factorySpy.getSchema(emptySchema));
    }

    /**
     * Valid inputs (JSON, YAML, $ref) – the method should return a non‑null
     * {@link JsonSchema} instance.
     */
    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("validSchemaProvider")
    void getSchema_ValidInput_ReturnsJsonSchema(String schema, String description) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        JsonSchema result = factory.getSchema(schema);
        assertNotNull(result, "Result must not be null for: " + description);
        assertTrue(result instanceof JsonSchema, "Result must be an instance of JsonSchema for: " + description);
    }

    /**
     * Edge‑case inputs that should still be accepted.
     */
    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("edgeCaseSchemaProvider")
    void getSchema_EdgeCases_ReturnsJsonSchema(String schema, String description) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        JsonSchema result = factory.getSchema(schema);
        assertNotNull(result, "Result must not be null for: " + description);
    }

    @Test
    void getSchema_WhitespaceOnly_ThrowsIllegalArgumentException() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema("   \t\n  "), "Whitespace‑only input must trigger IllegalArgumentException");
    }

    @Test
    void getSchema_MalformedJson_ThrowsJsonParseException() {
        String malformedJson = "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" }";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        assertThrows(JsonParseException.class, () -> factory.getSchema(malformedJson), "Malformed JSON should result in JsonParseException");
    }

    @Test
    void getSchema_MalformedYaml_ThrowsJsonParseException() {
        String malformedYaml = "type: object\nproperties:\n  age: \n    type: integer: extra";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        assertThrows(JsonParseException.class, () -> factory.getSchema(malformedYaml), "Malformed YAML should result in JsonParseException");
    }

    @Test
    void getSchema_InvalidRefSyntax_ThrowsJsonSchemaException() {
        String invalidRef = "{ \"$ref\": \"not-a-valid-uri\" }";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(invalidRef), "Invalid $ref URI should trigger JsonSchemaException");
    }

    @Test
    void getSchema_UnresolvableExternalUri_ThrowsIOException() {
        String externalRef = "{ \"$ref\": \"http://nonexistent.example.com/schema.json\" }";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        assertThrows(IOException.class, () -> factory.getSchema(externalRef), "Unresolvable external URI should propagate IOException");
    }

    @Test
    void getSchema_UnsupportedSpecVersion_ThrowsIllegalArgumentException() {
        String unknownDraft = "{ \"$schema\": \"http://json-schema.org/draft-99/schema#\" }";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(unknownDraft), "Unknown draft version should cause IllegalArgumentException");
    }
}
