```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Comprehensive unit tests for {@link JsonSchema#validate(JsonNode)}.
 *
 * <p>The test suite covers a wide range of validation scenarios described in the
 * supplied JSON test case definition, including required properties, type
 * mismatches, enum, pattern, numeric ranges, array constraints, nested objects,
 * $ref handling, and error‑handling for illegal inputs.</p>
 *
 * <p>Each test group uses a schema that matches the scenario; the schema is
 * created via {@link JsonSchemaFactory#getInstance(VersionFlag)}.  The
 * {@link ObjectMapper} is reused for all tests.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_2_Test {

    /** Shared mapper for creating {@link JsonNode}s from JSON text. */
    private static ObjectMapper mapper;

    /** Schema under test – re‑initialised in each test method where needed. */
    private JsonSchema schema;

    /** Valid JSON document (conforms to the schema). */
    private JsonNode validNode;

    /** Invalid JSON document (violates the schema). */
    private JsonNode invalidNode;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a thread‑safe ObjectMapper once for all tests.
        mapper = new ObjectMapper();
    }

    @AfterAll
    static void teardownAfterAll() {
        mapper = null;
    }

    /* --------------------------------------------------------------------- *
     *  Helper utilities
     * --------------------------------------------------------------------- */

    /** Parse a JSON literal into a {@link JsonNode}. */
    private static JsonNode node(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Create a {@link JsonSchema} from a raw schema JSON string. */
    private static JsonSchema schema(String schemaJson) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
        return factory.getSchema(schemaJson);
    }

    /* --------------------------------------------------------------------- *
     *  Existing simple tests (kept unchanged)
     * --------------------------------------------------------------------- */

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Simple schema used by the two original tests.
        String schemaJson = "{\n"
                + "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n"
                + "  \"type\": \"object\",\n"
                + "  \"properties\": {\n"
                + "    \"name\": { \"type\": \"string\" }\n"
                + "  },\n"
                + "  \"required\": [\"name\"]\n"
                + "}";
        this.schema = schema(schemaJson);
        this.validNode = node("{\"name\":\"John Doe\"}");
        this.invalidNode = node("{\"name\":123}");
    }

    @AfterEach
    void teardownAfterEach() {
        this.schema = null;
        this.validNode = null;
        this.invalidNode = null;
    }

    /** {@link JsonSchema#validate(JsonNode)} should return an empty set for valid input. */
    @Test
    void validate_ValidNode_ReturnsEmptySet() {
        Set<ValidationMessage> messages = schema.validate(validNode);
        assertNotNull(messages, "The returned set must never be null");
        assertTrue(messages.isEmpty(), "A valid document should produce no validation messages");
    }

    /** {@link JsonSchema#validate(JsonNode)} should report at least one message for invalid input. */
    @Test
    void validate_InvalidNode_ReturnsMessages() {
        Set<ValidationMessage> messages = schema.validate(invalidNode);
        assertNotNull(messages, "The returned set must never be null");
        assertFalse(messages.isEmpty(), "An invalid document should produce validation messages");
        assertEquals(1, messages.size(),
                "For the given schema the type mismatch should generate exactly one validation message");
    }

    /* --------------------------------------------------------------------- *
     *  Parameterised tests for the remaining groups
     * --------------------------------------------------------------------- */

    /** Group: Missing required property triggers validation error */
    @Test
    void validate_MissingRequiredProperty_ReturnsMessage() {
        JsonSchema s = schema("{\"type\":\"object\",\"required\":[\"name\"]}");
        Set<ValidationMessage> msgs = s.validate(node("{}"));
        assertFalse(msgs.isEmpty(), "Missing required property should yield a message");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().contains("required")), "Message must mention 'required'");
    }

    /** Group: Wrong type for property triggers validation error */
    @Test
    void validate_WrongTypeProperty_ReturnsMessage() {
        JsonSchema s = schema("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}},\"required\":[\"name\"]}");
        Set<ValidationMessage> msgs = s.validate(node("{\"name\":123}"));
        assertFalse(msgs.isEmpty(), "Type mismatch should yield a message");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().contains("type")), "Message must mention 'type'");
    }

    /** Group: Null JsonNode handling – expect NullPointerException */
    @Test
    void validate_NullNode_ThrowsNPE() {
        assertThrows(NullPointerException.class,
                () -> schema.validate((JsonNode) null),
                "Calling validate(null) should throw NullPointerException");
    }

    /** Group: Schema with no constraints accepts any JSON */
    @Test
    void validate_EmptySchema_AcceptsAnything() {
        JsonSchema s = schema("{}");
        Set<ValidationMessage> msgs = s.validate(node("{\"anyKey\":\"anyValue\",\"num\":42}"));
        assertTrue(msgs.isEmpty(), "Empty schema must accept any JSON");
    }

    /** Group: Additional properties disallowed */
    @Test
    void validate_AdditionalPropertiesNotAllowed_ReturnsMessage() {
        String sch = "{"
                + "\"type\":\"object\","
                + "\"properties\":{\"name\":{\"type\":\"string\"}},"
                + "\"required\":[\"name\"],"
                + "\"additionalProperties\":false"
                + "}";
        JsonSchema s = schema(sch);
        Set<ValidationMessage> msgs = s.validate(node("{\"name\":\"John\",\"extra\":\"value\"}"));
        assertFalse(msgs.isEmpty(), "Disallowed additional property should produce a message");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().contains("additional")), "Message must mention 'additional'");
    }

    /** Group: Enum constraint violation */
    @Test
    void validate_EnumViolation_ReturnsMessage() {
        String sch = "{\"type\":\"string\",\"enum\":[\"red\",\"green\",\"blue\"]}";
        JsonSchema s = schema(sch);
        Set<ValidationMessage> msgs = s.validate(node("\"yellow\""));
        assertFalse(msgs.isEmpty(), "Enum violation should produce a message");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().contains("enum")), "Message must mention 'enum'");
    }

    /** Group: Pattern constraint violation */
    @Test
    void validate_PatternViolation_ReturnsMessage() {
        String sch = "{\"type\":\"string\",\"pattern\":\"^[A-Z]{3}$\"}";
        JsonSchema s = schema(sch);
        Set<ValidationMessage> msgs = s.validate(node("\"ab1\""));
        assertFalse(msgs.isEmpty(), "Pattern violation should produce a message");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().contains("pattern")), "Message must mention 'pattern'");
    }

    /** Group: Number range violations (minimum & maximum) */
    @Test
    void validate_NumberRangeViolations_ReturnsMessages() {
        String sch = "{\"type\":\"number\",\"minimum\":10,\"maximum\":20}";
        JsonSchema s = schema(sch);

        Set<ValidationMessage> below = s.validate(node("5"));
        assertFalse(below.isEmpty(), "Number below minimum should produce a message");
        assertTrue(below.stream().anyMatch(m -> m.getMessage().contains("minimum")), "Message must mention 'minimum'");

        Set<ValidationMessage> above = s.validate(node("25"));
        assertFalse(above.isEmpty(), "Number above maximum should produce a message");
        assertTrue(above.stream().anyMatch(m -> m.getMessage().contains("maximum")), "Message must mention 'maximum'");
    }

    /** Group: Array constraints violations (minItems & uniqueItems) */
    @Test
    void validate_ArrayConstraintsViolations_ReturnsMessages() {
        String sch = "{"
                + "\"type\":\"array\","
                + "\"minItems\":2,"
                + "\"uniqueItems\":true,"
                + "\"items\":{\"type\":\"integer\"}"
                + "}";
        JsonSchema s = schema(sch);

        // fewer items than minItems
        Set<ValidationMessage> few = s.validate(node("[1]"));
        assertFalse(few.isEmpty(), "Array with too few items should produce a message");
        assertTrue(few.stream().anyMatch(m -> m.getMessage().contains("minItems")), "Message must mention 'minItems'");

        // duplicate items while uniqueItems = true
        Set<ValidationMessage> dup = s.validate(node("[1,1]"));
        assertFalse(dup.isEmpty(), "Array with duplicate items should produce a message");
        assertTrue(dup.stream().anyMatch(m -> m.getMessage().contains("unique")), "Message must mention 'unique'");
    }

    /** Group: Nested object required property missing */
    @Test
    void validate_NestedObjectMissingRequired_ReturnsMessage() {
        String sch = "{"
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "  \"address\":{"
                + "    \"type\":\"object\","
                + "    \"properties\":{"
                + "      \"city\":{\"type\":\"string\"}"
                + "    },"
                + "    \"required\":[\"city\"]"
                + "  }"
                + "},"
                + "\"required\":[\"address\"]"
                + "}";
        JsonSchema s = schema(sch);
        Set<ValidationMessage> msgs = s.validate(node("{\"address\":{}}"));
        assertFalse(msgs.isEmpty(), "Missing nested required property should produce a message");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().contains("city")), "Message must mention missing 'city'");
    }

    /** Group: $ref resolution works */
    @Test
    void validate_RefResolution_ValidEnumValue() {
        String sch = "{"
                + "\"$defs\":{"
                + "  \"colorDef\":{"
                + "    \"type\":\"string\","
                + "    \"enum\":[\"red\",\"green\"]"
                + "  }"
                + "},"
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "  \"color\":{\"$ref\":\"#/$defs/colorDef\"}"
                + "},"
                + "\"required\":[\"color\"]"
                + "}";
        JsonSchema s = schema(sch);
        Set<ValidationMessage> msgs = s.validate(node("{\"color\":\"red\"}"));
        assertTrue(msgs.isEmpty(), "$ref should resolve correctly and accept valid enum value");
    }

    /** Group: Multiple validation errors aggregated */
    @Test
    void validate_MultipleErrorsAggregated() {
        String sch = "{"
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "  \"name\":{\"type\":\"string\"},"
                + "  \"age\":{\"type\":\"integer\"}"
                + "},"
                + "\"required\":[\"name\",\"age\"]"
                + "}";
        JsonSchema s = schema(sch);
        Set<ValidationMessage> msgs = s.validate(node("{}"));
        assertEquals(2, msgs.size(), "Both missing 'name' and missing 'age' should be reported");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().contains("name")), "Message must mention 'name'");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().contains("age")), "Message must mention 'age'");
    }

    /** Group: Null node accepted when type is null */
    @Test
    void validate_NullTypeAcceptsNullNode() {
        JsonSchema s = schema("{\"type\":\"null\"}");
        Set<ValidationMessage> msgs = s.validate(node("null"));
        assertTrue(msgs.isEmpty(), "Null node should be valid when schema type is null");
    }

    /** Group: Boolean type validation */
    @Test
    void validate_BooleanType_ValidAndInvalid() {
        JsonSchema s = schema("{\"type\":\"boolean\"}");

        // valid boolean
        Set<ValidationMessage> ok = s.validate(node("true"));
        assertTrue(ok.isEmpty(), "Valid boolean should produce no messages");

        // invalid – string "true"
        Set<ValidationMessage> bad = s.validate(node("\"true\""));
        assertFalse(bad.isEmpty(), "String instead of boolean should produce a type‑mismatch message");
        assertTrue(bad.stream().anyMatch(m -> m.getMessage().contains("type")), "Message must mention 'type'");
    }

    /** Group: Invalid $ref encoding triggers UnsupportedEncodingException */
    @Test
    void validate_InvalidRefEncoding_ThrowsUnsupportedEncodingException() {
        String sch = "{ \"$ref\": \"#%ZZ\" }";
        JsonSchema s = schema(sch);
        // The validator attempts to URL‑decode the fragment; we expect a
        // RuntimeException wrapping the UnsupportedEncodingException.
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> s.validate(node("{}")),
                "Invalid percent‑encoding in $ref should cause a RuntimeException");
        assertTrue(ex.getCause() instanceof UnsupportedEncodingException,
                "Root cause must be UnsupportedEncodingException");
    }

    /** Group: MissingNode as root triggers IllegalArgumentException */
    @Test
    void validate_MissingNodeRoot_ThrowsIllegalArgumentException() {
        JsonSchema s = schema("{\"type\":\"object\"}");
        JsonNode missing = JsonNodeFactory.instance.missingNode();
        assertThrows(IllegalArgumentException.class,
                () -> s.validate(missing),
                "MissingNode should cause IllegalArgumentException");
    }

    /** Group: Circular $ref leads to StackOverflowError */
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

    /** Group: Root node of unexpected type (binary) triggers IllegalArgumentException */
    @Test
    void validate_BinaryNodeRoot_ThrowsIllegalArgumentException() {
        JsonSchema s = schema("{\"type\":\"object\"}");
        BinaryNode binary = JsonNodeFactory.instance.binaryNode(new byte[]{0x01, 0x02});
        assertThrows(IllegalArgumentException.class,
                () -> s.validate(binary),
                "BinaryNode as root should cause IllegalArgumentException");
    }
}
```