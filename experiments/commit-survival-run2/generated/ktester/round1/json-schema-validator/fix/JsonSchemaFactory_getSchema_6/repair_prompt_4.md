


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class JsonSchemaFactory_getSchema_6_Test {

    /**
     * Factory used for all test cases.
     */
    private static JsonSchemaFactory factory;

    /**
     * Holds the data for each successful loading scenario.
     */
    private static class SuccessCase {

        // JSON text that represents the schema
        final String json;

        // Config passed to the method (may be null)
        final SchemaValidatorsConfig config;

        SuccessCase(String json, SchemaValidatorsConfig config) {
            this.json = json;
            this.config = config;
        }

        @Override
        public String toString() {
            return "SuccessCase{json=" + json + ", config=" + (config == null ? "null" : "non‑null") + '}';
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Use a concrete, publicly‑available factory instance.
        // The V201909 flag is one of the supported spec versions.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        assertNotNull(factory, "Factory should be created successfully");
    }

    /**
     * Supplies the successful test cases defined in the JSON test matrix.
     */
    private static Stream<SuccessCase> successfulSchemaProvider() {
        return Stream.of(// case 1 – object schema with a non‑null config
        new SuccessCase("{\"type\":\"object\"}", new SchemaValidatorsConfig()), // case 2 – array schema with a null config
        new SuccessCase("{\"type\":\"array\",\"items\":{\"type\":\"string\"}}", null));
    }

    /**
     * Verify that a valid JSON schema supplied as an {@link InputStream}
     * results in a non‑null {@link JsonSchema} instance when the configuration
     * argument is {@code null}.
     */
    @Test
    void shouldReturnJsonSchemaWhenInputStreamIsValidAndConfigIsNull() {
        // GIVEN
        String json = "{\"type\":\"object\"}";
        InputStream schemaStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        // WHEN
        JsonSchema result = factory.getSchema(schemaStream, null);
        // THEN
        assertNotNull(result, "The returned JsonSchema should not be null");
    }

    /**
     * Verify that a valid JSON schema supplied as an {@link InputStream}
     * results in a non‑null {@link JsonSchema} instance when a non‑null
     * {@link SchemaValidatorsConfig} is supplied.
     */
    @Test
    void shouldReturnJsonSchemaWhenInputStreamIsValidAndConfigIsProvided() {
        // GIVEN
        String json = "{\"type\":\"object\"}";
        InputStream schemaStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // WHEN
        JsonSchema result = factory.getSchema(schemaStream, config);
        // THEN
        assertNotNull(result, "The returned JsonSchema should not be null");
    }

    /**
     * Verify that passing a {@code null} {@link InputStream} results in a
     * {@link JsonSchemaException} (wrapped around the underlying {@link IOException}).
     */
    @Test
    void shouldThrowJsonSchemaExceptionWhenInputStreamIsNull() {
        // GIVEN
        InputStream nullStream = null;
        // config value does not matter
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // WHEN / THEN
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> factory.getSchema(nullStream, config), "Expected JsonSchemaException when InputStream is null");
        // Ensure the cause is a NullPointerException (or at least non‑null)
        assertNotNull(ex.getCause(), "The cause of JsonSchemaException should be non‑null");
        assertTrue(ex.getCause() instanceof NullPointerException, "Expected cause to be NullPointerException but was " + ex.getCause().getClass().getSimpleName());
    }

    /**
     * Verify that a valid JSON schema supplied as an {@link InputStream}
     * results in a non‑null {@link JsonSchema} instance, irrespective of whether
     * the {@link SchemaValidatorsConfig} argument is {@code null} or not.
     *
     * The test also checks that the parsed JSON structure matches the source
     * string by using reflection to read the private {@code schemaNode}
     * field of {@link JsonSchema}.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulSchemaProvider")
    void shouldReturnJsonSchemaWhenInputStreamIsValid(SuccessCase tc) throws Exception {
        // GIVEN
        InputStream schemaStream = new ByteArrayInputStream(tc.json.getBytes(StandardCharsets.UTF_8));
        // WHEN
        JsonSchema result = factory.getSchema(schemaStream, tc.config);
        // THEN
        assertNotNull(result, "The returned JsonSchema should not be null");
        // Retrieve the internal JsonNode via the public accessor.
        JsonNode node = result.getSchemaNode();
        assertNotNull(node, "The internal JsonNode should not be null");
        // Compare the canonical (whitespace‑agnostic) representation of the
        // parsed node with the original JSON.
        ObjectMapper mapper = new ObjectMapper();
        String expectedCanonical = mapper.readTree(tc.json).toString();
        String actualCanonical = node.toString();
        assertEquals(expectedCanonical, actualCanonical, "The parsed JsonNode should be semantically equal to the input JSON");
    }

    /**
     * Verify that passing a {@code null} {@link InputStream} results in an
     * {@link IllegalArgumentException}.  The library throws this exception
     * directly rather than wrapping it in a {@link JsonSchemaException}.
     */
    @Test
    void shouldThrowIllegalArgumentExceptionWhenInputStreamIsNull() {
        // GIVEN
        InputStream nullStream = null;
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> factory.getSchema(nullStream, config), "Expected IllegalArgumentException when InputStream is null");
        // Ensure the message mentions the argument name – this is optional but useful.
        assertTrue(ex.getMessage().contains("in"), "Exception message should indicate the null argument");
    }
}

```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ JsonSchemaFactory_getSchema_6_Test ✔
│     ├─ shouldThrowIllegalArgumentExceptionWhenInputStreamIsNull() ✔
│     ├─ shouldThrowJsonSchemaExceptionWhenInputStreamIsNull() ✘ Expected JsonSchemaException when InputStream is null ==> Unexpected exception type thrown, expected: <com.networknt.schema.JsonSchemaException> but was: <java.lang.IllegalArgumentException>
│     ├─ shouldReturnJsonSchemaWhenInputStreamIsValidAndConfigIsNull() ✔
│     ├─ shouldReturnJsonSchemaWhenInputStreamIsValid(SuccessCase) ✔
│     │  ├─ [1] SuccessCase{json={"type":"object"}, config=non‑null} ✔
│     │  └─ [2] SuccessCase{json={"type":"array","items":{"type":"string"}}, config=null} ✔
│     └─ shouldReturnJsonSchemaWhenInputStreamIsValidAndConfigIsProvided() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (1):
  JUnit Jupiter:JsonSchemaFactory_getSchema_6_Test:shouldThrowJsonSchemaExceptionWhenInputStreamIsNull()
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_6_Test', methodName = 'shouldThrowJsonSchemaExceptionWhenInputStreamIsNull', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Expected JsonSchemaException when InputStream is null ==> Unexpected exception type thrown, expected: <com.networknt.schema.JsonSchemaException> but was: <java.lang.IllegalArgumentException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3099)
       com.networknt.schema.JsonSchemaFactory_getSchema_6_Test.shouldThrowJsonSchemaExceptionWhenInputStreamIsNull(JsonSchemaFactory_getSchema_6_Test.java:104)
       [...]
     Caused by: java.lang.IllegalArgumentException: argument "in" is null
       com.fasterxml.jackson.databind.ObjectMapper._assertNotNull(ObjectMapper.java:4980)
       com.fasterxml.jackson.databind.ObjectMapper.readTree(ObjectMapper.java:3198)
       com.networknt.schema.JsonSchemaFactory.getSchema(JsonSchemaFactory.java:391)
       com.networknt.schema.JsonSchemaFactory_getSchema_6_Test.lambda$shouldThrowJsonSchemaExceptionWhenInputStreamIsNull$0(JsonSchemaFactory_getSchema_6_Test.java:104)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:53)
       [...]

Test run finished after 193 ms
[         5 containers found      ]
[         0 containers skipped    ]
[         5 containers started    ]
[         0 containers aborted    ]
[         5 containers successful ]
[         0 containers failed     ]
[         6 tests found           ]
[         0 tests skipped         ]
[         6 tests started         ]
[         0 tests aborted         ]
[         5 tests successful      ]
[         1 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

return type: 
com.networknt.schema.JsonSchema

dependent classes: 
class `com.networknt.schema.JsonSchemaFactory`:
	dependent fields:
		com.fasterxml.jackson.databind.ObjectMapper com.networknt.schema.JsonSchemaFactory.jsonMapper;
		org.slf4j.Logger com.networknt.schema.JsonSchemaFactory.logger;
	related functions:
		method `private boolean idMatchesSourceUri(JsonMetaSchema, JsonNode, URI)` returns `boolean`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(InputStream, SchemaValidatorsConfig)`;
		method `public JsonSchema getSchema(URI, JsonNode, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(InputStream, SchemaValidatorsConfig)`;
		method `public static Builder builder(JsonSchemaFactory)` returns `com.networknt.schema.JsonSchemaFactory.Builder`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(InputStream, SchemaValidatorsConfig)`;
		method `public JsonSchema getSchema(URI, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(InputStream, SchemaValidatorsConfig)`;
		method `public JsonSchema getSchema(String, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(InputStream, SchemaValidatorsConfig)`;
class `com.networknt.schema.JsonMetaSchema`:
	related functions:
		method `public JsonValidator newValidator(ValidationContext, String, String, JsonNode, JsonSchema, String)` returns `com.networknt.schema.JsonValidator`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(InputStream, SchemaValidatorsConfig)`;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
