
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.UnsupportedEncodingException;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_7_Test {

    /**
     * Shared objects for all tests
     */
    private static JsonSchema schema;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * A simple schema that requires a string property {@code name}. Used by a few tests.
     */
    private static JsonSchema simpleNameSchema;

    /**
     * Collects {@link ValidationMessage}s into a {@link List}.
     */
    private static final OutputFormat<List<ValidationMessage>> COLLECTING_FORMAT = messages -> new ArrayList<>(messages);

    /**
     * Returns {@code true} when there are no validation messages, {@code false} otherwise.
     */
    private static final OutputFormat<Boolean> BOOLEAN_FORMAT = messages -> messages.isEmpty();

    @BeforeAll
    static void setupBeforeAll() {
        // A tiny schema that requires a string property "name"
        String schemaData = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              },
              "required": ["name"]
            }""";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        schema = factory.getSchema(schemaData);
        // The same simple schema is used in several tests
        simpleNameSchema = factory.getSchema(schemaData);
    }

    private static Stream<Arguments> nullCheckProvider() {
        JsonNode validNode = MAPPER.createObjectNode().put("name", "John");
        return Stream.of(// executionContext == null
        Arguments.of(null, validNode, COLLECTING_FORMAT, "executionContext must not be null"), // node == null
        Arguments.of(new ExecutionContext(), null, COLLECTING_FORMAT, "node must not be null"), // format == null
        Arguments.of(new ExecutionContext(), validNode, null, "format must not be null"));
    }

    /**
     * Verify that a valid JSON node produces an empty list of validation messages
     * when the {@code validate(ExecutionContext, JsonNode, OutputFormat)} method
     * is used.
     */
    @Test
    void testValidateValidNode() throws Exception {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = MAPPER.readTree("{\"name\":\"John\"}");
        List<ValidationMessage> result = schema.validate(ctx, node, COLLECTING_FORMAT);
        assertTrue(result.isEmpty(), "Expected no validation messages for a valid node");
    }

    /**
     * Verify that an invalid JSON node (wrong type for {@code name}) yields a
     * non‑empty list of validation messages.
     */
    @Test
    void testValidateInvalidNode() throws Exception {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = MAPPER.readTree("{\"name\":1}");
        List<ValidationMessage> result = schema.validate(ctx, node, COLLECTING_FORMAT);
        assertFalse(result.isEmpty(), "Expected validation messages for an invalid node");
        assertEquals(1, result.size(), "Exactly one validation error should be reported");
    }

    @ParameterizedTest
    @MethodSource("nullCheckProvider")
    @DisplayName("Argument validation – null checks")
    void testValidateArgumentNullChecks(ExecutionContext ctx, JsonNode node, OutputFormat<?> format, String expectedMessage) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> simpleNameSchema.validate(ctx, node, format));
        assertTrue(ex.getMessage().toLowerCase(Locale.ROOT).contains(expectedMessage.toLowerCase(Locale.ROOT)), () -> "Exception message should contain '" + expectedMessage + "'");
    }

    @Test
    @DisplayName("Invalid node (type mismatch) yields a validation message")
    void testValidateInvalidType() throws Exception {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = MAPPER.readTree("{\"name\":1}");
        List<ValidationMessage> result = simpleNameSchema.validate(ctx, node, COLLECTING_FORMAT);
        assertFalse(result.isEmpty(), "Expected validation messages for an invalid node");
        assertEquals(1, result.size(), "Exactly one validation error should be reported");
        assertTrue(result.get(0).getMessage().toLowerCase(Locale.ROOT).contains("type"), "Message should indicate a type mismatch");
    }

    @Test
    @DisplayName("Missing required property produces a validation message")
    void testValidateMissingRequired() throws Exception {
        String schemaStr = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "properties": { "id": { "type": "string" } },
              "required": ["id"]
            }""";
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(schemaStr);
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = MAPPER.readTree("{}");
        List<ValidationMessage> result = schema.validate(ctx, node, COLLECTING_FORMAT);
        assertFalse(result.isEmpty(), "Expected a validation message for missing required property");
        assertTrue(result.stream().anyMatch(m -> m.getMessage().toLowerCase(Locale.ROOT).contains("required")), "Message should mention 'required'");
    }

    @Test
    @DisplayName("BooleanOutputFormat returns true for a valid instance")
    void testBooleanOutputFormatSuccess() throws Exception {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = MAPPER.readTree("{\"name\":\"Alice\"}");
        Boolean result = simpleNameSchema.validate(ctx, node, BOOLEAN_FORMAT);
        assertTrue(result, "BooleanOutputFormat should return true when validation succeeds");
    }

    @Test
    @DisplayName("BooleanOutputFormat returns false for an invalid instance")
    void testBooleanOutputFormatFailure() throws Exception {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = MAPPER.readTree("{\"name\":123}");
        Boolean result = simpleNameSchema.validate(ctx, node, BOOLEAN_FORMAT);
        assertFalse(result, "BooleanOutputFormat should return false when validation fails");
    }

    @Test
    @DisplayName("Recursive anchor – valid self‑referencing structure")
    void testRecursiveAnchorValid() throws Exception {
        String schemaStr = """
            {
              "$id": "http://example.com/linked-list",
              "$recursiveAnchor": true,
              "type": "object",
              "properties": {
                "value": { "type": "integer" },
                "next": { "$ref": "#" }
              },
              "required": ["value"]
            }""";
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(schemaStr);
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = MAPPER.readTree("{\"value\":1,\"next\":{\"value\":2}}");
        List<ValidationMessage> result = schema.validate(ctx, node, COLLECTING_FORMAT);
        assertTrue(result.isEmpty(), "Recursive schema should validate without infinite recursion");
    }

    @Test
    @DisplayName("Recursive anchor – nested violation is reported")
    void testRecursiveAnchorInvalidNested() throws Exception {
        String schemaStr = """
            {
              "$id": "http://example.com/linked-list",
              "$recursiveAnchor": true,
              "type": "object",
              "properties": {
                "value": { "type": "integer" },
                "next": { "$ref": "#" }
              },
              "required": ["value"]
            }""";
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(schemaStr);
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = MAPPER.readTree("{\"value\":1,\"next\":{\"value\":\"bad\"}}");
        List<ValidationMessage> result = schema.validate(ctx, node, COLLECTING_FORMAT);
        assertFalse(result.isEmpty(), "Expected a validation message for the nested type error");
        assertTrue(result.stream().anyMatch(m -> m.getMessage().toLowerCase(Locale.ROOT).contains("type")), "Message should indicate a type mismatch in the nested node");
    }

    @Test
    @DisplayName("Internal $ref resolves correctly")
    void testInternalRefResolution() throws Exception {
        String schemaStr = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "$ref": "#/definitions/person",
              "definitions": {
                "person": {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  },
                  "required": ["name"]
                }
              }
            }""";
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(schemaStr);
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = MAPPER.readTree("{\"name\":\"Bob\"}");
        List<ValidationMessage> result = schema.validate(ctx, node, COLLECTING_FORMAT);
        assertTrue(result.isEmpty(), "Internal $ref should be resolved and validation succeed");
    }

    @Test
    @DisplayName("Unknown keyword triggers JsonSchemaException")
    void testUnknownKeywordParsingError() {
        String schemaStr = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "unknownKeyword": true
            }""";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> factory.getSchema(schemaStr));
        assertTrue(ex.getMessage().toLowerCase(Locale.ROOT).contains("unknown"), "Exception message should mention 'unknown keyword'");
    }

    @Test
    @DisplayName("Malformed $ref URI causes IllegalArgumentException")
    void testMalformedRefUri() {
        String schemaStr = """
            {
              "$ref": "ht!tp://[invalid]"
            }""";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> factory.getSchema(schemaStr));
        assertTrue(ex.getMessage().toLowerCase(Locale.ROOT).contains("uri"), "Exception message should mention 'URI' or 'invalid $ref'");
    }

    @Test
    @DisplayName("Invalid percent‑encoding in $id throws UnsupportedEncodingException")
    void testUnsupportedEncodingInId() {
        String schemaStr = """
            {
              "$id": "http://example.com/schema%ZZ"
            }""";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        assertThrows(UnsupportedEncodingException.class, () -> factory.getSchema(schemaStr));
    }

    @Test
    @DisplayName("ExecutionContext without ValidationContext causes IllegalStateException")
    void testExecutionContextWithoutValidationContext() throws Exception {
        // Use reflection to invoke the (package‑private) constructor that accepts a ValidationContext.
        Constructor<ExecutionContext> ctor = ExecutionContext.class.getDeclaredConstructor(ValidationContext.class);
        ctor.setAccessible(true);
        // pass null ValidationContext
        ExecutionContext ctx = ctor.newInstance((Object) null);
        String schemaStr = """
            { "type": "string" }""";
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(schemaStr);
        JsonNode node = MAPPER.readTree("\"hello\"");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> schema.validate(ctx, node, COLLECTING_FORMAT));
        assertTrue(ex.getMessage().toLowerCase(Locale.ROOT).contains("validationcontext"), "Exception message should mention missing ValidationContext");
    }

    @Test
    @DisplayName("Circular $ref without $recursiveAnchor throws JsonSchemaException")
    void testCircularRefWithoutRecursiveAnchor() {
        String schemaStr = """
            {
              "$id": "#",
              "$ref": "#"
            }""";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> factory.getSchema(schemaStr));
        assertTrue(ex.getMessage().toLowerCase(Locale.ROOT).contains("circular"), "Exception message should contain 'circular reference'");
    }

    @Test
    @Disabled("External $ref resolution requires network/file system access which is out of scope for unit tests.")
    void testExternalRefResolution() {
        // Placeholder for an external $ref test. In a real integration test,
        // a mock URI resolver would be supplied to the JsonSchemaFactory.
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_validate_7_Test.java:14: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_7_Test.java:18: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_validate_7_Test.java:36: error: cannot infer type arguments for ArrayList<>
    private static final OutputFormat<List<ValidationMessage>> COLLECTING_FORMAT = messages -> new ArrayList<>(messages);
                                                                                               ^
src/test/java/com/networknt/schema/JsonSchema_validate_7_Test.java:36: error: incompatible types: incompatible parameter types in lambda expression
    private static final OutputFormat<List<ValidationMessage>> COLLECTING_FORMAT = messages -> new ArrayList<>(messages);
                                                                                   ^
src/test/java/com/networknt/schema/JsonSchema_validate_7_Test.java:41: error: cannot find symbol
    private static final OutputFormat<Boolean> BOOLEAN_FORMAT = messages -> messages.isEmpty();
                                                                                    ^
  symbol:   method isEmpty()
  location: variable messages of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_validate_7_Test.java:41: error: incompatible types: incompatible parameter types in lambda expression
    private static final OutputFormat<Boolean> BOOLEAN_FORMAT = messages -> messages.isEmpty();
                                                                ^
6 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method validate(ExecutionContext, JsonNode, OutputFormat<T>)': '\n     * Validates to a format.\n     * \n     * @param <T>              the result type\n     * @param executionContext the execution context\n     * @param node             the node\n     * @param format           the format\n     * @return the result\n     ', 'return type': 'T', 'dependent classes': 'class `com.networknt.schema.ExecutionContext`:\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public T validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, OutputFormat<T>)`, api document: \n     * Validates the given root JsonNode, starting at the root of the data path. The\n     * output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param rootNode            the root node\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;\n\t\tmethod `public T validate(JsonNode, OutputFormat<T>, Consumer<ExecutionContext>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, OutputFormat<T>)`, api document: \n     * Validates the given root JsonNode, starting at the root of the data path. The\n     * output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param rootNode            the root node\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;\n\t\tmethod `public T validate(ExecutionContext, JsonNode, OutputFormat<T>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, OutputFormat<T>)`, api document: \n     * Validates to a format.\n     * \n     * @param <T>              the result type\n     * @param executionContext the execution context\n     * @param node             the node\n     * @param format           the format\n     * @return the result\n     ;\n\t\tmethod `public T validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, OutputFormat<T>)`, api document: \n     * Validates the given input string using the input format, starting at the root\n     * of the data path. The output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param input               the input\n     * @param inputFormat         the inputFormat\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, OutputFormat<T>)`, api document: \n     * Returns the singleton instance of this validator.\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


