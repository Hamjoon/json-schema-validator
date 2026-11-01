```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for {@link JsonSchema#validate(JsonNode, Consumer)}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Null / empty inputs</li>
 *   <li>Valid and invalid JSON instances</li>
 *   <li>ExecutionContext customisation (flags, attributes, exception propagation)</li>
 *   <li>Branch coverage for internal flags (recursive‑anchor, schema‑resource‑root)</li>
 *   <li>Array and boolean schema handling</li>
 *   <li>Complex constraints (pattern, minimum, format)</li>
 *   <li>$ref resolution (including malformed $ref)</li>
 *   <li>Invalid schema definitions</li>
 *   <li>Customiser throwing Errors / RuntimeExceptions</li>
 * </ul>
 *
 * <p>All tests use the public {@link JsonSchemaFactory#getSchema(JsonNode)} factory
 * to obtain a {@link JsonSchema} instance.  The {@code @BeforeEach} method builds a
 * simple default schema (type object) that is overridden inside individual tests
 * when a different schema is required.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_3_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSchema jsonSchema;   // schema under test
    private JsonNode   schemaNode;   // schema definition for the current test

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Default schema – a plain object with no required properties.
        schemaNode = MAPPER.readTree("{\"type\":\"object\"}");
        jsonSchema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaNode);
        assertNotNull(jsonSchema, "JsonSchema instance should be created");
    }

    @AfterEach
    void teardownAfterEach() {
        // No cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown needed.
    }

    /* ---------------------------------------------------------------------- */
    /* 1. Null and Empty Inputs                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    void validate_nullRootNode_andNullCustomizer_throwsNPE() {
        // Both arguments are null – the method should NPE on the root node.
        assertThrows(NullPointerException.class,
                () -> jsonSchema.validate((JsonNode) null, (Consumer<ExecutionContext>) null));
    }

    @Test
    void validate_emptyObject_withoutCustomizer_returnsEmptySet() throws Exception {
        // Empty JSON object {} against a schema without required fields.
        JsonNode instance = MAPPER.readTree("{}");
        Set<ValidationMessage> messages = jsonSchema.validate(instance, (Consumer<ExecutionContext>) null);
        assertTrue(messages.isEmpty(), "Expected no validation messages for empty object");
    }

    /* ---------------------------------------------------------------------- */
    /* 2. Valid JSON Validation                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    void validate_validObject_noOpCustomizer_returnsEmptySet() throws Exception {
        // Schema with required string and integer fields.
        schemaNode = MAPPER.readTree("{\"type\":\"object\",\"required\":[\"name\",\"age\"],"
                + "\"properties\":{\"name\":{\"type\":\"string\"},\"age\":{\"type\":\"integer\"}}}");
        jsonSchema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaNode);

        JsonNode instance = MAPPER.readTree("{\"name\":\"John\",\"age\":30}");
        Consumer<ExecutionContext> noOp = ctx -> { };
        Set<ValidationMessage> msgs = jsonSchema.validate(instance, noOp);
        assertTrue(msgs.isEmpty(), "Valid instance should produce no messages");
    }

    @Test
    void validate_validObject_customizerDisablesSchemaValidation_returnsEmptySet() throws Exception {
        // Same schema as above, but the customizer disables validation.
        schemaNode = MAPPER.readTree("{\"type\":\"object\",\"required\":[\"name\",\"age\"],"
                + "\"properties\":{\"name\":{\"type\":\"string\"},\"age\":{\"type\":\"integer\"},"
                + "\"extra\":{\"type\":\"string\"}}}");
        jsonSchema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaNode);

        JsonNode instance = MAPPER.readTree("{\"name\":\"John\",\"age\":30,\"extra\":\"value\"}");
        Consumer<ExecutionContext> disable = ctx -> ctx.setValidateSchema(false);
        Set<ValidationMessage> msgs = jsonSchema.validate(instance, disable);
        assertTrue(msgs.isEmpty(), "When validation is disabled, no messages should be returned");
    }

    /* ---------------------------------------------------------------------- */
    /* 3. Invalid JSON Validation                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    void validate_typeMismatches_returnsMessages() throws Exception {
        schemaNode = MAPPER.readTree("{\"type\":\"object\",\"required\":[\"name\",\"age\"],"
                + "\"properties\":{\"name\":{\"type\":\"string\"},\"age\":{\"type\":\"integer\"}}}");
        jsonSchema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaNode);

        JsonNode instance = MAPPER.readTree("{\"name\":123,\"age\":\"thirty\"}");
        Set<ValidationMessage> msgs = jsonSchema.validate(instance, ctx -> {});

        assertEquals(2, msgs.size(), "Two type‑mismatch messages expected");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("name")),
                "Message should mention property 'name'");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("age")),
                "Message should mention property 'age'");
    }

    @Test
    void validate_missingRequiredProperty_returnsMessage() throws Exception {
        schemaNode = MAPPER.readTree("{\"type\":\"object\",\"required\":[\"name\"],"
                + "\"properties\":{\"name\":{\"type\":\"string\"}}}");
        jsonSchema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaNode);

        JsonNode instance = MAPPER.readTree("{\"age\":30}");
        Set<ValidationMessage> msgs = jsonSchema.validate(instance, ctx -> {});

        assertEquals(1, msgs.size(), "One required‑property message expected");
        assertTrue(msgs.iterator().next().getMessage().toLowerCase().contains("name"),
                "Message should mention missing property 'name'");
    }

    /* ---------------------------------------------------------------------- */
    /* 4. ExecutionCustomizer Effects                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    void validate_customizerAddsAttribute_stillValid() throws Exception {
        // Same schema as the first valid test.
        schemaNode = MAPPER.readTree("{\"type\":\"object\",\"required\":[\"name\",\"age\"],"
                + "\"properties\":{\"name\":{\"type\":\"string\"},\"age\":{\"type\":\"integer\"}}}");
        jsonSchema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaNode);

        JsonNode instance = MAPPER.readTree("{\"name\":\"John\",\"age\":30}");
        Consumer<ExecutionContext> addAttr = ctx -> ctx.setAttribute("test", true);
        Set<ValidationMessage> msgs = jsonSchema.validate(instance, addAttr);
        assertTrue(msgs.isEmpty(), "Custom attribute should not affect validation result");
    }

    @Test
    void validate_customizerThrowsRuntimeException_propagates() throws Exception {
        JsonNode instance = MAPPER.readTree("{\"name\":\"John\",\"age\":30}");
        Consumer<ExecutionContext> bad = ctx -> { throw new RuntimeException("customizer failure"); };
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> jsonSchema.validate(instance, bad));
        assertEquals("customizer failure", ex.getMessage());
    }

    /* ---------------------------------------------------------------------- */
    /* 5. Branch Coverage for Internal Flags                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    void validate_recursiveAnchorEnabled_stillValid() throws Exception {
        schemaNode = MAPPER.readTree("{\"type\":\"object\",\"required\":[\"name\",\"age\"],"
                + "\"properties\":{\"name\":{\"type\":\"string\"},\"age\":{\"type\":\"integer\"}}}");
        jsonSchema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaNode);

        JsonNode instance = MAPPER.readTree("{\"name\":\"John\",\"age\":30}");
        Consumer<ExecutionContext> setRecursive = ctx -> ctx.setRecursiveAnchor(true);
        Set<ValidationMessage> msgs = jsonSchema.validate(instance, setRecursive);
        assertTrue(msgs.isEmpty(), "Recursive‑anchor flag should not break a valid instance");
    }

    @Test
    void validate_schemaResourceRootDisabled_stillValid() throws Exception {
        schemaNode = MAPPER.readTree("{\"type\":\"object\",\"required\":[\"name\",\"age\"],"
                + "\"properties\":{\"name\":{\"type\":\"string\"},\"age\":{\"type\":\"integer\"}}}");
        jsonSchema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaNode);

        JsonNode instance = MAPPER.readTree("{\"name\":\"John\",\"age\":30}");
        Consumer<ExecutionContext> disableRoot = ctx -> ctx.setSchemaResourceRoot(false);
        Set<ValidationMessage> msgs = jsonSchema.validate(instance, disableRoot);
        assertTrue(msgs.isEmpty(), "Disabling schema‑resource‑root flag should not affect success");
    }

    /* ---------------------------------------------------------------------- */
    /* 6. Array Schema Validation                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    void validate_validIntegerArray_returnsEmptySet() throws Exception {
        schemaNode = MAPPER.readTree("{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}");
        jsonSchema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaNode);

        JsonNode instance = MAPPER.readTree("[1,2,3]");
        Set<ValidationMessage> msgs = jsonSchema.validate(instance, (Consumer<ExecutionContext>) null);
        assertTrue(msgs.isEmpty(), "Valid integer array should produce no messages");
    }

    @Test
    void validate_arrayWithWrongElement_returnsMessage() throws Exception {
        schemaNode = MAPPER.readTree("{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}");
        jsonSchema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaNode);

        JsonNode instance = MAPPER.readTree("[1,\"two\",3]");
        Set<ValidationMessage> msgs = jsonSchema.validate(instance, (Consumer<ExecutionContext>) null);
        assertEquals(1, msgs.size(), "One type‑mismatch message expected for element at index 1");
        assertTrue(msgs.iterator().next().getMessage().toLowerCase().contains("index 1"),
                "Message should reference the offending index");
    }

    /* ---------------------------------------------------------------------- */
    /* 7. Boolean Schema Validation                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    void validate_booleanTrueSchema_alwaysValid() throws Exception {
        schemaNode = MAPPER.readTree("true");
        jsonSchema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaNode);

        JsonNode instance = MAPPER.readTree("{\"any\":\"value\"}");
        Set<ValidationMessage> msgs = jsonSchema.validate(instance, (Consumer<ExecutionContext>) null);
        assertTrue(msgs.isEmpty(), "Boolean true schema should accept any instance");
    }

    @Test
    void validate_booleanFalseSchema_alwaysFails() throws Exception {
        schemaNode = MAPPER.readTree("false");
        jsonSchema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaNode);

        JsonNode instance = MAPPER.readTree("{\"any\":\"value\"}");
        Set<ValidationMessage> msgs = jsonSchema.validate(instance, (Consumer<ExecutionContext>) null);
        assertEquals(1, msgs.size(), "Boolean false schema should always produce a failure");
        assertTrue(msgs.iterator().next().getMessage().toLowerCase().contains("false"),
                "Message should indicate the false schema rejection");
    }

    /* ---------------------------------------------------------------------- */
    /* 8. Complex Constraint Validation                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    void validate_complexConstraints_allSatisfied_returnsEmptySet() throws Exception {
        String schemaJson = """
                {
                  "type":"object",
                  "required":["id","age","email"],
                  "properties":{
                    "id":{"type":"string","pattern":"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"},
                    "age":{"type":"integer","minimum":18},
                    "email":{"type":"string","format":"email"}
                  }
                }""";
        schemaNode = MAPPER.readTree(schemaJson);
        jsonSchema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaNode);

        JsonNode instance = MAPPER.readTree("""
                {"id":"123e4567-e89b-12d3-a456-426614174000","age":25,"email":"john@example.com"}""");
        Set<ValidationMessage> msgs = jsonSchema.validate(instance, (Consumer<ExecutionContext>) null);
        assertTrue(msgs.isEmpty(), "All constraints satisfied – expect no messages");
    }

    @Test
    void validate_complexConstraints_violationsReturned() throws Exception {
        String schemaJson = """
                {
                  "type":"object",
                  "required":["id","age","email"],
                  "properties":{
                    "id":{"type":"string","pattern":"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"},
                    "age":{"type":"integer","minimum":18},
                    "email":{"type":"string","format":"email"}
                  }
                }""";
        schemaNode = MAPPER.readTree(schemaJson);
        jsonSchema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaNode);

        JsonNode instance = MAPPER.readTree("""
                {"id":"not-a-uuid","age":15,"email":"not-an-email"}""");
        Set<ValidationMessage> msgs = jsonSchema.validate(instance, (Consumer<ExecutionContext>) null);
        assertEquals(3, msgs.size(), "Three distinct constraint violations expected");
        String joined = msgs.stream().map(ValidationMessage::getMessage).collect(Collectors.joining(" ").toLowerCase());
        assertTrue(joined.contains("pattern"), "Pattern mismatch should be reported");
        assertTrue(joined.contains("minimum"), "Minimum violation should be reported");
        assertTrue(joined.contains("email"), "Email format violation should be reported");
    }

    /* ---------------------------------------------------------------------- */
    /* 9. $ref Handling                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    void validate_refSchema_validInstance_returnsEmptySet() throws Exception {
        String schemaJson = """
                {
                  "type":"object",
                  "properties":{
                    "address":{"$ref":"#/definitions/address"}
                  },
                  "definitions":{
                    "address":{
                      "type":"object",
                      "required":["street","zip"],
                      "properties":{
                        "street":{"type":"string"},
                        "zip":{"type":"string"}
                      }
                    }
                  }
                }""";
        schemaNode = MAPPER.readTree(schemaJson);
        jsonSchema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaNode);

        JsonNode instance = MAPPER.readTree("{\"address\":{\"street\":\"Main St\",\"zip\":\"12345\"}}");
        Set<ValidationMessage> msgs = jsonSchema.validate(instance, (Consumer<ExecutionContext>) null);
        assertTrue(msgs.isEmpty(), "Valid $ref instance should produce no messages");
    }

    @Test
    void validate_refSchema_typeMismatch_returnsMessage() throws Exception {
        String schemaJson = """
                {
                  "type":"object",
                  "properties":{
                    "address":{"$ref":"#/definitions/address"}
                  },
                  "definitions":{
                    "address":{
                      "type":"object",
                      "required":["street","zip"],
                      "properties":{
                        "street":{"type":"string"},
                        "zip":{"type":"string"}
                      }
                    }
                  }
                }""";
        schemaNode = MAPPER.readTree(schemaJson);
        jsonSchema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaNode);

        JsonNode instance = MAPPER.readTree("{\"address\":{\"street\":10,\"zip\":\"12345\"}}");
        Set<ValidationMessage> msgs = jsonSchema.validate(instance, (Consumer<ExecutionContext>) null);
        assertEquals(1, msgs.size(), "One type‑mismatch message expected from $ref");
        assertTrue(msgs.iterator().next().getMessage().toLowerCase().contains("street"),
                "Message should reference property 'street' inside $ref");
    }

    /* ---------------------------------------------------------------------- */
    /* 10. Custom ExecutionContext Flags Interaction                         */
    /* ---------------------------------------------------------------------- */

    @Test
    void validate_disableValidateSchema_suppressesAllMessages() throws Exception {
        // Schema requires a property that will be missing.
        schemaNode = MAPPER.readTree("{\"type\":\"object\",\"required\":[\"age\"],"
                + "\"properties\":{\"age\":{\"type\":\"integer\"}}}");
        jsonSchema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaNode);

        JsonNode instance = MAPPER.readTree("{\"name\":\"John\"}");
        Consumer<ExecutionContext> disable = ctx -> ctx.setValidateSchema(false);
        Set<ValidationMessage> msgs = jsonSchema.validate(instance, disable);
        assertTrue(msgs.isEmpty(), "Disabling validation should suppress required‑property errors");
    }

    @Test
    void validate_validateSchemaRootFlagEffect() throws Exception {
        // Same schema as above, but we turn off root validation while keeping validation enabled.
        schemaNode = MAPPER.readTree("{\"type\":\"object\",\"required\":[\"age\"],"
                + "\"properties\":{\"age\":{\"type\":\"integer\"}}}");
        jsonSchema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaNode);

        JsonNode instance = MAPPER.readTree("{\"name\":\"John\"}");
        Consumer<ExecutionContext> customizer = ctx -> {
            ctx.setValidateSchema(true);
            ctx.setValidateSchemaRoot(false);
        };
        Set<ValidationMessage> msgs = jsonSchema.validate(instance, customizer);
        // The missing required property should still be reported because validation of the instance itself occurs.
        assertEquals(1, msgs.size(), "Missing required property should still be reported");
        assertTrue(msgs.iterator().next().getMessage().toLowerCase().contains("age"));
    }

    /* ---------------------------------------------------------------------- */
    /* 11. Invalid ExecutionCustomizer Handling                              */
    /* ---------------------------------------------------------------------- */

    @Test
    void validate_customizerThrowsIllegalArgumentException_propagates() {
        JsonNode instance = MAPPER.createObjectNode();
        Consumer<ExecutionContext> bad = ctx -> { throw new IllegalArgumentException("bad customizer"); };
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> jsonSchema.validate(instance, bad));
        assertEquals("bad customizer", ex.getMessage());
    }

    @Test
    void validate_customizerThrowsNullPointerException_propagates() {
        JsonNode instance = MAPPER.createObjectNode();
        Consumer<ExecutionContext> bad = ctx -> { throw new NullPointerException("NPE from customizer"); };
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> jsonSchema.validate(instance, bad));
        assertEquals("NPE from customizer", ex.getMessage());
    }

    /* ---------------------------------------------------------------------- */
    /* 12. Malformed $ref Handling                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    void validate_malformedRefUrlDecoderThrowsJsonSchemaException() {
        String schemaJson = """
                {
                  "type":"object",
                  "properties":{
                    "refField":{"$ref":"%ZZ"}
                  }
                }""";
        // The factory will parse the schema; the error surfaces during validation.
        schemaNode = MAPPER.readTree(schemaJson);
        jsonSchema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaNode);

        JsonNode instance = MAPPER.readTree("{\"refField\":\"value\"}");
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> jsonSchema.validate(instance, (Consumer<ExecutionContext>) null));
        assertTrue(ex.getMessage().contains("Unable to resolve $ref") ||
                ex.getMessage().contains("Invalid $ref"),
                "Exception message should indicate a $ref resolution problem");
    }

    @Test
    void validate_nonExistentRefThrowsIllegalArgumentException() {
        String schemaJson = """
                {
                  "type":"object",
                  "properties":{
                    "refField":{"$ref":"#/definitions/missing"}
                  }
                }""";
        schemaNode = MAPPER.readTree(schemaJson);
        jsonSchema = JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaNode);

        JsonNode instance = MAPPER.readTree("{\"refField\":\"value\"}");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> jsonSchema.validate(instance, (Consumer<ExecutionContext>) null));
        assertTrue(ex.getMessage().toLowerCase().contains("invalid $ref"),
                "Exception message should mention an invalid $ref");
    }

    /* ---------------------------------------------------------------------- */
    /* 13. Invalid Schema Definition                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    void validate_schemaWithUnsupportedTypeKeyword_throwsIllegalArgumentException() {
        String schemaJson = "{\"type\":\"unknownType\"}";
        schemaNode = MAPPER.readTree(schemaJson);
        // The factory itself validates the schema definition.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaNode));
        assertTrue(ex.getMessage().toLowerCase().contains("invalid type keyword"));
    }

    @Test
    void validate_malformedJsonSchema_throwsJsonProcessingException() {
        // Missing closing brace.
        String malformed = "{\"type\":\"object\"";
        assertThrows(JsonProcessingException.class, () -> MAPPER.readTree(malformed));
        // The exception occurs before we even obtain a JsonSchema instance, so we just verify parsing.
    }

    /* ---------------------------------------------------------------------- */
    /* 14. ExecutionCustomizer Throws Error                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    void validate_customizerThrowsAssertionError_propagates() {
        JsonNode instance = MAPPER.createObjectNode();
        Consumer<ExecutionContext> bad = ctx -> { throw new AssertionError("customizer assertion failure"); };
        AssertionError err = assertThrows(AssertionError.class,
                () -> jsonSchema.validate(instance, bad));
        assertEquals("customizer assertion failure", err.getMessage());
    }

    @Test
    void validate_customizerThrowsNullPointerExceptionAfterMutation_propagates() {
        JsonNode instance = MAPPER.createObjectNode();
        Consumer<ExecutionContext> bad = ctx -> {
            ctx.setValidateSchema(true);
            throw new NullPointerException("after mutation");
        };
        NullPointerException npe = assertThrows(NullPointerException.class,
                () -> jsonSchema.validate(instance, bad));
        assertEquals("after mutation", npe.getMessage());
    }
}
```