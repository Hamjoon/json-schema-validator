```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getRefSchema_Test {

    private ValidationContext validationContext;
    private SchemaLocation schemaLocation;
    private JsonNodePath evaluationPath;
    private JsonSchema realSchema;
    private JsonSchema spySchema;
    private ObjectMapper mapper;
    private JsonNode schemaNode;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

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
        realSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath,
                schemaNode, null, false);
        // Spy so that we can stub getSubSchema for the JSON‑Pointer test.
        spySchema = spy(realSchema);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the maps to avoid interference between tests.
        validationContext.getSchemaResources().clear();
        validationContext.getDynamicAnchors().clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    @Test
    void testGetRefSchemaWithJsonPointer() {
        // Fragment with JSON_POINTER type – should delegate to getSubSchema().
        JsonNodePath fragment = new JsonNodePath(PathType.JSON_POINTER);
        JsonSchema expectedSubSchema = mock(JsonSchema.class);
        doReturn(expectedSubSchema).when(spySchema).getSubSchema(fragment);

        JsonSchema result = spySchema.getRefSchema(fragment);

        assertSame(expectedSubSchema, result,
                "When the fragment is a JSON pointer the method must return the sub‑schema");
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

        assertSame(expectedAnchorSchema, result,
                "The method must return the schema stored in schemaResources for the anchor");
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

        assertSame(expectedAnchorSchema, result,
                "If the anchor is not in schemaResources it must be looked up in dynamicAnchors");
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

        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> spySchema.getRefSchema(fragment),
                "An unknown anchor must cause a JsonSchemaException");

        assertEquals("Unable to find anchor " + anchorKey, ex.getMessage(),
                "Exception message should contain the missing anchor identifier");
    }
}
```