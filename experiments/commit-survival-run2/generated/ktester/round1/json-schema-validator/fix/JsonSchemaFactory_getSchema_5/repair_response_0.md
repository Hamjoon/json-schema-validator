```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(URI)}.
 *
 * <p>The original test used Mockito and a custom {@code TestCase} class that are not
 * available in this project.  To keep the test self‑contained we replace the external
 * dependencies with a simple inner {@code SimpleTestCase} class and rely only on the
 * real {@link JsonSchemaFactory} implementation.</p>
 */
class JsonSchemaFactory_getSchema_5_Test {

    /** Factory instance used for all tests. */
    private static JsonSchemaFactory factory;

    /** Temporary JSON schema file (valid). */
    private static Path tempJsonFile;

    /** Temporary YAML schema file (valid). */
    private static Path tempYamlFile;

    @BeforeAll
    static void setupBeforeAll() throws IOException {
        // Create a minimal JSON schema file.
        String json = "{\"type\":\"object\",\"definitions\":{\"address\":{\"type\":\"object\"}}}";
        tempJsonFile = Files.createTempFile("example-schema", ".json");
        Files.writeString(tempJsonFile, json);

        // Create a minimal YAML schema file.
        String yaml = "type: object\nproperties:\n  name:\n    type: string";
        tempYamlFile = Files.createTempFile("example-schema", ".yaml");
        Files.writeString(tempYamlFile, yaml);

        // Obtain a factory instance.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        assertNotNull(factory, "Factory must be created");
    }

    @AfterAll
    static void teardownAfterAll() throws IOException {
        Files.deleteIfExists(tempJsonFile);
        Files.deleteIfExists(tempYamlFile);
    }

    /** Simple data holder for the parameterised tests. */
    private static final class SimpleTestCase {
        private final String name;
        private final URI uri;
        private final String expected;
        private final String description;

        SimpleTestCase(String name, URI uri, String expected, String description) {
            this.name = name;
            this.uri = uri;
            this.expected = expected;
            this.description = description;
        }

        String name() { return name; }
        URI uri() { return uri; }
        String expected() { return expected; }
        String description() { return description; }
    }

    /** Supplies the test cases. */
    static Stream<SimpleTestCase> testCases() {
        return Stream.of(
            new SimpleTestCase(
                "Valid file URI (JSON)",
                tempJsonFile.toUri(),
                "Returns a non‑null JsonSchema instance",
                "File‑system URI that points to a readable JSON schema file."
            ),
            new SimpleTestCase(
                "Valid file URI (YAML)",
                tempYamlFile.toUri(),
                "Returns a non‑null JsonSchema instance",
                "YAML file should be parsed correctly."
            ),
            new SimpleTestCase(
                "Null URI",
                null,
                "Throws IllegalArgumentException",
                "The method must guard against a null argument."
            ),
            new SimpleTestCase(
                "Relative URI (no scheme)",
                URI.create("schema.json"),
                "Throws IllegalArgumentException",
                "Relative URIs must be rejected."
            ),
            new SimpleTestCase(
                "Empty path with scheme",
                URI.create("file:"),
                "Throws IllegalArgumentException",
                "URI with scheme but no path is invalid."
            ),
            new SimpleTestCase(
                "Unsupported scheme (ftp)",
                URI.create("ftp://example.com/schema.json"),
                "Throws JsonSchemaException",
                "FTP scheme is not supported."
            ),
            new SimpleTestCase(
                "Cache hit – same URI returns same instance",
                tempJsonFile.toUri(),
                "Cache hit – same instance",
                "Repeated calls for the same URI must return the cached schema."
            )
        );
    }

    /** Helper to create a simple JSON InputStream (used only for completeness). */
    private static InputStream simpleJsonInputStream() {
        String json = "{\"type\":\"object\"}";
        return new java.io.ByteArrayInputStream(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void getSchema_withValidUri_returnsJsonSchema() {
        // GIVEN
        URI schemaUri = tempJsonFile.toUri();
        // WHEN
        JsonSchema jsonSchema = factory.getSchema(schemaUri);
        // THEN
        assertNotNull(jsonSchema, "Returned JsonSchema should not be null");
        JsonNode rootNode = jsonSchema.getSchemaNode();
        assertNotNull(rootNode, "Root node of the schema should not be null");
        assertEquals("object", rootNode.get("type").asText(),
                "The 'type' property of the loaded schema should be 'object'");
    }

    @Test
    void getSchema_withNullUri_throwsIllegalArgumentException() {
        // GIVEN
        URI nullUri = null;
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(nullUri),
                "Calling getSchema(null) should throw IllegalArgumentException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("testCases")
    void getSchema_variousScenarios(SimpleTestCase tc) {
        switch (tc.expected()) {
            case "Throws IllegalArgumentException" -> {
                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                        () -> factory.getSchema(tc.uri()),
                        "Calling getSchema with illegal argument must throw");
                assertTrue(ex.getMessage().toLowerCase(Locale.ROOT).contains("schemauri"),
                        "Exception message should mention 'schemaUri'");
            }
            case "Throws JsonSchemaException" -> {
                assertThrows(JsonSchemaException.class,
                        () -> factory.getSchema(tc.uri()),
                        "Unsupported scheme must result in JsonSchemaException");
            }
            case "Returns a non‑null JsonSchema instance" -> {
                JsonSchema schema = factory.getSchema(tc.uri());
                assertNotNull(schema, "Returned JsonSchema must not be null");
                JsonNode root = schema.getSchemaNode();
                assertNotNull(root, "Root node must be present");
                // Both JSON and YAML examples contain a "type" property.
                assertTrue(root.has("type"), "Schema should contain a 'type' property");
            }
            case "Cache hit – same instance" -> {
                // First call – populate the cache.
                JsonSchema first = factory.getSchema(tc.uri());
                // Second call – should hit the cache.
                JsonSchema second = factory.getSchema(tc.uri());
                assertSame(first, second, "Factory must return the same cached JsonSchema instance");
            }
            default -> fail("Unrecognised expected outcome: " + tc.expected());
        }
    }
}
```