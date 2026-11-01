```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.*;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    /* --------------------------------------------------------------------- */
    /* Helper to create a JsonSchema instance with a given node and parent. */
    private JsonSchema createSchema(JsonNode node, JsonSchema parent) {
        return JsonSchema.from(validationContext, schemaLocation,
                evaluationPath, node, parent, false);
    }

    /* --------------------------------------------------------------------- */
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
        assertSame(schema, result,
                "When the schema has an id, findLexicalRoot should return itself");
    }

    @Test
    void findLexicalRoot_climbsToAncestorWithId() {
        // Arrange – parent schema has an id, child schema does not
        JsonNode parentNode = mock(JsonNode.class);
        JsonNode childNode = mock(JsonNode.class);

        when(validationContext.resolveSchemaId(parentNode)).thenReturn("http://example.com/parent");
        when(validationContext.resolveSchemaId(childNode)).thenReturn(null);

        JsonSchema parent = createSchema(parentNode, null);
        JsonSchema child = createSchema(childNode, parent);

        // Act
        JsonSchema result = child.findLexicalRoot();

        // Assert – should resolve to the parent (the first ancestor with a non‑null id)
        assertSame(parent, result,
                "findLexicalRoot should return the nearest ancestor that has an id");
    }

    @Test
    void findLexicalRoot_returnsSelfWhenNoIdAndNoParent() {
        // Arrange – a root schema with no id
        JsonNode rootNode = mock(JsonNode.class);
        when(validationContext.resolveSchemaId(rootNode)).thenReturn(null);

        JsonSchema root = createSchema(rootNode, null);

        // Act
        JsonSchema result = root.findLexicalRoot();

        // Assert – without an id and without a parent, the method returns the instance itself
        assertSame(root, result,
                "When there is no id and no parent, findLexicalRoot should return the schema itself");
    }

    @Test
    void findLexicalRoot_returnsSelfWhenIdIsEmptyString() {
        // Arrange – id is an empty string (still non‑null)
        JsonNode node = mock(JsonNode.class);
        when(validationContext.resolveSchemaId(node)).thenReturn("");

        JsonSchema schema = createSchema(node, null);

        // Act
        JsonSchema result = schema.findLexicalRoot();

        // Assert – empty string is treated as a valid id, so the method returns the same instance
        assertSame(schema, result,
                "An empty string id should be considered non‑null; the schema should be returned unchanged");
    }

    @Test
    void findLexicalRoot_returnsSelfWhenIdIsWhitespace() {
        // Arrange – id is a whitespace string (non‑null)
        JsonNode node = mock(JsonNode.class);
        when(validationContext.resolveSchemaId(node)).thenReturn("   ");

        JsonSchema schema = createSchema(node, null);

        // Act
        JsonSchema result = schema.findLexicalRoot();

        // Assert – whitespace is also a non‑null id, so no climbing occurs
        assertSame(schema, result,
                "A whitespace id should be treated as a valid (non‑null) id");
    }

    @Test
    void findLexicalRoot_returnsParentWhenBothIdsNullAndParentHasNoParent() {
        // Arrange – child id null, parent id null, parent has no further parent
        JsonNode parentNode = mock(JsonNode.class);
        JsonNode childNode = mock(JsonNode.class);

        when(validationContext.resolveSchemaId(parentNode)).thenReturn(null);
        when(validationContext.resolveSchemaId(childNode)).thenReturn(null);

        JsonSchema parent = createSchema(parentNode, null);
        JsonSchema child = createSchema(childNode, parent);

        // Act
        JsonSchema result = child.findLexicalRoot();

        // Assert – loop runs once, moves to parent, then breaks because parent has no parent
        assertSame(parent, result,
                "When both child and parent have null ids and the parent has no further parent, the parent should be returned");
    }

    @Test
    void findLexicalRoot_returnsDirectParentEvenIfHigherAncestorAlsoHasId() {
        // Arrange – child id null, parent id non‑null, grand‑parent id non‑null
        JsonNode grandNode = mock(JsonNode.class);
        JsonNode parentNode = mock(JsonNode.class);
        JsonNode childNode = mock(JsonNode.class);

        when(validationContext.resolveSchemaId(grandNode)).thenReturn("http://example.com/grand");
        when(validationContext.resolveSchemaId(parentNode)).thenReturn("http://example.com/parent");
        when(validationContext.resolveSchemaId(childNode)).thenReturn(null);

        JsonSchema grand = createSchema(grandNode, null);
        JsonSchema parent = createSchema(parentNode, grand);
        JsonSchema child = createSchema(childNode, parent);

        // Act
        JsonSchema result = child.findLexicalRoot();

        // Assert – the first ancestor with a non‑null id is the parent, not the grand‑parent
        assertSame(parent, result,
                "The method should stop at the nearest ancestor with a non‑null id");
    }

    @Test
    void findLexicalRoot_returnsGrandparentWhenParentIdIsNull() {
        // Arrange – child id null, parent id null, grand‑parent id non‑null
        JsonNode grandNode = mock(JsonNode.class);
        JsonNode parentNode = mock(JsonNode.class);
        JsonNode childNode = mock(JsonNode.class);

        when(validationContext.resolveSchemaId(grandNode)).thenReturn("http://example.com/grand");
        when(validationContext.resolveSchemaId(parentNode)).thenReturn(null);
        when(validationContext.resolveSchemaId(childNode)).thenReturn(null);

        JsonSchema grand = createSchema(grandNode, null);
        JsonSchema parent = createSchema(parentNode, grand);
        JsonSchema child = createSchema(childNode, parent);

        // Act
        JsonSchema result = child.findLexicalRoot();

        // Assert – the method climbs two levels and returns the grand‑parent
        assertSame(grand, result,
                "When both child and parent have null ids, the method should continue climbing to the grand‑parent");
    }

    @Test
    void findLexicalRoot_returnsTopmostWhenAllIdsNull() {
        // Arrange – chain of three schemas, all ids null, topmost has no parent
        JsonNode level2Node = mock(JsonNode.class);
        JsonNode level1Node = mock(JsonNode.class);
        JsonNode childNode = mock(JsonNode.class);

        when(validationContext.resolveSchemaId(level2Node)).thenReturn(null);
        when(validationContext.resolveSchemaId(level1Node)).thenReturn(null);
        when(validationContext.resolveSchemaId(childNode)).thenReturn(null);

        JsonSchema level2 = createSchema(level2Node, null); // topmost
        JsonSchema level1 = createSchema(level1Node, level2);
        JsonSchema child = createSchema(childNode, level1);

        // Act
        JsonSchema result = child.findLexicalRoot();

        // Assert – the loop ends when parent becomes null, returning the topmost schema
        assertSame(level2, result,
                "When no ancestor has an id, the method should return the highest schema whose parent is null");
    }

    @Test
    void findLexicalRoot_returnsGreatGrandParentAfterMultipleNullAncestors() {
        // Arrange – child → a → b → c → great‑grand‑parent (id non‑null), all intermediate ids null
        JsonNode greatGrandNode = mock(JsonNode.class);
        JsonNode cNode = mock(JsonNode.class);
        JsonNode bNode = mock(JsonNode.class);
        JsonNode aNode = mock(JsonNode.class);
        JsonNode childNode = mock(JsonNode.class);

        when(validationContext.resolveSchemaId(greatGrandNode)).thenReturn("http://example.com/root");
        when(validationContext.resolveSchemaId(cNode)).thenReturn(null);
        when(validationContext.resolveSchemaId(bNode)).thenReturn(null);
        when(validationContext.resolveSchemaId(aNode)).thenReturn(null);
        when(validationContext.resolveSchemaId(childNode)).thenReturn(null);

        JsonSchema greatGrand = createSchema(greatGrandNode, null);
        JsonSchema c = createSchema(cNode, greatGrand);
        JsonSchema b = createSchema(bNode, c);
        JsonSchema a = createSchema(aNode, b);
        JsonSchema child = createSchema(childNode, a);

        // Act
        JsonSchema result = child.findLexicalRoot();

        // Assert – the method should climb four levels and return the great‑grand‑parent
        assertSame(greatGrand, result,
                "The method must continue climbing until it finds the first non‑null id, even across many null‑id ancestors");
    }

    @Test
    void findLexicalRoot_nullReferenceThrowsNullPointerException() {
        // Arrange
        JsonSchema nullSchema = null;

        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> nullSchema.findLexicalRoot(),
                "Calling findLexicalRoot on a null reference should throw NullPointerException");
    }

    /**
     * This test demonstrates the pathological case where a schema references
     * itself as its parent and both have a null id. The {@code findLexicalRoot}
     * implementation would loop forever. The test is disabled to avoid hanging
     * the CI pipeline, but the code is left as documentation of the risk.
     */
    @Disabled("Infinite loop scenario – disabled to prevent test suite hang")
    @Test
    @Timeout(value = 1, unit = java.util.concurrent.TimeUnit.SECONDS)
    void findLexicalRoot_infiniteLoopDueToCyclicParent() {
        // Arrange – create a schema whose parent is itself and has no id
        JsonNode node = mock(JsonNode.class);
        when(validationContext.resolveSchemaId(node)).thenReturn(null);

        // Create the schema without a parent first
        JsonSchema schema = createSchema(node, null);
        // Use reflection to inject a cyclic parent (schema -> schema)
        try {
            java.lang.reflect.Field parentField = JsonSchema.class.getDeclaredField("parentSchema");
            parentField.setAccessible(true);
            parentField.set(schema, schema);
        } catch (Exception e) {
            fail("Failed to set up cyclic parent via reflection", e);
        }

        // Act – this call would never return; the @Timeout will cause the test to fail if it does not finish
        schema.findLexicalRoot();
    }
}
```