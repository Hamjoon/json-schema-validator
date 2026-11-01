


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

// @ExtendWith(MockitoExtension.class)
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
     * and stores the set of {@link ValidationMessage}s it received.
     */
    private static class RecordingOutputFormat<T> implements OutputFormat<T> {

        private final T result;

        private Set<ValidationMessage> capturedMessages;

        RecordingOutputFormat(T result) {
            this.result = result;
        }

        @Override
        public T format(JsonSchema schema, Set<ValidationMessage> messages, ExecutionContext executionContext, ValidationContext validationContext) {
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

//     @Test
//     void validate_ValidNode_UsesOutputFormat(@Mock OutputFormat<String> format) throws Exception {
//         JsonNode validNode = MAPPER.readTree("{\"age\":30}");
//         RecordingOutputFormat<String> format = new RecordingOutputFormat<>("formatted‑ok");
//         String result = schema.validate(validNode, format);
//         assertEquals("formatted‑ok", result);
//         assertNotNull(format.getCapturedMessages());
//         assertTrue(format.getCapturedMessages().isEmpty(), "For a valid instance the message set must be empty");
//     }

//     @Test
//     void validate_InvalidNode_UsesOutputFormat(@Mock OutputFormat<String> format) throws Exception {
//         JsonNode invalidNode = MAPPER.readTree("{\"age\":\"thirty\"}");
//         RecordingOutputFormat<String> format = new RecordingOutputFormat<>("formatted‑error");
//         String result = schema.validate(invalidNode, format);
//         assertEquals("formatted‑error", result);
//         assertNotNull(format.getCapturedMessages());
//         assertFalse(format.getCapturedMessages().isEmpty(), "For an invalid instance the message set must contain at least one ValidationMessage");
//     }

    // -----------------------------------------------------------------
//     @Test
//     void validate_NullRootNode_ThrowsNullPointerException(@Mock OutputFormat<String> format) {
//         JsonNode nullNode = null;
//         RecordingOutputFormat<String> format = new RecordingOutputFormat<>("unused");
//         assertThrows(NullPointerException.class, () -> schema.validate(nullNode, format), "When rootNode is null a NullPointerException is expected");
//     }

    @Test
    void validate_NullFormat_ThrowsIllegalArgumentException() throws Exception {
        JsonNode validNode = MAPPER.readTree("{\"age\":30}");
        OutputFormat<String> nullFormat = null;
        assertThrows(IllegalArgumentException.class, () -> schema.validate(validNode, nullFormat), "When format is null an IllegalArgumentException is expected");
    }

    // -----------------------------------------------------------------
//     @Test
//     void validate_OutputFormatThrowsRuntimeException(@Mock OutputFormat<String> format) throws Exception {
//         JsonNode validNode = MAPPER.readTree("{\"age\":30}");
//         OutputFormat<String> throwing = new OutputFormat<>() {
// 
//             @Override
//             public String format(JsonSchema schema, Set<ValidationMessage> messages, ExecutionContext executionContext, ValidationContext validationContext) {
//                 throw new RuntimeException("boom");
//             }
//         };
//         RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.validate(validNode, throwing));
//         assertEquals("boom", ex.getMessage());
//     }

    // -----------------------------------------------------------------
//     @Test
//     void validate_OutputFormatThrowsError(@Mock OutputFormat<String> format) throws Exception {
//         JsonNode validNode = MAPPER.readTree("{\"age\":30}");
//         OutputFormat<String> throwing = new OutputFormat<>() {
// 
//             @Override
//             public String format(JsonSchema schema, Set<ValidationMessage> messages, ExecutionContext executionContext, ValidationContext validationContext) {
//                 throw new AssertionError("assertion failed");
//             }
//         };
//         AssertionError err = assertThrows(AssertionError.class, () -> schema.validate(validNode, throwing));
//         assertEquals("assertion failed", err.getMessage());
//     }

    // -----------------------------------------------------------------
//     @Test
//     void validate_OutputFormatReturnsNull(@Mock OutputFormat<String> format) throws Exception {
//         JsonNode validNode = MAPPER.readTree("{\"age\":30}");
//         RecordingOutputFormat<String> format = new RecordingOutputFormat<>(null);
//         String result = schema.validate(validNode, format);
//         assertNull(result, "When OutputFormat.apply returns null the method should propagate null");
//     }

//     @Test
//     void validate_OutputFormatReturnsCustomObject(@Mock OutputFormat<CustomResult> format) throws Exception {
//         JsonNode validNode = MAPPER.readTree("{\"age\":30}");
//         CustomResult expected = new CustomResult("dto");
//         RecordingOutputFormat<CustomResult> format = new RecordingOutputFormat<>(expected);
//         CustomResult actual = schema.validate(validNode, format);
//         assertSame(expected, actual, "The custom object returned by the format must be propagated unchanged");
//     }

    // -----------------------------------------------------------------
//     @Test
//     void validate_MissingNode_TreatedAsValid(@Mock OutputFormat<String> format) throws Exception {
//         JsonNode missingNode = JsonNodeFactory.instance.missingNode();
//         RecordingOutputFormat<String> format = new RecordingOutputFormat<>("ok");
//         String result = emptySchema.validate(missingNode, format);
//         assertEquals("ok", result);
//         assertNotNull(format.getCapturedMessages());
//         assertTrue(format.getCapturedMessages().isEmpty(), "Empty schema should treat MissingNode as valid, producing an empty message set");
//     }

    // -----------------------------------------------------------------
//     @Test
//     void validate_JsonNullAgainstObjectSchema_ProducesMessage(@Mock OutputFormat<String> format) throws Exception {
//         JsonNode jsonNull = JsonNodeFactory.instance.nullNode();
//         RecordingOutputFormat<String> format = new RecordingOutputFormat<>("failure");
//         String result = objectSchema.validate(jsonNull, format);
//         assertEquals("failure", result);
//         assertNotNull(format.getCapturedMessages());
//         assertFalse(format.getCapturedMessages().isEmpty(), "Validating a JSON null against an object‑type schema must produce a validation message");
//     }

    // -----------------------------------------------------------------
//     @Test
//     void validate_RefSchemaInvalidInstance_ProducesMessage(@Mock OutputFormat<String> format) throws Exception {
//         JsonNode invalid = MAPPER.readTree("{\"person\":{}}");
//         RecordingOutputFormat<String> format = new RecordingOutputFormat<>("ref‑failure");
//         String result = refSchema.validate(invalid, format);
//         assertEquals("ref‑failure", result);
//         assertNotNull(format.getCapturedMessages());
//         assertFalse(format.getCapturedMessages().isEmpty(), "Missing required property inside $ref should generate a validation message");
//     }

    // -----------------------------------------------------------------
//     @Test
//     void validate_RecursiveAnchorValidInstance(@Mock OutputFormat<String> format) throws Exception {
//         JsonNode valid = MAPPER.readTree("{\"self\":{}}");
//         RecordingOutputFormat<String> format = new RecordingOutputFormat<>("recursive‑ok");
//         String result = recursiveSchema.validate(valid, format);
//         assertEquals("recursive‑ok", result);
//         assertNotNull(format.getCapturedMessages());
//         assertTrue(format.getCapturedMessages().isEmpty(), "A valid instance for the recursive schema must result in an empty message set");
//     }

    // -----------------------------------------------------------------
//     @Test
//     void validate_WithExecutionContextCustomizer_CustomizerIsInvoked(@Mock OutputFormat<String> format) throws Exception {
//         JsonNode validNode = MAPPER.readTree("{\"age\":30}");
//         RecordingOutputFormat<String> format = new RecordingOutputFormat<>("customizer‑ok");
//         final boolean[] customizerCalled = { false };
//         ExecutionContextCustomizer customizer = executionContext -> customizerCalled[0] = true;
//         String result = schema.validate(validNode, format, customizer);
//         assertEquals("customizer‑ok", result);
//         assertTrue(customizerCalled[0], "The ExecutionContextCustomizer must have been executed");
//         assertNotNull(format.getCapturedMessages());
//         assertTrue(format.getCapturedMessages().isEmpty(), "When the instance is valid the message set must be empty");
//     }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

18:37:43.277 [main] WARN com.networknt.schema.UnknownKeywordFactory -- Unknown keyword $recursiveAnchor - you should define your own Meta Schema. If the keyword is irrelevant for validation, just use a NonValidationKeyword or if it should generate annotations AnnotationKeyword
18:37:43.278 [main] WARN com.networknt.schema.UnknownKeywordFactory -- Unknown keyword $recursiveRef - you should define your own Meta Schema. If the keyword is irrelevant for validation, just use a NonValidationKeyword or if it should generate annotations AnnotationKeyword
╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema_validate_5_Test ✔
│     └─ validate_NullFormat_ThrowsIllegalArgumentException() ✘ When format is null an IllegalArgumentException is expected ==> Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <java.lang.NullPointerException>
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (1):
  JUnit Jupiter:JsonSchema_validate_5_Test:validate_NullFormat_ThrowsIllegalArgumentException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_5_Test', methodName = 'validate_NullFormat_ThrowsIllegalArgumentException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: When format is null an IllegalArgumentException is expected ==> Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <java.lang.NullPointerException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       com.networknt.schema.JsonSchema_validate_5_Test.validate_NullFormat_ThrowsIllegalArgumentException(JsonSchema_validate_5_Test.java:173)
       [...]
     Caused by: java.lang.NullPointerException: Cannot invoke "com.networknt.schema.OutputFormat.customize(com.networknt.schema.ExecutionContext, com.networknt.schema.ValidationContext)" because "format" is null
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:925)
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:754)
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:734)
       com.networknt.schema.JsonSchema_validate_5_Test.lambda$validate_NullFormat_ThrowsIllegalArgumentException$0(JsonSchema_validate_5_Test.java:173)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:53)
       [...]

Test run finished after 380 ms
[         4 containers found      ]
[         0 containers skipped    ]
[         4 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         0 containers failed     ]
[         1 tests found           ]
[         0 tests skipped         ]
[         1 tests started         ]
[         0 tests aborted         ]
[         0 tests successful      ]
[         1 tests failed          ]


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
 

api document of method validate(JsonNode, OutputFormat<T>): 

     * Validates the given root JsonNode, starting at the root of the data path. The
     * output will be formatted using the formatter specified.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     * 
     * @param <T>      the result type
     * @param rootNode the root node
     * @param format   the formatter
     * @return the result
     

return type: 
T

dependent classes: 
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `public Set<ValidationMessage> validate(String, InputFormat, ExecutionContextCustomizer)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, OutputFormat<T>)`, api document: 
     * Validate the given input string using the input format, starting at the root
     * of the data path.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     *
     * @param input               the input
     * @param inputFormat         the inputFormat
     * @param executionCustomizer the execution customizer
     * @return the assertions
     ;
		method `public Set<ValidationMessage> validate(JsonNode, ExecutionContextCustomizer)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, OutputFormat<T>)`, api document: 
     * Validate the given root JsonNode, starting at the root of the data path.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     *
     * @param rootNode            the root node
     * @param executionCustomizer the execution customizer
     * @return the assertions
     ;
class `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:
	related functions:
		method `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, OutputFormat<T>)`, api document: 
     * Returns the singleton instance of this validator.
     * @return the singleton instance of this validator
     ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
