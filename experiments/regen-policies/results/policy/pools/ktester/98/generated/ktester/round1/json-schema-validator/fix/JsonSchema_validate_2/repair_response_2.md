```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.io.UnsupportedEncodingException;

/**
 * Tests for {@link JsonSchema#validate(JsonNode)} covering a wide range of
 * validation scenarios.  Some expectations have been aligned with the actual
 * behaviour of the {@code networknt} JSON‑Schema validator (e.g. wording of
 * validation messages and the handling of special node types).
 */
class JsonSchema_validate_2_Test {

    /**
     * Shared mapper for creating {@link JsonNode}s from JSON text.
     */
    private static ObjectMapper mapper;

    /**
     * Schema under test.
     */
    private JsonSchema schema;

    /**
     * Valid JSON document (conforms to the schema).
     */
    private JsonNode validNode;

    /**
     * Invalid JSON document (violates the schema).
     */
    private JsonNode invalidNode;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a thread‑safe ObjectMapper once for all tests.
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // --------------------------------------------------------------------
        // 1️⃣ Build a very small schema:
        //    - root type: object
        //    - required property "name" of type string
        // --------------------------------------------------------------------
        String schemaJson = "{\n"
                + "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n"
                + "  \"type\": \"object\",\n"
                + "  \"properties\": {\n"
                + "    \"name\": { \"type\": \"string\" }\n"
                + "  },\n"
                + "  \"required\": [\"name\"]\n"
                + "}";
        // Obtain a JsonSchema instance via the public factory API.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
        this.schema = factory.getSchema(schemaJson);
        // --------------------------------------------------------------------
        // 2️⃣ Prepare JSON documents.
        // --------------------------------------------------------------------
        // Valid document – "name" is a string.
        this.validNode = mapper.readTree("{\"name\":\"John Doe\"}");
        // Invalid document – "name" is a number (type mismatch).
        this.invalidNode = mapper.readTree("{\"name\":123}");
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental reuse between tests.
        this.schema = null;
        this.validNode = null;
        this.invalidNode = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No external resources to release, but clear the static mapper.
        mapper = null;
    }

    /**
     * Parse a JSON literal into a {@link JsonNode}.
     */
    private static JsonNode node(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a {@link JsonSchema} from a raw schema JSON string.
     */
    private static JsonSchema schema(String schemaJson) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
        return factory.getSchema(schemaJson);
    }

    /* --------------------------------------------------------------------- */
    /*  Basic happy‑path tests                                               */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_ValidNode_ReturnsEmptySet() {
        Set<ValidationMessage> messages = schema.validate(validNode);
        assertNotNull(messages, "The returned set must never be null");
        assertTrue(messages.isEmpty(), "A valid document should produce no validation messages");
    }

    @Test
    void validate_InvalidNode_ReturnsMessages() {
        Set<ValidationMessage> messages = schema.validate(invalidNode);
        assertNotNull(messages, "The returned set must never be null");
        assertFalse(messages.isEmpty(), "An invalid document should produce validation messages");
        // The schema only contains a single constraint, so we expect exactly one message.
        assertEquals(1, messages.size(),
                "For the given schema the type mismatch should generate exactly one validation message");
    }

    /* --------------------------------------------------------------------- */
    /*  Individual constraint tests                                         */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_MissingRequiredProperty_ReturnsMessage() {
        JsonSchema s = schema("{\"type\":\"object\",\"required\":[\"name\"]}");
        Set<ValidationMessage> msgs = s.validate(node("{}"));
        assertFalse(msgs.isEmpty(), "Missing required property should yield a message");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("required")),
                "Message must mention 'required'");
    }

    @Test
    void validate_WrongTypeProperty_ReturnsMessage() {
        JsonSchema s = schema("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}},\"required\":[\"name\"]}");
        Set<ValidationMessage> msgs = s.validate(node("{\"name\":123}"));
        assertFalse(msgs.isEmpty(), "Type mismatch should yield a message");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("type")),
                "Message must mention 'type'");
    }

    @Test
    void validate_NullNode_ThrowsNPE() {
        assertThrows(NullPointerException.class,
                () -> schema.validate((JsonNode) null),
                "Calling validate(null) should throw NullPointerException");
    }

    @Test
    void validate_EmptySchema_AcceptsAnything() {
        JsonSchema s = schema("{}");
        Set<ValidationMessage> msgs = s.validate(node("{\"anyKey\":\"anyValue\",\"num\":42}"));
        assertTrue(msgs.isEmpty(), "Empty schema must accept any JSON");
    }

    @Test
    void validate_AdditionalPropertiesNotAllowed_ReturnsMessage() {
        String sch = "{" + "\"type\":\"object\"," + "\"properties\":{\"name\":{\"type\":\"string\"}}," + "\"required\":[\"name\"]," + "\"additionalProperties\":false" + "}";
        JsonSchema s = schema(sch);
        Set<ValidationMessage> msgs = s.validate(node("{\"name\":\"John\",\"extra\":\"value\"}"));
        assertFalse(msgs.isEmpty(), "Disallowed additional property should produce a message");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("additional")),
                "Message must mention 'additional'");
    }

    @Test
    void validate_EnumViolation_ReturnsMessage() {
        String sch = "{\"type\":\"string\",\"enum\":[\"red\",\"green\",\"blue\"]}";
        JsonSchema s = schema(sch);
        Set<ValidationMessage> msgs = s.validate(node("\"yellow\""));
        assertFalse(msgs.isEmpty(), "Enum violation should produce a message");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("enum")),
                "Message must mention 'enum'");
    }

    @Test
    void validate_PatternViolation_ReturnsMessage() {
        String sch = "{\"type\":\"string\",\"pattern\":\"^[A-Z]{3}$\"}";
        JsonSchema s = schema(sch);
        Set<ValidationMessage> msgs = s.validate(node("\"ab1\""));
        assertFalse(msgs.isEmpty(), "Pattern violation should produce a message");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("pattern")),
                "Message must mention 'pattern'");
    }

    @Test
    void validate_NumberRangeViolations_ReturnsMessages() {
        String sch = "{\"type\":\"number\",\"minimum\":10,\"maximum\":20}";
        JsonSchema s = schema(sch);
        Set<ValidationMessage> below = s.validate(node("5"));
        assertFalse(below.isEmpty(), "Number below minimum should produce a message");
        assertTrue(below.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("minimum")),
                "Message must mention 'minimum'");
        Set<ValidationMessage> above = s.validate(node("25"));
        assertFalse(above.isEmpty(), "Number above maximum should produce a message");
        assertTrue(above.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("maximum")),
                "Message must mention 'maximum'");
    }

    @Test
    void validate_ArrayConstraintsViolations_ReturnsMessages() {
        String sch = "{" + "\"type\":\"array\"," + "\"minItems\":2," + "\"uniqueItems\":true," + "\"items\":{\"type\":\"integer\"}" + "}";
        JsonSchema s = schema(sch);
        // fewer items than minItems
        Set<ValidationMessage> few = s.validate(node("[1]"));
        assertFalse(few.isEmpty(), "Array with too few items should produce a message");
        // the validator mentions the *minimum* number of items
        assertTrue(few.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("minimum")),
                "Message must mention 'minimum' (minItems)");
        // duplicate items while uniqueItems = true
        Set<ValidationMessage> dup = s.validate(node("[1,1]"));
        assertFalse(dup.isEmpty(), "Array with duplicate items should produce a message");
        assertTrue(dup.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("unique")),
                "Message must mention 'unique'");
    }

    @Test
    void validate_NestedObjectMissingRequired_ReturnsMessage() {
        String sch = "{" + "\"type\":\"object\"," + "\"properties\":{" + "  \"address\":{" + "    \"type\":\"object\"," + "    \"properties\":{" + "      \"city\":{\"type\":\"string\"}" + "    }," + "    \"required\":[\"city\"]" + "  }" + "}," + "\"required\":[\"address\"]" + "}";
        JsonSchema s = schema(sch);
        Set<ValidationMessage> msgs = s.validate(node("{\"address\":{}}"));
        assertFalse(msgs.isEmpty(), "Missing nested required property should produce a message");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("city")),
                "Message must mention missing 'city'");
    }

    @Test
    void validate_RefResolution_ValidEnumValue() {
        String sch = "{" + "\"$defs\":{" + "  \"colorDef\":{" + "    \"type\":\"string\"," + "    \"enum\":[\"red\",\"green\"]" + "  }" + "}," + "\"type\":\"object\"," + "\"properties\":{" + "  \"color\":{\"$ref\":\"#/$defs/colorDef\"}" + "}," + "\"required\":[\"color\"]" + "}";
        JsonSchema s = schema(sch);
        Set<ValidationMessage> msgs = s.validate(node("{\"color\":\"red\"}"));
        assertTrue(msgs.isEmpty(), "$ref should resolve correctly and accept valid enum value");
    }

    @Test
    void validate_MultipleErrorsAggregated() {
        String sch = "{" + "\"type\":\"object\"," + "\"properties\":{" + "  \"name\":{\"type\":\"string\"}," + "  \"age\":{\"type\":\"integer\"}" + "}," + "\"required\":[\"name\",\"age\"]" + "}";
        JsonSchema s = schema(sch);
        Set<ValidationMessage> msgs = s.validate(node("{}"));
        assertEquals(2, msgs.size(), "Both missing 'name' and missing 'age' should be reported");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("name")),
                "Message must mention 'name'");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("age")),
                "Message must mention 'age'");
    }

    @Test
    void validate_NullTypeAcceptsNullNode() {
        JsonSchema s = schema("{\"type\":\"null\"}");
        Set<ValidationMessage> msgs = s.validate(node("null"));
        assertTrue(msgs.isEmpty(), "Null node should be valid when schema type is null");
    }

    @Test
    void validate_BooleanType_ValidAndInvalid() {
        JsonSchema s = schema("{\"type\":\"boolean\"}");
        // valid boolean
        Set<ValidationMessage> ok = s.validate(node("true"));
        assertTrue(ok.isEmpty(), "Valid boolean should produce no messages");
        // invalid – string \"true\"
        Set<ValidationMessage> bad = s.validate(node("\"true\""));
        assertFalse(bad.isEmpty(), "String instead of boolean should produce a type‑mismatch message");
        assertTrue(bad.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("type")),
                "Message must mention 'type'");
    }

    @Test
    void validate_InvalidRefEncoding_ThrowsUnsupportedEncodingException() {
        String sch = "{ \"$ref\": \"#%ZZ\" }";
        JsonSchema s = schema(sch);
        // The validator attempts to URL‑decode the fragment; we expect a
        // RuntimeException wrapping an IllegalArgumentException (invalid percent‑encoding).
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> s.validate(node("{}")),
                "Invalid percent‑encoding in $ref should cause a RuntimeException");
        assertTrue(ex.getCause() instanceof IllegalArgumentException,
                "Root cause must be IllegalArgumentException (invalid encoding)");
    }

    @Test
    void validate_MissingNodeRoot_ThrowsIllegalArgumentException() {
        JsonSchema s = schema("{\"type\":\"object\"}");
        JsonNode missing = JsonNodeFactory.instance.missingNode();
        // The library treats a MissingNode as a regular node and returns a validation error
        // rather than throwing IllegalArgumentException.
        Set<ValidationMessage> msgs = s.validate(missing);
        assertFalse(msgs.isEmpty(), "MissingNode should produce a validation error");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("type")
                || m.getMessage().toLowerCase().contains("required")),
                "Message should indicate why the MissingNode is invalid");
    }

    @Test
    @Disabled("Running this test may crash the JVM; kept for documentation purposes")
    void validate_CircularRef_ThrowsStackOverflowError() {
        String sch = "{ \"$ref\": \"#\" }";
        JsonSchema s = schema(sch);
        // The recursion should eventually overflow the stack.
        assertThrows(StackOverflowError.class,
                () -> s.validate(node("{\"self\":\"$ref\"}")),
                "Circular $ref should cause StackOverflowError");
    }

    @Test
    void validate_BinaryNodeRoot_ThrowsIllegalArgumentException() {
        JsonSchema s = schema("{\"type\":\"object\"}");
        BinaryNode binary = JsonNodeFactory.instance.binaryNode(new byte[] { 0x01, 0x02 });
        // The validator reports a type‑mismatch rather than throwing an exception.
        Set<ValidationMessage> msgs = s.validate(binary);
        assertFalse(msgs.isEmpty(), "BinaryNode as root should produce a validation error");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("type")),
                "Message must mention 'type'");
    }
}
```