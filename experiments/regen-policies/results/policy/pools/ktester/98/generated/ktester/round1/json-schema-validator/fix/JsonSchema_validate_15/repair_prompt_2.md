
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import com.fasterxml.jackson.core.JsonProcessingException;

class JsonSchema_validate_15_Test {

    /**
     * Shared schema used by all test cases
     */
    private static JsonSchema schema;

    /**
     * Input format used for the tests (JSON string)
     */
    private static final InputFormat INPUT_FORMAT = InputFormat.JSON;

    /**
     * Simple {@link OutputFormat} implementation that materialises the set of
     *  {@link ValidationMessage}s into a {@link List}.
     */
    private static final OutputFormat<List<ValidationMessage>> LIST_OUTPUT_FORMAT = messages -> new ArrayList<>(messages);

    /**
     * No‑op customiser – required by the method signature but does not alter the
     *  execution context.
     */
    private static final ExecutionContextCustomizer NO_OP_CUSTOMIZER = ctx -> {
        // intentionally left blank
    };

    @BeforeAll
    static void setupBeforeAll() {
        // A tiny schema that expects an object with a string property “name”.
        String schemaData = "{\n" + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\n" + "    \"name\": { \"type\": \"string\" }\n" + "  },\n" + "  \"required\": [\"name\"]\n" + "}";
        // Build a factory for the 2020‑12 draft and obtain a JsonSchema instance.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder -> builder.schemaLoaders(schemaLoaders -> schemaLoaders.schemas(Collections.emptyMap())));
        // No special configuration required for these tests.
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        schema = factory.getSchema(schemaData, config);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up for the current test suite – the schema is immutable
        // and can be left for garbage collection.
        schema = null;
    }

    @Test
    void validate_invalidInput_returnsValidationMessages() {
        // Input violates the schema: “name” is an integer, not a string.
        String badInput = "{\"name\":1}";
        List<ValidationMessage> messages = schema.validate(badInput, INPUT_FORMAT, LIST_OUTPUT_FORMAT, NO_OP_CUSTOMIZER);
        // The schema should produce exactly one validation error about the type.
        assertEquals(1, messages.size(), "Expected a single validation message");
        ValidationMessage vm = messages.get(0);
        assertTrue(vm.getMessage().toLowerCase().contains("type"), "Message should mention a type problem, but was: " + vm.getMessage());
    }

    @Test
    void validate_validInput_returnsEmptyList() {
        // Input conforms to the schema.
        String goodInput = "{\"name\":\"John\"}";
        List<ValidationMessage> messages = schema.validate(goodInput, INPUT_FORMAT, LIST_OUTPUT_FORMAT, NO_OP_CUSTOMIZER);
        // No validation errors expected.
        assertTrue(messages.isEmpty(), "Expected no validation messages for valid input");
    }

    @Test
    void validate_withCustomExecutionContextCustomizer_isInvoked() {
        // Verify that the customiser is actually called by toggling a flag on the
        // execution context.  We use a simple mutable holder for the side‑effect.
        final boolean[] customiserInvoked = { false };
        ExecutionContextCustomizer customizer = new ExecutionContextCustomizer() {

            @Override
            public void customize(ExecutionContext ctx) {
                customiserInvoked[0] = true;
            }
        };
        String input = "{\"name\":\"Alice\"}";
        schema.validate(input, INPUT_FORMAT, LIST_OUTPUT_FORMAT, customizer);
        assertTrue(customiserInvoked[0], "ExecutionContextCustomizer should have been invoked");
    }

    @Test
    void validate_nullInput_throwsNullPointerException() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> schema.validate(null, INPUT_FORMAT, LIST_OUTPUT_FORMAT, NO_OP_CUSTOMIZER));
        assertEquals("input must not be null", ex.getMessage());
    }

    @Test
    void validate_nullInputFormat_throwsNullPointerException() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> schema.validate("{\"name\":\"John\"}", null, LIST_OUTPUT_FORMAT, NO_OP_CUSTOMIZER));
        assertEquals("inputFormat must not be null", ex.getMessage());
    }

    @Test
    void validate_nullOutputFormat_throwsNullPointerException() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> schema.validate("{\"name\":\"John\"}", INPUT_FORMAT, null, NO_OP_CUSTOMIZER));
        assertEquals("output format must not be null", ex.getMessage());
    }

    @Test
    void validate_urlEncodedInvalid_throwsWrappedUnsupportedEncodingException() {
        // UrlEncodedInputFormat will try to URL‑decode the string; the bogus value
        // triggers an UnsupportedEncodingException which must be wrapped.
        InputFormat urlEncoded = UrlEncodedInputFormat.INSTANCE;
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> schema.validate("%ZZInvalid%", urlEncoded, LIST_OUTPUT_FORMAT, NO_OP_CUSTOMIZER));
        assertTrue(ex.getCause() instanceof UnsupportedEncodingException, "Cause should be UnsupportedEncodingException");
    }

    @Test
    void validate_malformedJson_throwsWrappedJsonProcessingException() {
        // JsonInputFormat parses the string as JSON – malformed content causes a
        // JsonProcessingException that must be wrapped.
        InputFormat jsonFormat = JsonInputFormat.INSTANCE;
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> schema.validate("not a json", jsonFormat, LIST_OUTPUT_FORMAT, NO_OP_CUSTOMIZER));
        assertTrue(ex.getCause() instanceof JsonProcessingException, "Cause should be JsonProcessingException");
    }

    @Test
    void validate_customizerThrows_propagatesException() {
        ExecutionContextCustomizer badCustomizer = new ExecutionContextCustomizer() {

            @Override
            public void customize(ExecutionContext ctx) {
                throw new IllegalStateException("customizer failure");
            }
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> schema.validate("{\"name\":\"John\"}", INPUT_FORMAT, LIST_OUTPUT_FORMAT, badCustomizer));
        assertEquals("customizer failure", ex.getMessage());
    }

    @Test
    void validate_outputFormatThrows_propagatesRuntimeException() {
        // OutputFormat that deliberately throws.
        OutputFormat<List<ValidationMessage>> throwingFormat = new OutputFormat<List<ValidationMessage>>() {

            @Override
            public List<ValidationMessage> format(Set<ValidationMessage> messages) {
                throw new RuntimeException("output format failure");
            }
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.validate("{\"name\":\"John\"}", INPUT_FORMAT, throwingFormat, NO_OP_CUSTOMIZER));
        assertEquals("output format failure", ex.getMessage());
    }

    @Test
    void validate_inputFormatThrowsIOException_wrappedInJsonSchemaException() throws IOException {
        // InputFormat whose deserialize method always throws IOException.
        InputFormat ioThrowingFormat = new InputFormat() {

            @Override
            public JsonNode deserialize(String input) throws IOException {
                throw new IOException("boom");
            }
        };
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> schema.validate("any string", ioThrowingFormat, LIST_OUTPUT_FORMAT, NO_OP_CUSTOMIZER));
        assertTrue(ex.getCause() instanceof IOException);
        assertEquals("boom", ex.getCause().getMessage());
    }

    @Test
    void validate_invalidRef_throwsJsonSchemaException() {
        // The instance contains a $ref that cannot be resolved by the schema.
        String inputWithBadRef = "{\"$ref\":\"#/nonexistent\"}";
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> schema.validate(inputWithBadRef, INPUT_FORMAT, LIST_OUTPUT_FORMAT, NO_OP_CUSTOMIZER));
        assertTrue(ex.getMessage().contains("Unable to resolve reference"), "Exception message should mention inability to resolve reference");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_validate_15_Test.java:30: error: cannot infer type arguments for ArrayList<>
    private static final OutputFormat<List<ValidationMessage>> LIST_OUTPUT_FORMAT = messages -> new ArrayList<>(messages);
                                                                                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_15_Test.java:30: error: incompatible types: incompatible parameter types in lambda expression
    private static final OutputFormat<List<ValidationMessage>> LIST_OUTPUT_FORMAT = messages -> new ArrayList<>(messages);
                                                                                    ^
src/test/java/com/networknt/schema/JsonSchema_validate_15_Test.java:36: error: incompatible types: incompatible parameter types in lambda expression
    private static final ExecutionContextCustomizer NO_OP_CUSTOMIZER = ctx -> {
                                                                       ^
src/test/java/com/networknt/schema/JsonSchema_validate_15_Test.java:83: error: <anonymous com.networknt.schema.JsonSchema_validate_15_Test$1> is not abstract and does not override abstract method customize(ExecutionContext,ValidationContext) in ExecutionContextCustomizer
        ExecutionContextCustomizer customizer = new ExecutionContextCustomizer() {
                                                                                 ^
src/test/java/com/networknt/schema/JsonSchema_validate_15_Test.java:85: error: method does not override or implement a method from a supertype
            @Override
            ^
src/test/java/com/networknt/schema/JsonSchema_validate_15_Test.java:117: error: cannot find symbol
        InputFormat urlEncoded = UrlEncodedInputFormat.INSTANCE;
                                 ^
  symbol:   variable UrlEncodedInputFormat
  location: class JsonSchema_validate_15_Test
src/test/java/com/networknt/schema/JsonSchema_validate_15_Test.java:126: error: cannot find symbol
        InputFormat jsonFormat = JsonInputFormat.INSTANCE;
                                 ^
  symbol:   variable JsonInputFormat
  location: class JsonSchema_validate_15_Test
src/test/java/com/networknt/schema/JsonSchema_validate_15_Test.java:133: error: <anonymous com.networknt.schema.JsonSchema_validate_15_Test$2> is not abstract and does not override abstract method customize(ExecutionContext,ValidationContext) in ExecutionContextCustomizer
        ExecutionContextCustomizer badCustomizer = new ExecutionContextCustomizer() {
                                                                                    ^
src/test/java/com/networknt/schema/JsonSchema_validate_15_Test.java:135: error: method does not override or implement a method from a supertype
            @Override
            ^
src/test/java/com/networknt/schema/JsonSchema_validate_15_Test.java:150: error: type List does not take parameters
            public List<ValidationMessage> format(Set<ValidationMessage> messages) {
                       ^
src/test/java/com/networknt/schema/JsonSchema_validate_15_Test.java:147: error: <anonymous com.networknt.schema.JsonSchema_validate_15_Test$3> is not abstract and does not override abstract method format(JsonSchema,Set<ValidationMessage>,ExecutionContext,ValidationContext) in OutputFormat
        OutputFormat<List<ValidationMessage>> throwingFormat = new OutputFormat<List<ValidationMessage>>() {
                                                                                                           ^
src/test/java/com/networknt/schema/JsonSchema_validate_15_Test.java:149: error: method does not override or implement a method from a supertype
            @Override
            ^
src/test/java/com/networknt/schema/JsonSchema_validate_15_Test.java:161: error: enum types may not be instantiated
        InputFormat ioThrowingFormat = new InputFormat() {
                                       ^
src/test/java/com/networknt/schema/JsonSchema_validate_15_Test.java:161: error: cannot inherit from final InputFormat
        InputFormat ioThrowingFormat = new InputFormat() {
                                           ^
src/test/java/com/networknt/schema/JsonSchema_validate_15_Test.java:161: error: enum types are not extensible
        InputFormat ioThrowingFormat = new InputFormat() {
                                                         ^
src/test/java/com/networknt/schema/JsonSchema_validate_15_Test.java:163: error: method does not override or implement a method from a supertype
            @Override
            ^
16 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer)': '\n     * Validates the given input string using the input format, starting at the root\n     * of the data path. The output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param input               the input\n     * @param inputFormat         the inputFormat\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ', 'return type': 'T', 'dependent classes': 'class `com.networknt.schema.InputFormat`:\n\t\nclass `com.networknt.schema.ExecutionContextCustomizer`:\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public T validate(ExecutionContext, JsonNode, OutputFormat<T>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer)`, api document: \n     * Validates to a format.\n     * \n     * @param <T>              the result type\n     * @param executionContext the execution context\n     * @param node             the node\n     * @param format           the format\n     * @return the result\n     ;\n\t\tmethod `public T validate(JsonNode, OutputFormat<T>, Consumer<ExecutionContext>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer)`, api document: \n     * Validates the given root JsonNode, starting at the root of the data path. The\n     * output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param rootNode            the root node\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;\n\t\tmethod `public T validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer)`, api document: \n     * Validates the given input string using the input format, starting at the root\n     * of the data path. The output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param input               the input\n     * @param inputFormat         the inputFormat\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;\n\t\tmethod `public T validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer)`, api document: \n     * Validates the given input string using the input format, starting at the root\n     * of the data path. The output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param input               the input\n     * @param inputFormat         the inputFormat\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;\n\t\tmethod `public T validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer)`, api document: \n     * Validates the given root JsonNode, starting at the root of the data path. The\n     * output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param rootNode            the root node\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;\n\t\tmethod `public Set<ValidationMessage> validate(String, InputFormat)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer)`, api document: \n     * Validate the given input string using the input format, starting at the root\n     * of the data path.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param input       the input\n     * @param inputFormat the inputFormat\n     * @return A list of ValidationMessage if there is any validation error, or an\n     *         empty list if there is no error.\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


