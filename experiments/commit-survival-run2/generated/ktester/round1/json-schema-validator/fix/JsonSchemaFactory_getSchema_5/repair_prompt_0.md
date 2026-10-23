
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
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.uri.URITranslator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import com.networknt.schema.suite.TestCase;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_5_Test {

    /**
     * A factory instance that can be reused for all tests.
     */
    private static JsonSchemaFactory factory;

    /**
     * Temporary file that holds a minimal JSON schema used by the tests.
     */
    private static Path tempSchemaFile;

    /**
     * Temporary JSON and YAML files that act as “real” schemas for the file‑ and classpath‑based tests.
     */
    private static Path tempJsonFile;

    private static Path tempYamlFile;

    /**
     * Mocked translator that replaces the default network/classpath loader.
     */
    @Mock
    private URITranslator mockedTranslator;

    /**
     * Spy wrapper around the real factory – needed to replace the internal translator.
     */
    @InjectMocks
    private JsonSchemaFactory factorySpy;

    @BeforeAll
    static void setupBeforeAll() throws IOException {
        // Create a very small JSON schema: {"type":"object"}
        String schemaContent = "{\"type\":\"object\"}";
        tempSchemaFile = Files.createTempFile("minimal-schema", ".json");
        Files.writeString(tempSchemaFile, schemaContent);
        // Obtain a factory instance using the public static factory method.
        // V4 is the most common version flag; adjust if needed.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        assertNotNull(factory, "Factory should have been created");
        // ---------- create a minimal JSON schema file ----------
        String json = "{\"type\":\"object\",\"definitions\":{\"address\":{\"type\":\"object\"}}}";
        tempJsonFile = Files.createTempFile("example-schema", ".json");
        Files.writeString(tempJsonFile, json);
        // ---------- create a minimal YAML schema file ----------
        String yaml = "type: object\nproperties:\n  name:\n    type: string";
        tempYamlFile = Files.createTempFile("example-schema", ".yaml");
        Files.writeString(tempYamlFile, yaml);
        // ---------- obtain a real factory instance ----------
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        assertNotNull(factory, "Factory must be created");
    }

    @BeforeEach
    void setUpEach() throws Exception {
        // Create a fresh spy for every test so that state (e.g. cache) does not leak.
        factorySpy = Mockito.spy(factory);
        // Replace the internal URITranslator with our mock.
        // The field name in JsonSchemaFactory is "uriTranslator" (checked against the source).
        Field translatorField = JsonSchemaFactory.class.getDeclaredField("uriTranslator");
        translatorField.setAccessible(true);
        translatorField.set(factorySpy, mockedTranslator);
    }

    @AfterAll
    static void teardownAfterAll() throws IOException {
        // Clean up the temporary file.
        if (tempSchemaFile != null) {
            Files.deleteIfExists(tempSchemaFile);
        }
        Files.deleteIfExists(tempJsonFile);
        Files.deleteIfExists(tempYamlFile);
    }

    /**
     * Generates a {@link Stream} of {@link TestCase}s matching the JSON specification.
     *
     * <p>Only the cases that can be exercised without external network access are
     * included; for the remaining cases we rely on the mocked {@link URITranslator}
     * to throw the appropriate exception.</p>
     */
    static Stream<TestCase> testCases() {
        return Stream.of(// ---------- Valid URI – file system ----------
        new TestCase("Valid file URI", toUri(tempJsonFile.toUri()), "Returns a non‑null JsonSchema instance", "File‑system URI that points to a readable JSON schema file."), // ---------- Valid URI – YAML ----------
        new TestCase("YAML schema loading via file", toUri(tempYamlFile.toUri()), "Returns a non‑null JsonSchema instance", "YAML file should be parsed correctly."), // ---------- Null URI ----------
        new TestCase("Null URI", null, "Throws IllegalArgumentException", "The method must guard against a null argument."), // ---------- Relative URI ----------
        new TestCase("Relative URI (no scheme)", URI.create("schema.json"), "Throws IllegalArgumentException", "Relative URIs must be rejected."), // ---------- Empty path with scheme ----------
        new TestCase("Empty path with scheme", URI.create("file:"), "Throws IllegalArgumentException", "URI with scheme but no path is invalid."), // ---------- Unsupported scheme ----------
        new TestCase("Unsupported scheme", URI.create("ftp://example.com/schema.json"), "Throws JsonSchemaException", "FTP scheme is not supported."), // ---------- Upper‑case HTTP scheme ----------
        new TestCase("Upper‑case scheme handling", URI.create("HTTP://example.com/remote-schema.json"), "Returns a non‑null JsonSchema instance", "Scheme handling must be case‑insensitive."), // ---------- Meta‑schema normalization ----------
        new TestCase("Meta‑schema URI normalization", URI.create("http://json-schema.org/draft-04/schema#"), "Returns built‑in Draft‑04 JsonSchema", "Known meta‑schema URI should be cached."), // ---------- Caching behaviour ----------
        new TestCase("Caching – same URI returns same instance", toUri(tempJsonFile.toUri()), "Cache hit – same instance", "Repeated calls for the same URI must return the cached schema."), // ---------- Unsupported URN ----------
        new TestCase("Unsupported URN scheme", URI.create("urn:example:schema"), "Throws JsonSchemaException", "URN scheme is not handled."));
    }

    private static URI toUri(URI uri) {
        // Helper to make test‑data creation more readable.
        return uri;
    }

    private static InputStream simpleJsonInputStream() {
        String json = "{\"type\":\"object\"}";
        return new java.io.ByteArrayInputStream(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Verifies that {@code JsonSchemaFactory#getSchema(URI)} correctly loads a
     * schema from a valid {@link URI} and returns a non‑null {@link JsonSchema}
     * whose root node matches the supplied JSON.
     */
    @Test
    void getSchema_withValidUri_returnsJsonSchema() {
        // GIVEN
        URI schemaUri = tempSchemaFile.toUri();
        // WHEN
        JsonSchema jsonSchema = factory.getSchema(schemaUri);
        // THEN
        assertNotNull(jsonSchema, "Returned JsonSchema should not be null");
        JsonNode rootNode = jsonSchema.getSchemaNode();
        assertNotNull(rootNode, "Root node of the schema should not be null");
        assertEquals("object", rootNode.get("type").asText(), "The 'type' property of the loaded schema should be 'object'");
    }

    /**
     * Verifies that passing {@code null} as the URI results in an
     * {@link IllegalArgumentException}. The actual implementation of the method
     * performs a null‑check, so the test ensures the contract is honoured.
     */
    @Test
    void getSchema_withNullUri_throwsIllegalArgumentException() {
        // GIVEN
        URI nullUri = null;
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(nullUri), "Calling getSchema(null) should throw IllegalArgumentException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("testCases")
    void getSchema_variousScenarios(TestCase tc) throws Exception {
        // -----------------------------------------------------------------
        // Arrange – set up mock behaviour according to the expected outcome
        // -----------------------------------------------------------------
        if (tc.uri() != null) {
            String scheme = tc.uri().getScheme().toLowerCase(Locale.ROOT);
            switch(scheme) {
                case "http", "https" ->
                    {
                        // For HTTP(S) we simulate a successful fetch by returning a simple JSON document.
                        when(mockedTranslator.translate(tc.uri())).thenReturn(simpleJsonInputStream());
                    }
                case "ftp", "urn" ->
                    {
                        // Unsupported schemes cause URITranslatorException which is wrapped by JsonSchemaException.
                        when(mockedTranslator.translate(tc.uri())).thenThrow(new URITranslatorException("Scheme not supported"));
                    }
                case "file" ->
                    {
                        // The default implementation for file:// is still used (real file access).
                        // No mocking required – let the factory read the temporary file.
                    }
                default ->
                    {
                        // Any other scheme – behave like unsupported.
                        when(mockedTranslator.translate(tc.uri())).thenThrow(new URITranslatorException("Unsupported scheme"));
                    }
            }
        }
        // -----------------------------------------------------------------
        // Act / Assert – choose verification based on the expected result
        // -----------------------------------------------------------------
        switch(tc.expected()) {
            case "Throws IllegalArgumentException" ->
                {
                    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> factorySpy.getSchema(tc.uri()), "Calling getSchema with illegal argument must throw");
                    assertTrue(ex.getMessage().toLowerCase().contains("schemauri"), "Exception message should mention 'schemaUri'");
                }
            case "Throws JsonSchemaException" ->
                {
                    // JsonSchemaException is a runtime exception in the library.
                    assertThrows(JsonSchemaException.class, () -> factorySpy.getSchema(tc.uri()), "Unsupported scheme must result in JsonSchemaException");
                }
            case "Returns a non‑null JsonSchema instance" ->
                {
                    JsonSchema schema = factorySpy.getSchema(tc.uri());
                    assertNotNull(schema, "Returned JsonSchema must not be null");
                    JsonNode root = schema.getSchemaNode();
                    assertNotNull(root, "Root node must be present");
                    // Minimal sanity check – the temporary JSON/YAML files contain a “type” field.
                    if (tc.description().contains("YAML")) {
                        assertEquals("object", root.get("type").asText(), "YAML schema must be parsed correctly");
                    } else {
                        assertTrue(root.has("type"), "JSON schema should contain a 'type' property");
                    }
                }
            case "Returns built‑in Draft‑04 JsonSchema" ->
                {
                    JsonSchema schema = factorySpy.getSchema(tc.uri());
                    assertNotNull(schema, "Built‑in Draft‑04 schema must be returned");
                    // The meta‑schema for Draft‑04 contains the "$schema" keyword with its own URI.
                    assertEquals("http://json-schema.org/draft-04/schema#", schema.getId(), "Meta‑schema ID must match Draft‑04 URI");
                }
            case "Cache hit – same instance" ->
                {
                    // First call – populate the cache.
                    JsonSchema first = factorySpy.getSchema(tc.uri());
                    // Second call – should hit the cache.
                    JsonSchema second = factorySpy.getSchema(tc.uri());
                    assertSame(first, second, "Factory must return the same cached JsonSchema instance");
                }
            default ->
                fail("Test case '" + tc.group() + "' has an unrecognised expected outcome: " + tc.expected());
        }
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_5_Test.java:9: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_5_Test.java:22: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_5_Test.java:23: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_5_Test.java:25: error: package com.networknt.schema.suite does not exist
import com.networknt.schema.suite.TestCase;
                                 ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_5_Test.java:110: error: cannot find symbol
    static Stream<TestCase> testCases() {
                  ^
  symbol:   class TestCase
  location: class JsonSchemaFactory_getSchema_5_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_5_Test.java:167: error: cannot find symbol
    void getSchema_variousScenarios(TestCase tc) throws Exception {
                                    ^
  symbol:   class TestCase
  location: class JsonSchemaFactory_getSchema_5_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_5_Test.java:16: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_5_Test.java:24: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_5_Test.java:27: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_5_Test.java:50: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_5_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_5_Test.java:56: error: cannot find symbol
    @InjectMocks
     ^
  symbol:   class InjectMocks
  location: class JsonSchemaFactory_getSchema_5_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_5_Test.java:85: error: cannot find symbol
        factorySpy = Mockito.spy(factory);
                     ^
  symbol:   variable Mockito
  location: class JsonSchemaFactory_getSchema_5_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_5_Test.java:112: error: cannot find symbol
        new TestCase("Valid file URI", toUri(tempJsonFile.toUri()), "Returns a non‑null JsonSchema instance", "File‑system URI that points to a readable JSON schema file."), // ---------- Valid URI – YAML ----------
            ^
  symbol:   class TestCase
  location: class JsonSchemaFactory_getSchema_5_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_5_Test.java:113: error: cannot find symbol
        new TestCase("YAML schema loading via file", toUri(tempYamlFile.toUri()), "Returns a non‑null JsonSchema instance", "YAML file should be parsed correctly."), // ---------- Null URI ----------
            ^
  symbol:   class TestCase
  location: class JsonSchemaFactory_getSchema_5_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_5_Test.java:114: error: cannot find symbol
        new TestCase("Null URI", null, "Throws IllegalArgumentException", "The method must guard against a null argument."), // ---------- Relative URI ----------
            ^
  symbol:   class TestCase
  location: class JsonSchemaFactory_getSchema_5_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_5_Test.java:115: error: cannot find symbol
        new TestCase("Relative URI (no scheme)", URI.create("schema.json"), "Throws IllegalArgumentException", "Relative URIs must be rejected."), // ---------- Empty path with scheme ----------
            ^
  symbol:   class TestCase
  location: class JsonSchemaFactory_getSchema_5_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_5_Test.java:116: error: cannot find symbol
        new TestCase("Empty path with scheme", URI.create("file:"), "Throws IllegalArgumentException", "URI with scheme but no path is invalid."), // ---------- Unsupported scheme ----------
            ^
  symbol:   class TestCase
  location: class JsonSchemaFactory_getSchema_5_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_5_Test.java:117: error: cannot find symbol
        new TestCase("Unsupported scheme", URI.create("ftp://example.com/schema.json"), "Throws JsonSchemaException", "FTP scheme is not supported."), // ---------- Upper‑case HTTP scheme ----------
            ^
  symbol:   class TestCase
  location: class JsonSchemaFactory_getSchema_5_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_5_Test.java:118: error: cannot find symbol
        new TestCase("Upper‑case scheme handling", URI.create("HTTP://example.com/remote-schema.json"), "Returns a non‑null JsonSchema instance", "Scheme handling must be case‑insensitive."), // ---------- Meta‑schema normalization ----------
            ^
  symbol:   class TestCase
  location: class JsonSchemaFactory_getSchema_5_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_5_Test.java:119: error: cannot find symbol
        new TestCase("Meta‑schema URI normalization", URI.create("http://json-schema.org/draft-04/schema#"), "Returns built‑in Draft‑04 JsonSchema", "Known meta‑schema URI should be cached."), // ---------- Caching behaviour ----------
            ^
  symbol:   class TestCase
  location: class JsonSchemaFactory_getSchema_5_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_5_Test.java:120: error: cannot find symbol
        new TestCase("Caching – same URI returns same instance", toUri(tempJsonFile.toUri()), "Cache hit – same instance", "Repeated calls for the same URI must return the cached schema."), // ---------- Unsupported URN ----------
            ^
  symbol:   class TestCase
  location: class JsonSchemaFactory_getSchema_5_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_5_Test.java:121: error: cannot find symbol
        new TestCase("Unsupported URN scheme", URI.create("urn:example:schema"), "Throws JsonSchemaException", "URN scheme is not handled."));
            ^
  symbol:   class TestCase
  location: class JsonSchemaFactory_getSchema_5_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_5_Test.java:182: error: cannot find symbol
                        when(mockedTranslator.translate(tc.uri())).thenThrow(new URITranslatorException("Scheme not supported"));
                                                                                 ^
  symbol:   class URITranslatorException
  location: class JsonSchemaFactory_getSchema_5_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_5_Test.java:192: error: cannot find symbol
                        when(mockedTranslator.translate(tc.uri())).thenThrow(new URITranslatorException("Unsupported scheme"));
                                                                                 ^
  symbol:   class URITranslatorException
  location: class JsonSchemaFactory_getSchema_5_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_5_Test.java:228: error: cannot find symbol
                    assertEquals("http://json-schema.org/draft-04/schema#", schema.getId(), "Meta‑schema ID must match Draft‑04 URI");
                                                                                  ^
  symbol:   method getId()
  location: variable schema of type JsonSchema
25 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchemaFactory`:\n\trelated functions:\n\t\tmethod `public JsonSchema getSchema(URI)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI)`;\nclass `com.networknt.schema.ValidationContext`:\n\trelated functions:\n\t\tmethod `public SchemaValidatorsConfig getConfig()` returns `com.networknt.schema.SchemaValidatorsConfig`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI)`;\nclass `com.networknt.schema.walk.WalkEvent`:\n\trelated functions:\n\t\tmethod `public JsonSchema getRefSchema(URI)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI)`;\nclass `com.networknt.schema.BaseJsonValidator`:\n\trelated functions:\n\t\tmethod `private static JsonSchema obtainSubSchemaNode(JsonNode, ValidationContext)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI)`;\nclass `com.networknt.schema.RefValidator`:\n\trelated functions:\n\t\tmethod ` static JsonSchemaRef getRefSchema(JsonSchema, ValidationContext, String)` returns `com.networknt.schema.JsonSchemaRef`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI)`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI)`, api document: \n     * Returns the singleton instance of this validator.\n     *\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


