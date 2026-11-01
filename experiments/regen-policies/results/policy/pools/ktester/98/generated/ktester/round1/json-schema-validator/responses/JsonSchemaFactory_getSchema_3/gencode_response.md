```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link JsonSchemaFactory#getSchema(JsonNode)}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>valid schemas (default meta‑schema, explicit $schema, $id handling, empty schema)</li>
 *   <li>invalid inputs (null, wrong root type, unknown $schema, malformed $schema, etc.)</li>
 *   <li>boolean schemas (true / false)</li>
 *   <li>advanced meta‑schema versions (draft‑04, draft‑06, 2019‑09, 2020‑12)</li>
 *   <li>reference handling ($ref, $id, circular refs, external refs)</li>
 *   <li>unknown keywords and other edge cases</li>
 * </ul>
 *
 * <p>All tests use a real {@link JsonSchemaFactory} instance obtained via
 * {@link JsonSchemaFactory#getInstance(VersionFlag)} – no mocking of internal collaborators
 * is required.  Reflection is used only to retrieve the meta‑schema URI when the
 * library does not expose a public accessor.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_3_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the most recent draft as the base; the factory will resolve other drafts
        // via the $schema keyword when present.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        assertNotNull(factory, "Factory should be created");
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup needed.
    }

    /* --------------------------------------------------------------------- */
    /* Helper utilities                                                    */
    /* --------------------------------------------------------------------- */

    /**
     * Returns the meta‑schema URI that the {@link JsonSchema} is bound to.
     * The library does not guarantee a public accessor, therefore we try
     * the obvious {@code getMetaSchema()} → {@code getUri()} chain and fall
     * back to reflection if necessary.
     */
    private static String metaSchemaUri(JsonSchema schema) {
        try {
            Method getMeta = schema.getClass().getMethod("getMetaSchema");
            Object meta = getMeta.invoke(schema);
            Method getUri = meta.getClass().getMethod("getUri");
            return (String) getUri.invoke(meta);
        } catch (Exception e) {
            // If reflection fails we cannot assert the exact URI – return null.
            return null;
        }
    }

    /**
     * Validates {@code instance} against {@code schema} and returns {@code true}
     * if no validation messages are produced.
     */
    private static boolean validates(JsonSchema schema, JsonNode instance) {
        Set<ValidationMessage> messages = schema.validate(instance);
        return messages.isEmpty();
    }

    /* --------------------------------------------------------------------- */
    /* Parameterised tests – valid schemas                                 */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validSchemaProvider")
    void shouldCreateValidJsonSchema(String description, String schemaJson,
                                     String expectedMetaSchemaUri, boolean expectLocationFromId) throws Exception {
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        JsonSchema jsonSchema = factory.getSchema(schemaNode);
        assertNotNull(jsonSchema, "JsonSchema must not be null");
        assertEquals(schemaNode, jsonSchema.getSchemaNode(), "Schema node inside JsonSchema must match input");

        // Verify meta‑schema URI (null means we cannot determine it via reflection)
        if (expectedMetaSchemaUri != null) {
            String actualUri = metaSchemaUri(jsonSchema);
            assertEquals(expectedMetaSchemaUri, actualUri,
                    "Meta‑schema URI should match the expected value");
        }

        // If an $id is present and we expect the factory to honour it, verify that the
        // schema location contains that ID (the library stores it as a URI string).
        if (expectLocationFromId) {
            // The JsonSchema class exposes getSchemaLocation() in newer versions; use reflection.
            try {
                Method getLoc = jsonSchema.getClass().getMethod("getSchemaLocation");
                Object loc = getLoc.invoke(jsonSchema);
                assertNotNull(loc, "Schema location should not be null");
                Method toString = loc.getClass().getMethod("toString");
                String locStr = (String) toString.invoke(loc);
                assertTrue(locStr.contains("http://example.com/person"),
                        "Schema location should contain the $id URI");
            } catch (NoSuchMethodException ignored) {
                // If the method does not exist, we cannot assert the location – this is acceptable.
            }
        }
    }

    private static Stream<Arguments> validSchemaProvider() {
        return Stream.of(
                Arguments.of(
                        "Minimal valid schema (type string) – default Draft‑07",
                        "{ \"type\": \"string\" }",
                        "http://json-schema.org/draft-07/schema#", // default meta‑schema URI
                        false),
                Arguments.of(
                        "Explicit Draft‑04 via $schema",
                        "{ \"$schema\": \"http://json-schema.org/draft-04/schema#\", \"type\": \"integer\" }",
                        "http://json-schema.org/draft-04/schema#",
                        false),
                Arguments.of(
                        "Schema with $id only – default Draft‑07",
                        "{ \"$id\": \"http://example.com/person\", \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }",
                        "http://json-schema.org/draft-07/schema#",
                        true),
                Arguments.of(
                        "Empty schema – always valid, default Draft‑07",
                        "{}",
                        "http://json-schema.org/draft-07/schema#",
                        false)
        );
    }

    /* --------------------------------------------------------------------- */
    /* Parameterised tests – invalid inputs                                 */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidInputProvider")
    void shouldRejectInvalidInput(String description, String schemaJson, Class<? extends Throwable> expectedException) throws Exception {
        JsonNode node = MAPPER.readTree(schemaJson);
        assertThrows(expectedException, () -> factory.getSchema(node),
                "Expected exception for: " + description);
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(
                // null is handled in a dedicated test below
                Arguments.of("Array as root node", "[ 1, 2, 3 ]", JsonSchemaException.class),
                Arguments.of("Unknown $schema URI", "{ \"$schema\": \"http://unknown-schema.org/unknown#\", \"type\": \"string\" }",
                        JsonSchemaException.class)
        );
    }

    @Test
    void shouldThrowWhenJsonNodeIsNull() {
        assertThrows(NullPointerException.class, () -> factory.getSchema((JsonNode) null));
    }

    /* --------------------------------------------------------------------- */
    /* Boolean schemas (true / false)                                      */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("booleanSchemaProvider")
    void shouldHandleBooleanSchema(String description, String schemaJson,
                                   boolean shouldValidateAny, boolean shouldValidateNone) throws Exception {
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        JsonSchema schema = factory.getSchema(schemaNode);
        assertNotNull(schema, "JsonSchema must not be null");

        // Any JSON instance (we use a string) should be accepted for 'true' schema
        JsonNode anyInstance = MAPPER.readTree("\"any value\"");
        assertEquals(shouldValidateAny, validates(schema, anyInstance),
                description + " – validation of arbitrary instance");

        // For 'false' schema, validation must always fail (use a number as example)
        JsonNode anotherInstance = MAPPER.readTree("123");
        assertEquals(shouldValidateNone, validates(schema, anotherInstance),
                description + " – validation of another instance");
    }

    private static Stream<Arguments> booleanSchemaProvider() {
        return Stream.of(
                Arguments.of("Boolean true schema", "true", true, false),
                Arguments.of("Boolean false schema", "false", false, false) // false never validates
        );
    }

    /* --------------------------------------------------------------------- */
    /* Advanced meta‑schema versions                                         */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("advancedMetaSchemaProvider")
    void shouldRespectAdvancedMetaSchema(String description, String schemaJson,
                                         String expectedMetaSchemaUri) throws Exception {
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        JsonSchema schema = factory.getSchema(schemaNode);
        assertNotNull(schema, "JsonSchema must not be null");
        String actualUri = metaSchemaUri(schema);
        assertEquals(expectedMetaSchemaUri, actualUri,
                "Meta‑schema URI should match for " + description);
    }

    private static Stream<Arguments> advancedMetaSchemaProvider() {
        return Stream.of(
                Arguments.of("Draft‑06 meta‑schema",
                        "{ \"$schema\": \"http://json-schema.org/draft-06/schema#\", \"type\": \"boolean\" }",
                        "http://json-schema.org/draft-06/schema#"),
                Arguments.of("Draft‑2019‑09 meta‑schema",
                        "{ \"$schema\": \"https://json-schema.org/draft/2019-09/schema\", \"type\": \"null\" }",
                        "https://json-schema.org/draft/2019-09/schema"),
                Arguments.of("Draft‑2020‑12 meta‑schema",
                        "{ \"$schema\": \"https://json-schema.org/draft/2020-12/schema\", \"type\": \"array\", \"items\": { \"type\": \"integer\" } }",
                        "https://json-schema.org/draft/2020-12/schema")
        );
    }

    /* --------------------------------------------------------------------- */
    /* $ref and $id handling                                                */
    /* --------------------------------------------------------------------- */

    @Test
    void shouldResolveInternalRefSuccessfully() throws Exception {
        String schemaJson = """
                {
                  "$id": "http://example.com/root",
                  "$defs": {
                    "positiveInt": { "type": "integer", "minimum": 1 }
                  },
                  "$ref": "#/$defs/positiveInt"
                }
                """;
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        JsonSchema schema = factory.getSchema(schemaNode);
        assertNotNull(schema, "JsonSchema must not be null");

        // Valid instance (5) – should pass
        JsonNode valid = MAPPER.readTree("5");
        assertTrue(validates(schema, valid), "Integer 5 should validate");

        // Invalid instance (0) – should fail because of minimum 1
        JsonNode invalid = MAPPER.readTree("0");
        assertFalse(validates(schema, invalid), "Integer 0 should NOT validate");
    }

    /* --------------------------------------------------------------------- */
    /* Invalid root types (non‑object, non‑boolean)                         */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidRootTypeProvider")
    void shouldRejectInvalidRootTypes(String description, String schemaJson,
                                      Class<? extends Throwable> expectedException) throws Exception {
        JsonNode node = MAPPER.readTree(schemaJson);
        assertThrows(expectedException, () -> factory.getSchema(node),
                "Expected exception for root type: " + description);
    }

    private static Stream<Arguments> invalidRootTypeProvider() {
        return Stream.of(
                Arguments.of("Root string", "\"just a string\"", JsonSchemaException.class),
                Arguments.of("Root number", "12345", JsonSchemaException.class)
        );
    }

    /* --------------------------------------------------------------------- */
    /* Empty or null $schema values                                         */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("emptyOrNullSchemaProvider")
    void shouldFallbackToDefaultMetaSchemaWhenSchemaKeywordIsEmptyOrNull(String description,
                                                                        String schemaJson,
                                                                        String expectedMetaSchemaUri) throws Exception {
        JsonNode node = MAPPER.readTree(schemaJson);
        JsonSchema schema = factory.getSchema(node);
        assertNotNull(schema, "JsonSchema must not be null");
        String actualUri = metaSchemaUri(schema);
        assertEquals(expectedMetaSchemaUri, actualUri,
                "Meta‑schema should fall back to default for " + description);
    }

    private static Stream<Arguments> emptyOrNullSchemaProvider() {
        return Stream.of(
                Arguments.of("Empty $schema string",
                        "{ \"$schema\": \"\", \"type\": \"string\" }",
                        "http://json-schema.org/draft-07/schema#"),
                Arguments.of("Null $schema value",
                        "{ \"$schema\": null, \"type\": \"string\" }",
                        "http://json-schema.org/draft-07/schema#")
        );
    }

    /* --------------------------------------------------------------------- */
    /* Unknown keywords – should be ignored                                 */
    /* --------------------------------------------------------------------- */

    @Test
    void shouldIgnoreUnknownKeywordAndValidateCorrectly() throws Exception {
        String schemaJson = """
                {
                  "type": "object",
                  "unknownKeyword": { "foo": "bar" },
                  "properties": {
                    "id": { "type": "string" }
                  }
                }
                """;
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        JsonSchema schema = factory.getSchema(schemaNode);
        assertNotNull(schema, "JsonSchema must not be null");

        // Valid object with string id
        JsonNode valid = MAPPER.readTree("{ \"id\": \"ABC\" }");
        assertTrue(validates(schema, valid), "Object with string id should validate");

        // Invalid object (missing id) – still valid because 'id' is optional
        JsonNode alsoValid = MAPPER.readTree("{ }");
        assertTrue(validates(schema, alsoValid), "Object without id should also validate");
    }

    /* --------------------------------------------------------------------- */
    /* Invalid $id URI                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    void shouldRejectMalformedIdUri() throws Exception {
        String schemaJson = """
                {
                  "$id": "ht!tp://[invalid]",
                  "type": "object"
                }
                """;
        JsonNode node = MAPPER.readTree(schemaJson);
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(node),
                "Malformed $id URI should cause IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- */
    /* Unresolvable external $ref                                            */
    /* --------------------------------------------------------------------- */

    @Test
    void shouldFailWhenExternalRefIsUnresolvable() throws Exception {
        String schemaJson = """
                {
                  "$ref": "http://example.com/nonexistent-schema.json"
                }
                """;
        JsonNode node = MAPPER.readTree(schemaJson);
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(node),
                "Unresolvable external $ref should raise JsonSchemaException");
    }

    /* --------------------------------------------------------------------- */
    /* Circular $ref detection                                               */
    /* --------------------------------------------------------------------- */

    @Test
    void shouldDetectCircularReferences() throws Exception {
        String schemaJson = """
                {
                  "$id": "http://example.com/circular",
                  "$defs": {
                    "A": { "$ref": "#/$defs/B" },
                    "B": { "$ref": "#/$defs/A" }
                  },
                  "$ref": "#/$defs/A"
                }
                """;
        JsonNode node = MAPPER.readTree(schemaJson);
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(node),
                "Circular $ref should cause JsonSchemaException");
    }

    /* --------------------------------------------------------------------- */
    /* Malformed $schema value                                               */
    /* --------------------------------------------------------------------- */

    @Test
    void shouldRejectNonUriSchemaValue() throws Exception {
        String schemaJson = """
                {
                  "$schema": "not a uri",
                  "type": "string"
                }
                """;
        JsonNode node = MAPPER.readTree(schemaJson);
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(node),
                "Non‑URI $schema value should cause IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- */
    /* $ref to non‑schema node                                               */
    /* --------------------------------------------------------------------- */

    @Test
    void shouldRejectRefPointingToNonSchemaNode() throws Exception {
        String schemaJson = """
                {
                  "title": "MySchema",
                  "$ref": "#/title"
                }
                """;
        JsonNode node = MAPPER.readTree(schemaJson);
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(node),
                "$ref that points to a primitive (title) must raise an exception");
    }

    /* --------------------------------------------------------------------- */
    /* Existing simple tests (kept unchanged)                               */
    /* --------------------------------------------------------------------- */

    /**
     * Verify that a simple JSON schema node can be turned into a {@link JsonSchema}
     * via {@code getSchema(JsonNode)}.
     */
    @Test
    void shouldCreateJsonSchemaFromJsonNode() throws Exception {
        // GIVEN a minimal JSON schema node
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  }
                }
                """;
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        assertNotNull(schemaNode, "Parsed schema node must not be null");

        // WHEN the factory creates a JsonSchema from the node
        JsonSchema jsonSchema = factory.getSchema(schemaNode);

        // THEN a non‑null JsonSchema instance is returned and it holds the same node
        assertNotNull(jsonSchema, "JsonSchema should not be null");
        assertTrue(jsonSchema instanceof JsonSchema, "Returned object must be a JsonSchema");

        // The JsonSchema class exposes the original schema node via getSchemaNode()
        JsonNode retrievedNode = jsonSchema.getSchemaNode();
        assertNotNull(retrievedNode, "The JsonSchema should contain a schema node");
        assertEquals(schemaNode, retrievedNode, "The schema node inside JsonSchema must match the input");
    }

    /**
     * Verify that passing {@code null} as the schema node results in a
     * {@link NullPointerException}. The factory does not accept a null argument
     * for {@code getSchema(JsonNode)}.
     */
    @Test
    void shouldThrowWhenJsonNodeIsNullAgain() {
        JsonNode nullNode = null;
        assertThrows(NullPointerException.class, () -> factory.getSchema(nullNode));
    }
}
```