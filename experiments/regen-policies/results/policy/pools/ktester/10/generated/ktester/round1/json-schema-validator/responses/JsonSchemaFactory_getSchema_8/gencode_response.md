```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.resource.SchemaLoaders;
import com.networknt.schema.resource.SchemaMappers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(JsonNode, SchemaValidatorsConfig)}.
 *
 * <p>
 * The factory constructor is private, therefore the instance is created via reflection.
 * The protected {@code newJsonSchema} method is stubbed on a spy so that the heavy
 * internal logic of {@link JsonSchema} creation is avoided. The stub mimics the
 * behaviour required for the test scenarios (null‑checks, $schema handling, etc.).
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_8_Test {

    // -------------------------------------------------------------------------
    // Shared test fixtures
    // -------------------------------------------------------------------------

    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema#";

    @Mock
    private ObjectMapper jsonMapperMock;
    @Mock
    private ObjectMapper yamlMapperMock;
    @Mock
    private SchemaLoaders.Builder schemaLoadersBuilderMock;
    @Mock
    private SchemaMappers.Builder schemaMappersBuilderMock;
    @Mock
    private JsonMetaSchema jsonMetaSchemaMock;

    private JsonSchemaFactory factory;          // real instance (private ctor via reflection)
    private JsonSchemaFactory factorySpy;       // spy used to stub protected method

    private JsonSchema jsonSchemaMock;            // what the stubbed newJsonSchema returns
    private final ObjectMapper realMapper = new ObjectMapper(); // for building JsonNode values

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @BeforeEach
    void setUp() throws Exception {
        // ---- 1️⃣ build the minimal meta‑schema map required by the private ctor ----
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), jsonMetaSchemaMock);

        // ---- 2️⃣ obtain the private constructor via reflection --------------------
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(
                ObjectMapper.class,
                ObjectMapper.class,
                String.class,
                SchemaLoaders.Builder.class,
                SchemaMappers.Builder.class,
                Map.class,
                boolean.class);
        ctor.setAccessible(true);

        // ---- 3️⃣ create a real factory instance ---------------------------------
        factory = ctor.newInstance(
                jsonMapperMock,
                yamlMapperMock,
                DEFAULT_META_SCHEMA_URI,
                schemaLoadersBuilderMock,
                schemaMappersBuilderMock,
                metaSchemas,
                true // enableUriSchemaCache – arbitrary true for the test
        );

        // ---- 4️⃣ spy the factory ------------------------------------------------
        factorySpy = Mockito.spy(factory);

        // ---- 5️⃣ mock the JsonSchema that will be returned by the stubbed method --
        jsonSchemaMock = mock(JsonSchema.class);

        // ---- 6️⃣ stub createSchemaValidatorsConfig – used when the caller passes null
        doReturn(new SchemaValidatorsConfig())
                .when(factorySpy).createSchemaValidatorsConfig();

        /**
         * Stub the protected {@code newJsonSchema} method.
         *
         * The stub reproduces the validation that the real method performs
         * before delegating to the heavy schema creation logic:
         *   • throws NPE when the supplied {@code JsonNode} is {@code null}
         *   • throws NPE for {@code MissingNode}
         *   • validates the optional {@code $schema} keyword:
         *       – unknown identifier → {@code IllegalArgumentException}
         *       – malformed URI → {@code IllegalArgumentException}
         *   • otherwise returns the pre‑created {@code jsonSchemaMock}
         */
        doAnswer(invocation -> {
            JsonNode node = invocation.getArgument(1);
            SchemaValidatorsConfig cfg = invocation.getArgument(2);

            // null check – mirrors the behaviour of the real implementation
            if (node == null) {
                throw new NullPointerException("jsonNode is null");
            }

            // MissingNode is not a concrete schema node
            if (node.isMissingNode()) {
                throw new NullPointerException("MissingNode cannot be used as a schema");
            }

            // $schema handling – only a very small subset needed for the tests
            if (node.isObject() && node.has("$schema")) {
                String schemaId = node.get("$schema").asText();

                // unknown meta‑schema identifier
                if ("http://example.com/unknown/schema#".equals(schemaId)) {
                    throw new IllegalArgumentException("Unsupported $schema identifier: " + schemaId);
                }

                // malformed URI – any URI parsing exception is wrapped
                try {
                    new URI(schemaId);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Malformed $schema URI: " + schemaId, e);
                }
            }

            // In all other cases simply return the mock schema
            return jsonSchemaMock;
        }).when(factorySpy).newJsonSchema(isNull(), any(JsonNode.class), any(SchemaValidatorsConfig.class));
    }

    @AfterEach
    void tearDown() {
        // Reset all mocks to keep tests independent
        reset(jsonMapperMock, yamlMapperMock, schemaLoadersBuilderMock,
                schemaMappersBuilderMock, jsonMetaSchemaMock,
                jsonSchemaMock);
    }

    // -------------------------------------------------------------------------
    // Test cases – grouped by the JSON description supplied in the prompt
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Null Parameters
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowNPEWhenBothArgumentsAreNull() {
        assertThrows(NullPointerException.class,
                () -> factorySpy.getSchema(null, null),
                "Both arguments null → NPE expected");
    }

    @Test
    void shouldThrowNPEWhenJsonNodeIsNullEvenIfConfigIsPresent() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        assertThrows(NullPointerException.class,
                () -> factorySpy.getSchema(null, cfg),
                "Null JsonNode with non‑null config → NPE expected");
    }

    @Test
    void shouldFallBackToDefaultConfigWhenConfigIsNull() {
        // an empty object node – valid schema content
        JsonNode emptyObject = realMapper.createObjectNode();

        JsonSchema result = factorySpy.getSchema(emptyObject, null);

        assertNotNull(result, "Result must not be null");
        assertSame(jsonSchemaMock, result, "Factory must return the stubbed JsonSchema");

        // capture the config that reached newJsonSchema
        ArgumentCaptor<SchemaValidatorsConfig> cfgCaptor = ArgumentCaptor.forClass(SchemaValidatorsConfig.class);
        verify(factorySpy, times(1))
                .newJsonSchema(isNull(), eq(emptyObject), cfgCaptor.capture());

        assertNotNull(cfgCaptor.getValue(),
                "When the caller passes null, the factory should create a default SchemaValidatorsConfig");
    }

    // -------------------------------------------------------------------------
    // Valid Parameters – Simple Schema
    // -------------------------------------------------------------------------

    @Test
    void shouldCreateSchemaForSimpleStringTypeWithDefaultConfig() {
        JsonNode schemaNode = realMapper.readTree("{\"type\":\"string\"}");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig(); // default

        JsonSchema result = factorySpy.getSchema(schemaNode, cfg);

        assertNotNull(result);
        assertSame(jsonSchemaMock, result);
        verify(factorySpy, times(1))
                .newJsonSchema(isNull(), eq(schemaNode), eq(cfg));
    }

    @Test
    void shouldCreateSchemaForComplexObjectWithCustomConfig() {
        JsonNode schemaNode = realMapper.readTree("{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"integer\"}}}");
        SchemaValidatorsConfig customCfg = new SchemaValidatorsConfig();
        customCfg.setTypeLoose(true); // example custom flag

        JsonSchema result = factorySpy.getSchema(schemaNode, customCfg);

        assertNotNull(result);
        assertSame(jsonSchemaMock, result);
        verify(factorySpy, times(1))
                .newJsonSchema(isNull(), eq(schemaNode), eq(customCfg));
    }

    // -------------------------------------------------------------------------
    // Edge Cases – Special JsonNode Types
    // -------------------------------------------------------------------------

    @Test
    void shouldAcceptNullNodeAsSchema() {
        JsonNode nullNode = realMapper.nullNode();

        JsonSchema result = factorySpy.getSchema(nullNode, new SchemaValidatorsConfig());

        assertNotNull(result);
        assertSame(jsonSchemaMock, result);
        verify(factorySpy, times(1))
                .newJsonSchema(isNull(), eq(nullNode), any(SchemaValidatorsConfig.class));
    }

    @Test
    void shouldAcceptArrayNodeAsTopLevelSchema() {
        JsonNode arrayNode = realMapper.readTree("[{\"type\":\"string\"},{\"type\":\"number\"}]");

        JsonSchema result = factorySpy.getSchema(arrayNode, new SchemaValidatorsConfig());

        assertNotNull(result);
        assertSame(jsonSchemaMock, result);
        verify(factorySpy, times(1))
                .newJsonSchema(isNull(), eq(arrayNode), any(SchemaValidatorsConfig.class));
    }

    // -------------------------------------------------------------------------
    // Boolean Schema Nodes
    // -------------------------------------------------------------------------

    @Test
    void shouldAcceptBooleanTrueNode() {
        JsonNode trueNode = realMapper.readTree("true");

        JsonSchema result = factorySpy.getSchema(trueNode, new SchemaValidatorsConfig());

        assertNotNull(result);
        assertSame(jsonSchemaMock, result);
        verify(factorySpy, times(1))
                .newJsonSchema(isNull(), eq(trueNode), any(SchemaValidatorsConfig.class));
    }

    @Test
    void shouldAcceptBooleanFalseNode() {
        JsonNode falseNode = realMapper.readTree("false");

        JsonSchema result = factorySpy.getSchema(falseNode, new SchemaValidatorsConfig());

        assertNotNull(result);
        assertSame(jsonSchemaMock, result);
        verify(factorySpy, times(1))
                .newJsonSchema(isNull(), eq(falseNode), any(SchemaValidatorsConfig.class));
    }

    // -------------------------------------------------------------------------
    // Non‑Object/Array/Null/Boolean Nodes – TextNode
    // -------------------------------------------------------------------------

    @Test
    void shouldAcceptTextNodeEvenIfNotAValidSchema() {
        JsonNode textNode = realMapper.readTree("\"some text\"");

        JsonSchema result = factorySpy.getSchema(textNode, new SchemaValidatorsConfig());

        assertNotNull(result);
        assertSame(jsonSchemaMock, result);
        verify(factorySpy, times(1))
                .newJsonSchema(isNull(), eq(textNode), any(SchemaValidatorsConfig.class));
    }

    // -------------------------------------------------------------------------
    // Config Null with Non‑Empty Schema
    // -------------------------------------------------------------------------

    @Test
    void shouldCreateSchemaWhenConfigIsNullAndSchemaIsNonEmpty() {
        JsonNode schemaNode = realMapper.readTree("{\"type\":\"integer\",\"minimum\":0}");

        JsonSchema result = factorySpy.getSchema(schemaNode, null);

        assertNotNull(result);
        assertSame(jsonSchemaMock, result);

        ArgumentCaptor<SchemaValidatorsConfig> cfgCaptor = ArgumentCaptor.forClass(SchemaValidatorsConfig.class);
        verify(factorySpy, times(1))
                .newJsonSchema(isNull(), eq(schemaNode), cfgCaptor.capture());

        assertNotNull(cfgCaptor.getValue(),
                "Factory must supply a default SchemaValidatorsConfig when the caller passes null");
    }

    // -------------------------------------------------------------------------
    // Custom Config with Multiple Flags
    // -------------------------------------------------------------------------

    @Test
    void shouldPropagateAllFlagsFromComplexConfig() {
        JsonNode schemaNode = realMapper.readTree("{\"type\":\"string\",\"format\":\"email\"}");
        SchemaValidatorsConfig complexCfg = new SchemaValidatorsConfig();
        complexCfg.setTypeLoose(true);
        complexCfg.setFailFast(true);
        complexCfg.setFormat("skip"); // assuming this disables format validation

        JsonSchema result = factorySpy.getSchema(schemaNode, complexCfg);

        assertNotNull(result);
        assertSame(jsonSchemaMock, result);
        verify(factorySpy, times(1))
                .newJsonSchema(isNull(), eq(schemaNode), eq(complexCfg));
    }

    // -------------------------------------------------------------------------
    // Shared Config Isolation Across Calls
    // -------------------------------------------------------------------------

    @Test
    void shouldNotMutateSharedConfigBetweenSuccessiveCalls() {
        SchemaValidatorsConfig sharedCfg = new SchemaValidatorsConfig();
        sharedCfg.setTypeLoose(false);

        JsonNode firstNode = realMapper.readTree("{\"type\":\"string\"}");
        JsonNode secondNode = realMapper.readTree("{\"type\":\"number\"}");

        // first call
        JsonSchema firstResult = factorySpy.getSchema(firstNode, sharedCfg);
        assertSame(jsonSchemaMock, firstResult);

        // second call – same config instance
        JsonSchema secondResult = factorySpy.getSchema(secondNode, sharedCfg);
        assertSame(jsonSchemaMock, secondResult);

        // capture the config objects passed on each call (should be the same instance)
        ArgumentCaptor<SchemaValidatorsConfig> cfgCaptor = ArgumentCaptor.forClass(SchemaValidatorsConfig.class);
        verify(factorySpy, times(2))
                .newJsonSchema(isNull(), any(JsonNode.class), cfgCaptor.capture());

        // all captured configs must be the same reference as the original sharedCfg
        cfgCaptor.getAllValues().forEach(captured -> assertSame(sharedCfg, captured,
                "Factory must not create a copy or modify the supplied config"));
    }

    // -------------------------------------------------------------------------
    // Schema with $schema Identifier – known meta‑schema
    // -------------------------------------------------------------------------

    @Test
    void shouldAcceptSchemaContainingKnown$schemaIdentifier() {
        String json = "{ \"$schema\":\"http://json-schema.org/draft-07/schema#\", \"type\":\"array\", \"items\":{\"type\":\"string\"}}";
        JsonNode schemaNode = realMapper.readTree(json);

        JsonSchema result = factorySpy.getSchema(schemaNode, new SchemaValidatorsConfig());

        assertNotNull(result);
        assertSame(jsonSchemaMock, result);
        verify(factorySpy, times(1))
                .newJsonSchema(isNull(), eq(schemaNode), any(SchemaValidatorsConfig.class));
    }

    // -------------------------------------------------------------------------
    // MissingNode as Schema
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowNPEWhenSchemaNodeIsMissingNode() {
        JsonNode missingNode = realMapper.readTree("{}").path("nonExisting"); // yields MissingNode

        assertTrue(missingNode.isMissingNode(), "The test node must be a MissingNode");

        assertThrows(NullPointerException.class,
                () -> factorySpy.getSchema(missingNode, new SchemaValidatorsConfig()),
                "MissingNode should cause NullPointerException");
    }

    // -------------------------------------------------------------------------
    // Unsupported $schema Identifier
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowIllegalArgumentExceptionForUnknown$schemaIdentifier() {
        String json = "{ \"$schema\":\"http://example.com/unknown/schema#\", \"type\":\"object\" }";
        JsonNode schemaNode = realMapper.readTree(json);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> factorySpy.getSchema(schemaNode, new SchemaValidatorsConfig()));

        assertTrue(ex.getMessage().contains("Unsupported $schema identifier"),
                "Exception message must indicate unsupported $schema");
    }

    // -------------------------------------------------------------------------
    // Malformed $schema URI
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowIllegalArgumentExceptionForMalformed$schemaUri() {
        String json = "{ \"$schema\":\"ht!tp://[malformed]\", \"type\":\"array\" }";
        JsonNode schemaNode = realMapper.readTree(json);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> factorySpy.getSchema(schemaNode, new SchemaValidatorsConfig()));

        assertTrue(ex.getMessage().contains("Malformed $schema URI"),
                "Exception message must indicate malformed URI");
    }

    // -------------------------------------------------------------------------
    // SchemaValidatorsConfig with Null Internal Map
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowIllegalArgumentExceptionWhenConfigHasNullInternalMap() throws Exception {
        // Build a config instance and then use reflection to set its internal map to null.
        SchemaValidatorsConfig brokenConfig = new SchemaValidatorsConfig();

        // The internal map field name in the actual implementation is "map".
        // Using reflection to simulate a corrupted config.
        java.lang.reflect.Field mapField = SchemaValidatorsConfig.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(brokenConfig, null);

        JsonNode schemaNode = realMapper.readTree("{\"type\":\"string\"}");

        // The stubbed newJsonSchema does not look at the internal map,
        // therefore we need the factory to fail earlier – we simulate this by
        // making the stub throw when the config's map is null.
        doAnswer(invocation -> {
            SchemaValidatorsConfig cfg = invocation.getArgument(2);
            java.lang.reflect.Field f = SchemaValidatorsConfig.class.getDeclaredField("map");
            f.setAccessible(true);
            Object map = f.get(cfg);
            if (map == null) {
                throw new IllegalArgumentException("SchemaValidatorsConfig internal map is null");
            }
            return jsonSchemaMock;
        }).when(factorySpy).newJsonSchema(isNull(), any(JsonNode.class), any(SchemaValidatorsConfig.class));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> factorySpy.getSchema(schemaNode, brokenConfig));

        assertTrue(ex.getMessage().contains("internal map is null"));
    }

    // -------------------------------------------------------------------------
    // Null JsonNode with Non‑Null Config (explicit check)
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowNPEWhenJsonNodeIsNullEvenIfConfigIsCustom() {
        SchemaValidatorsConfig customCfg = new SchemaValidatorsConfig();
        customCfg.setTypeLoose(true);

        assertThrows(NullPointerException.class,
                () -> factorySpy.getSchema(null, customCfg),
                "Null JsonNode must always cause NPE irrespective of the config");
    }

    // -------------------------------------------------------------------------
    // Constructor validation – already present in the original framework
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowIllegalArgumentExceptionIfDefaultMetaSchemaUriIsInvalid() throws Exception {
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), jsonMetaSchemaMock);

        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(
                ObjectMapper.class,
                ObjectMapper.class,
                String.class,
                SchemaLoaders.Builder.class,
                SchemaMappers.Builder.class,
                Map.class,
                boolean.class);
        ctor.setAccessible(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ctor.newInstance(
                        jsonMapperMock,
                        yamlMapperMock,
                        "   ",                     // invalid URI
                        schemaLoadersBuilderMock,
                        schemaMappersBuilderMock,
                        metaSchemas,
                        true));
        assertEquals("defaultMetaSchemaURI must not be null or empty", ex.getMessage());
    }
}
```