```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Tests for the {@link JsonSchema#walk(ExecutionContext, String, InputFormat, boolean)} method.
 * <p>
 * The helper methods use reflection with {@code setAccessible(true)} because the
 * {@code ValidationResult} class in the version of the library used for this
 * kata does not expose its API methods as {@code public}. This makes the tests
 * independent of the exact visibility of those methods.
 */
class JsonSchema_walk_7_Test {

    /** Factory used to create {@link JsonSchema} instances for the tests. */
    private static JsonSchemaFactory factory;

    /** Simple schema that requires an object with a string property {@code name}. */
    private static final String SIMPLE_SCHEMA = """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "type": "object",
          "properties": {
            "name": { "type": "string" }
          },
          "required": [ "name" ]
        }
        """;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a factory for the 2020‑12 draft – this is sufficient for the
        // walk‑method tests and does not involve any external resources.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    /** Helper that invokes {@code ValidationResult.isSuccess()} via reflection. */
    private static boolean isSuccess(Object result) {
        try {
            Method m = result.getClass().getDeclaredMethod("isSuccess");
            m.setAccessible(true);
            return (Boolean) m.invoke(result);
        } catch (Exception e) {
            throw new RuntimeException("Unable to invoke isSuccess on ValidationResult", e);
        }
    }

    /** Helper that invokes {@code ValidationResult.getMessages()} via reflection. */
    @SuppressWarnings("unchecked")
    private static Set<?> getMessages(Object result) {
        try {
            Method m = result.getClass().getDeclaredMethod("getMessages");
            m.setAccessible(true);
            return (Set<?>) m.invoke(result);
        } catch (Exception e) {
            throw new RuntimeException("Unable to invoke getMessages on ValidationResult", e);
        }
    }

    /** Walk a valid JSON document – the result must be valid and contain no messages. */
    @Test
    void walkValidDocument_returnsValidResult() {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        ExecutionContext ctx = new ExecutionContext();
        String validJson = """
            { "name": "John Doe" }""";
        ValidationResult result = schema.walk(ctx, validJson, InputFormat.JSON, true);
        assertTrue(isSuccess(result), "Result should be valid for a correct document");
        assertTrue(getMessages(result).isEmpty(), "There should be no validation messages for a valid document");
    }

    /** Walk an invalid JSON document – the result must be invalid and contain exactly one message. */
    @Test
    void walkInvalidDocument_returnsInvalidResult() {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        ExecutionContext ctx = new ExecutionContext();
        String invalidJson = """
            { "name": 123 }""";
        ValidationResult result = schema.walk(ctx, invalidJson, InputFormat.JSON, true);
        assertFalse(isSuccess(result), "Result should be invalid for an incorrect document");
        assertEquals(1, getMessages(result).size(),
                "Exactly one validation message is expected for the type error");
    }

    /** When the supplied {@link ExecutionContext} is {@code null} the method is expected to fail early. */
    @Test
    void walk_nullExecutionContext_throwsNPE() {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        String json = """
            { "name":"John" }""";
        assertThrows(NullPointerException.class,
                () -> schema.walk(null, json, InputFormat.JSON, true),
                "Calling walk with a null ExecutionContext should throw NullPointerException");
    }

    /** Walk a valid JSON document with {@code validate = true}. The schema does not require any property. */
    @Test
    void walk_validJson_validateTrue_returnsSuccess() {
        String schemaWithoutRequired = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object"
            }
            """;
        JsonSchema schema = factory.getSchema(schemaWithoutRequired);
        ExecutionContext ctx = new ExecutionContext();
        String json = """
            { "age":30 }""";
        ValidationResult result = schema.walk(ctx, json, InputFormat.JSON, true);
        assertTrue(isSuccess(result),
                "When validate = true the walk should succeed for a schema that does not enforce constraints");
    }

    /** Walk a valid JSON document with {@code validate = false}. No validation is performed. */
    @Test
    void walk_validJson_validateFalse_returnsSuccess() {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        ExecutionContext ctx = new ExecutionContext();
        String json = """
            { "age":30 }""";
        ValidationResult result = schema.walk(ctx, json, InputFormat.JSON, false);
        assertTrue(isSuccess(result),
                "When validate = false the walk should always return a successful ValidationResult");
    }

    /** Walk a YAML document with validation enabled. */
    @Test
    void walk_yamlInput_validateTrue_returnsSuccess() {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        ExecutionContext ctx = new ExecutionContext();
        String yaml = """
            name: Jane
            age: 25
            """;
        ValidationResult result = schema.walk(ctx, yaml, InputFormat.YAML, true);
        assertTrue(isSuccess(result), "YAML input should be deserialized and validated successfully");
    }

    /** Passing a {@code null} {@link InputFormat} is considered unsupported. */
    @Test
    void walk_unsupportedNullInputFormat_throwsException() {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        ExecutionContext ctx = new ExecutionContext();
        String json = "<xml></xml>";
        assertThrows(RuntimeException.class,
                () -> schema.walk(ctx, json, null, true),
                "A null InputFormat should result in an exception");
    }

    /** Malformed JSON triggers an {@link IOException} inside the private {@code deserialize} method. */
    @Test
    void walk_malformedJson_throwsWrappedException() {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        ExecutionContext ctx = new ExecutionContext();
        String malformed = "{ \"name\": \"John\" ";
        assertThrows(RuntimeException.class,
                () -> schema.walk(ctx, malformed, InputFormat.JSON, true),
                "Malformed JSON should cause a wrapped runtime exception");
    }

    /** Passing a {@code null} input string must result in a runtime exception. */
    @Test
    void walk_nullInputString_throwsException() {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        ExecutionContext ctx = new ExecutionContext();
        assertThrows(RuntimeException.class,
                () -> schema.walk(ctx, null, InputFormat.JSON, true),
                "Null input string should cause a runtime exception");
    }

    /** An empty JSON string cannot be parsed; the library treats it as valid with no messages. */
    @Test
    void walk_emptyInputString_returnsSuccess() {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        ExecutionContext ctx = new ExecutionContext();
        String empty = "";
        ValidationResult result = schema.walk(ctx, empty, InputFormat.JSON, true);
        assertTrue(isSuccess(result), "Empty input should be considered successful (no validation performed)");
        assertTrue(getMessages(result).isEmpty(),
                "No validation messages are expected for an empty input");
    }

    /** The library does not support {@link InputFormat#XML}. Supplying this value must lead to a runtime exception. */
    // @Test
    // void walk_unsupportedInputFormatValue_throwsException() {
    //     JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
    //     ExecutionContext ctx = new ExecutionContext();
    //     String xml = "<xml></xml>";
    //     assertThrows(RuntimeException.class,
    //             () -> schema.walk(ctx, xml, InputFormat.XML, true),
    //             "Unsupported InputFormat (XML) should cause a runtime exception");
    // }

    /** Invalid percent‑encoding should result in a wrapped runtime exception. */
    @Test
    void walk_invalidPercentEncoding_throwsWrappedException() {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        ExecutionContext ctx = new ExecutionContext();
        String badEncoding = "%ZZ";
        assertThrows(RuntimeException.class,
                () -> schema.walk(ctx, badEncoding, InputFormat.JSON, true),
                "Invalid percent‑encoding should result in a wrapped runtime exception");
    }
}
```