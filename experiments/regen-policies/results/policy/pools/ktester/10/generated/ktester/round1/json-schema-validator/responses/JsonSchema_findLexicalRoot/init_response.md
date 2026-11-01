```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Tests for {@link JsonSchema#findLexicalRoot()}.
 *
 * The tests create {@link JsonSchema} instances via the package‑private
 * {@code JsonSchema.from(...)} factory method. All collaborators are mocked
 * because the method under test only depends on the {@code id} and the
 * {@code parentSchema} of a {@code JsonSchema} instance.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_findLexicalRoot_Test {

    // Common collaborators used by all tests
    private ValidationContext validationContext;
    private JsonMetaSchema metaSchema;
    private JsonSchemaFactory schemaFactory;
    private SchemaLocation schemaLocation;
    private JsonNodePath evaluationPath;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required
    }

    @BeforeEach
    void setupBeforeEach() {
        // Mock the heavy collaborators
        metaSchema = mock(JsonMetaSchema.class);
        schemaFactory = mock(JsonSchemaFactory.class);
        validationContext = mock(ValidationContext.class);
        schemaLocation = mock(SchemaLocation.class);
        evaluationPath = mock(JsonNodePath.class);

        // Default stubbing for methods used inside the JsonSchema constructor
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getSchemaResources()).thenReturn(new ConcurrentHashMap<>());
        when(validationContext.getDynamicAnchors()).thenReturn(new ConcurrentHashMap<>());
        when(validationContext.getSchemaReferences()).thenReturn(new ConcurrentHashMap<>());
        when(validationContext.getJsonSchemaFactory()).thenReturn(schemaFactory);

        when(metaSchema.readAnchor(any(JsonNode.class))).thenReturn(null);
        when(metaSchema.readDynamicAnchor(any(JsonNode.class))).thenReturn(null);

        when(schemaLocation.getAbsoluteIri()).thenReturn(null);
        when(schemaLocation.toString()).thenReturn("mockLocation");
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup needed
    }

    @Test
    void findLexicalRoot_returnsSelfWhenIdIsPresent() {
        // Arrange – a schema whose id is non‑null
        JsonNode schemaNode = mock(JsonNode.class);
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn("http://example.com/schema");

        JsonSchema schema = JsonSchema.from(validationContext, schemaLocation,
                evaluationPath, schemaNode, null, false);

        // Act
        JsonSchema result = schema.findLexicalRoot();

        // Assert – should be the same instance
        assertSame(schema, result, "When the schema has an id, findLexicalRoot should return itself");
    }

    @Test
    void findLexicalRoot_climbsToAncestorWithId() {
        // Arrange – parent schema has an id, child schema does not
        JsonNode parentNode = mock(JsonNode.class);
        JsonNode childNode = mock(JsonNode.class);

        // Parent gets an id, child gets null
        when(validationContext.resolveSchemaId(parentNode)).thenReturn("http://example.com/parent");
        when(validationContext.resolveSchemaId(childNode)).thenReturn(null);

        JsonSchema parent = JsonSchema.from(validationContext, schemaLocation,
                evaluationPath, parentNode, null, false);
        JsonSchema child = JsonSchema.from(validationContext, schemaLocation,
                evaluationPath, childNode, parent, false);

        // Act
        JsonSchema result = child.findLexicalRoot();

        // Assert – should resolve to the parent (the first ancestor with a non‑null id)
        assertSame(parent, result, "findLexicalRoot should return the nearest ancestor that has an id");
    }

    @Test
    void findLexicalRoot_returnsSelfWhenNoIdAndNoParent() {
        // Arrange – a root schema with no id
        JsonNode rootNode = mock(JsonNode.class);
        when(validationContext.resolveSchemaId(rootNode)).thenReturn(null);

        JsonSchema root = JsonSchema.from(validationContext, schemaLocation,
                evaluationPath, rootNode, null, false);

        // Act
        JsonSchema result = root.findLexicalRoot();

        // Assert – without an id and without a parent, the method returns the instance itself
        assertSame(root, result, "When there is no id and no parent, findLexicalRoot should return the schema itself");
    }
}
```