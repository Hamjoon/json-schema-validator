```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;

/**
 * Unit tests for {@link JsonSchema#getRefSchema(JsonNodePath)}.
 *
 * The original test used Mockito's {@code MockitoExtension}, but Mockito is not a
 * dependency of this project. Since the tests do not rely on any Mockito features,
 * the extension has been removed and the corresponding import eliminated.
 *
 * Additionally, the helper method {@code setPrivateField} originally declared a
 * checked {@code Exception}. Some test methods invoked it without declaring a
 * {@code throws} clause, which caused compilation failures. The method now
 * handles reflection exceptions internally and re‑throws them as unchecked
 * {@link RuntimeException}s.
 */
class JsonSchema_getRefSchema_Test {

    private ValidationContext validationContext;
    private SchemaLocation schemaLocation;
    private JsonNodePath evaluationPath;
    private JsonSchema realSchema;
    private ObjectMapper mapper;
    private JsonNode schemaNode;

    /**
     * Simple concrete implementation of {@link JsonNodePath} that allows us
     * to control the {@code toString()} representation used when building an
     * anchor key.
     */
    private static final class SimpleJsonNodePath extends JsonNodePath {

        private final String representation;

        SimpleJsonNodePath(PathType type, String representation) {
            super(type);
            this.representation = representation;
        }

        @Override
        public String toString() {
            return representation;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Minimal empty schema node.
        mapper = new ObjectMapper();
        schemaNode = mapper.createObjectNode();

        // Build a ValidationContext using the default meta‑schema and a fresh config.
        JsonMetaSchema metaSchema = JsonMetaSchema.getV202012();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        validationContext = new ValidationContext(metaSchema, factory, config);

        // Absolute IRI for the root schema.
        schemaLocation = new SchemaLocation(new AbsoluteIri("http://example.org/schema.json"));

        // Evaluation path – root with JSON_POINTER type.
        evaluationPath = new JsonNodePath(PathType.JSON_POINTER);

        // Create a JsonSchema instance (package‑private factory method).
        realSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
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
    private static void setPrivateField(Object target, String fieldName, Object value) {
        try {
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
        } catch (Exception e) {
            throw new RuntimeException("Failed to set private field '" + fieldName + "'", e);
        }
    }

    @Test
    void testGetRefSchemaWithJsonPointer() {
        // Fragment with JSON_POINTER type – should delegate to getSubSchema().
        JsonNodePath fragment = new JsonNodePath(PathType.JSON_POINTER);
        JsonSchema result = realSchema.getRefSchema(fragment);
        // The empty schema node does not contain any subschema, therefore null is expected.
        assertNull(result, "When the fragment is a JSON pointer and no sub‑schema exists the method must return null");
    }

    @Test
    void testGetRefSchemaWithAnchorFoundInResources() {
        // Anchor fragment (non‑JSON_POINTER) that exists in schemaResources.
        JsonNodePath fragment = new SimpleJsonNodePath(PathType.JSON_PATH, "myAnchor");
        String anchorKey = schemaLocation.getAbsoluteIri().toString() + "#myAnchor";
        JsonSchema expectedAnchorSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, mapper.createObjectNode(), null, false);
        validationContext.getSchemaResources().put(anchorKey, expectedAnchorSchema);
        JsonSchema result = realSchema.getRefSchema(fragment);
        assertSame(expectedAnchorSchema, result, "The method must return the schema stored in schemaResources for the anchor");
    }

    @Test
    void testGetRefSchemaWithAnchorFoundInDynamicAnchors() {
        // Anchor fragment that is only present in dynamicAnchors.
        JsonNodePath fragment = new SimpleJsonNodePath(PathType.JSON_PATH, "dynAnchor");
        String anchorKey = schemaLocation.getAbsoluteIri().toString() + "#dynAnchor";
        // Ensure the key is not in schemaResources.
        validationContext.getSchemaResources().remove(anchorKey);
        JsonSchema expectedAnchorSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, mapper.createObjectNode(), null, false);
        validationContext.getDynamicAnchors().put(anchorKey, expectedAnchorSchema);
        JsonSchema result = realSchema.getRefSchema(fragment);
        assertSame(expectedAnchorSchema, result, "If the anchor is not in schemaResources it must be looked up in dynamicAnchors");
    }

    @Test
    void testGetRefSchemaWithAnchorNotFoundThrowsException() {
        // Anchor fragment that cannot be resolved.
        JsonNodePath fragment = new SimpleJsonNodePath(PathType.JSON_PATH, "missingAnchor");
        String anchorKey = schemaLocation.getAbsoluteIri().toString() + "#missingAnchor";
        // Ensure both maps are empty for this key.
        validationContext.getSchemaResources().remove(anchorKey);
        validationContext.getDynamicAnchors().remove(anchorKey);
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> realSchema.getRefSchema(fragment),
                "An unknown anchor must cause a JsonSchemaException");
        assertEquals("Unable to find anchor " + anchorKey, ex.getMessage(),
                "Exception message should contain the missing anchor identifier");
    }

    @Test
    void testGetRefSchemaJsonPointerSubSchemaReturnsNull() {
        // Using a JSON pointer that does not resolve to any subschema – expect null.
        JsonNodePath fragment = new JsonNodePath(PathType.JSON_POINTER);
        JsonSchema result = realSchema.getRefSchema(fragment);
        assertNull(result, "When getSubSchema returns null the method must propagate null");
    }

    @Test
    void testGetRefSchemaWithNullFragmentThrowsNPE() {
        // Passing a null fragment should result in a NullPointerException.
        assertThrows(NullPointerException.class,
                () -> realSchema.getRefSchema(null),
                "Calling getRefSchema with a null fragment must throw NPE");
    }

    @Test
    void testGetRefSchemaWithNullSchemaLocationThrowsNPE() throws Exception {
        // Create a schema with a valid location then set it to null via reflection.
        JsonSchema schemaWithNullLocation = realSchema;
        setPrivateField(schemaWithNullLocation, "schemaLocation", null);
        JsonNodePath fragment = new SimpleJsonNodePath(PathType.JSON_PATH, "anyAnchor");
        assertThrows(NullPointerException.class,
                () -> schemaWithNullLocation.getRefSchema(fragment),
                "If schemaLocation is null a NPE should be thrown before any lookup");
    }

    @Test
    void testGetRefSchemaWithNullValidationContextThrowsNPE() throws Exception {
        // Set the validationContext field to null via reflection.
        JsonSchema schemaWithNullContext = realSchema;
        setPrivateField(schemaWithNullContext, "validationContext", null);
        JsonNodePath fragment = new SimpleJsonNodePath(PathType.JSON_PATH, "anyAnchor");
        assertThrows(NullPointerException.class,
                () -> schemaWithNullContext.getRefSchema(fragment),
                "If validationContext is null a NPE should be thrown when accessing it");
    }

    @Test
    void testGetRefSchemaWithNullSchemaResourcesMapThrowsNPE() {
        // Nullify the schemaResources map inside the ValidationContext.
        setPrivateField(validationContext, "schemaResources", null);
        JsonNodePath fragment = new SimpleJsonNodePath(PathType.JSON_PATH, "anyAnchor");
        assertThrows(NullPointerException.class,
                () -> realSchema.getRefSchema(fragment),
                "A null schemaResources map must cause a NPE on .get(anchor)");
    }

    @Test
    void testGetRefSchemaWithNullDynamicAnchorsMapThrowsNPE() {
        // Nullify the dynamicAnchors map inside the ValidationContext.
        setPrivateField(validationContext, "dynamicAnchors", null);
        JsonNodePath fragment = new SimpleJsonNodePath(PathType.JSON_PATH, "anyAnchor");
        assertThrows(NullPointerException.class,
                () -> realSchema.getRefSchema(fragment),
                "A null dynamicAnchors map must cause a NPE on fallback lookup");
    }
}
```