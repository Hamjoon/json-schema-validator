
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_5_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSchema schema;

    // a schema without constraints (used for MissingNode)
    private JsonSchema emptySchema;

    // schema that requires an object (used for json‑null test)
    private JsonSchema objectSchema;

    // schema that contains a $ref
    private JsonSchema refSchema;

    // schema that uses "$recursiveAnchor": true
    private JsonSchema recursiveSchema;

    /**
     * Simple DTO used in one of the generic tests.
     */
    static final class CustomResult {

        final String payload;

        CustomResult(String payload) {
            this.payload = payload;
        }
    }

    /**
     * Minimal {@link OutputFormat} implementation that returns a predefined value
     *  and stores the set of {@link ValidationMessage}s it received.
     */
    private static class RecordingOutputFormat<T> implements OutputFormat<T> {

        private final T result;

        private Set<ValidationMessage> capturedMessages;

        RecordingOutputFormat(T result) {
            this.result = result;
        }

        @Override
        public T apply(Set<ValidationMessage> messages) {
            this.capturedMessages = messages;
            return result;
        }

        Set<ValidationMessage> getCapturedMessages() {
            return capturedMessages;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // -----------------------------------------------------------------
        // 1️⃣  Simple schema that requires a numeric "age" property
        // -----------------------------------------------------------------
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "age": { "type": "integer" }
              },
              "required": [ "age" ]
            }
            """;
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        schema = factory.getSchema(schemaJson);
        assertNotNull(schema, "Base schema should be created");
        // -----------------------------------------------------------------
        // 2️⃣  Empty schema – validates everything (used for MissingNode case)
        // -----------------------------------------------------------------
        emptySchema = factory.getSchema("{}");
        assertNotNull(emptySchema, "Empty schema should be created");
        // -----------------------------------------------------------------
        // 3️⃣  Object‑type schema – used to verify that a JSON null node fails
        // -----------------------------------------------------------------
        String objectSchemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object"
            }
            """;
        objectSchema = factory.getSchema(objectSchemaJson);
        assertNotNull(objectSchema, "Object schema should be created");
        // -----------------------------------------------------------------
        // 4️⃣  Schema with $ref – the referenced definition requires a "name"
        // -----------------------------------------------------------------
        String refSchemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "person": { "$ref": "#/definitions/person" }
              },
              "definitions": {
                "person": {
                  "type": "object",
                  "required": [ "name" ],
                  "properties": {
                    "name": { "type": "string" }
                  }
                }
              }
            }
            """;
        refSchema = factory.getSchema(refSchemaJson);
        assertNotNull(refSchema, "$ref schema should be created");
        // -----------------------------------------------------------------
        // 5️⃣  Recursive‑anchor schema – simple self‑reference example
        // -----------------------------------------------------------------
        String recursiveSchemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$recursiveAnchor": true,
              "type": "object",
              "properties": {
                "self": { "$recursiveRef": "#" }
              }
            }
            """;
        recursiveSchema = factory.getSchema(recursiveSchemaJson);
        assertNotNull(recursiveSchema, "Recursive schema should be created");
    }

    @Test
    void validate_ValidNode_UsesOutputFormat(@Mock OutputFormat<String> format) throws Exception {
        JsonNode validNode = MAPPER.readTree("{\"age\":30}");
        RecordingOutputFormat<String> format = new RecordingOutputFormat<>("formatted‑ok");
        String result = schema.validate(validNode, format);
        assertEquals("formatted‑ok", result);
        assertNotNull(format.getCapturedMessages());
        assertTrue(format.getCapturedMessages().isEmpty(), "For a valid instance the message set must be empty");
    }

    @Test
    void validate_InvalidNode_UsesOutputFormat(@Mock OutputFormat<String> format) throws Exception {
        JsonNode invalidNode = MAPPER.readTree("{\"age\":\"thirty\"}");
        RecordingOutputFormat<String> format = new RecordingOutputFormat<>("formatted‑error");
        String result = schema.validate(invalidNode, format);
        assertEquals("formatted‑error", result);
        assertNotNull(format.getCapturedMessages());
        assertFalse(format.getCapturedMessages().isEmpty(), "For an invalid instance the message set must contain at least one ValidationMessage");
    }

    // -----------------------------------------------------------------
    @Test
    void validate_NullRootNode_ThrowsNullPointerException(@Mock OutputFormat<String> format) {
        JsonNode nullNode = null;
        RecordingOutputFormat<String> format = new RecordingOutputFormat<>("unused");
        assertThrows(NullPointerException.class, () -> schema.validate(nullNode, format), "When rootNode is null a NullPointerException is expected");
    }

    @Test
    void validate_NullFormat_ThrowsIllegalArgumentException() throws Exception {
        JsonNode validNode = MAPPER.readTree("{\"age\":30}");
        OutputFormat<String> nullFormat = null;
        assertThrows(IllegalArgumentException.class, () -> schema.validate(validNode, nullFormat), "When format is null an IllegalArgumentException is expected");
    }

    // -----------------------------------------------------------------
    @Test
    void validate_OutputFormatThrowsRuntimeException(@Mock OutputFormat<String> format) throws Exception {
        JsonNode validNode = MAPPER.readTree("{\"age\":30}");
        OutputFormat<String> throwing = messages -> {
            throw new RuntimeException("boom");
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.validate(validNode, throwing));
        assertEquals("boom", ex.getMessage());
    }

    // -----------------------------------------------------------------
    @Test
    void validate_OutputFormatThrowsError(@Mock OutputFormat<String> format) throws Exception {
        JsonNode validNode = MAPPER.readTree("{\"age\":30}");
        OutputFormat<String> throwing = messages -> {
            throw new AssertionError("assertion failed");
        };
        AssertionError err = assertThrows(AssertionError.class, () -> schema.validate(validNode, throwing));
        assertEquals("assertion failed", err.getMessage());
    }

    // -----------------------------------------------------------------
    @Test
    void validate_OutputFormatReturnsNull(@Mock OutputFormat<String> format) throws Exception {
        JsonNode validNode = MAPPER.readTree("{\"age\":30}");
        RecordingOutputFormat<String> format = new RecordingOutputFormat<>(null);
        String result = schema.validate(validNode, format);
        assertNull(result, "When OutputFormat.apply returns null the method should propagate null");
    }

    @Test
    void validate_OutputFormatReturnsCustomObject(@Mock OutputFormat<CustomResult> format) throws Exception {
        JsonNode validNode = MAPPER.readTree("{\"age\":30}");
        CustomResult expected = new CustomResult("dto");
        RecordingOutputFormat<CustomResult> format = new RecordingOutputFormat<>(expected);
        CustomResult actual = schema.validate(validNode, format);
        assertSame(expected, actual, "The custom object returned by the format must be propagated unchanged");
    }

    // -----------------------------------------------------------------
    @Test
    void validate_MissingNode_TreatedAsValid(@Mock OutputFormat<String> format) throws Exception {
        JsonNode missingNode = JsonNodeFactory.instance.missingNode();
        RecordingOutputFormat<String> format = new RecordingOutputFormat<>("ok");
        String result = emptySchema.validate(missingNode, format);
        assertEquals("ok", result);
        assertNotNull(format.getCapturedMessages());
        assertTrue(format.getCapturedMessages().isEmpty(), "Empty schema should treat MissingNode as valid, producing an empty message set");
    }

    // -----------------------------------------------------------------
    @Test
    void validate_JsonNullAgainstObjectSchema_ProducesMessage(@Mock OutputFormat<String> format) throws Exception {
        JsonNode jsonNull = JsonNodeFactory.instance.nullNode();
        RecordingOutputFormat<String> format = new RecordingOutputFormat<>("failure");
        String result = objectSchema.validate(jsonNull, format);
        assertEquals("failure", result);
        assertNotNull(format.getCapturedMessages());
        assertFalse(format.getCapturedMessages().isEmpty(), "Validating a JSON null against an object‑type schema must produce a validation message");
    }

    // -----------------------------------------------------------------
    @Test
    void validate_RefSchemaInvalidInstance_ProducesMessage(@Mock OutputFormat<String> format) throws Exception {
        JsonNode invalid = MAPPER.readTree("{\"person\":{}}");
        RecordingOutputFormat<String> format = new RecordingOutputFormat<>("ref‑failure");
        String result = refSchema.validate(invalid, format);
        assertEquals("ref‑failure", result);
        assertNotNull(format.getCapturedMessages());
        assertFalse(format.getCapturedMessages().isEmpty(), "Missing required property inside $ref should generate a validation message");
    }

    // -----------------------------------------------------------------
    @Test
    void validate_RecursiveAnchorValidInstance(@Mock OutputFormat<String> format) throws Exception {
        JsonNode valid = MAPPER.readTree("{\"self\":{}}");
        RecordingOutputFormat<String> format = new RecordingOutputFormat<>("recursive‑ok");
        String result = recursiveSchema.validate(valid, format);
        assertEquals("recursive‑ok", result);
        assertNotNull(format.getCapturedMessages());
        assertTrue(format.getCapturedMessages().isEmpty(), "A valid instance for the recursive schema must result in an empty message set");
    }

    // -----------------------------------------------------------------
    @Test
    void validate_WithExecutionContextCustomizer_CustomizerIsInvoked(@Mock OutputFormat<String> format) throws Exception {
        JsonNode validNode = MAPPER.readTree("{\"age\":30}");
        RecordingOutputFormat<String> format = new RecordingOutputFormat<>("customizer‑ok");
        final boolean[] customizerCalled = { false };
        ExecutionContextCustomizer customizer = executionContext -> customizerCalled[0] = true;
        String result = schema.validate(validNode, format, customizer);
        assertEquals("customizer‑ok", result);
        assertTrue(customizerCalled[0], "The ExecutionContextCustomizer must have been executed");
        assertNotNull(format.getCapturedMessages());
        assertTrue(format.getCapturedMessages().isEmpty(), "When the instance is valid the message set must be empty");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:8: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:10: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:15: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:146: error: cannot find symbol
    void validate_ValidNode_UsesOutputFormat(@Mock OutputFormat<String> format) throws Exception {
                                              ^
  symbol:   class Mock
  location: class JsonSchema_validate_5_Test
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:156: error: cannot find symbol
    void validate_InvalidNode_UsesOutputFormat(@Mock OutputFormat<String> format) throws Exception {
                                                ^
  symbol:   class Mock
  location: class JsonSchema_validate_5_Test
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:167: error: cannot find symbol
    void validate_NullRootNode_ThrowsNullPointerException(@Mock OutputFormat<String> format) {
                                                           ^
  symbol:   class Mock
  location: class JsonSchema_validate_5_Test
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:182: error: cannot find symbol
    void validate_OutputFormatThrowsRuntimeException(@Mock OutputFormat<String> format) throws Exception {
                                                      ^
  symbol:   class Mock
  location: class JsonSchema_validate_5_Test
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:193: error: cannot find symbol
    void validate_OutputFormatThrowsError(@Mock OutputFormat<String> format) throws Exception {
                                           ^
  symbol:   class Mock
  location: class JsonSchema_validate_5_Test
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:204: error: cannot find symbol
    void validate_OutputFormatReturnsNull(@Mock OutputFormat<String> format) throws Exception {
                                           ^
  symbol:   class Mock
  location: class JsonSchema_validate_5_Test
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:212: error: cannot find symbol
    void validate_OutputFormatReturnsCustomObject(@Mock OutputFormat<CustomResult> format) throws Exception {
                                                   ^
  symbol:   class Mock
  location: class JsonSchema_validate_5_Test
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:222: error: cannot find symbol
    void validate_MissingNode_TreatedAsValid(@Mock OutputFormat<String> format) throws Exception {
                                              ^
  symbol:   class Mock
  location: class JsonSchema_validate_5_Test
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:233: error: cannot find symbol
    void validate_JsonNullAgainstObjectSchema_ProducesMessage(@Mock OutputFormat<String> format) throws Exception {
                                                               ^
  symbol:   class Mock
  location: class JsonSchema_validate_5_Test
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:244: error: cannot find symbol
    void validate_RefSchemaInvalidInstance_ProducesMessage(@Mock OutputFormat<String> format) throws Exception {
                                                            ^
  symbol:   class Mock
  location: class JsonSchema_validate_5_Test
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:255: error: cannot find symbol
    void validate_RecursiveAnchorValidInstance(@Mock OutputFormat<String> format) throws Exception {
                                                ^
  symbol:   class Mock
  location: class JsonSchema_validate_5_Test
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:266: error: cannot find symbol
    void validate_WithExecutionContextCustomizer_CustomizerIsInvoked(@Mock OutputFormat<String> format) throws Exception {
                                                                      ^
  symbol:   class Mock
  location: class JsonSchema_validate_5_Test
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:50: error: RecordingOutputFormat is not abstract and does not override abstract method format(JsonSchema,Set<ValidationMessage>,ExecutionContext,ValidationContext) in OutputFormat
    private static class RecordingOutputFormat<T> implements OutputFormat<T> {
                   ^
  where T is a type-variable:
    T extends Object declared in class RecordingOutputFormat
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:60: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:148: error: variable format is already defined in method validate_ValidNode_UsesOutputFormat(OutputFormat<String>)
        RecordingOutputFormat<String> format = new RecordingOutputFormat<>("formatted‑ok");
                                      ^
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:158: error: variable format is already defined in method validate_InvalidNode_UsesOutputFormat(OutputFormat<String>)
        RecordingOutputFormat<String> format = new RecordingOutputFormat<>("formatted‑error");
                                      ^
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:169: error: variable format is already defined in method validate_NullRootNode_ThrowsNullPointerException(OutputFormat<String>)
        RecordingOutputFormat<String> format = new RecordingOutputFormat<>("unused");
                                      ^
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:184: error: incompatible types: incompatible parameter types in lambda expression
        OutputFormat<String> throwing = messages -> {
                                        ^
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:195: error: incompatible types: incompatible parameter types in lambda expression
        OutputFormat<String> throwing = messages -> {
                                        ^
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:206: error: variable format is already defined in method validate_OutputFormatReturnsNull(OutputFormat<String>)
        RecordingOutputFormat<String> format = new RecordingOutputFormat<>(null);
                                      ^
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:215: error: variable format is already defined in method validate_OutputFormatReturnsCustomObject(OutputFormat<CustomResult>)
        RecordingOutputFormat<CustomResult> format = new RecordingOutputFormat<>(expected);
                                            ^
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:224: error: variable format is already defined in method validate_MissingNode_TreatedAsValid(OutputFormat<String>)
        RecordingOutputFormat<String> format = new RecordingOutputFormat<>("ok");
                                      ^
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:235: error: variable format is already defined in method validate_JsonNullAgainstObjectSchema_ProducesMessage(OutputFormat<String>)
        RecordingOutputFormat<String> format = new RecordingOutputFormat<>("failure");
                                      ^
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:246: error: variable format is already defined in method validate_RefSchemaInvalidInstance_ProducesMessage(OutputFormat<String>)
        RecordingOutputFormat<String> format = new RecordingOutputFormat<>("ref‑failure");
                                      ^
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:257: error: variable format is already defined in method validate_RecursiveAnchorValidInstance(OutputFormat<String>)
        RecordingOutputFormat<String> format = new RecordingOutputFormat<>("recursive‑ok");
                                      ^
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:268: error: variable format is already defined in method validate_WithExecutionContextCustomizer_CustomizerIsInvoked(OutputFormat<String>)
        RecordingOutputFormat<String> format = new RecordingOutputFormat<>("customizer‑ok");
                                      ^
src/test/java/com/networknt/schema/JsonSchema_validate_5_Test.java:270: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer customizer = executionContext -> customizerCalled[0] = true;
                                                ^
30 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method validate(JsonNode, OutputFormat<T>)': '\n     * Validates the given root JsonNode, starting at the root of the data path. The\n     * output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>      the result type\n     * @param rootNode the root node\n     * @param format   the formatter\n     * @return the result\n     ', 'return type': 'T', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(String, InputFormat, ExecutionContextCustomizer)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, OutputFormat<T>)`, api document: \n     * Validate the given input string using the input format, starting at the root\n     * of the data path.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     *\n     * @param input               the input\n     * @param inputFormat         the inputFormat\n     * @param executionCustomizer the execution customizer\n     * @return the assertions\n     ;\n\t\tmethod `public Set<ValidationMessage> validate(JsonNode, ExecutionContextCustomizer)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, OutputFormat<T>)`, api document: \n     * Validate the given root JsonNode, starting at the root of the data path.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     *\n     * @param rootNode            the root node\n     * @param executionCustomizer the execution customizer\n     * @return the assertions\n     ;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, OutputFormat<T>)`, api document: \n     * Returns the singleton instance of this validator.\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


