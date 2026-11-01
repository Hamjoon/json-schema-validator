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
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the protected {@code JsonSchemaFactory.getSchemaLocation}
 * method. The method is exercised via reflection because it is not public.
 *
 * The test suite covers all scenarios described in the specification JSON:
 * <ul>
 *   <li>resolved $id takes precedence over the supplied retrieval URI</li>
 *   <li>fallback to retrieval URI when $id is {@code null}</li>
 *   <li>return {@link SchemaLocation#DOCUMENT} when both are {@code null}</li>
 *   <li>handling of empty strings, malformed URIs and exceptional
 *       {@link ValidationContext} behaviour.</li>
 * </ul>
 */
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

    @BeforeEach
    void setup() throws Exception {
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

    /**
     * Helper that invokes the protected {@code getSchemaLocation} method via reflection.
     */
    private SchemaLocation invokeGetSchemaLocation(SchemaLocation uri,
                                                  JsonNode node,
                                                  ValidationContext ctx) throws Exception {
        return (SchemaLocation) getSchemaLocationMethod.invoke(factory, uri, node, ctx);
    }

    /* -------------------------------------------------------------------------
     *  Positive scenarios – method returns a proper SchemaLocation
     * ---------------------------------------------------------------------- */

    @Test
    void whenSchemaIdResolved_thenReturnedLocationMatchesResolvedId() throws Exception {
        // ValidationContext resolves a non‑null $id.
        String resolvedId = "http://example.com/my-schema.json";
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn(resolvedId);

        SchemaLocation result = invokeGetSchemaLocation(null, schemaNode, validationContext);

        assertNotNull(result);
        assertEquals(resolvedId, result.toString(),
                "Returned SchemaLocation should be based on the resolved $id");
    }

    @Test
    void whenSchemaIdNotResolved_andRetrievalUriProvided_thenReturnedLocationIsRetrievalUri() throws Exception {
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn(null);
        SchemaLocation retrievalUri = SchemaLocation.of("http://fallback.com/schema");

        SchemaLocation result = invokeGetSchemaLocation(retrievalUri, schemaNode, validationContext);

        assertNotNull(result);
        assertEquals(retrievalUri.toString(), result.toString(),
                "Should fall back to the supplied retrieval URI when $id is null");
    }

    @Test
    void whenNeitherSchemaIdNorRetrievalUri_thenDocumentLocationReturned() throws Exception {
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn(null);

        SchemaLocation result = invokeGetSchemaLocation(null, schemaNode, validationContext);

        assertNotNull(result);
        assertSame(SchemaLocation.DOCUMENT, result,
                "Both null -> should return SchemaLocation.DOCUMENT");
    }

    @Test
    void whenBothResolvedIdAndRetrievalUriProvided_resolvedIdTakesPrecedence() throws Exception {
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn("http://example.com/schema");
        SchemaLocation retrievalUri = SchemaLocation.of("http://fallback.com/schema");

        SchemaLocation result = invokeGetSchemaLocation(retrievalUri, schemaNode, validationContext);

        assertNotNull(result);
        assertEquals("http://example.com/schema", result.toString(),
                "Resolved $id must win over the retrieval URI");
    }

    @Test
    void whenSchemaNodeAndContextReturnNull_thenDocumentLocationReturned() throws Exception {
        // schemaNode is null, validationContext returns null for that argument.
        when(validationContext.resolveSchemaId(null)).thenReturn(null);

        SchemaLocation result = invokeGetSchemaLocation(null, null, validationContext);

        assertNotNull(result);
        assertSame(SchemaLocation.DOCUMENT, result);
    }

    @Test
    void whenResolvedIdIsEmptyString_thenReturnedLocationIsEmpty() throws Exception {
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn("");

        SchemaLocation result = invokeGetSchemaLocation(null, schemaNode, validationContext);

        assertNotNull(result);
        assertEquals("", result.toString(),
                "Empty string resolved ID should be accepted and returned");
    }

    @Test
    void whenRetrievalUriIsEmptyString_andResolvedIdIsNull_thenReturnedLocationIsEmpty() throws Exception {
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn(null);
        SchemaLocation emptyUri = SchemaLocation.of("");

        SchemaLocation result = invokeGetSchemaLocation(emptyUri, schemaNode, validationContext);

        assertNotNull(result);
        assertEquals("", result.toString(),
                "Empty string fallback URI should be returned when no $id");
    }

    @Test
    void whenResolvedIdIsEmptyString_andRetrievalUriProvided_thenResolvedIdWins() throws Exception {
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn("");
        SchemaLocation fallback = SchemaLocation.of("http://fallback.com/schema");

        SchemaLocation result = invokeGetSchemaLocation(fallback, schemaNode, validationContext);

        assertNotNull(result);
        assertEquals("", result.toString(),
                "Resolved empty string must take precedence over non‑empty fallback");
    }

    /* -------------------------------------------------------------------------
     *  Negative / exceptional scenarios
     * ---------------------------------------------------------------------- */

    @Test
    void whenValidationContextIsNull_thenNullPointerExceptionIsThrown() {
        // The method dereferences the ValidationContext, so a NPE is expected.
        assertThrows(NullPointerException.class, () ->
                invokeGetSchemaLocation(null, schemaNode, null));
    }

    @Test
    void whenValidationContextThrowsException_thenExceptionPropagates() {
        when(validationContext.resolveSchemaId(schemaNode))
                .thenThrow(new IllegalStateException("boom"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                invokeGetSchemaLocation(null, schemaNode, validationContext));

        assertEquals("boom", ex.getMessage());
    }

    @Test
    void whenResolvedIdIsMalformedUri_thenIllegalArgumentExceptionFromSchemaLocation() {
        // SchemaLocation.of validates the URI and throws IllegalArgumentException.
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn("http://[invalid]");

        assertThrows(IllegalArgumentException.class, () ->
                invokeGetSchemaLocation(null, schemaNode, validationContext));
    }

    @Test
    void whenRetrievalUriIsMalformed_andResolvedIdIsNull_thenIllegalArgumentExceptionFromSchemaLocation() {
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn(null);
        // Construct a SchemaLocation with an invalid IRI – this will trigger the exception
        // when the factory method calls SchemaLocation.of on its string representation.
        SchemaLocation badUri = SchemaLocation.of("ht!tp://bad");

        assertThrows(IllegalArgumentException.class, () ->
                invokeGetSchemaLocation(badUri, schemaNode, validationContext));
    }
}
```