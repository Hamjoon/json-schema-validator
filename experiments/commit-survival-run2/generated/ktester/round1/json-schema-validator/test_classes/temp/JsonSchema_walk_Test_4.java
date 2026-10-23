package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JsonSchema_walk_Test {

    //     @Mock
    ValidationContext mockValidationContext;

    //     @Mock
    JsonNode mockSchemaNode;

    //     @Mock
    JsonNode mockInstanceNode;

    /**
     * The {@link JsonSchema} under test.
     */
    JsonSchema jsonSchema;

    /**
     * Jackson helper for building {@link JsonNode} trees from JSON literals.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    //     @BeforeEach
    void setupBeforeEach() {
        // -----------------------------------------------------------------
        // Configure the minimal behaviour required by the JsonSchema ctor.
        // -----------------------------------------------------------------
        // The constructor reads meta‑schema and configuration from the context.
        // Returning null for getConfig() disables the keyword‑walk listener setup.
        //         when(mockValidationContext.getMetaSchema()).thenReturn(mock(JsonMetaSchema.class));
        //         when(mockValidationContext.getConfig()).thenReturn(null);
        // Build the JsonSchema instance using the public factory method.
        // All arguments other than the validation context are dummy values.
        // ValidationContext
        // ValidationContext
        // schemaPath (empty – not used in this test)
        jsonSchema = // currentUri (null is accepted)
        JsonSchema.// schemaNode (mocked)
        from(// parent schema (none)
        mockValidationContext, // suppressSubSchemaRetrieval
        "", null, mockSchemaNode, null, false);
        assertNotNull(jsonSchema, "JsonSchema instance should be created successfully");
        // Minimal behaviour required by the JsonSchema factory.
        //         when(mockValidationContext.getMetaSchema()).thenReturn(mock(JsonMetaSchema.class));
        //         when(mockValidationContext.getConfig()).thenReturn(null);
    }

    /**
     * Builds a {@link JsonSchema} from a JSON string describing the schema.
     */
    private JsonSchema buildSchema(String schemaJson) {
        try {
            JsonNode schemaNode = MAPPER.readTree(schemaJson);
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
            JsonSchema schema = factory.getSchema(schemaNode);
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

    @Test
    void walk_ShouldReturnValidationResultWithoutException() {
        JsonSchema schema = buildSchema("{\"type\":\"object\"}");
        JsonNode instance = MAPPER.createObjectNode();
        ValidationResult result = assertDoesNotThrow(() -> schema.walk(schema.createExecutionContext(), instance, false), "walk(...) should not throw any exception");
        assertNotNull(result, "walk should return a non‑null ValidationResult");
    }

//     @Test
//     void walk_successfulValidation() throws Exception {
//         JsonSchema schema = buildSchema("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}");
//         JsonNode instance = MAPPER.readTree("{\"name\":\"John\"}");
//         ValidationResult result = walk(schema, instance, true);
//         assertTrue(result.isSuccess(), "Result should be successful");
//         assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
//     }

//     @Test
//     void walk_failureDueToTypeMismatch() throws Exception {
//         JsonSchema schema = buildSchema("{\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}}");
//         JsonNode instance = MAPPER.readTree("{\"age\":\"twenty\"}");
//         ValidationResult result = walk(schema, instance, true);
//         assertFalse(result.isSuccess(), "Result must indicate failure");
//         assertTrue(result.getMessages().stream().anyMatch(m -> "type".equals(m.getKeyword())), "Message with keyword 'type' expected");
//     }

//     @Test
//     void walk_missingRequiredProperty() throws Exception {
//         JsonSchema schema = buildSchema("{\"type\":\"object\",\"required\":[\"id\"],\"properties\":{\"id\":{\"type\":\"string\"}}}");
//         JsonNode instance = MAPPER.readTree("{}");
//         ValidationResult result = walk(schema, instance, true);
//         assertFalse(result.isSuccess(), "Result must indicate failure");
//         assertTrue(result.getMessages().stream().anyMatch(m -> "required".equals(m.getKeyword())), "Message with keyword 'required' expected");
//     }

//     @Test
//     void walk_noValidationWhenFlagFalse() throws Exception {
//         JsonSchema schema = buildSchema("{\"type\":\"object\",\"required\":[\"id\"],\"properties\":{\"id\":{\"type\":\"string\"}}}");
//         JsonNode instance = MAPPER.readTree("{}");
//         ValidationResult result = walk(schema, instance, false);
//         assertTrue(result.isSuccess(), "Result should be successful when validation is disabled");
//         assertTrue(result.getMessages().isEmpty(), "No messages expected when validation is disabled");
//     }

    @Test
    void walk_nullNode() {
        JsonSchema schema = buildSchema("{\"type\":\"object\"}");
        ExecutionContext ctx = schema.createExecutionContext();
        assertThrows(NullPointerException.class, () -> schema.walk(ctx, null, true), "A null JsonNode should trigger NullPointerException");
    }

    @Test
    void walk_nullExecutionContext() throws Exception {
        JsonSchema schema = buildSchema("{\"type\":\"object\"}");
        JsonNode instance = MAPPER.readTree("{\"any\":\"value\"}");
        assertThrows(IllegalArgumentException.class, () -> schema.walk(null, instance, true), "A null ExecutionContext should trigger IllegalArgumentException");
    }

//     @Test
//     void walk_schemaWithExternalRef() throws Exception {
//         String schemaJson = "{" + "\"$ref\":\"#/definitions/person\"," + "\"definitions\":{" + "   \"person\":{" + "       \"type\":\"object\"," + "       \"properties\":{\"firstName\":{\"type\":\"string\"}}" + "   }" + "}" + "}";
//         JsonSchema schema = buildSchema(schemaJson);
//         JsonNode instance = MAPPER.readTree("{\"firstName\":\"Alice\"}");
//         ValidationResult result = walk(schema, instance, true);
//         assertTrue(result.isSuccess(), "Reference should resolve and validation succeed");
//         assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
//     }

//     @Test
//     void walk_dynamicAnchorResolution() throws Exception {
//         String schemaJson = "{" + "\"$dynamicAnchor\":\"myAnchor\"," + "\"type\":\"object\"," + "\"properties\":{\"code\":{\"type\":\"string\"}}" + "}";
//         JsonSchema schema = buildSchema(schemaJson);
//         JsonNode instance = MAPPER.readTree("{\"code\":\"XYZ\"}");
//         ValidationResult result = walk(schema, instance, true);
//         assertTrue(result.isSuccess(), "Dynamic anchor should not interfere with validation");
//         assertTrue(result.getMessages().isEmpty(), "No messages expected");
//     }

    @Test
    void walk_malformedRefUri() {
        String schemaJson = "{\"$ref\":\"ht!tp://[invalid]\"}";
        JsonSchema schema = buildSchema(schemaJson);
        JsonNode instance = MAPPER.createObjectNode();
        ExecutionContext ctx = schema.createExecutionContext();
        assertThrows(RuntimeException.class, () -> schema.walk(ctx, instance, true), "Malformed $ref URI should cause an exception");
    }

    @Test
    void walk_unresolvableExternalRef() {
        String schemaJson = "{\"$ref\":\"http://nonexistent.example.com/schema.json\"}";
        JsonSchema schema = buildSchema(schemaJson);
        JsonNode instance = MAPPER.createObjectNode();
        ExecutionContext ctx = schema.createExecutionContext();
        assertThrows(RuntimeException.class, () -> schema.walk(ctx, instance, true), "Unresolvable external $ref should raise an exception");
    }

    @Test
    void walk_circularRefDetection() {
        String schemaJson = "{" + "\"definitions\":{" + "   \"A\":{\"$ref\":\"#/definitions/B\"}," + "   \"B\":{\"$ref\":\"#/definitions/A\"}" + "}," + "\"$ref\":\"#/definitions/A\"" + "}";
        JsonSchema schema = buildSchema(schemaJson);
        JsonNode instance = MAPPER.createObjectNode();
        ExecutionContext ctx = schema.createExecutionContext();
        assertThrows(RuntimeException.class, () -> schema.walk(ctx, instance, true), "Circular $ref resolution should be detected and abort with an exception");
    }

    @Test
    void walk_unsupportedKeyword() {
        String schemaJson = "{" + "\"type\":\"object\"," + "\"properties\":{" + "   \"age\":{\"type\":\"integer\"}" + "}," + "\"unknownKeyword\":123" + "}";
        JsonSchema schema = buildSchema(schemaJson);
        JsonNode instance = MAPPER.createObjectNode().put("age", 30);
        ExecutionContext ctx = schema.createExecutionContext();
        assertThrows(RuntimeException.class, () -> schema.walk(ctx, instance, true), "Unsupported keyword should cause a JsonSchemaException");
    }

    @Test
    void walk_schemaNotObject() {
        String schemaJson = "[{\"type\":\"string\"}]";
        assertThrows(RuntimeException.class, () -> buildSchema(schemaJson), "Root schema that is not an object should raise an exception");
    }
}