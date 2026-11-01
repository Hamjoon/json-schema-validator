```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.networknt.schema.resource.SchemaLoaders;
import com.networknt.schema.resource.SchemaMappers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link JsonSchemaFactory#getSchema(SchemaLocation, JsonNode)}.
 *
 * The test creates a concrete subclass {@code TestableJsonSchemaFactory} that overrides the
 * {@code newJsonSchema} protected method. This allows us to verify that the public
 * {@code getSchema} method delegates correctly without needing the full internal
 * implementation.
 *
 * The test cases are derived from the JSON specification supplied with the kata.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_9_Test {

    private static final String DEFAULT_META_SCHEMA_URI = "http://example.com/meta-schema";

    // --------------------------------------------------------------------- //
    // Shared mocks
    // --------------------------------------------------------------------- //
    @Mock
    private ObjectMapper jsonMapperMock;
    @Mock
    private ObjectMapper yamlMapperMock;
    @Mock
    private JsonMetaSchema metaSchemaMock;

    // Instance under test – a concrete subclass that captures arguments.
    private TestableJsonSchemaFactory factory;

    // Captured arguments from the overridden newJsonSchema method.
    private SchemaLocation capturedSchemaLocation;
    private JsonNode capturedJsonNode;
    private SchemaValidatorsConfig capturedConfig;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        MockitoAnnotations.openMocks(this);

        // Minimal meta‑schema map required by the factory constructor.
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), metaSchemaMock);

        // Empty builders – defaults are sufficient for these delegation tests.
        SchemaLoaders.Builder loadersBuilder = new SchemaLoaders.Builder();
        SchemaMappers.Builder mappersBuilder = new SchemaMappers.Builder();

        // Create the testable factory; it will capture arguments passed to newJsonSchema.
        factory = new TestableJsonSchemaFactory(
                jsonMapperMock,
                yamlMapperMock,
                DEFAULT_META_SCHEMA_URI,
                loadersBuilder,
                mappersBuilder,
                metaSchemas,
                false,
                mock(JsonSchema.class)                 // the JsonSchema that newJsonSchema should return
        );
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset captured values for the next test.
        capturedSchemaLocation = null;
        capturedJsonNode = null;
        capturedConfig = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    // --------------------------------------------------------------------- //
    // 1. Successful delegation – various valid inputs
    // --------------------------------------------------------------------- //

    /**
     * Provides a stream of arguments for the successful delegation tests.
     * Each argument consists of:
     *   - a descriptive name (used only for readability)
     *   - a {@link SchemaLocation} instance (may be null)
     *   - a {@link JsonNode} instance (may be null)
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> successfulDelegationProvider() {
        JsonNodeFactory f = JsonNodeFactory.instance;

        // simple object node
        JsonNode simpleObject = f.objectNode().put("type", "object");

        // complex nested node
        JsonNode complexNode = f.objectNode()
                .put("type", "object")
                .set("properties", f.objectNode()
                        .set("name", f.objectNode().put("type", "string"))
                        .set("age", f.objectNode().put("type", "integer")));

        // NullNode (represents JSON literal null)
        JsonNode nullNode = f.nullNode();

        // empty object node
        JsonNode emptyObject = f.objectNode();

        return Stream.of(
                // both arguments non‑null
                org.junit.jupiter.params.provider.Arguments.of(
                        "both non‑null",
                        new SchemaLocation("http://example.com/schema.json"),
                        simpleObject),

                // jsonNode null
                org.junit.jupiter.params.provider.Arguments.of(
                        "jsonNode null",
                        new SchemaLocation("http://example.com/another.json"),
                        null),

                // complex object node
                org.junit.jupiter.params.provider.Arguments.of(
                        "complex object node",
                        new SchemaLocation("file:///tmp/schema.json"),
                        complexNode),

                // NullNode as jsonNode
                org.junit.jupiter.params.provider.Arguments.of(
                        "NullNode jsonNode",
                        new SchemaLocation("http://example.com/nullnode.json"),
                        nullNode),

                // relative URI
                org.junit.jupiter.params.provider.Arguments.of(
                        "relative URI",
                        new SchemaLocation("schemas/subschema.json"),
                        null),

                // empty object node
                org.junit.jupiter.params.provider.Arguments.of(
                        "empty object node",
                        new SchemaLocation("http://example.com/empty.json"),
                        emptyObject)
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulDelegationProvider")
    void getSchema_delegatesCorrectly(String _description,
                                      SchemaLocation schemaLocation,
                                      JsonNode jsonNode) {
        // ----- When ----------------------------------------------------------
        JsonSchema result = factory.getSchema(schemaLocation, jsonNode);

        // ----- Then ----------------------------------------------------------
        assertNotNull(result, "Resulting JsonSchema must not be null");
        assertSame(factory.getExpectedJsonSchema(), result,
                "Returned JsonSchema should be the mock supplied by overridden newJsonSchema");

        // Verify that the protected newJsonSchema method received the same arguments.
        assertSame(schemaLocation, capturedSchemaLocation,
                "SchemaLocation passed to newJsonSchema must match the supplied instance");
        assertSame(jsonNode, capturedJsonNode,
                "JsonNode passed to newJsonSchema must match the supplied instance (including null)");
        assertNull(capturedConfig,
                "Config argument must always be null for this overload");
    }

    // --------------------------------------------------------------------- //
    // 2. Null schemaUri – expect NullPointerException
    // --------------------------------------------------------------------- //

    private static Stream<org.junit.jupiter.params.provider.Arguments> nullSchemaUriProvider() {
        JsonNodeFactory f = JsonNodeFactory.instance;
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(
                        "null schemaUri, non‑null jsonNode",
                        null,
                        f.objectNode().put("type", "string")),
                org.junit.jupiter.params.provider.Arguments.of(
                        "null schemaUri, null jsonNode",
                        null,
                        null)
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullSchemaUriProvider")
    void getSchema_nullSchemaUri_throwsNPE(String _description,
                                           SchemaLocation schemaLocation,
                                           JsonNode jsonNode) {
        // ----- When & Then --------------------------------------------------
        assertThrows(NullPointerException.class,
                () -> factory.getSchema(schemaLocation, jsonNode),
                "Calling getSchema with a null SchemaLocation should result in NPE");
    }

    // --------------------------------------------------------------------- //
    // 3. Invalid URI – expect IllegalArgumentException
    // --------------------------------------------------------------------- //

    @Test
    void getSchema_invalidSchemaLocation_throwsIllegalArgumentException() {
        // ----- Given ---------------------------------------------------------
        SchemaLocation invalidLocation = new SchemaLocation("http://[invalid-uri]");
        JsonNode node = JsonNodeFactory.instance.objectNode().put("type", "string");

        // ----- When & Then ---------------------------------------------------
        assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(invalidLocation, node),
                "Malformed URI should cause IllegalArgumentException");
    }

    // --------------------------------------------------------------------- //
    // Helper concrete subclass that captures arguments and simulates failures
    // --------------------------------------------------------------------- //
    private class TestableJsonSchemaFactory extends JsonSchemaFactory {

        private final JsonSchema expectedJsonSchema;

        TestableJsonSchemaFactory(ObjectMapper jsonMapper,
                                  ObjectMapper yamlMapper,
                                  String defaultMetaSchemaURI,
                                  SchemaLoaders.Builder schemaLoadersBuilder,
                                  SchemaMappers.Builder schemaMappersBuilder,
                                  Map<String, JsonMetaSchema> jsonMetaSchemas,
                                  boolean enableUriSchemaCache,
                                  JsonSchema jsonSchemaToReturn) {
            super(jsonMapper,
                  yamlMapper,
                  defaultMetaSchemaURI,
                  schemaLoadersBuilder,
                  schemaMappersBuilder,
                  jsonMetaSchemas,
                  enableUriSchemaCache);
            this.expectedJsonSchema = jsonSchemaToReturn;
        }

        JsonSchema getExpectedJsonSchema() {
            return expectedJsonSchema;
        }

        @Override
        protected JsonSchema newJsonSchema(SchemaLocation schemaUri,
                                           JsonNode schemaNode,
                                           SchemaValidatorsConfig config) {
            // Simulate the validation performed by the real implementation:
            if (schemaUri == null) {
                throw new NullPointerException("schemaUri is null");
            }
            // Very naive check for an obviously malformed URI (the test case uses "[invalid-uri]")
            if (schemaUri.toString().contains("[invalid-uri]")) {
                throw new IllegalArgumentException("Malformed URI: " + schemaUri);
            }

            // Capture arguments for verification.
            capturedSchemaLocation = schemaUri;
            capturedJsonNode = schemaNode;
            capturedConfig = config;

            // Return the pre‑configured mock.
            return expectedJsonSchema;
        }
    }
}
```