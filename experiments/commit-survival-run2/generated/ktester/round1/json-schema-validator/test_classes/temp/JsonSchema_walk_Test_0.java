package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_Test {

    /**
     * Mocked validation context used to build the {@link JsonSchema} instance.
     */
    @Mock
    ValidationContext mockValidationContext;

    /**
     * Mocked root schema node – the content of the schema is irrelevant for this test.
     */
    @Mock
    JsonNode mockSchemaNode;

    /**
     * Mocked JSON instance that will be walked/validated.
     */
    @Mock
    JsonNode mockInstanceNode;

    /**
     * The {@link JsonSchema} under test.
     */
    JsonSchema jsonSchema;

    /**
     * Jackson helper for building {@link JsonNode} trees from JSON literals.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setupBeforeEach() {
        // -----------------------------------------------------------------
        // Configure the minimal behaviour required by the JsonSchema ctor.
        // -----------------------------------------------------------------
        // The constructor reads meta‑schema and configuration from the context.
        // Returning null for getConfig() disables the keyword‑walk listener setup.
        when(mockValidationContext.getMetaSchema()).thenReturn(mock(JsonMetaSchema.class));
        when(mockValidationContext.getConfig()).thenReturn(null);
        // Build the JsonSchema instance using the public factory method.
        // All arguments other than the validation context are dummy values.
        jsonSchema = JsonSchema.from(// ValidationContext
        mockValidationContext, // schemaPath (empty – not used in this test)
        "", // currentUri (null is accepted)
        null, // schemaNode (mocked)
        mockSchemaNode, // parent schema (none)
        null, // suppressSubSchemaRetrieval
        false);
        assertNotNull(jsonSchema, "JsonSchema instance should be created successfully");
        // Minimal behaviour required by the JsonSchema factory.
        when(mockValidationContext.getMetaSchema()).thenReturn(mock(JsonMetaSchema.class));
        when(mockValidationContext.getConfig()).thenReturn(null);
    }

    /**
     * Builds a {@link JsonSchema} from a JSON string describing the schema.
     */
    private JsonSchema buildSchema(String schemaJson) {
        try {
            JsonNode schemaNode = MAPPER.readTree(schemaJson);
            JsonSchema schema = JsonSchema.from(mockValidationContext, // schemaPath – not used in these tests
            "", // currentUri – null is fine for in‑memory schemas
            null, schemaNode, // parent schema – none
            null, // suppressSubSchemaRetrieval – default behaviour
            false);
            assertNotNull(schema, "JsonSchema instance must be created");
            return schema;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build schema from JSON", e);
        }
    }

    /**
     * Executes the walk operation with the supplied parameters.
     */
    private ValidationResult walk(JsonSchema schema, JsonNode instance, boolean validate) {
        ExecutionContext ctx = schema.createExecutionContext();
        return schema.walk(ctx, instance, validate);
    }

    /**
     * Verifies that {@link JsonSchema#walk(ExecutionContext, JsonNode, boolean)} executes
     * without throwing and returns a non‑null {@link ValidationResult}.
     *
     * <p>The test does not assert the content of the result because the schema is
     * mocked and therefore contains no concrete validators. The important contract
     * is that the method handles the supplied arguments gracefully.</p>
     */
    @Test
    void walk_ShouldReturnValidationResultWithoutException() {
        // GIVEN
        // real, lightweight
        ExecutionContext executionContext = new ExecutionContext();
        // the flag does not affect the mocked path
        boolean shouldValidateSchema = false;
        // WHEN
        ValidationResult result = assertDoesNotThrow(() -> jsonSchema.walk(executionContext, mockInstanceNode, shouldValidateSchema), "walk(...) should not throw any exception");
        // THEN
        assertNotNull(result, "walk should return a non‑null ValidationResult");
        // The result may be empty because the schema does not define any validators.
        // An empty result is still a valid outcome for this test.
    }

    /**
     * Successful validation of a simple object schema.
     */
    @Test
    void walk_successfulValidation() throws Exception {
        // schema: {"type":"object","properties":{"name":{"type":"string"}}}
        JsonSchema schema = buildSchema("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}");
        JsonNode instance = MAPPER.readTree("{\"name\":\"John\"}");
        ValidationResult result = walk(schema, instance, true);
        assertTrue(result.isSuccess(), "Result should be successful");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

    /**
     * Type‑mismatch validation error (integer expected, string supplied).
     */
    @Test
    void walk_failureDueToTypeMismatch() throws Exception {
        JsonSchema schema = buildSchema("{\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}}");
        JsonNode instance = MAPPER.readTree("{\"age\":\"twenty\"}");
        ValidationResult result = walk(schema, instance, true);
        assertFalse(result.isSuccess(), "Result must indicate failure");
        assertTrue(result.getMessages().stream().anyMatch(m -> "type".equals(m.getKeyword())), "Message with keyword 'type' expected");
    }

    /**
     * Missing required property should produce a validation error.
     */
    @Test
    void walk_missingRequiredProperty() throws Exception {
        JsonSchema schema = buildSchema("{\"type\":\"object\",\"required\":[\"id\"],\"properties\":{\"id\":{\"type\":\"string\"}}}");
        JsonNode instance = MAPPER.readTree("{}");
        ValidationResult result = walk(schema, instance, true);
        assertFalse(result.isSuccess(), "Result must indicate failure");
        assertTrue(result.getMessages().stream().anyMatch(m -> "required".equals(m.getKeyword())), "Message with keyword 'required' expected");
    }

    /**
     * When the validation flag is false the walk should skip validation completely.
     */
    @Test
    void walk_noValidationWhenFlagFalse() throws Exception {
        JsonSchema schema = buildSchema("{\"type\":\"object\",\"required\":[\"id\"],\"properties\":{\"id\":{\"type\":\"string\"}}}");
        JsonNode instance = MAPPER.readTree("{}");
        ValidationResult result = walk(schema, instance, false);
        // The contract states that validation is skipped → success with no messages.
        assertTrue(result.isSuccess(), "Result should be successful when validation is disabled");
        assertTrue(result.getMessages().isEmpty(), "No messages expected when validation is disabled");
    }

    /**
     * Passing a null JSON instance must raise a NullPointerException.
     */
    @Test
    void walk_nullNode() {
        JsonSchema schema = buildSchema("{\"type\":\"object\"}");
        ExecutionContext ctx = schema.createExecutionContext();
        assertThrows(NullPointerException.class, () -> schema.walk(ctx, null, true), "A null JsonNode should trigger NullPointerException");
    }

    /**
     * Passing a null ExecutionContext must raise an IllegalArgumentException.
     */
    @Test
    void walk_nullExecutionContext() throws Exception {
        JsonSchema schema = buildSchema("{\"type\":\"object\"}");
        JsonNode instance = MAPPER.readTree("{\"any\":\"value\"}");
        assertThrows(IllegalArgumentException.class, () -> schema.walk(null, instance, true), "A null ExecutionContext should trigger IllegalArgumentException");
    }

    /**
     * Internal $ref resolution must work and validate successfully.
     */
    @Test
    void walk_schemaWithExternalRef() throws Exception {
        String schemaJson = "{" + "\"$ref\":\"#/definitions/person\"," + "\"definitions\":{" + "   \"person\":{" + "       \"type\":\"object\"," + "       \"properties\":{\"firstName\":{\"type\":\"string\"}}" + "   }" + "}" + "}";
        JsonSchema schema = buildSchema(schemaJson);
        JsonNode instance = MAPPER.readTree("{\"firstName\":\"Alice\"}");
        ValidationResult result = walk(schema, instance, true);
        assertTrue(result.isSuccess(), "Reference should resolve and validation succeed");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

    /**
     * Dynamic anchor should behave like a normal schema for validation purposes.
     */
    @Test
    void walk_dynamicAnchorResolution() throws Exception {
        String schemaJson = "{" + "\"$dynamicAnchor\":\"myAnchor\"," + "\"type\":\"object\"," + "\"properties\":{\"code\":{\"type\":\"string\"}}" + "}";
        JsonSchema schema = buildSchema(schemaJson);
        JsonNode instance = MAPPER.readTree("{\"code\":\"XYZ\"}");
        ValidationResult result = walk(schema, instance, true);
        assertTrue(result.isSuccess(), "Dynamic anchor should not interfere with validation");
        assertTrue(result.getMessages().isEmpty(), "No messages expected");
    }

    /**
     * Malformed $ref URI must cause an exception when walking.
     */
    @Test
    void walk_malformedRefUri() {
        String schemaJson = "{\"$ref\":\"ht!tp://[invalid]\"}";
        JsonSchema schema = buildSchema(schemaJson);
        // empty object
        JsonNode instance = MAPPER.createObjectNode();
        ExecutionContext ctx = schema.createExecutionContext();
        // The implementation may wrap URI syntax problems in IllegalArgumentException.
        assertThrows(RuntimeException.class, () -> schema.walk(ctx, instance, true), "Malformed $ref URI should cause an exception");
    }

    /**
     * Unresolvable external $ref must result in a JsonSchemaException / IllegalArgumentException.
     */
    @Test
    void walk_unresolvableExternalRef() {
        String schemaJson = "{\"$ref\":\"http://nonexistent.example.com/schema.json\"}";
        JsonSchema schema = buildSchema(schemaJson);
        // empty object
        JsonNode instance = MAPPER.createObjectNode();
        ExecutionContext ctx = schema.createExecutionContext();
        assertThrows(RuntimeException.class, () -> schema.walk(ctx, instance, true), "Unresolvable external $ref should raise an exception");
    }

    /**
     * Circular $ref definitions must be detected and cause an exception rather than stack overflow.
     */
    @Test
    void walk_circularRefDetection() {
        String schemaJson = "{" + "\"definitions\":{" + "   \"A\":{\"$ref\":\"#/definitions/B\"}," + "   \"B\":{\"$ref\":\"#/definitions/A\"}" + "}," + "\"$ref\":\"#/definitions/A\"" + "}";
        JsonSchema schema = buildSchema(schemaJson);
        // empty object
        JsonNode instance = MAPPER.createObjectNode();
        ExecutionContext ctx = schema.createExecutionContext();
        assertThrows(RuntimeException.class, () -> schema.walk(ctx, instance, true), "Circular $ref resolution should be detected and abort with an exception");
    }

    /**
     * Schema containing an unsupported keyword must raise an exception.
     */
    @Test
    void walk_unsupportedKeyword() {
        String schemaJson = "{" + "\"type\":\"object\"," + "\"properties\":{" + "   \"age\":{\"type\":\"integer\"}" + "}," + "\"unknownKeyword\":123" + "}";
        JsonSchema schema = buildSchema(schemaJson);
        JsonNode instance = MAPPER.createObjectNode().put("age", 30);
        ExecutionContext ctx = schema.createExecutionContext();
        assertThrows(RuntimeException.class, () -> schema.walk(ctx, instance, true), "Unsupported keyword should cause a JsonSchemaException");
    }

    /**
     * Root schema must be a JSON object; an array root should trigger IllegalArgumentException.
     */
    @Test
    void walk_schemaNotObject() {
        // array, not object
        String schemaJson = "[{\"type\":\"string\"}]";
        // The factory itself validates the root node shape; we expect an exception here.
        assertThrows(RuntimeException.class, () -> buildSchema(schemaJson), "Root schema that is not an object should raise an exception");
    }
}
