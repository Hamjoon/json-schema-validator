```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.resource.SchemaLoaders;
import com.networknt.schema.resource.SchemaMappers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for {@link JsonSchemaFactory#getSchema(URI)}.
 *
 * The test creates a real {@link JsonSchemaFactory} instance with the minimal
 * required constructor arguments and then spies on it to stub the protected
 * {@code getMappedSchema} method. This allows us to focus on the public
 * {@code getSchema(URI)} overload without exercising the whole schema loading
 * machinery.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_6_Test {

    /** Mocks for the constructor arguments */
    @Mock
    private ObjectMapper jsonMapperMock;
    @Mock
    private ObjectMapper yamlMapperMock;
    @Mock
    private JsonMetaSchema defaultMetaSchemaMock;

    /** The factory under test (a spy so we can stub protected methods) */
    private JsonSchemaFactory factorySpy;

    /** A dummy {@link JsonSchema} that the stubbed method will return */
    private JsonSchema dummySchema;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for this test class.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build the minimal meta‑schema map required by the constructor.
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
        // The default meta‑schema URI must be present in the map after normalisation.
        String defaultMetaSchemaUri = "http://example.com/meta-schema";
        metaSchemas.put(defaultMetaSchemaUri, defaultMetaSchemaMock);

        // Create a real factory instance with the mocked mappers and the minimal map.
        JsonSchemaFactory realFactory = new JsonSchemaFactory(
                jsonMapperMock,
                yamlMapperMock,
                defaultMetaSchemaUri,
                new SchemaLoaders.Builder(),
                new SchemaMappers.Builder(),
                metaSchemas,
                false // URI schema cache disabled for simplicity
        );

        // Spy on the real instance so we can stub protected methods.
        factorySpy = Mockito.spy(realFactory);

        // Prepare a dummy JsonSchema that will be returned by the stubbed method.
        dummySchema = Mockito.mock(JsonSchema.class);

        // Stub the protected getMappedSchema(...) method which is ultimately used
        // by the public getSchema(URI) overload.
        Mockito.doReturn(dummySchema)
                .when(factorySpy)
                .getMappedSchema(any(SchemaLocation.class), any(SchemaValidatorsConfig.class));
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the spy to avoid cross‑test interference.
        Mockito.reset(factorySpy);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    @Test
    void shouldReturnMappedSchemaWhenCallingGetSchemaWithUri() throws Exception {
        // GIVEN
        URI schemaUri = new URI("http://example.com/schema.json");

        // WHEN
        JsonSchema result = factorySpy.getSchema(schemaUri);

        // THEN
        assertNotNull(result, "Resulting JsonSchema should not be null");
        assertSame(dummySchema, result, "The factory should return the stubbed JsonSchema instance");
    }
}
```