package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.resource.SchemaLoaders;
import com.networknt.schema.resource.SchemaMappers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_Test {

    // Mocks for constructor dependencies
    @Mock
    ObjectMapper jsonMapperMock;

    @Mock
    ObjectMapper yamlMapperMock;

    @Mock
    JsonMetaSchema metaSchemaMock;

    // The factory under test (spied so we can stub protected methods)
    JsonSchemaFactory factory;

    // Common test data
    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema#";

    private static final String SIMPLE_SCHEMA = "{\"type\":\"object\"}";

    @BeforeEach
    void setupBeforeEach() {
        // Prepare a minimal meta‑schema map required by the constructor
        Map<String, JsonMetaSchema> metaMap = new HashMap<>();
        metaMap.put(DEFAULT_META_SCHEMA_URI, metaSchemaMock);
        // Build the factory with real builders (they have no state) and the mocked mappers
        JsonSchemaFactory realFactory = new JsonSchemaFactory(jsonMapperMock, yamlMapperMock, DEFAULT_META_SCHEMA_URI, new SchemaLoaders.Builder(), new SchemaMappers.Builder(), metaMap, false);
        // Spy the factory so we can stub the protected newJsonSchema method
        factory = spy(realFactory);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference
        reset(jsonMapperMock, yamlMapperMock, metaSchemaMock);
    }

    private static Stream<Arguments> provideValidSchemas() {
        SchemaValidatorsConfig failFast = new SchemaValidatorsConfig();
        failFast.setFailFast(true);
        return Stream.of(// simple object schema, non‑null config
        Arguments.of("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}", new SchemaValidatorsConfig()), // empty object, null config
        Arguments.of("{}", null), // array schema, null config
        Arguments.of("{\"type\":\"array\",\"items\":{\"type\":\"string\"}}", null), // fail‑fast config, simple object schema
        Arguments.of("{\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}}", failFast));
    }

    private static Stream<Arguments> provideMalformedSchemas() {
        return Stream.of(// missing closing brace
        Arguments.of("{invalid json"), // whitespace only
        Arguments.of("   "), // empty string
        Arguments.of(""));
    }

    @Test
    void testGetSchemaReturnsJsonSchema() throws Exception {
        // GIVEN
        JsonNode parsedNode = mock(JsonNode.class);
        JsonSchema expectedSchema = mock(JsonSchema.class);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // jsonMapper.readTree should return the parsed JsonNode
        when(jsonMapperMock.readTree(eq(SIMPLE_SCHEMA))).thenReturn(parsedNode);
        // newJsonSchema (protected) should return the mocked JsonSchema
        doReturn(expectedSchema).when(factory).newJsonSchema(isNull(), eq(parsedNode), any());
        // WHEN
        JsonSchema actual = factory.getSchema(SIMPLE_SCHEMA, config);
        // THEN
        assertSame(expectedSchema, actual, "Factory should return the JsonSchema produced by newJsonSchema");
        // Verify interactions
        verify(jsonMapperMock, times(1)).readTree(eq(SIMPLE_SCHEMA));
        verify(factory, times(1)).newJsonSchema(isNull(), eq(parsedNode), eq(config));
    }

    @Test
    void testGetSchemaIOExceptionThrowsJsonSchemaException() throws Exception {
        // GIVEN
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        IOException ioException = new IOException("boom");
        // Simulate a failure while parsing the schema string
        when(jsonMapperMock.readTree(anyString())).thenThrow(ioException);
        // WHEN / THEN
        JsonSchemaException thrown = assertThrows(JsonSchemaException.class, () -> factory.getSchema(SIMPLE_SCHEMA, config), "An IOException while reading the schema should be wrapped in JsonSchemaException");
        assertSame(ioException, thrown.getCause(), "The cause of the JsonSchemaException should be the original IOException");
        verify(jsonMapperMock, times(1)).readTree(eq(SIMPLE_SCHEMA));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] schema={0}, configNull={1}")
    @MethodSource("provideValidSchemas")
    void testGetSchemaValidInputs(String schema, SchemaValidatorsConfig config) throws Exception {
        // GIVEN
        JsonNode parsedNode = mock(JsonNode.class);
        JsonSchema expectedSchema = mock(JsonSchema.class);
        when(jsonMapperMock.readTree(eq(schema))).thenReturn(parsedNode);
        doReturn(expectedSchema).when(factory).newJsonSchema(isNull(), eq(parsedNode), any());
        // WHEN
        JsonSchema actual = factory.getSchema(schema, config);
        // THEN
        assertSame(expectedSchema, actual, "Factory should return the JsonSchema created by newJsonSchema");
        verify(jsonMapperMock, times(1)).readTree(eq(schema));
        // Verify that the exact config instance (or null) is forwarded
        verify(factory, times(1)).newJsonSchema(isNull(), eq(parsedNode), eq(config));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] malformed schema={0}")
    @MethodSource("provideMalformedSchemas")
    void testGetSchemaMalformedJsonThrowsJsonSchemaException(String schema) throws Exception {
        // GIVEN
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        IOException ioe = new IOException("Invalid JSON");
        when(jsonMapperMock.readTree(eq(schema))).thenThrow(ioe);
        // WHEN / THEN
        JsonSchemaException thrown = assertThrows(JsonSchemaException.class, () -> factory.getSchema(schema, config), "Malformed JSON should be wrapped in JsonSchemaException");
        assertSame(ioe, thrown.getCause(), "The cause of the JsonSchemaException must be the original IOException");
        verify(jsonMapperMock, times(1)).readTree(eq(schema));
    }

    // -----------------------------------------------------------------
    @Test
    void testGetSchemaNullSchemaArgumentPropagatesIllegalArgumentException() throws Exception {
        // GIVEN
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        when(jsonMapperMock.readTree(isNull())).thenThrow(new IllegalArgumentException("schema is null"));
        // WHEN / THEN
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> factory.getSchema(null, config), "A null schema string should cause IllegalArgumentException to bubble up");
        assertEquals("schema is null", thrown.getMessage());
        verify(jsonMapperMock, times(1)).readTree(isNull());
    }

    // -----------------------------------------------------------------
    @Test
    void testGetSchemaLiteralNullJsonNodeThrowsIllegalArgumentException() throws Exception {
        // GIVEN
        // JSON literal null
        String schema = "null";
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        JsonNode nullNode = NullNode.getInstance();
        when(jsonMapperMock.readTree(eq(schema))).thenReturn(nullNode);
        // newJsonSchema should reject NullNode and throw IllegalArgumentException
        doThrow(new IllegalArgumentException("NullNode not allowed")).when(factory).newJsonSchema(isNull(), eq(nullNode), any());
        // WHEN / THEN
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> factory.getSchema(schema, config), "Literal JSON null should result in IllegalArgumentException from newJsonSchema");
        assertEquals("NullNode not allowed", thrown.getMessage());
        verify(jsonMapperMock, times(1)).readTree(eq(schema));
        verify(factory, times(1)).newJsonSchema(isNull(), eq(nullNode), eq(config));
    }

    // -----------------------------------------------------------------
    @Test
    void testGetSchemaUnknownMetaSchemaThrowsJsonSchemaException() throws Exception {
        // GIVEN
        String schema = "{\"$schema\":\"http://unknown/meta/schema\",\"type\":\"object\"}";
        SchemaValidatorsConfig config = null;
        JsonNode parsedNode = mock(JsonNode.class);
        JsonSchemaException unknownMeta = new JsonSchemaException("Unknown meta schema");
        when(jsonMapperMock.readTree(eq(schema))).thenReturn(parsedNode);
        doThrow(unknownMeta).when(factory).newJsonSchema(isNull(), eq(parsedNode), any());
        // WHEN / THEN
        JsonSchemaException thrown = assertThrows(JsonSchemaException.class, () -> factory.getSchema(schema, config), "Unknown meta‑schema should propagate JsonSchemaException");
        assertSame(unknownMeta, thrown, "The thrown exception must be the one from newJsonSchema");
        verify(jsonMapperMock, times(1)).readTree(eq(schema));
        verify(factory, times(1)).newJsonSchema(isNull(), eq(parsedNode), eq(config));
    }

    // -----------------------------------------------------------------
    @Test
    void testGetSchemaWhenJsonMapperIsNullThrowsNPE() {
        // GIVEN – subclass that overrides getJsonMapper() to return null
        JsonSchemaFactory factoryWithNullMapper = new JsonSchemaFactory(// not used, but constructor required
        jsonMapperMock, yamlMapperMock, DEFAULT_META_SCHEMA_URI, new SchemaLoaders.Builder(), new SchemaMappers.Builder(), Map.of(DEFAULT_META_SCHEMA_URI, metaSchemaMock), false) {

            @Override
            protected ObjectMapper getJsonMapper() {
                // force NPE inside getSchema
                return null;
            }
        };
        String schema = "{}";
        SchemaValidatorsConfig config = null;
        // WHEN / THEN
        NullPointerException npe = assertThrows(NullPointerException.class, () -> factoryWithNullMapper.getSchema(schema, config), "If getJsonMapper() returns null, a NullPointerException should be thrown");
        // The exact message is not guaranteed, but we ensure the exception type
        assertNotNull(npe);
    }

    // -----------------------------------------------------------------
    @Test
    void testGetSchemaConfigIsPassedToNewJsonSchema() throws Exception {
        // GIVEN
        String schema = "{\"type\":\"object\"}";
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // non‑default flag
        config.setFailFast(true);
        JsonNode parsedNode = mock(JsonNode.class);
        JsonSchema expectedSchema = mock(JsonSchema.class);
        when(jsonMapperMock.readTree(eq(schema))).thenReturn(parsedNode);
        doReturn(expectedSchema).when(factory).newJsonSchema(isNull(), eq(parsedNode), eq(config));
        // WHEN
        JsonSchema actual = factory.getSchema(schema, config);
        // THEN
        assertSame(expectedSchema, actual);
        verify(factory, times(1)).newJsonSchema(isNull(), eq(parsedNode), eq(config));
    }
}
