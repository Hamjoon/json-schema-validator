
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.UnsupportedEncodingException;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
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
        String schemaJson = "{\n" + "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\n" + "    \"name\": { \"type\": \"string\" }\n" + "  },\n" + "  \"required\": [\"name\"]\n" + "}";
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

    /**
     * {@link JsonSchema#validate(JsonNode)} should return an empty set for valid input.
     */
    @Test
    void validate_ValidNode_ReturnsEmptySet() {
        Set<ValidationMessage> messages = schema.validate(validNode);
        assertNotNull(messages, "The returned set must never be null");
        assertTrue(messages.isEmpty(), "A valid document should produce no validation messages");
    }

    /**
     * {@link JsonSchema#validate(JsonNode)} should report at least one message for invalid input.
     */
    @Test
    void validate_InvalidNode_ReturnsMessages() {
        Set<ValidationMessage> messages = schema.validate(invalidNode);
        assertNotNull(messages, "The returned set must never be null");
        assertFalse(messages.isEmpty(), "An invalid document should produce validation messages");
        // The schema only contains a single constraint, so we expect exactly one message.
        assertEquals(1, messages.size(), "For the given schema the type mismatch should generate exactly one validation message");
    }

    /**
     * Group: Missing required property triggers validation error
     */
    @Test
    void validate_MissingRequiredProperty_ReturnsMessage() {
        JsonSchema s = schema("{\"type\":\"object\",\"required\":[\"name\"]}");
        Set<ValidationMessage> msgs = s.validate(node("{}"));
        assertFalse(msgs.isEmpty(), "Missing required property should yield a message");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().contains("required")), "Message must mention 'required'");
    }

    /**
     * Group: Wrong type for property triggers validation error
     */
    @Test
    void validate_WrongTypeProperty_ReturnsMessage() {
        JsonSchema s = schema("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}},\"required\":[\"name\"]}");
        Set<ValidationMessage> msgs = s.validate(node("{\"name\":123}"));
        assertFalse(msgs.isEmpty(), "Type mismatch should yield a message");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().contains("type")), "Message must mention 'type'");
    }

    /**
     * Group: Null JsonNode handling – expect NullPointerException
     */
    @Test
    void validate_NullNode_ThrowsNPE() {
        assertThrows(NullPointerException.class, () -> schema.validate((JsonNode) null), "Calling validate(null) should throw NullPointerException");
    }

    /**
     * Group: Schema with no constraints accepts any JSON
     */
    @Test
    void validate_EmptySchema_AcceptsAnything() {
        JsonSchema s = schema("{}");
        Set<ValidationMessage> msgs = s.validate(node("{\"anyKey\":\"anyValue\",\"num\":42}"));
        assertTrue(msgs.isEmpty(), "Empty schema must accept any JSON");
    }

    /**
     * Group: Additional properties disallowed
     */
    @Test
    void validate_AdditionalPropertiesNotAllowed_ReturnsMessage() {
        String sch = "{" + "\"type\":\"object\"," + "\"properties\":{\"name\":{\"type\":\"string\"}}," + "\"required\":[\"name\"]," + "\"additionalProperties\":false" + "}";
        JsonSchema s = schema(sch);
        Set<ValidationMessage> msgs = s.validate(node("{\"name\":\"John\",\"extra\":\"value\"}"));
        assertFalse(msgs.isEmpty(), "Disallowed additional property should produce a message");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().contains("additional")), "Message must mention 'additional'");
    }

    /**
     * Group: Enum constraint violation
     */
    @Test
    void validate_EnumViolation_ReturnsMessage() {
        String sch = "{\"type\":\"string\",\"enum\":[\"red\",\"green\",\"blue\"]}";
        JsonSchema s = schema(sch);
        Set<ValidationMessage> msgs = s.validate(node("\"yellow\""));
        assertFalse(msgs.isEmpty(), "Enum violation should produce a message");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().contains("enum")), "Message must mention 'enum'");
    }

    /**
     * Group: Pattern constraint violation
     */
    @Test
    void validate_PatternViolation_ReturnsMessage() {
        String sch = "{\"type\":\"string\",\"pattern\":\"^[A-Z]{3}$\"}";
        JsonSchema s = schema(sch);
        Set<ValidationMessage> msgs = s.validate(node("\"ab1\""));
        assertFalse(msgs.isEmpty(), "Pattern violation should produce a message");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().contains("pattern")), "Message must mention 'pattern'");
    }

    /**
     * Group: Number range violations (minimum & maximum)
     */
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

    /**
     * Group: Array constraints violations (minItems & uniqueItems)
     */
    @Test
    void validate_ArrayConstraintsViolations_ReturnsMessages() {
        String sch = "{" + "\"type\":\"array\"," + "\"minItems\":2," + "\"uniqueItems\":true," + "\"items\":{\"type\":\"integer\"}" + "}";
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

    /**
     * Group: Nested object required property missing
     */
    @Test
    void validate_NestedObjectMissingRequired_ReturnsMessage() {
        String sch = "{" + "\"type\":\"object\"," + "\"properties\":{" + "  \"address\":{" + "    \"type\":\"object\"," + "    \"properties\":{" + "      \"city\":{\"type\":\"string\"}" + "    }," + "    \"required\":[\"city\"]" + "  }" + "}," + "\"required\":[\"address\"]" + "}";
        JsonSchema s = schema(sch);
        Set<ValidationMessage> msgs = s.validate(node("{\"address\":{}}"));
        assertFalse(msgs.isEmpty(), "Missing nested required property should produce a message");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().contains("city")), "Message must mention missing 'city'");
    }

    /**
     * Group: $ref resolution works
     */
    @Test
    void validate_RefResolution_ValidEnumValue() {
        String sch = "{" + "\"$defs\":{" + "  \"colorDef\":{" + "    \"type\":\"string\"," + "    \"enum\":[\"red\",\"green\"]" + "  }" + "}," + "\"type\":\"object\"," + "\"properties\":{" + "  \"color\":{\"$ref\":\"#/$defs/colorDef\"}" + "}," + "\"required\":[\"color\"]" + "}";
        JsonSchema s = schema(sch);
        Set<ValidationMessage> msgs = s.validate(node("{\"color\":\"red\"}"));
        assertTrue(msgs.isEmpty(), "$ref should resolve correctly and accept valid enum value");
    }

    /**
     * Group: Multiple validation errors aggregated
     */
    @Test
    void validate_MultipleErrorsAggregated() {
        String sch = "{" + "\"type\":\"object\"," + "\"properties\":{" + "  \"name\":{\"type\":\"string\"}," + "  \"age\":{\"type\":\"integer\"}" + "}," + "\"required\":[\"name\",\"age\"]" + "}";
        JsonSchema s = schema(sch);
        Set<ValidationMessage> msgs = s.validate(node("{}"));
        assertEquals(2, msgs.size(), "Both missing 'name' and missing 'age' should be reported");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().contains("name")), "Message must mention 'name'");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().contains("age")), "Message must mention 'age'");
    }

    /**
     * Group: Null node accepted when type is null
     */
    @Test
    void validate_NullTypeAcceptsNullNode() {
        JsonSchema s = schema("{\"type\":\"null\"}");
        Set<ValidationMessage> msgs = s.validate(node("null"));
        assertTrue(msgs.isEmpty(), "Null node should be valid when schema type is null");
    }

    /**
     * Group: Boolean type validation
     */
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

    /**
     * Group: Invalid $ref encoding triggers UnsupportedEncodingException
     */
    @Test
    void validate_InvalidRefEncoding_ThrowsUnsupportedEncodingException() {
        String sch = "{ \"$ref\": \"#%ZZ\" }";
        JsonSchema s = schema(sch);
        // The validator attempts to URL‑decode the fragment; we expect a
        // RuntimeException wrapping the UnsupportedEncodingException.
        RuntimeException ex = assertThrows(RuntimeException.class, () -> s.validate(node("{}")), "Invalid percent‑encoding in $ref should cause a RuntimeException");
        assertTrue(ex.getCause() instanceof UnsupportedEncodingException, "Root cause must be UnsupportedEncodingException");
    }

    /**
     * Group: MissingNode as root triggers IllegalArgumentException
     */
    @Test
    void validate_MissingNodeRoot_ThrowsIllegalArgumentException() {
        JsonSchema s = schema("{\"type\":\"object\"}");
        JsonNode missing = JsonNodeFactory.instance.missingNode();
        assertThrows(IllegalArgumentException.class, () -> s.validate(missing), "MissingNode should cause IllegalArgumentException");
    }

    /**
     * Group: Circular $ref leads to StackOverflowError
     */
    @Test
    @Disabled("Running this test may crash the JVM; kept for documentation purposes")
    void validate_CircularRef_ThrowsStackOverflowError() {
        String sch = "{ \"$ref\": \"#\" }";
        JsonSchema s = schema(sch);
        // The recursion should eventually overflow the stack.
        assertThrows(StackOverflowError.class, () -> s.validate(node("{\"self\":\"$ref\"}")), "Circular $ref should cause StackOverflowError");
    }

    /**
     * Group: Root node of unexpected type (binary) triggers IllegalArgumentException
     */
    @Test
    void validate_BinaryNodeRoot_ThrowsIllegalArgumentException() {
        JsonSchema s = schema("{\"type\":\"object\"}");
        BinaryNode binary = JsonNodeFactory.instance.binaryNode(new byte[] { 0x01, 0x02 });
        assertThrows(IllegalArgumentException.class, () -> s.validate(binary), "BinaryNode as root should cause IllegalArgumentException");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_validate_2_Test.java:16: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_2_Test.java:19: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
2 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method validate(JsonNode)': '\n     * Validate the given root JsonNode, starting at the root of the data path.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param rootNode the root node\n     * @return A list of ValidationMessage if there is any validation error, or an\n     *         empty list if there is no error.\n     ', 'dependent classes': 'class `com.networknt.schema.OutputFormat`:\n\tdependent fields:\n\t\tcom.networknt.schema.OutputFormat.Default com.networknt.schema.OutputFormat.DEFAULT;\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(JsonNode, ExecutionContextCustomizer)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(JsonNode)`, api document: \n     * Validate the given root JsonNode, starting at the root of the data path.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     *\n     * @param rootNode            the root node\n     * @param executionCustomizer the execution customizer\n     * @return the assertions\n     ;\n\t\tmethod `public Set<ValidationMessage> validate(String, InputFormat)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(JsonNode)`, api document: \n     * Validate the given input string using the input format, starting at the root\n     * of the data path.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param input       the input\n     * @param inputFormat the inputFormat\n     * @return A list of ValidationMessage if there is any validation error, or an\n     *         empty list if there is no error.\n     ;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.validate(JsonNode)`, api document: \n     * Returns the singleton instance of this validator.\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


