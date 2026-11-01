```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.resource.SchemaLoaders;
import com.networknt.schema.resource.SchemaMappers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchemaLocation_Test {

    /** Instance of the class under test created via reflection (its constructor is private). */
    private JsonSchemaFactory factory;

    /** Reflective handle to the protected method we want to test. */
    private Method getSchemaLocationMethod;

    @Mock
    private ValidationContext validationContext;   // mocked to control resolveSchemaId

    @Mock
    private JsonNode schemaNode;                  // the node passed to the method

    @Mock
    private JsonMetaSchema metaSchema;            // needed only for factory construction

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ---- create a JsonSchemaFactory instance ---------------------------------
        // Minimal but valid arguments for the private constructor.
        ObjectMapper jsonMapper = mock(ObjectMapper.class);
        ObjectMapper yamlMapper = mock(ObjectMapper.class);
        String defaultMetaSchemaURI = "http://json-schema.org/draft-07/schema";

        SchemaLoaders.Builder loadersBuilder = new SchemaLoaders.Builder();
        SchemaMappers.Builder mappersBuilder = new SchemaMappers.Builder();

        // The factory expects a map that contains the default meta‑schema.
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(defaultMetaSchemaURI), metaSchema);

        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class
                .getDeclaredConstructor(ObjectMapper.class,
                        ObjectMapper.class,
                        String.class,
                        SchemaLoaders.Builder.class,
                        SchemaMappers.Builder.class,
                        Map.class,
                        boolean.class);
        ctor.setAccessible(true);
        factory = ctor.newInstance(jsonMapper,
                yamlMapper,
                defaultMetaSchemaURI,
                loadersBuilder,
                mappersBuilder,
                metaSchemas,
                false);

        // ---- obtain a reference to the protected method ---------------------------
        getSchemaLocationMethod = JsonSchemaFactory.class
                .getDeclaredMethod("getSchemaLocation",
                        SchemaLocation.class,
                        JsonNode.class,
                        ValidationContext.class);
        getSchemaLocationMethod.setAccessible(true);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * Helper that invokes the protected {@code getSchemaLocation} method via reflection.
     */
    private SchemaLocation invokeGetSchemaLocation(SchemaLocation uri,
                                                   JsonNode node,
                                                   ValidationContext ctx) throws Exception {
        return (SchemaLocation) getSchemaLocationMethod.invoke(factory, uri, node, ctx);
    }

    @Test
    void whenSchemaIdResolved_thenReturnedLocationMatchesResolvedId() throws Exception {
        // Arrange
        String resolvedId = "http://example.com/my-schema.json";
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn(resolvedId);
        // schemaRetrievalUri is irrelevant when a resolved id exists
        SchemaLocation schemaRetrievalUri = null;

        // Act
        SchemaLocation result = invokeGetSchemaLocation(schemaRetrievalUri, schemaNode, validationContext);

        // Assert
        assertNotNull(result);
        // SchemaLocation.of(String) creates an instance whose string representation is the id.
        assertEquals(resolvedId, result.toString(),
                "The returned SchemaLocation should be based on the resolved schema id");
    }

    @Test
    void whenSchemaIdNotResolved_andRetrievalUriProvided_thenReturnedLocationIsRetrievalUri() throws Exception {
        // Arrange
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn(null);
        SchemaLocation retrievalUri = SchemaLocation.of("http://example.com/uri-schema.json");

        // Act
        SchemaLocation result = invokeGetSchemaLocation(retrievalUri, schemaNode, validationContext);

        // Assert
        assertNotNull(result);
        assertEquals(retrievalUri.toString(), result.toString(),
                "When no schema id is resolved, the method should fall back to the supplied SchemaLocation");
    }

    @Test
    void whenNeitherSchemaIdNorRetrievalUri_thenDocumentLocationReturned() throws Exception {
        // Arrange
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn(null);
        SchemaLocation retrievalUri = null;

        // Act
        SchemaLocation result = invokeGetSchemaLocation(retrievalUri, schemaNode, validationContext);

        // Assert
        assertNotNull(result);
        assertSame(SchemaLocation.DOCUMENT, result,
                "If both resolved id and retrieval URI are null the method must return SchemaLocation.DOCUMENT");
    }
}
```