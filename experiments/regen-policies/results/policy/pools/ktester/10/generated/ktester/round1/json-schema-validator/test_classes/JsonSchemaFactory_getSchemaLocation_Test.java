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

    /**
     * Instance of the class under test created via reflection (its constructor is private).
     */
    private JsonSchemaFactory factory;

    /**
     * Reflective handle to the protected method we want to test.
     */
    private Method getSchemaLocationMethod;

    @Mock
    private ValidationContext // mocked to control resolveSchemaId
    validationContext;

    @Mock
    private JsonNode // the node passed to the method
    schemaNode;

    @Mock
    private JsonMetaSchema // needed only for factory construction
    metaSchema;

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
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, ObjectMapper.class, String.class, SchemaLoaders.Builder.class, SchemaMappers.Builder.class, Map.class, boolean.class);
        ctor.setAccessible(true);
        factory = ctor.newInstance(jsonMapper, yamlMapper, defaultMetaSchemaURI, loadersBuilder, mappersBuilder, metaSchemas, false);
        // ---- obtain a reference to the protected method ---------------------------
        getSchemaLocationMethod = JsonSchemaFactory.class.getDeclaredMethod("getSchemaLocation", SchemaLocation.class, JsonNode.class, ValidationContext.class);
        getSchemaLocationMethod.setAccessible(true);
        // Minimal but valid arguments for the private constructor.
        ObjectMapper jsonMapper = mock(ObjectMapper.class);
        ObjectMapper yamlMapper = mock(ObjectMapper.class);
        String defaultMetaSchemaURI = "http://json-schema.org/draft-07/schema";
        SchemaLoaders.Builder loadersBuilder = new SchemaLoaders.Builder();
        SchemaMappers.Builder mappersBuilder = new SchemaMappers.Builder();
        // The factory expects a map that contains the default meta‑schema.
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(defaultMetaSchemaURI), metaSchema);
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, ObjectMapper.class, String.class, SchemaLoaders.Builder.class, SchemaMappers.Builder.class, Map.class, boolean.class);
        ctor.setAccessible(true);
        factory = ctor.newInstance(jsonMapper, yamlMapper, defaultMetaSchemaURI, loadersBuilder, mappersBuilder, metaSchemas, false);
        // ---- obtain a reference to the protected method ---------------------------
        getSchemaLocationMethod = JsonSchemaFactory.class.getDeclaredMethod("getSchemaLocation", SchemaLocation.class, JsonNode.class, ValidationContext.class);
        getSchemaLocationMethod.setAccessible(true);
    }

    /**
     * Helper that invokes the protected {@code getSchemaLocation} method via reflection.
     */
    private SchemaLocation invokeGetSchemaLocation(SchemaLocation uri, JsonNode node, ValidationContext ctx) throws Exception {
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
        assertEquals(resolvedId, result.toString(), "The returned SchemaLocation should be based on the resolved schema id");
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
        assertEquals(retrievalUri.toString(), result.toString(), "When no schema id is resolved, the method should fall back to the supplied SchemaLocation");
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
        assertSame(SchemaLocation.DOCUMENT, result, "If both resolved id and retrieval URI are null the method must return SchemaLocation.DOCUMENT");
    }

    @Test
    void whenBothResolvedIdAndRetrievalUriProvided_resolvedIdTakesPrecedence() throws Exception {
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn("http://example.com/schema");
        SchemaLocation retrievalUri = SchemaLocation.of("http://fallback.com/schema");
        SchemaLocation result = invokeGetSchemaLocation(retrievalUri, schemaNode, validationContext);
        assertNotNull(result);
        assertEquals("http://example.com/schema", result.toString(), "Resolved $id must win over the retrieval URI");
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
        assertEquals("", result.toString(), "Empty string resolved ID should be accepted and returned");
    }

    @Test
    void whenRetrievalUriIsEmptyString_andResolvedIdIsNull_thenReturnedLocationIsEmpty() throws Exception {
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn(null);
        SchemaLocation emptyUri = SchemaLocation.of("");
        SchemaLocation result = invokeGetSchemaLocation(emptyUri, schemaNode, validationContext);
        assertNotNull(result);
        assertEquals("", result.toString(), "Empty string fallback URI should be returned when no $id");
    }

    @Test
    void whenResolvedIdIsEmptyString_andRetrievalUriProvided_thenResolvedIdWins() throws Exception {
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn("");
        SchemaLocation fallback = SchemaLocation.of("http://fallback.com/schema");
        SchemaLocation result = invokeGetSchemaLocation(fallback, schemaNode, validationContext);
        assertNotNull(result);
        assertEquals("", result.toString(), "Resolved empty string must take precedence over non‑empty fallback");
    }

    @Test
    void whenValidationContextIsNull_thenNullPointerExceptionIsThrown() {
        // The method dereferences the ValidationContext, so a NPE is expected.
        assertThrows(NullPointerException.class, () -> invokeGetSchemaLocation(null, schemaNode, null));
    }

    @Test
    void whenValidationContextThrowsException_thenExceptionPropagates() {
        when(validationContext.resolveSchemaId(schemaNode)).thenThrow(new IllegalStateException("boom"));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> invokeGetSchemaLocation(null, schemaNode, validationContext));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void whenResolvedIdIsMalformedUri_thenIllegalArgumentExceptionFromSchemaLocation() {
        // SchemaLocation.of validates the URI and throws IllegalArgumentException.
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn("http://[invalid]");
        assertThrows(IllegalArgumentException.class, () -> invokeGetSchemaLocation(null, schemaNode, validationContext));
    }

    @Test
    void whenRetrievalUriIsMalformed_andResolvedIdIsNull_thenIllegalArgumentExceptionFromSchemaLocation() {
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn(null);
        // Construct a SchemaLocation with an invalid IRI – this will trigger the exception
        // when the factory method calls SchemaLocation.of on its string representation.
        SchemaLocation badUri = SchemaLocation.of("ht!tp://bad");
        assertThrows(IllegalArgumentException.class, () -> invokeGetSchemaLocation(badUri, schemaNode, validationContext));
    }
}
