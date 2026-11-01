package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getRefSchema_Test {

    private ValidationContext validationContext;

    private SchemaLocation schemaLocation;

    private JsonNodePath evaluationPath;

    private JsonSchema realSchema;

    private JsonSchema spySchema;

    private ObjectMapper mapper;

    private JsonNode schemaNode;

    @BeforeEach
    void setupBeforeEach() {
        // Minimal empty schema node.
        mapper = new ObjectMapper();
        schemaNode = mapper.createObjectNode();
        // Build a ValidationContext using the default meta‑schema and a fresh config.
        JsonMetaSchema metaSchema = JsonMetaSchema.getV202012();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        validationContext = new ValidationContext(metaSchema, factory, config);
        // Absolute IRI for the root schema.
        schemaLocation = new SchemaLocation(new AbsoluteIri("http://example.org/schema.json"));
        // Evaluation path – root with JSON_POINTER type.
        evaluationPath = new JsonNodePath(PathType.JSON_POINTER);
        // Create a JsonSchema instance (package‑private factory method).
        realSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
        // Spy so that we can stub getSubSchema for the JSON‑Pointer test.
        spySchema = spy(realSchema);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the maps to avoid interference between tests.
        validationContext.getSchemaResources().clear();
        validationContext.getDynamicAnchors().clear();
    }

    /* --------------------------------------------------------------
     *  Helper method to set private fields via reflection.
     * -------------------------------------------------------------- */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = null;
        Class<?> clazz = target.getClass();
        // Walk up the class hierarchy to find the field.
        while (clazz != null) {
            try {
                field = clazz.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        if (field == null) {
            throw new IllegalStateException("Field '" + fieldName + "' not found in class hierarchy");
        }
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void testGetRefSchemaWithJsonPointer() {
        // Fragment with JSON_POINTER type – should delegate to getSubSchema().
        JsonNodePath fragment = new JsonNodePath(PathType.JSON_POINTER);
        JsonSchema expectedSubSchema = mock(JsonSchema.class);
        doReturn(expectedSubSchema).when(spySchema).getSubSchema(fragment);
        JsonSchema result = spySchema.getRefSchema(fragment);
        assertSame(expectedSubSchema, result, "When the fragment is a JSON pointer the method must return the sub‑schema");
    }

    @Test
    void testGetRefSchemaWithAnchorFoundInResources() {
        // Anchor fragment (non‑JSON_POINTER) that exists in schemaResources.
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.JSON_PATH);
        when(fragment.toString()).thenReturn("myAnchor");
        String anchorKey = schemaLocation.getAbsoluteIri().toString() + "#myAnchor";
        JsonSchema expectedAnchorSchema = mock(JsonSchema.class);
        validationContext.getSchemaResources().put(anchorKey, expectedAnchorSchema);
        JsonSchema result = spySchema.getRefSchema(fragment);
        assertSame(expectedAnchorSchema, result, "The method must return the schema stored in schemaResources for the anchor");
    }

    @Test
    void testGetRefSchemaWithAnchorFoundInDynamicAnchors() {
        // Anchor fragment that is only present in dynamicAnchors.
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.JSON_PATH);
        when(fragment.toString()).thenReturn("dynAnchor");
        String anchorKey = schemaLocation.getAbsoluteIri().toString() + "#dynAnchor";
        JsonSchema expectedAnchorSchema = mock(JsonSchema.class);
        // Ensure the key is not in schemaResources.
        validationContext.getSchemaResources().remove(anchorKey);
        validationContext.getDynamicAnchors().put(anchorKey, expectedAnchorSchema);
        JsonSchema result = spySchema.getRefSchema(fragment);
        assertSame(expectedAnchorSchema, result, "If the anchor is not in schemaResources it must be looked up in dynamicAnchors");
    }

    @Test
    void testGetRefSchemaWithAnchorNotFoundThrowsException() {
        // Anchor fragment that cannot be resolved.
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.JSON_PATH);
        when(fragment.toString()).thenReturn("missingAnchor");
        String anchorKey = schemaLocation.getAbsoluteIri().toString() + "#missingAnchor";
        // Ensure both maps are empty for this key.
        validationContext.getSchemaResources().remove(anchorKey);
        validationContext.getDynamicAnchors().remove(anchorKey);
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> spySchema.getRefSchema(fragment), "An unknown anchor must cause a JsonSchemaException");
        assertEquals("Unable to find anchor " + anchorKey, ex.getMessage(), "Exception message should contain the missing anchor identifier");
    }

    @Test
    void testGetRefSchemaJsonPointerSubSchemaReturnsNull() {
        // Stub getSubSchema to return null and verify the method propagates null.
        JsonNodePath fragment = new JsonNodePath(PathType.JSON_POINTER);
        doReturn(null).when(spySchema).getSubSchema(fragment);
        JsonSchema result = spySchema.getRefSchema(fragment);
        assertNull(result, "When getSubSchema returns null the method must propagate null");
        // Verify getSubSchema was called exactly once.
        verify(spySchema, times(1)).getSubSchema(fragment);
    }

    @Test
    void testGetRefSchemaWithNullFragmentThrowsNPE() {
        // Passing a null fragment should result in a NullPointerException.
        assertThrows(NullPointerException.class, () -> spySchema.getRefSchema(null), "Calling getRefSchema with a null fragment must throw NPE");
    }

    @Test
    void testGetRefSchemaWithNullSchemaLocationThrowsNPE() throws Exception {
        // Create a schema with a valid location then set it to null via reflection.
        JsonSchema schemaWithNullLocation = spy(realSchema);
        setPrivateField(schemaWithNullLocation, "schemaLocation", null);
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.JSON_PATH);
        when(fragment.toString()).thenReturn("anyAnchor");
        // The method should attempt to call getSchemaLocation().getAbsoluteIri()
        // and therefore throw NPE.
        assertThrows(NullPointerException.class, () -> schemaWithNullLocation.getRefSchema(fragment), "If schemaLocation is null a NPE should be thrown before any lookup");
    }

    @Test
    void testGetRefSchemaWithNullValidationContextThrowsNPE() throws Exception {
        // Set the validationContext field to null via reflection.
        JsonSchema schemaWithNullContext = spy(realSchema);
        setPrivateField(schemaWithNullContext, "validationContext", null);
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.JSON_PATH);
        when(fragment.toString()).thenReturn("anyAnchor");
        assertThrows(NullPointerException.class, () -> schemaWithNullContext.getRefSchema(fragment), "If validationContext is null a NPE should be thrown when accessing it");
    }

    @Test
    void testGetRefSchemaWithNullSchemaResourcesMapThrowsNPE() {
        // Mock ValidationContext to return null for getSchemaResources().
        ValidationContext mockContext = mock(ValidationContext.class);
        when(mockContext.getSchemaResources()).thenReturn(null);
        when(mockContext.getDynamicAnchors()).thenReturn(Collections.emptyMap());
        JsonSchema schema = JsonSchema.from(mockContext, schemaLocation, evaluationPath, schemaNode, null, false);
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.JSON_PATH);
        when(fragment.toString()).thenReturn("anyAnchor");
        assertThrows(NullPointerException.class, () -> schema.getRefSchema(fragment), "A null schemaResources map must cause a NPE on .get(anchor)");
    }

    @Test
    void testGetRefSchemaWithNullDynamicAnchorsMapThrowsNPE() {
        // Mock ValidationContext to return a valid map for resources but null for dynamicAnchors.
        ValidationContext mockContext = mock(ValidationContext.class);
        when(mockContext.getSchemaResources()).thenReturn(Collections.emptyMap());
        when(mockContext.getDynamicAnchors()).thenReturn(null);
        JsonSchema schema = JsonSchema.from(mockContext, schemaLocation, evaluationPath, schemaNode, null, false);
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.JSON_PATH);
        when(fragment.toString()).thenReturn("anyAnchor");
        assertThrows(NullPointerException.class, () -> schema.getRefSchema(fragment), "A null dynamicAnchors map must cause a NPE on fallback lookup");
    }
}
