


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;

class JsonSchema_validate_2_Test {

    /**
     * The schema under test – created from a minimal (empty) JSON schema.
     */
    private JsonSchema jsonSchema;

    /**
     * Jackson mapper used to build JSON payloads for the tests.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setupBeforeEach() {
        /*
         * Build a JsonSchema instance using the public factory method.
         * An empty schema (`{}`) imposes no constraints, therefore any
         * JSON document should validate successfully and return an empty
         * set of ValidationMessage objects.
         */
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        jsonSchema = factory.getSchema("{}");
        assertNotNull(jsonSchema, "JsonSchema instance must be created");
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up – the JsonSchema instance is stateless and thread‑safe.
        jsonSchema = null;
    }

    /**
     * Parses a JSON string into a {@link JsonNode}.
     */
    private static JsonNode node(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON supplied to test", e);
        }
    }

    /**
     * Creates a {@link JsonSchema} from the supplied schema JSON.
     */
    private static JsonSchema schema(String schemaJson) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        return factory.getSchema(schemaJson);
    }

    /**
     * Returns the textual representation of the first validation message, or null if none.
     */
    private static String firstMessage(Set<ValidationMessage> messages) {
        return messages.isEmpty() ? null : messages.iterator().next().getMessage();
    }

    /**
     * Verify that {@code JsonSchema.validate(JsonNode)} returns an empty set
     * when the schema does not define any constraints.
     */
    @Test
    void shouldReturnEmptyValidationMessagesForValidJson() throws IOException {
        // given: a simple JSON document
        JsonNode validNode = MAPPER.readTree("{\"name\":\"John\",\"age\":30}");
        // when: the document is validated against the empty schema
        Set<ValidationMessage> messages = jsonSchema.validate(validNode);
        // then: no validation messages are produced
        assertNotNull(messages, "Resulting set must not be null");
        assertTrue(messages.isEmpty(), "Expected no validation messages for a valid document");
    }

    /**
     * Verify that passing {@code null} as the root node results in a
     * {@link NullPointerException}. The actual implementation of
     * {@code JsonSchema.validate(JsonNode)} does not guard against null,
     * so the JDK will throw NPE when the node is dereferenced.
     */
    @Test
    void shouldThrowWhenRootNodeIsNull() {
        // given: a null JsonNode
        JsonNode nullNode = null;
        // when / then: NPE is thrown
        assertThrows(NullPointerException.class, () -> jsonSchema.validate(nullNode));
    }

    /**
     * Verify that passing {@code null} as the root node yields an empty validation
     * result. The {@link JsonSchema#validate(JsonNode)} implementation does not
     * throw a {@link NullPointerException}; it simply treats a {@code null}
     * document as valid when the schema imposes no constraints.
     */
    @Test
    void shouldReturnEmptyValidationMessagesWhenRootNodeIsNull() {
        // given: a null JsonNode
        JsonNode nullNode = null;
        // when: the document is validated
        Set<ValidationMessage> messages = jsonSchema.validate(nullNode);
        // then: the result is an empty set (no errors)
        assertNotNull(messages, "Resulting set must not be null");
        assertTrue(messages.isEmpty(), "Expected no validation messages for a null document");
    }
}

```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema_validate_2_Test ✔
│     ├─ shouldReturnEmptyValidationMessagesForValidJson() ✔
│     ├─ shouldReturnEmptyValidationMessagesWhenRootNodeIsNull() ✔
│     └─ shouldThrowWhenRootNodeIsNull() ✘ Expected java.lang.NullPointerException to be thrown, but nothing was thrown.
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (1):
  JUnit Jupiter:JsonSchema_validate_2_Test:shouldThrowWhenRootNodeIsNull()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_2_Test', methodName = 'shouldThrowWhenRootNodeIsNull', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Expected java.lang.NullPointerException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3083)
       com.networknt.schema.JsonSchema_validate_2_Test.shouldThrowWhenRootNodeIsNull(JsonSchema_validate_2_Test.java:93)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       [...]

Test run finished after 170 ms
[         4 containers found      ]
[         0 containers skipped    ]
[         4 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         0 containers failed     ]
[         3 tests found           ]
[         0 tests skipped         ]
[         3 tests started         ]
[         0 tests aborted         ]
[         2 tests successful      ]
[         1 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

api document of class com.networknt.schema.JsonSchema: 

 * This is the core of json constraint implementation. It parses json constraint
 * file and generates JsonValidators. The class is thread safe, once it is
 * constructed, it can be used to validate multiple json data concurrently.
 

api document of method validate(JsonNode): 

     * Validate the given root JsonNode, starting at the root of the data path.
     * @param rootNode JsonNode
     *
     * @return A list of ValidationMessage if there is any validation error, or an empty
     * list if there is no error.
     

dependent classes: 
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `public ValidationResult validateAndCollect(JsonNode)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.validate(JsonNode)`;
		method `public Set<ValidationMessage> validate(JsonNode)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(JsonNode)`, api document: 
     * Validate the given root JsonNode, starting at the root of the data path.
     * @param rootNode JsonNode
     *
     * @return A list of ValidationMessage if there is any validation error, or an empty
     * list if there is no error.
     ;
		method `public ValidationResult walk(JsonNode, boolean)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.validate(JsonNode)`;
class `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:
	related functions:
		method `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.validate(JsonNode)`, api document: 
     * Returns the singleton instance of this validator.
     *
     * @return the singleton instance of this validator
     ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
