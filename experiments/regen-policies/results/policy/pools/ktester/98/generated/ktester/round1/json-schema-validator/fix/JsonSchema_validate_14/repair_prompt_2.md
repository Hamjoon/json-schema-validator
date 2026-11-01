


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.function.Consumer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

// @ExtendWith(MockitoExtension.class)
class JsonSchema_validate_14_Test {

    /**
     * Instance of {@link JsonSchema} used in the tests.
     */
    private JsonSchema schema;

    /**
     * Simple JSON schema that accepts any object.
     */
    private static final String SIMPLE_SCHEMA = "{\"type\":\"object\"}";

    /**
     * Simple JSON document that conforms to {@link #SIMPLE_SCHEMA}.
     */
    private static final String VALID_INPUT = "{}";

    /**
     * A tiny {@link Consumer} implementation that records whether it has been
     * invoked and stores the {@link ExecutionContext} it received.
     */
    private static final class RecordingConsumer implements Consumer<ExecutionContext> {

        boolean invoked = false;

        ExecutionContext receivedContext = null;

        @Override
        public void accept(ExecutionContext ctx) {
            this.invoked = true;
            this.receivedContext = ctx;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a JsonSchema instance using the public factory API.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        this.schema = factory.getSchema(SIMPLE_SCHEMA);
    }

    /**
     * Verifies that the {@code Consumer<ExecutionContext>} passed to the
     * {@code validate(String, InputFormat, OutputFormat, Consumer)} method
     * is invoked exactly once with the execution context used for validation.
     */
//     @Test
//     void validate_shouldInvokeConsumerWithExecutionContext() {
//         // Arrange
//         RecordingConsumer consumer = new RecordingConsumer();
//         // Act
//         JsonNode result = schema.validate(VALID_INPUT, InputFormat.JSON, OutputFormat.JSON, consumer);
//         // Assert
//         assertNotNull(result, "The validation should return a non‑null JsonNode result.");
//         assertTrue(consumer.invoked, "The consumer must have been invoked.");
//         assertNotNull(consumer.receivedContext, "The consumer should have received an ExecutionContext.");
//     }

    /**
     * A well‑formed JSON document that conforms to the schema should result in an
     * empty set of validation messages.
     */
//     @Test
//     void validateValidJsonString_returnsEmptySet() {
//         // Act
//         Set<ValidationMessage> messages = schema.validate("{\"name\":\"John\",\"age\":30}", InputFormat.JSON, OutputFormat.SET);
//         // Assert
//         assertTrue(messages.isEmpty(), "Expected no validation messages for a valid JSON document.");
//     }

    /**
     * A JSON document that violates the schema (age is a string instead of a number)
     * should produce a non‑empty set of validation messages.
     */
//     @Test
//     void validateInvalidJsonString_returnsNonEmptySet() {
//         // Act
//         Set<ValidationMessage> messages = schema.validate("{\"name\":\"John\",\"age\":\"thirty\"}", InputFormat.JSON, OutputFormat.SET);
//         // Assert
//         assertFalse(messages.isEmpty(), "Expected validation messages for an invalid JSON document.");
//     }

    /**
     * An empty string cannot be parsed as JSON and should cause a {@link JsonProcessingException}
     * to be thrown by the deserialize step.
     */
//     @Test
//     void validateEmptyString_throwsJsonProcessingException() {
//         // Act & Assert
//         assertThrows(JsonProcessingException.class, () -> schema.validate("", InputFormat.JSON, OutputFormat.JSON, ctx -> {
//         }), "Deserialising an empty string should throw JsonProcessingException.");
//     }

    /**
     * Passing a {@code null} input string should result in a {@link NullPointerException}
     * before any validation occurs.
     */
//     @Test
//     void validateNullInput_throwsNullPointerException() {
//         // Act & Assert
//         assertThrows(NullPointerException.class, () -> schema.validate(null, InputFormat.JSON, OutputFormat.JSON, ctx -> {
//         }), "A null input string should cause NullPointerException.");
//     }

    /**
     * Supplying an {@link InputFormat} that the implementation does not support should
     * cause an {@link IllegalArgumentException}.
     * <p>
     * The library defines an {@code UNSUPPORTED} constant for the purpose of this test.
     */
//     @Test
//     void validateUnsupportedInputFormat_throwsIllegalArgumentException() {
//         // Arrange
//         Consumer<ExecutionContext> noop = ctx -> {
//         };
//         // Act & Assert
//         assertThrows(IllegalArgumentException.class, () -> // assumed to exist for the test scenario
//         schema.// assumed to exist for the test scenario
//         validate("{\"name\":\"John\"}", InputFormat.UNSUPPORTED, OutputFormat.SET, noop), "Using an unsupported InputFormat should throw IllegalArgumentException.");
//     }

    /**
     * The consumer should be invoked and be able to mutate the {@link ExecutionContext}
     * (e.g., setting a custom attribute). Validation should still succeed and return an empty set.
     */
//     @Test
//     void validateWithCustomizerModifiesContext() {
//         // Arrange
//         Consumer<ExecutionContext> customiser = ctx -> ctx.setAttribute("custom", Boolean.TRUE);
//         // Act
//         Set<ValidationMessage> messages = schema.validate("{\"name\":\"John\"}", InputFormat.JSON, OutputFormat.SET, customiser);
//         // Assert that validation succeeded (empty set)
//         assertTrue(messages.isEmpty(), "Validation should succeed with no messages.");
//         // Capture the context from a subsequent validation to verify the attribute was set
//         RecordingConsumer capture = new RecordingConsumer();
//         schema.validate("{\"name\":\"John\"}", InputFormat.JSON, OutputFormat.JSON, capture);
//         assertEquals(Boolean.TRUE, capture.receivedContext.getAttribute("custom"), "The custom attribute should be present and set to true.");
//     }

    /**
     * If the consumer throws a {@link RuntimeException}, the exception should propagate
     * out of the {@code validate} method.
     */
//     @Test
//     void validateCustomizerThrowsRuntimeException_propagatesException() {
//         // Arrange
//         Consumer<ExecutionContext> failingCustomizer = ctx -> {
//             throw new RuntimeException("custom failure");
//         };
//         // Act & Assert
//         RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.validate("{\"name\":\"John\"}", InputFormat.JSON, OutputFormat.SET, failingCustomizer), "Exception thrown by the customiser should propagate.");
//         assertEquals("custom failure", ex.getMessage(), "Exception message should match the one thrown by the customiser.");
//     }

    /**
     * A valid YAML document should be correctly deserialized and validated,
     * resulting in an empty set of validation messages.
     */
//     @Test
//     void validateYamlInputFormat_returnsEmptySet() {
//         // Arrange
//         String yaml = "name: John\nage: 30";
//         // Act
//         Set<ValidationMessage> messages = schema.validate(yaml, InputFormat.YAML, OutputFormat.SET);
//         // Assert
//         assertTrue(messages.isEmpty(), "Valid YAML input should produce an empty validation set.");
//     }

    /**
     * Passing a {@code null} {@link InputFormat} should cause a {@link NullPointerException}
     * before any deserialization or validation occurs.
     */
//     @Test
//     void validateNullInputFormat_throwsNullPointerException() {
//         // Act & Assert
//         assertThrows(NullPointerException.class, () -> schema.validate("{\"name\":\"John\"}", null, OutputFormat.JSON, ctx -> {
//         }), "A null InputFormat should cause NullPointerException.");
//     }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Test run finished after 40 ms
[         3 containers found      ]
[         0 containers skipped    ]
[         3 containers started    ]
[         0 containers aborted    ]
[         3 containers successful ]
[         0 containers failed     ]
[         0 tests found           ]
[         0 tests skipped         ]
[         0 tests started         ]
[         0 tests aborted         ]
[         0 tests successful      ]
[         0 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

api document of class com.networknt.schema.JsonSchema: 

 * Used for creating a schema with validators for validating inputs. This is
 * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}
 * and should be cached for performance.
 * <p>
 * This is the core of json constraint implementation. It parses json constraint
 * file and generates JsonValidators. The class is thread safe, once it is
 * constructed, it can be used to validate multiple json data concurrently.
 * <p>
 * JsonSchema instances are thread-safe provided its configuration is not
 * modified.
 

api document of method validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>): 

     * Validates the given input string using the input format, starting at the root
     * of the data path. The output will be formatted using the formatter specified.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     * 
     * @param <T>                 the result type
     * @param input               the input
     * @param inputFormat         the inputFormat
     * @param format              the formatter
     * @param executionCustomizer the execution customizer
     * @return the result
     

return type: 
T

dependent classes: 
class `com.networknt.schema.InputFormat`:
	
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `public T validate(JsonNode, OutputFormat<T>, Consumer<ExecutionContext>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)`, api document: 
     * Validates the given root JsonNode, starting at the root of the data path. The
     * output will be formatted using the formatter specified.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     * 
     * @param <T>                 the result type
     * @param rootNode            the root node
     * @param format              the formatter
     * @param executionCustomizer the execution customizer
     * @return the result
     ;
		method `public T validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)`, api document: 
     * Validates the given input string using the input format, starting at the root
     * of the data path. The output will be formatted using the formatter specified.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     * 
     * @param <T>                 the result type
     * @param input               the input
     * @param inputFormat         the inputFormat
     * @param format              the formatter
     * @param executionCustomizer the execution customizer
     * @return the result
     ;
		method `public ValidationResult walk(String, InputFormat, boolean)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)`, api document: 
     * Walk the input.
     * 
     * @param input       the input
     * @param inputFormat the input format
     * @param validate    true to validate the input against the schema
     * @return the validation result
     ;
		method `public T validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)`, api document: 
     * Validates the given root JsonNode, starting at the root of the data path. The
     * output will be formatted using the formatter specified.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     * 
     * @param <T>                 the result type
     * @param rootNode            the root node
     * @param format              the formatter
     * @param executionCustomizer the execution customizer
     * @return the result
     ;
class `com.networknt.schema.SchemaValidatorsConfig.Builder`:
	related functions:
		method `public Builder itemWalkListeners(Consumer<List<JsonSchemaWalkListener>>)` returns `com.networknt.schema.SchemaValidatorsConfig.Builder`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)`;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
