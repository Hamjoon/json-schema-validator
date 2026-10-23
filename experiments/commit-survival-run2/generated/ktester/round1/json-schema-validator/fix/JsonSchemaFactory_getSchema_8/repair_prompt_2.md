
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
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.*;
import java.net.URI;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;
import com.networknt.schema.urn.URNFactory;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_8_Test {

    // Mocks for constructor dependencies
    @Mock
    ObjectMapper jsonMapper;

    @Mock
    YAMLMapper yamlMapper;

    @Mock
    URISchemeFactory uriFactory;

    @Mock
    URISchemeFetcher uriFetcher;

    @Mock
    URNFactory urnFactory;

    @Mock
    URITranslator.CompositeURITranslator uriTranslators;

    // Additional objects needed for construction
    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema";

    private Map<String, JsonMetaSchema> jsonMetaSchemas;

    private Map<String, String> uriMap;

    private JsonSchemaFactory factorySpy;

    /**
     * The factory under test – we use the public default instance.
     */
    private JsonSchemaFactory factory;

    @BeforeEach
    void setupBeforeEach() {
        // Use the public factory instance – it is fully initialised and does not need
        // any Mockito stubbing.
        factory = JsonSchemaFactory.getInstance();
    }

    /* -----------------------------------------------------------------------
     * Helper to turn a JSON string into a JsonNode.  Using a real ObjectMapper
     * keeps the test independent of the mocked jsonMapper that is only needed
     * for the factory construction.
     * ----------------------------------------------------------------------- */
    private static JsonNode parseJson(String json) {
        try {
            return new ObjectMapper().readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Invalid test JSON", e);
        }
    }

    /* -----------------------------------------------------------------------
     * SUCCESSFUL CASES – the factory should return a non‑null JsonSchema.
     * ----------------------------------------------------------------------- */
    static Stream<Arguments> successCases() {
        return Stream.of(Arguments.of("http JSON schema", "http://example.com/schema.json", "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }"), Arguments.of("file URI with YAML‑compatible JSON node", "file:/tmp/local-schema.yaml", "{ \"type\": \"object\", \"properties\": { \"age\": { \"type\": \"integer\" } } }"), Arguments.of("empty JSON object", "http://example.com/empty-schema.json", "{}"));
    }

    /* -----------------------------------------------------------------------
     * EXCEPTION CASES – the factory should propagate the exception thrown by
     * newJsonSchema (or by URI construction when the literal is malformed).
     * ----------------------------------------------------------------------- */
    static Stream<Arguments> exceptionCases() {
        return Stream.of(// Null arguments
        Arguments.of("null URI", null, "{ \"type\": \"object\" }", NullPointerException.class), Arguments.of("null JsonNode", "http://example.com/any-schema.json", null, NullPointerException.class), Arguments.of("both null", null, null, NullPointerException.class), // Unsupported / malformed URIs
        Arguments.of("unsupported scheme (ftp)", "ftp://example.com/schema.json", "{ \"type\": \"object\" }", IllegalArgumentException.class), Arguments.of("relative URI", "schemas/schema.json", "{ \"type\": \"object\" }", IllegalArgumentException.class), Arguments.of("data URI scheme", "data:application/json,{\"type\":\"object\"}", "{ \"type\": \"object\" }", IllegalArgumentException.class), // Invalid JSON node shapes
        Arguments.of("JSON array node", "http://example.com/schema.json", "[ { \"type\": \"object\" } ]", IllegalArgumentException.class), Arguments.of("primitive JSON node", "http://example.com/schema.json", "\"string\"", IllegalArgumentException.class), // Invalid schema content (unknown type)
        Arguments.of("unknown type keyword", "http://example.com/schema.json", "{ \"type\": \"unknown\" }", IllegalArgumentException.class));
    }

    @Test
    void getSchema_withUriAndJsonNode_returnsJsonSchema() throws Exception {
        // GIVEN
        URI schemaUri = new URI("http://example.com/my-schema");
        JsonNode schemaNode = parseJson("{ \"type\": \"object\" }");
        // WHEN
        JsonSchema actual = factory.getSchema(schemaUri, schemaNode);
        // THEN
        assertNotNull(actual, "The returned JsonSchema should not be null");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successCases")
    void getSchema_successful(String caseName, String uriString, String json) throws Exception {
        // GIVEN
        URI schemaUri = new URI(uriString);
        JsonNode schemaNode = parseJson(json);
        // WHEN
        JsonSchema result = factory.getSchema(schemaUri, schemaNode);
        // THEN
        assertNotNull(result, "Factory should return a non‑null JsonSchema for case: " + caseName);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionCases")
    void getSchema_exceptions(String caseName, String uriString, String json, Class<? extends Throwable> expectedException) throws Exception {
        // -------------------------------------------------
        // Build parameters – handling nulls and malformed URIs
        // -------------------------------------------------
        URI schemaUri = null;
        JsonNode schemaNode = null;
        if (uriString != null) {
            try {
                schemaUri = new URI(uriString);
            } catch (Exception e) {
                // If the URI literal itself is malformed we expect the test
                // data to map to IllegalArgumentException.
                assertTrue(IllegalArgumentException.class.isAssignableFrom(expectedException), "Test data inconsistency: malformed URI should map to IllegalArgumentException");
                assertThrows(expectedException, () -> factory.getSchema(new URI(uriString), parseJson("{\"type\":\"object\"}")));
                return;
            }
        }
        if (json != null) {
            schemaNode = parseJson(json);
        }
        // -------------------------------------------------
        // Exercise the method under test and verify the exception.
        // -------------------------------------------------
        assertThrows(expectedException, () -> factory.getSchema(schemaUri, schemaNode), () -> "Expected " + expectedException.getSimpleName() + " for case: " + caseName);
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_8_Test.java:14: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_8_Test.java:29: error: cannot find symbol
    URISchemeFactory uriFactory;
    ^
  symbol:   class URISchemeFactory
  location: class JsonSchemaFactory_getSchema_8_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_8_Test.java:32: error: cannot find symbol
    URISchemeFetcher uriFetcher;
    ^
  symbol:   class URISchemeFetcher
  location: class JsonSchemaFactory_getSchema_8_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_8_Test.java:38: error: package URITranslator does not exist
    URITranslator.CompositeURITranslator uriTranslators;
                 ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_8_Test.java:15: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_8_Test.java:18: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_8_Test.java:22: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_8_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_8_Test.java:25: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_8_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_8_Test.java:28: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_8_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_8_Test.java:31: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_8_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_8_Test.java:34: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_8_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_8_Test.java:37: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_8_Test
Note: src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_8_Test.java uses or overrides a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
12 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchemaFactory`:\n\trelated functions:\n\t\tmethod `public JsonSchema getSchema(URI, JsonNode, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, JsonNode)`;\n\t\tmethod `public JsonSchema getSchema(String, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, JsonNode)`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, JsonNode)`, api document: \n     * Returns the singleton instance of this validator.\n     *\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


