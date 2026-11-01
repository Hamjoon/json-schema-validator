package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.io.IOException;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_7_Test {

    /**
     * Factory used to create {@link JsonSchema} instances for the tests.
     */
    private static JsonSchemaFactory factory;

    /**
     * Simple schema that requires an object with a string property {@code name}.
     */
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

    /**
     * Walk a valid JSON document – the result must be valid and contain no messages.
     */
    @Test
    void walkValidDocument_returnsValidResult() {
        // given
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        ExecutionContext ctx = new ExecutionContext();
        String validJson = """
            { "name": "John Doe" }""";
        // when
        ValidationResult result = schema.walk(ctx, validJson, InputFormat.JSON, true);
        // then
        assertTrue(result.isSuccess(), "Result should be valid for a correct document");
        assertTrue(result.getMessages().isEmpty(), "There should be no validation messages for a valid document");
    }

    /**
     * Walk an invalid JSON document – the result must be invalid and contain exactly one message.
     */
    @Test
    void walkInvalidDocument_returnsInvalidResult() {
        // given
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        ExecutionContext ctx = new ExecutionContext();
        // {@code name} is a number, which violates the {@code string} type constraint.
        String invalidJson = """
            { "name": 123 }""";
        // when
        ValidationResult result = schema.walk(ctx, invalidJson, InputFormat.JSON, true);
        // then
        assertFalse(result.isSuccess(), "Result should be invalid for an incorrect document");
        assertEquals(1, result.getMessages().size(), "Exactly one validation message is expected for the type error");
    }

    /**
     * When the supplied {@link ExecutionContext} is {@code null} the method is
     * expected to fail early – the implementation throws a {@link NullPointerException}.
     */
    @Test
    void walk_nullExecutionContext_throwsNPE() {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        String json = """
            { "name":"John" }""";
        assertThrows(NullPointerException.class, () -> schema.walk(null, json, InputFormat.JSON, true), "Calling walk with a null ExecutionContext should throw NullPointerException");
    }

    /**
     * Walk a valid JSON document with {@code validate = true}. The schema is
     * deliberately simple (does not require any property) so the walk succeeds.
     */
    @Test
    void walk_validJson_validateTrue_returnsSuccess() {
        // Use a schema without required fields for this particular case.
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
        assertTrue(result.isSuccess(), "When validate = true the walk should succeed for a schema that does not enforce constraints");
    }

    /**
     * Walk a valid JSON document with {@code validate = false}. No validation
     * is performed, therefore the result must be successful regardless of the
     * schema content.
     */
    @Test
    void walk_validJson_validateFalse_returnsSuccess() {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        ExecutionContext ctx = new ExecutionContext();
        String json = """
            { "age":30 }""";
        ValidationResult result = schema.walk(ctx, json, InputFormat.JSON, false);
        assertTrue(result.isSuccess(), "When validate = false the walk should always return a successful ValidationResult");
    }

    /**
     * Walk a YAML document with validation enabled. The library supports
     * {@link InputFormat#YAML}; the method should deserialize correctly and return
     * a successful result because the schema does not impose any constraints on the
     * provided fields.
     */
    @Test
    void walk_yamlInput_validateTrue_returnsSuccess() {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        ExecutionContext ctx = new ExecutionContext();
        String yaml = """
            name: Jane
            age: 25
            """;
        ValidationResult result = schema.walk(ctx, yaml, InputFormat.YAML, true);
        assertTrue(result.isSuccess(), "YAML input should be deserialized and validated successfully");
    }

    /**
     * Passing a {@code null} {@link InputFormat} is considered unsupported.
     * The implementation should throw an {@link IllegalArgumentException}
     * (or a library‑specific {@link JsonSchemaException} which extends
     * {@link RuntimeException}). We assert for {@link RuntimeException}
     * to stay compatible with both possibilities.
     */
    @Test
    void walk_unsupportedNullInputFormat_throwsException() {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        ExecutionContext ctx = new ExecutionContext();
        String json = "<xml></xml>";
        assertThrows(RuntimeException.class, () -> schema.walk(ctx, json, null, true), "A null InputFormat should result in an exception");
    }

    /**
     * Malformed JSON triggers an {@link IOException} inside the private
     * {@code deserialize} method. The public {@code walk} method wraps it
     * into a runtime exception (typically {@link JsonSchemaException}).
     */
    @Test
    void walk_malformedJson_throwsWrappedException() {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        ExecutionContext ctx = new ExecutionContext();
        // Missing closing brace – invalid JSON.
        String malformed = "{ \"name\": \"John\" ";
        assertThrows(RuntimeException.class, () -> schema.walk(ctx, malformed, InputFormat.JSON, true), "Malformed JSON should cause a wrapped runtime exception");
    }

    /**
     * Passing a {@code null} input string cannot be deserialized and must
     * result in a {@link NullPointerException} (or a wrapped runtime exception).
     */
    @Test
    void walk_nullInputString_throwsException() {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        ExecutionContext ctx = new ExecutionContext();
        assertThrows(RuntimeException.class, () -> schema.walk(ctx, null, InputFormat.JSON, true), "Null input string should cause a runtime exception");
    }

    /**
     * An empty JSON string cannot be parsed; Jackson throws a
     * {@link com.fasterxml.jackson.core.JsonParseException} which is a subclass of
     * {@link IOException}. The walk method should wrap it.
     */
    @Test
    void walk_emptyInputString_throwsWrappedException() {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        ExecutionContext ctx = new ExecutionContext();
        String empty = "";
        assertThrows(RuntimeException.class, () -> schema.walk(ctx, empty, InputFormat.JSON, true), "Empty input should result in a wrapped runtime exception");
    }

    /**
     * The library does not support {@link InputFormat#XML}. Supplying this value
     * must lead to an {@link IllegalArgumentException} (or a library‑specific
     * runtime exception). The test asserts for {@link RuntimeException}.
     */
    @Test
    void walk_unsupportedInputFormatValue_throwsException() {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        ExecutionContext ctx = new ExecutionContext();
        String xml = "<xml></xml>";
        assertThrows(RuntimeException.class, () -> schema.walk(ctx, xml, InputFormat.XML, true), "Unsupported InputFormat (XML) should cause a runtime exception");
    }

    /**
     * Invalid percent‑encoding (e.g. "%ZZ") would cause
     * {@link UnsupportedEncodingException} inside the deserialization logic.
     * The public API wraps it into a runtime exception.
     */
    @Test
    void walk_invalidPercentEncoding_throwsWrappedException() {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        ExecutionContext ctx = new ExecutionContext();
        String badEncoding = "%ZZ";
        assertThrows(RuntimeException.class, () -> schema.walk(ctx, badEncoding, InputFormat.JSON, true), "Invalid percent‑encoding should result in a wrapped runtime exception");
    }
}
