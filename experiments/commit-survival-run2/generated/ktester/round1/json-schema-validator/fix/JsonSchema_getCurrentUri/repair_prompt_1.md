
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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.urn.URNFactory;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getCurrentUri_Test {

    // -------------------------------------------------------------------------
    @Mock
    static URIFactory mockUriFactory;

    @Mock
    static URNFactory mockUrnFactory;

    @Mock
    static JsonMetaSchema mockMetaSchema;

    @Mock
    static JsonSchemaFactory mockJsonSchemaFactory;

    @Mock
    static SchemaValidatorsConfig mockConfig;

    private static ValidationContext validationContext;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // The ValidationContext constructor validates that none of the arguments are null.
        // Supplying the mocked collaborators is sufficient for our tests.
        validationContext = new ValidationContext(mockUriFactory, mockUrnFactory, mockMetaSchema, mockJsonSchemaFactory, mockConfig);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset mock interactions before each test to keep them independent.
        reset(mockUriFactory, mockUrnFactory, mockMetaSchema, mockJsonSchemaFactory, mockConfig);
    }

    private static Stream<ResolutionCase> resolutionCases() {
        return Stream.of(new ResolutionCase("relative schemaPath resolves against base URI", "http://example.com/base/", "sub/schema.json", "http://example.com/base/sub/schema.json"), new ResolutionCase("absolute schemaPath replaces the base URI", "http://example.com/base/", "http://example.com/absolute/schema.json", "http://example.com/absolute/schema.json"), new ResolutionCase("schemaPath with fragment is preserved", "http://example.com/base/", "schema.json#fragment", "http://example.com/base/schema.json#fragment"), new ResolutionCase("schemaPath with query string is preserved", "http://example.com/base/", "schema.json?query=1", "http://example.com/base/schema.json?query=1"), new ResolutionCase("empty schemaPath leaves base URI unchanged", "http://example.com/base/", "", "http://example.com/base/"));
    }

    private static Stream<EncodingCase> encodingCases() {
        return Stream.of(new EncodingCase("space in path must be percent‑encoded", "http://example.com/base/", "space in name.json", "http://example.com/base/space%20in%20name.json"), new EncodingCase("Unicode check‑mark must be percent‑encoded", "http://example.com/base/", "✓.json", "http://example.com/base/%E2%9C%93.json"), new EncodingCase("leading hash is treated as fragment", "http://example.com/base/", "#hash.json", "http://example.com/base/#hash.json"));
    }

    @Test
    void getCurrentUri_withoutId_returnsProvidedUri() throws Exception {
        // GIVEN
        URI expectedUri = new URI("http://example.com/schema.json");
        // no "$id" property
        JsonNode emptySchemaNode = MAPPER.readTree("{}");
        // WHEN
        JsonSchema schema = JsonSchema.from(validationContext, // schemaPath – not relevant for this test
        "#", expectedUri, emptySchemaNode, // no parent schema
        null, // suppressSubSchemaRetrieval
        false);
        // THEN
        assertNotNull(schema, "JsonSchema instance should be created");
        assertEquals(expectedUri, schema.getCurrentUri(), "When the schema node does not contain an $id, getCurrentUri() must return the URI supplied to the constructor");
    }

    @Test
    void getCurrentUri_withId_combinesBaseAndId() throws Exception {
        // GIVEN
        URI baseUri = new URI("http://example.com/base/");
        // Schema node contains an $id that is relative to the base URI
        JsonNode schemaWithId = MAPPER.readTree("{\"$id\": \"sub/schema.json\"}");
        // WHEN
        JsonSchema schema = JsonSchema.from(validationContext, "#", baseUri, schemaWithId, null, false);
        // THEN
        assertNotNull(schema, "JsonSchema instance should be created");
        URI actual = schema.getCurrentUri();
        // how combineCurrentUriWithIds should behave
        URI expected = baseUri.resolve("sub/schema.json");
        assertEquals(expected, actual, "When the schema node contains an $id, getCurrentUri() must reflect the combined URI");
    }

    @Test
    void getCurrentUri_whenInternalFieldIsNull_returnsNull() throws Exception {
        // GIVEN – a normal schema instance
        URI uri = new URI("http://example.com/any.json");
        JsonSchema schema = JsonSchema.from(validationContext, "#", uri, MAPPER.readTree("{}"), null, false);
        // Use reflection to set the private field `currentUri` to null.
        Field field = JsonSchema.class.getDeclaredField("currentUri");
        field.setAccessible(true);
        field.set(schema, null);
        // WHEN / THEN
        assertNull(schema.getCurrentUri(), "If the internal `currentUri` field is null, the getter must return null");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("resolutionCases")
    void getCurrentUri_uriResolutionBehavior(ResolutionCase rc) throws Exception {
        // GIVEN
        URI base = new URI(rc.baseUri());
        JsonNode emptyNode = MAPPER.readTree("{}");
        // WHEN
        JsonSchema schema = JsonSchema.from(validationContext, rc.schemaPath(), base, emptyNode, null, false);
        // THEN
        assertNotNull(schema, "JsonSchema instance should be created");
        assertEquals(new URI(rc.expected()), schema.getCurrentUri(), rc.description());
    }

    @Test
    void getCurrentUri_returnsSameInstance_whenSuppliedNonNullUri() throws Exception {
        // GIVEN
        URI supplied = new URI("http://example.com/identical.json");
        JsonSchema schema = JsonSchema.from(validationContext, "#", supplied, MAPPER.readTree("{}"), null, false);
        // WHEN
        URI result = schema.getCurrentUri();
        // THEN
        assertSame(supplied, result, "The getter must return the exact same URI object (reference equality)");
    }

    @Test
    void getCurrentUri_returnsBaseInstance_whenSchemaPathResolvesToSameUri() throws Exception {
        // GIVEN
        URI base = new URI("http://example.com/dir/");
        // schemaPath "." resolves to the base URI itself
        JsonSchema schema = JsonSchema.from(validationContext, ".", base, MAPPER.readTree("{}"), null, false);
        // WHEN
        URI result = schema.getCurrentUri();
        // THEN
        assertSame(base, result, "When the resolved URI is identical to the base, the original URI instance must be returned");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("encodingCases")
    void getCurrentUri_encodesSpecialCharactersCorrectly(EncodingCase ec) throws Exception {
        // GIVEN
        URI base = new URI(ec.baseUri());
        JsonSchema schema = JsonSchema.from(validationContext, ec.schemaPath(), base, MAPPER.readTree("{}"), null, false);
        // WHEN
        URI actual = schema.getCurrentUri();
        // THEN
        assertEquals(new URI(ec.expected()), actual, ec.description());
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_getCurrentUri_Test.java:7: error: package org.mockito does not exist
import org.mockito.Mock;
                  ^
src/test/java/com/networknt/schema/JsonSchema_getCurrentUri_Test.java:8: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_getCurrentUri_Test.java:11: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchema_getCurrentUri_Test.java:16: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_getCurrentUri_Test.java:20: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_getCurrentUri_Test.java:59: error: cannot find symbol
    private static Stream<ResolutionCase> resolutionCases() {
                          ^
  symbol:   class ResolutionCase
  location: class JsonSchema_getCurrentUri_Test
src/test/java/com/networknt/schema/JsonSchema_getCurrentUri_Test.java:63: error: cannot find symbol
    private static Stream<EncodingCase> encodingCases() {
                          ^
  symbol:   class EncodingCase
  location: class JsonSchema_getCurrentUri_Test
src/test/java/com/networknt/schema/JsonSchema_getCurrentUri_Test.java:114: error: cannot find symbol
    void getCurrentUri_uriResolutionBehavior(ResolutionCase rc) throws Exception {
                                             ^
  symbol:   class ResolutionCase
  location: class JsonSchema_getCurrentUri_Test
src/test/java/com/networknt/schema/JsonSchema_getCurrentUri_Test.java:150: error: cannot find symbol
    void getCurrentUri_encodesSpecialCharactersCorrectly(EncodingCase ec) throws Exception {
                                                         ^
  symbol:   class EncodingCase
  location: class JsonSchema_getCurrentUri_Test
src/test/java/com/networknt/schema/JsonSchema_getCurrentUri_Test.java:17: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_getCurrentUri_Test.java:21: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_getCurrentUri_Test.java:23: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_getCurrentUri_Test.java:27: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_getCurrentUri_Test
src/test/java/com/networknt/schema/JsonSchema_getCurrentUri_Test.java:30: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_getCurrentUri_Test
src/test/java/com/networknt/schema/JsonSchema_getCurrentUri_Test.java:33: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_getCurrentUri_Test
src/test/java/com/networknt/schema/JsonSchema_getCurrentUri_Test.java:36: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_getCurrentUri_Test
src/test/java/com/networknt/schema/JsonSchema_getCurrentUri_Test.java:39: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_getCurrentUri_Test
src/test/java/com/networknt/schema/JsonSchema_getCurrentUri_Test.java:56: error: cannot find symbol
        reset(mockUriFactory, mockUrnFactory, mockMetaSchema, mockJsonSchemaFactory, mockConfig);
        ^
  symbol:   method reset(URIFactory,URNFactory,JsonMetaSchema,JsonSchemaFactory,SchemaValidatorsConfig)
  location: class JsonSchema_getCurrentUri_Test
src/test/java/com/networknt/schema/JsonSchema_getCurrentUri_Test.java:60: error: cannot find symbol
        return Stream.of(new ResolutionCase("relative schemaPath resolves against base URI", "http://example.com/base/", "sub/schema.json", "http://example.com/base/sub/schema.json"), new ResolutionCase("absolute schemaPath replaces the base URI", "http://example.com/base/", "http://example.com/absolute/schema.json", "http://example.com/absolute/schema.json"), new ResolutionCase("schemaPath with fragment is preserved", "http://example.com/base/", "schema.json#fragment", "http://example.com/base/schema.json#fragment"), new ResolutionCase("schemaPath with query string is preserved", "http://example.com/base/", "schema.json?query=1", "http://example.com/base/schema.json?query=1"), new ResolutionCase("empty schemaPath leaves base URI unchanged", "http://example.com/base/", "", "http://example.com/base/"));
                             ^
  symbol:   class ResolutionCase
  location: class JsonSchema_getCurrentUri_Test
src/test/java/com/networknt/schema/JsonSchema_getCurrentUri_Test.java:60: error: cannot find symbol
        return Stream.of(new ResolutionCase("relative schemaPath resolves against base URI", "http://example.com/base/", "sub/schema.json", "http://example.com/base/sub/schema.json"), new ResolutionCase("absolute schemaPath replaces the base URI", "http://example.com/base/", "http://example.com/absolute/schema.json", "http://example.com/absolute/schema.json"), new ResolutionCase("schemaPath with fragment is preserved", "http://example.com/base/", "schema.json#fragment", "http://example.com/base/schema.json#fragment"), new ResolutionCase("schemaPath with query string is preserved", "http://example.com/base/", "schema.json?query=1", "http://example.com/base/schema.json?query=1"), new ResolutionCase("empty schemaPath leaves base URI unchanged", "http://example.com/base/", "", "http://example.com/base/"));
                                                                                                                                                                                            ^
  symbol:   class ResolutionCase
  location: class JsonSchema_getCurrentUri_Test
src/test/java/com/networknt/schema/JsonSchema_getCurrentUri_Test.java:60: error: cannot find symbol
        return Stream.of(new ResolutionCase("relative schemaPath resolves against base URI", "http://example.com/base/", "sub/schema.json", "http://example.com/base/sub/schema.json"), new ResolutionCase("absolute schemaPath replaces the base URI", "http://example.com/base/", "http://example.com/absolute/schema.json", "http://example.com/absolute/schema.json"), new ResolutionCase("schemaPath with fragment is preserved", "http://example.com/base/", "schema.json#fragment", "http://example.com/base/schema.json#fragment"), new ResolutionCase("schemaPath with query string is preserved", "http://example.com/base/", "schema.json?query=1", "http://example.com/base/schema.json?query=1"), new ResolutionCase("empty schemaPath leaves base URI unchanged", "http://example.com/base/", "", "http://example.com/base/"));
                                                                                                                                                                                                                                                                                                                                                                               ^
  symbol:   class ResolutionCase
  location: class JsonSchema_getCurrentUri_Test
src/test/java/com/networknt/schema/JsonSchema_getCurrentUri_Test.java:60: error: cannot find symbol
        return Stream.of(new ResolutionCase("relative schemaPath resolves against base URI", "http://example.com/base/", "sub/schema.json", "http://example.com/base/sub/schema.json"), new ResolutionCase("absolute schemaPath replaces the base URI", "http://example.com/base/", "http://example.com/absolute/schema.json", "http://example.com/absolute/schema.json"), new ResolutionCase("schemaPath with fragment is preserved", "http://example.com/base/", "schema.json#fragment", "http://example.com/base/schema.json#fragment"), new ResolutionCase("schemaPath with query string is preserved", "http://example.com/base/", "schema.json?query=1", "http://example.com/base/schema.json?query=1"), new ResolutionCase("empty schemaPath leaves base URI unchanged", "http://example.com/base/", "", "http://example.com/base/"));
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                ^
  symbol:   class ResolutionCase
  location: class JsonSchema_getCurrentUri_Test
src/test/java/com/networknt/schema/JsonSchema_getCurrentUri_Test.java:60: error: cannot find symbol
        return Stream.of(new ResolutionCase("relative schemaPath resolves against base URI", "http://example.com/base/", "sub/schema.json", "http://example.com/base/sub/schema.json"), new ResolutionCase("absolute schemaPath replaces the base URI", "http://example.com/base/", "http://example.com/absolute/schema.json", "http://example.com/absolute/schema.json"), new ResolutionCase("schemaPath with fragment is preserved", "http://example.com/base/", "schema.json#fragment", "http://example.com/base/schema.json#fragment"), new ResolutionCase("schemaPath with query string is preserved", "http://example.com/base/", "schema.json?query=1", "http://example.com/base/schema.json?query=1"), new ResolutionCase("empty schemaPath leaves base URI unchanged", "http://example.com/base/", "", "http://example.com/base/"));
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   ^
  symbol:   class ResolutionCase
  location: class JsonSchema_getCurrentUri_Test
src/test/java/com/networknt/schema/JsonSchema_getCurrentUri_Test.java:64: error: cannot find symbol
        return Stream.of(new EncodingCase("space in path must be percent‑encoded", "http://example.com/base/", "space in name.json", "http://example.com/base/space%20in%20name.json"), new EncodingCase("Unicode check‑mark must be percent‑encoded", "http://example.com/base/", "✓.json", "http://example.com/base/%E2%9C%93.json"), new EncodingCase("leading hash is treated as fragment", "http://example.com/base/", "#hash.json", "http://example.com/base/#hash.json"));
                             ^
  symbol:   class EncodingCase
  location: class JsonSchema_getCurrentUri_Test
src/test/java/com/networknt/schema/JsonSchema_getCurrentUri_Test.java:64: error: cannot find symbol
        return Stream.of(new EncodingCase("space in path must be percent‑encoded", "http://example.com/base/", "space in name.json", "http://example.com/base/space%20in%20name.json"), new EncodingCase("Unicode check‑mark must be percent‑encoded", "http://example.com/base/", "✓.json", "http://example.com/base/%E2%9C%93.json"), new EncodingCase("leading hash is treated as fragment", "http://example.com/base/", "#hash.json", "http://example.com/base/#hash.json"));
                                                                                                                                                                                            ^
  symbol:   class EncodingCase
  location: class JsonSchema_getCurrentUri_Test
src/test/java/com/networknt/schema/JsonSchema_getCurrentUri_Test.java:64: error: cannot find symbol
        return Stream.of(new EncodingCase("space in path must be percent‑encoded", "http://example.com/base/", "space in name.json", "http://example.com/base/space%20in%20name.json"), new EncodingCase("Unicode check‑mark must be percent‑encoded", "http://example.com/base/", "✓.json", "http://example.com/base/%E2%9C%93.json"), new EncodingCase("leading hash is treated as fragment", "http://example.com/base/", "#hash.json", "http://example.com/base/#hash.json"));
                                                                                                                                                                                                                                                                                                                                            ^
  symbol:   class EncodingCase
  location: class JsonSchema_getCurrentUri_Test
26 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n ', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\tdependent fields:\n\t\tjava.net.URI com.networknt.schema.JsonSchema.currentUri;\n\trelated functions:\n\t\tmethod `private void updateThisAsSubschema(URI)` returns `void`, related with `com.networknt.schema.JsonSchema.getCurrentUri()`, api document: \n     * Creates a new parent schema from the current state and updates this object to refer to the subschema instead.\n     ;\n\t\tmethod `public URI getCurrentUri()` returns `java.net.URI`, related with `com.networknt.schema.JsonSchema.getCurrentUri()`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.getCurrentUri()`, api document: \n     * Returns the singleton instance of this validator.\n     *\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


