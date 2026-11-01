```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Additional unit tests for {@link JsonSchemaFactory#getSchema(String, SchemaValidatorsConfig)}.
 *
 * <p>The tests are derived from the exhaustive JSON test‑case list supplied in the
 * assignment. They cover happy‑path parsing, configuration propagation,
 * validation of edge‑cases (null / empty / whitespace schema strings),
 * handling of malformed JSON, unknown types, internal / external $ref
 * resolution, meta‑schema selection, boolean schemas, and error handling for
 * unsupported drafts or malformed URIs.</p>
 *
 * <p>All tests reuse the factory created in {@code @BeforeEach}. The factory is
 * instantiated with an empty {@code SchemaLoaders} map to avoid any network
 * calls – external $ref look‑ups will therefore fail deterministically.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_Test {

    private static final String SIMPLE_SCHEMA = "{ \"type\": \"string\" }";

    private JsonSchemaFactory factory;
    private SchemaValidatorsConfig defaultConfig;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        /*
         * Create a factory that knows about the Draft‑2020‑12 meta‑schema.
         * The builder is used only to ensure the factory can be instantiated
         * without pulling external resources.
         */
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012,
                builder -> builder.schemaLoaders(loaders -> loaders
                        .schemas(Collections.emptyMap())));

        // Default (empty) configuration.
        defaultConfig = SchemaValidatorsConfig.builder().build();
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* --------------------------------------------------------------------- */
    /* Existing baseline tests (kept unchanged)                              */
    /* --------------------------------------------------------------------- */

    @Test
    void getSchema_returnsNonNullJsonSchema() {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA, defaultConfig);
        assertNotNull(schema, "Factory should return a non‑null JsonSchema instance");
        // Basic sanity check – the schema should be able to validate a matching instance.
        assertTrue(schema.validate("hello").isSuccess(),
                "A string value should be valid against a simple 'type:string' schema");
    }

    @Test
    void getSchema_returnsSameInstanceForIdenticalInput() {
        JsonSchema first = factory.getSchema(SIMPLE_SCHEMA, defaultConfig);
        JsonSchema second = factory.getSchema(SIMPLE_SCHEMA, defaultConfig);
        assertSame(first, second, "Repeated calls with identical schema and config must return the same cached instance");
    }

    @Test
    void getSchema_concurrentAccessKeepsCacheInvariant() throws InterruptedException {
        final int threadCount = 30;
        final JsonSchema[] holder = new JsonSchema[1];
        final boolean[] mismatch = new boolean[1];
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    JsonSchema s = factory.getSchema(SIMPLE_SCHEMA, defaultConfig);
                    synchronized (holder) {
                        if (holder[0] == null) {
                            holder[0] = s;
                        } else if (holder[0] != s) {
                            mismatch[0] = true;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }, "t-" + i).start();
        }

        // Release all threads simultaneously.
        startLatch.countDown();
        // Wait for all threads to finish.
        doneLatch.await();

        assertFalse(mismatch[0], "All threads should receive the same cached JsonSchema instance");
        assertNotNull(holder[0], "A JsonSchema instance should have been created");
    }

    /* --------------------------------------------------------------------- */
    /* Parameterised happy‑path tests                                      */
    /* --------------------------------------------------------------------- */

    /**
     * Simple object schema with a single string property.
     */
    @Test
    void getSchema_happyPathObjectSchema() {
        String schemaStr = "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }";
        JsonSchema schema = factory.getSchema(schemaStr, defaultConfig);
        assertNotNull(schema, "Schema should be parsed successfully");

        // Valid instance
        assertTrue(schema.validate("{\"name\":\"John\"}").isSuccess(),
                "Valid object should pass validation");

        // Invalid instance (wrong type)
        assertFalse(schema.validate("{\"name\":10}").isSuccess(),
                "Invalid type should cause validation failure");
    }

    /**
     * Custom configuration – fail‑fast enabled.
     */
    @Test
    void getSchema_customConfigFailFast() {
        String schemaStr = "{ \"type\": \"object\", \"properties\": { \"age\": { \"type\": \"integer\" } } }";
        SchemaValidatorsConfig cfg = SchemaValidatorsConfig.builder()
                .setFailFast(true)
                .build();

        JsonSchema schema = factory.getSchema(schemaStr, cfg);
        assertNotNull(schema, "Schema should be created with custom config");

        // The library returns a {@link ValidationResult}; fail‑fast influences
        // the validation process internally. We simply verify that validation
        // still works and that the custom config does not break parsing.
        assertFalse(schema.validate("{\"age\":\"not-an-int\"}").isSuccess(),
                "Invalid integer should fail validation even with fail‑fast enabled");
    }

    /* --------------------------------------------------------------------- */
    /* Exception‑driven tests                                               */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("exceptionTestProvider")
    void getSchema_exceptionScenarios(String description, String schema, SchemaValidatorsConfig cfg,
                                      Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> factory.getSchema(schema, cfg),
                description);
    }

    /**
     * Supplies arguments for {@link #getSchema_exceptionScenarios}.
     *
     * <p>Each argument contains:
     * <ul>
     *   <li>a human‑readable description (used as the test name),
     *   <li>the schema string (or {@code null}),
     *   <li>the configuration (may be {@code null}),
     *   <li>the expected exception type.</ul>
     */
    private static Stream<Arguments> exceptionTestProvider() {
        SchemaValidatorsConfig defaultCfg = SchemaValidatorsConfig.builder().build();

        return Stream.of(
                // null schema string
                Arguments.of("null schema string → IllegalArgumentException",
                        null, defaultCfg, IllegalArgumentException.class),

                // empty string
                Arguments.of("empty schema string → IllegalArgumentException",
                        "", defaultCfg, IllegalArgumentException.class),

                // blank (whitespace) string
                Arguments.of("blank schema string → IllegalArgumentException",
                        "   \t\n  ", defaultCfg, IllegalArgumentException.class),

                // malformed JSON (missing comma)
                Arguments.of("malformed JSON → JsonSchemaException",
                        "{ \"type\": \"object\" \"properties\": { \"name\": { \"type\": \"string\" } } }",
                        defaultCfg, JsonSchemaException.class),

                // unknown type keyword
                Arguments.of("unknown type in schema → JsonSchemaException",
                        "{ \"type\": \"object\", \"properties\": { \"id\": { \"type\": \"unknown\" } } }",
                        defaultCfg, JsonSchemaException.class),

                // YAML content supplied while default format is JSON
                Arguments.of("YAML content with JSON format → JsonSchemaException",
                        "name: John\nage: 30",
                        defaultCfg, JsonSchemaException.class),

                // unsupported $schema draft‑99
                Arguments.of("unsupported $schema draft → JsonSchemaException",
                        "{ \"$schema\": \"http://json-schema.org/draft-99/schema#\", \"type\": \"object\" }",
                        defaultCfg, JsonSchemaException.class),

                // malformed $id URI
                Arguments.of("malformed $id URI → IllegalArgumentException",
                        "{ \"$id\": \"ht!tp://[invalid]\", \"type\": \"object\" }",
                        defaultCfg, IllegalArgumentException.class),

                // top‑level array as schema
                Arguments.of("top‑level array schema → JsonSchemaException",
                        "[ { \"type\": \"object\" } ]",
                        defaultCfg, JsonSchemaException.class),

                // external $ref that cannot be resolved
                Arguments.of("unresolvable external $ref → JsonSchemaException",
                        "{ \"$ref\": \"http://nonexistent.example.com/schema.json\" }",
                        defaultCfg, JsonSchemaException.class),

                // cyclic $ref within same document
                Arguments.of("cyclic $ref → JsonSchemaException",
                        "{ \"definitions\": { \"A\": { \"$ref\": \"#/definitions/B\" }, \"B\": { \"$ref\": \"#/definitions/A\" } }, \"$ref\": \"#/definitions/A\" }",
                        defaultCfg, JsonSchemaException.class),

                // malformed JSON‑Pointer in $ref
                Arguments.of("malformed JSON‑Pointer in $ref → JsonSchemaException",
                        "{ \"$ref\": \"#/invalid~\" }",
                        defaultCfg, JsonSchemaException.class)
        );
    }

    /* --------------------------------------------------------------------- */
    /* Specific functional tests                                            */
    /* --------------------------------------------------------------------- */

    @Test
    void getSchema_nullConfigFallsBackToFactoryDefault() {
        String schemaStr = "{ \"type\": \"object\" }";
        // Pass null config – the factory should use its internal default.
        JsonSchema schema = factory.getSchema(schemaStr, null);
        assertNotNull(schema, "Schema should be created even when config is null");
        assertTrue(schema.validate("{}").isSuccess(),
                "Empty object should be valid against an empty object schema");
    }

    @Test
    void getSchema_booleanTrueSchemaIsPermissive() {
        JsonSchema schema = factory.getSchema("true", defaultConfig);
        assertNotNull(schema, "Boolean true schema should be parsed");
        // Any instance should validate – we test a string, number and object.
        assertTrue(schema.validate("\"any\"").isSuccess());
        assertTrue(schema.validate("123").isSuccess());
        assertTrue(schema.validate("{\"foo\":\"bar\"}").isSuccess());
    }

    @Test
    void getSchema_booleanFalseSchemaRejectsEverything() {
        JsonSchema schema = factory.getSchema("false", defaultConfig);
        assertNotNull(schema, "Boolean false schema should be parsed");
        // All instances must fail.
        assertFalse(schema.validate("\"any\"").isSuccess());
        assertFalse(schema.validate("123").isSuccess());
        assertFalse(schema.validate("{\"foo\":\"bar\"}").isSuccess());
    }

    @Test
    void getSchema_internalRefResolution() {
        String schemaStr = "{ \"$ref\": \"#/definitions/Person\", \"definitions\": { \"Person\": { \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } } } }";
        JsonSchema schema = factory.getSchema(schemaStr, defaultConfig);
        assertNotNull(schema, "Schema with internal $ref should be resolved");

        // Validate a correct instance
        assertTrue(schema.validate("{\"name\":\"Alice\"}").isSuccess(),
                "Resolved $ref should allow valid data");
        // Invalid instance
        assertFalse(schema.validate("{\"name\":10}").isSuccess(),
                "Resolved $ref should reject invalid data");
    }

    @Test
    void getSchema_draft04MetaSchemaIsHonoured() {
        String schemaStr = "{ \"$schema\": \"http://json-schema.org/draft-04/schema#\", \"type\": \"object\", \"properties\": { \"id\": { \"type\": \"integer\" } } }";
        JsonSchema schema = factory.getSchema(schemaStr, defaultConfig);
        assertNotNull(schema, "Schema with explicit Draft‑04 $schema should be parsed");

        // The meta‑schema URI should be the Draft‑04 identifier.
        JsonMetaSchema meta = schema.getMetaSchema();
        assertEquals("http://json-schema.org/draft-04/schema#", meta.getUri(),
                "Meta‑schema URI should match the $schema declaration");

        // Valid instance
        assertTrue(schema.validate("{\"id\":1}").isSuccess());
    }

    @Test
    void getSchema_anyOfCombinatorWorks() {
        String schemaStr = "{ \"anyOf\": [ { \"type\": \"string\" }, { \"type\": \"integer\" } ] }";
        JsonSchema schema = factory.getSchema(schemaStr, defaultConfig);
        assertNotNull(schema, "Schema using anyOf should be parsed");

        // Both a string and an integer are valid.
        assertTrue(schema.validate("\"text\"").isSuccess());
        assertTrue(schema.validate("42").isSuccess());

        // Something else (e.g., boolean) must fail.
        assertFalse(schema.validate("true").isSuccess());
    }

    @Test
    void getSchema_schemaWithUtf8BomIsAccepted() {
        // Leading UTF‑8 BOM (0xEF,0xBB,0xBF) represented as Unicode \uFEFF
        String schemaStr = "\uFEFF{ \"type\": \"object\", \"properties\": { \"code\": { \"type\": \"string\" } } }";
        JsonSchema schema = factory.getSchema(schemaStr, defaultConfig);
        assertNotNull(schema, "Schema with leading BOM should be parsed");

        assertTrue(schema.validate("{\"code\":\"X\"}").isSuccess());
    }

    @Test
    void getSchema_relativeRefWithBaseIdResolvesCorrectly() {
        String schemaStr = "{ \"$id\": \"http://example.com/root.json\", \"type\": \"object\", \"definitions\": { \"inner\": { \"type\": \"string\" } }, \"properties\": { \"inner\": { \"$ref\": \"root.json#/definitions/inner\" } } }";
        JsonSchema schema = factory.getSchema(schemaStr, defaultConfig);
        assertNotNull(schema, "Schema with $id and relative $ref should be resolved");

        // Valid instance
        assertTrue(schema.validate("{\"inner\":\"value\"}").isSuccess());
    }

    @Test
    void getSchema_schemaWithInvalidPointerFragmentThrows() {
        String schemaStr = "{ \"$ref\": \"#/invalid~\" }";
        assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(schemaStr, defaultConfig),
                "Malformed JSON‑Pointer in $ref must cause JsonSchemaException");
    }
}
```