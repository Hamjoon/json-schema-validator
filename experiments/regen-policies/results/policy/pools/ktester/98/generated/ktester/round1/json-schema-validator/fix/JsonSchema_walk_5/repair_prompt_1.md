
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_5_Test {

    /**
     * Shared factory used to create the schema under test.
     */
    private static JsonSchemaFactory factory;

    /**
     * The schema instance used by all test methods.
     */
    private static JsonSchema schema;

    /**
     * Simple JSON schema used for the tests.
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
        // Initialise the factory once for the whole test class.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        // Build a schema without any special configuration.
        schema = factory.getSchema(SIMPLE_SCHEMA);
        assertNotNull(schema, "Schema should be created successfully");
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static references – helps the GC in long test suites.
        factory = null;
        schema = null;
    }

    /**
     * Verify that {@code walk(..., true)} performs validation and returns a
     * {@link ValidationResult} containing the expected validation messages.
     */
    @Test
    void walk_ValidateTrue_ReturnsMessages() {
        // Input that violates the schema (name must be a string).
        String invalidJson = "{ \"name\": 123 }";
        ValidationResult result = schema.walk(invalidJson, InputFormat.JSON, true);
        assertNotNull(result, "Result must not be null");
        // The result should contain at least one validation message.
        List<ValidationMessage> messages = result.getValidationMessages().stream().collect(Collectors.toList());
        assertFalse(messages.isEmpty(), "Validation messages should be present for invalid input");
        // Optionally assert that the message mentions the property 'name'.
        boolean mentionsName = messages.stream().anyMatch(m -> m.getPath().contains("name"));
        assertTrue(mentionsName, "At least one message should refer to the 'name' property");
    }

    /**
     * Verify that {@code walk(..., false)} does **not** perform validation.
     * The returned {@link ValidationResult} should contain no messages.
     */
    @Test
    void walk_ValidateFalse_NoMessages() {
        // Same invalid JSON, but validation is disabled.
        String invalidJson = "{ \"name\": 123 }";
        ValidationResult result = schema.walk(invalidJson, InputFormat.JSON, false);
        assertNotNull(result, "Result must not be null");
        // When validation is disabled the result should be empty.
        List<ValidationMessage> messages = result.getValidationMessages().stream().collect(Collectors.toList());
        assertTrue(messages.isEmpty(), "No validation messages should be returned when validation is disabled");
    }

    /**
     * A basic concurrency test similar to the one in {@link JsonSchemaTest},
     * but exercising the {@code walk} method.
     */
    @Test
    void walk_concurrentExecution() throws Exception {
        String validJson = "{ \"name\": \"John\" }";
        int threadCount = 30;
        var latch = new java.util.concurrent.CountDownLatch(1);
        var threads = new java.util.ArrayList<Thread>();
        var exceptionHolder = new Exception[1];
        for (int i = 0; i < threadCount; ++i) {
            Thread t = new Thread(() -> {
                try {
                    latch.await();
                    ValidationResult r = schema.walk(validJson, InputFormat.JSON, true);
                    assertNotNull(r);
                    assertTrue(r.getValidationMessages().isEmpty(), "Valid JSON should produce no validation messages");
                } catch (Exception e) {
                    synchronized (exceptionHolder) {
                        exceptionHolder[0] = e;
                    }
                }
            });
            t.start();
            threads.add(t);
        }
        // release all threads simultaneously
        latch.countDown();
        for (Thread t : threads) {
            t.join();
        }
        if (exceptionHolder[0] != null) {
            throw exceptionHolder[0];
        }
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_walk_5_Test.java:9: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_walk_5_Test.java:10: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_walk_5_Test.java:13: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_walk_5_Test.java:70: error: cannot find symbol
        boolean mentionsName = messages.stream().anyMatch(m -> m.getPath().contains("name"));
                                                                ^
  symbol:   method getPath()
  location: variable m of type ValidationMessage
4 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method walk(String, InputFormat, boolean)': '\n     * Walk the input.\n     * \n     * @param input       the input\n     * @param inputFormat the input format\n     * @param validate    true to validate the input against the schema\n     * @return the validation result\n     ', 'return type': 'com.networknt.schema.ValidationResult', 'dependent classes': 'class `com.networknt.schema.InputFormat`:\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public ValidationResult walk(JsonNode, boolean)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(String, InputFormat, boolean)`, api document: \n     * Walk the JSON node.\n     * \n     * @param node     the input\n     * @param validate true to validate the input against the schema\n     * @return the validation result\n     ;\n\t\tmethod `public T validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.walk(String, InputFormat, boolean)`, api document: \n     * Validates the given input string using the input format, starting at the root\n     * of the data path. The output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param input               the input\n     * @param inputFormat         the inputFormat\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;\n\t\tmethod `public T validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.walk(String, InputFormat, boolean)`, api document: \n     * Validates the given root JsonNode, starting at the root of the data path. The\n     * output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param rootNode            the root node\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;\n\t\tmethod `public T validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)` returns `T`, related with `com.networknt.schema.JsonSchema.walk(String, InputFormat, boolean)`, api document: \n     * Validates the given input string using the input format, starting at the root\n     * of the data path. The output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param input               the input\n     * @param inputFormat         the inputFormat\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;\n\t\tmethod `public ValidationResult walk(String, InputFormat, boolean)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(String, InputFormat, boolean)`, api document: \n     * Walk the input.\n     * \n     * @param input       the input\n     * @param inputFormat the input format\n     * @param validate    true to validate the input against the schema\n     * @return the validation result\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


