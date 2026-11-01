```java
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

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Prepare a minimal meta‑schema map required by the constructor
        Map<String, JsonMetaSchema> metaMap = new HashMap<>();
        metaMap.put(DEFAULT_META_SCHEMA_URI, metaSchemaMock);

        // Build the factory with real builders (they have no state) and the mocked mappers
        JsonSchemaFactory realFactory = new JsonSchemaFactory(
                jsonMapperMock,
                yamlMapperMock,
                DEFAULT_META_SCHEMA_URI,
                new SchemaLoaders.Builder(),
                new SchemaMappers.Builder(),
                metaMap,
                false);

        // Spy the factory so we can stub the protected newJsonSchema method
        factory = spy(realFactory);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference
        reset(jsonMapperMock, yamlMapperMock, metaSchemaMock);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up
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
        JsonSchemaException thrown = assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(SIMPLE_SCHEMA, config),
                "An IOException while reading the schema should be wrapped in JsonSchemaException");

        assertSame(ioException, thrown.getCause(),
                "The cause of the JsonSchemaException should be the original IOException");
        verify(jsonMapperMock, times(1)).readTree(eq(SIMPLE_SCHEMA));
    }
}
```