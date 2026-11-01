```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BaseJsonValidator#registerAndMergeDiscriminator(DiscriminatorContext, ObjectNode, JsonSchema, JsonNodePath)}.
 *
 * The tests cover all branches described in the JSON test‑case specification:
 * <ul>
 *   <li>No discriminator on the schema.</li>
 *   <li>Redefinition of {@code propertyName}.</li>
 *   <li>Copying a mapping when the root discriminator has none.</li>
 *   <li>Merging mappings without conflict.</li>
 *   <li>Mapping conflict (different values for the same key).</li>
 *   <li>Mapping with the same value (should be a no‑op).</li>
 *   <li>Null arguments that cause {@link NullPointerException}.</li>
 *   <li>Invalid (non‑object) mapping nodes that cause {@link ClassCastException}.</li>
 * </ul>
 *
 * The method under test is static package‑private, therefore the test class resides in the same
 * package to allow direct invocation.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_registerAndMergeDiscriminator_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Helper to inject a value into a private/protected field via reflection. */
    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // MockitoExtension already resets mocks.
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /* --------------------------------------------------------------------- */
    /* Existing test cases (kept unchanged)                                 */
    /* --------------------------------------------------------------------- */

    /**
     * Scenario 1 – the schema does NOT contain a "discriminator" keyword.
     * The method should simply register the (unchanged) discriminator.
     */
    @Test
    void shouldRegisterWhenNoDiscriminatorOnSchema() {
        // Arrange
        DiscriminatorContext ctx = mock(DiscriminatorContext.class);
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        rootDiscriminator.set("mapping", MAPPER.createObjectNode());

        // schema without discriminator
        JsonNode schemaNode = MAPPER.createObjectNode(); // empty object

        JsonSchema schema = mock(JsonSchema.class);
        setField(schema, "schemaNode", schemaNode);
        setField(schema, "schemaLocation", new SchemaLocation(null, null));

        JsonNodePath instanceLocation = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);

        // Act
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation);

        // Assert – verify that registerDiscriminator was called with the same discriminator object
        verify(ctx, times(1)).registerDiscriminator(any(), eq(rootDiscriminator));
    }

    /**
     * Scenario 2 – the schema contains a discriminator with a "propertyName".
     * According to the implementation this is illegal and must throw JsonSchemaException.
     */
    @Test
    void shouldThrowWhenDiscriminatorRedefinesPropertyName() {
        // Arrange
        DiscriminatorContext ctx = mock(DiscriminatorContext.class);
        ObjectNode rootDiscriminator = MAPPER.createObjectNode(); // empty root discriminator

        // Build schema node with discriminator.propertyName
        ObjectNode discriminatorNode = MAPPER.createObjectNode();
        discriminatorNode.put("propertyName", "type");
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("discriminator", discriminatorNode);

        JsonSchema schema = mock(JsonSchema.class);
        setField(schema, "schemaNode", schemaNode);
        setField(schema, "schemaLocation", new SchemaLocation(null, null));

        JsonNodePath instanceLocation = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);

        // Act & Assert
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () ->
                BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));

        assertTrue(ex.getMessage().contains("attempts redefining the discriminator property"));
    }

    /**
     * Scenario 3 – context discriminator has no "mapping" while schema provides one.
     * The method should copy the mapping from the schema onto the context discriminator.
     */
    @Test
    void shouldCopyMappingWhenContextHasNone() {
        // Arrange
        DiscriminatorContext ctx = mock(DiscriminatorContext.class);
        ObjectNode rootDiscriminator = MAPPER.createObjectNode(); // no mapping initially

        // schema discriminator with mapping
        ObjectNode schemaMapping = MAPPER.createObjectNode();
        schemaMapping.put("A", MAPPER.createObjectNode().put("$ref", "#/components/schemas/A"));
        ObjectNode discriminatorNode = MAPPER.createObjectNode();
        discriminatorNode.set("mapping", schemaMapping);
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("discriminator", discriminatorNode);

        JsonSchema schema = mock(JsonSchema.class);
        setField(schema, "schemaNode", schemaNode);
        setField(schema, "schemaLocation", new SchemaLocation(null, null));

        // make the context think a discriminator already exists for this location
        when(ctx.getDiscriminatorForPath(any())).thenReturn(new ObjectNode(MAPPER.getNodeFactory()));

        JsonNodePath instanceLocation = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);

        // Act
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation);

        // Assert – the root discriminator should now contain the mapping from the schema
        JsonNode resultingMapping = rootDiscriminator.get("mapping");
        assertNotNull(resultingMapping);
        assertEquals(schemaMapping, resultingMapping);
        verify(ctx, times(1)).registerDiscriminator(any(), eq(rootDiscriminator));
    }

    /**
     * Scenario 4 – both context and schema have mappings; they are merged without conflict.
     */
    @Test
    void shouldMergeMappingsWhenNoConflict() {
        // Arrange
        DiscriminatorContext ctx = mock(DiscriminatorContext.class);
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        ObjectNode contextMapping = MAPPER.createObjectNode();
        contextMapping.put("A", MAPPER.createObjectNode().put("$ref", "#/components/schemas/A"));
        rootDiscriminator.set("mapping", contextMapping);

        // schema mapping adds a new entry "B"
        ObjectNode schemaMapping = MAPPER.createObjectNode();
        schemaMapping.put("B", MAPPER.createObjectNode().put("$ref", "#/components/schemas/B"));
        ObjectNode discriminatorNode = MAPPER.createObjectNode();
        discriminatorNode.set("mapping", schemaMapping);
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("discriminator", discriminatorNode);

        JsonSchema schema = mock(JsonSchema.class);
        setField(schema, "schemaNode", schemaNode);
        setField(schema, "schemaLocation", new SchemaLocation(null, null));

        when(ctx.getDiscriminatorForPath(any())).thenReturn(new ObjectNode(MAPPER.getNodeFactory()));

        JsonNodePath instanceLocation = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);

        // Act
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation);

        // Assert – both entries should be present
        JsonNode merged = rootDiscriminator.get("mapping");
        assertNotNull(merged);
        assertEquals(2, merged.size());
        assertTrue(merged.has("A"));
        assertTrue(merged.has("B"));
        verify(ctx, times(1)).registerDiscriminator(any(), eq(rootDiscriminator));
    }

    /**
     * Scenario 5 – conflict: same mapping key with different values should raise an exception.
     */
    @Test
    void shouldThrowOnMappingRedefinitionConflict() {
        // Arrange
        DiscriminatorContext ctx = mock(DiscriminatorContext.class);
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        ObjectNode contextMapping = MAPPER.createObjectNode();
        // Existing mapping for "A"
        contextMapping.set("A", MAPPER.createObjectNode().put("$ref", "#/components/schemas/OldA"));
        rootDiscriminator.set("mapping", contextMapping);

        // Schema provides a different mapping for "A"
        ObjectNode schemaMapping = MAPPER.createObjectNode();
        schemaMapping.set("A", MAPPER.createObjectNode().put("$ref", "#/components/schemas/NewA"));
        ObjectNode discriminatorNode = MAPPER.createObjectNode();
        discriminatorNode.set("mapping", schemaMapping);
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("discriminator", discriminatorNode);

        JsonSchema schema = mock(JsonSchema.class);
        setField(schema, "schemaNode", schemaNode);
        setField(schema, "schemaLocation", new SchemaLocation(null, null));

        when(ctx.getDiscriminatorForPath(any())).thenReturn(new ObjectNode(MAPPER.getNodeFactory()));

        JsonNodePath instanceLocation = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);

        // Act & Assert
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () ->
                BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));

        assertTrue(ex.getMessage().contains("discriminator mapping redefinition"));
        verify(ctx, never()).registerDiscriminator(any(), any());
    }

    /* --------------------------------------------------------------------- */
    /* Additional test cases derived from the JSON specification               */
    /* --------------------------------------------------------------------- */

    /**
     * Mapping with the same key/value already present – the method should silently accept it.
     */
    @Test
    void shouldAcceptSameMappingValueWithoutException() {
        // Arrange
        DiscriminatorContext ctx = mock(DiscriminatorContext.class);
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        ObjectNode contextMapping = MAPPER.createObjectNode();
        // Existing mapping for "cat"
        contextMapping.set("cat", MAPPER.createObjectNode().put("$ref", "#/schemas/Cat"));
        rootDiscriminator.set("mapping", contextMapping);

        // Schema provides the *identical* mapping for "cat"
        ObjectNode schemaMapping = MAPPER.createObjectNode();
        schemaMapping.set("cat", MAPPER.createObjectNode().put("$ref", "#/schemas/Cat"));
        ObjectNode discriminatorNode = MAPPER.createObjectNode();
        discriminatorNode.set("mapping", schemaMapping);
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("discriminator", discriminatorNode);

        JsonSchema schema = mock(JsonSchema.class);
        setField(schema, "schemaNode", schemaNode);
        setField(schema, "schemaLocation", new SchemaLocation(null, null));

        when(ctx.getDiscriminatorForPath(any())).thenReturn(new ObjectNode(MAPPER.getNodeFactory()));

        JsonNodePath instanceLocation = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);

        // Act – should not throw
        assertDoesNotThrow(() ->
                BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));

        // Mapping must stay unchanged (still only one entry)
        JsonNode mapping = rootDiscriminator.get("mapping");
        assertNotNull(mapping);
        assertEquals(1, mapping.size());
        assertTrue(mapping.has("cat"));
        verify(ctx, times(1)).registerDiscriminator(any(), eq(rootDiscriminator));
    }

    /**
     * Passing a {@code null} {@link DiscriminatorContext} should result in a {@link NullPointerException}
     * because the method dereferences the context without a null‑check.
     */
    @Test
    void shouldThrowNPEWhenContextIsNull() {
        // Arrange
        DiscriminatorContext ctx = null;
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        JsonSchema schema = mock(JsonSchema.class);
        // any non‑null schemaNode is fine
        setField(schema, "schemaNode", MAPPER.createObjectNode());
        setField(schema, "schemaLocation", new SchemaLocation(null, null));

        JsonNodePath instanceLocation = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);

        // Act & Assert
        assertThrows(NullPointerException.class, () ->
                BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));
    }

    /**
     * Supplying a {@code null} discriminator node triggers a {@link NullPointerException}
     * when the method attempts to read its {@code "mapping"} field.
     */
    @Test
    void shouldThrowNPEWhenDiscriminatorNodeIsNull() {
        // Arrange
        DiscriminatorContext ctx = mock(DiscriminatorContext.class);
        ObjectNode rootDiscriminator = null; // deliberately null
        JsonSchema schema = mock(JsonSchema.class);
        setField(schema, "schemaNode", MAPPER.createObjectNode());
        setField(schema, "schemaLocation", new SchemaLocation(null, null));

        JsonNodePath instanceLocation = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);

        // Act & Assert
        assertThrows(NullPointerException.class, () ->
                BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));
    }

    /**
     * When the root discriminator's {@code "mapping"} field exists but is not an {@link ObjectNode},
     * a {@link ClassCastException} is expected during the cast.
     */
    @Test
    void shouldThrowClassCastWhenRootMappingIsNotObjectNode() {
        // Arrange
        DiscriminatorContext ctx = mock(DiscriminatorContext.class);
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        // mapping is a plain text node, not an object
        rootDiscriminator.set("mapping", MAPPER.textNode("foo"));

        // schema must contain a discriminator (even empty) to enter the block
        ObjectNode discriminatorNode = MAPPER.createObjectNode(); // empty object
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("discriminator", discriminatorNode);

        JsonSchema schema = mock(JsonSchema.class);
        setField(schema, "schemaNode", schemaNode);
        setField(schema, "schemaLocation", new SchemaLocation(null, null));

        // make the context return a non‑null discriminator for the location
        when(ctx.getDiscriminatorForPath(any())).thenReturn(new ObjectNode(MAPPER.getNodeFactory()));

        JsonNodePath instanceLocation = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);

        // Act & Assert
        assertThrows(ClassCastException.class, () ->
                BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));
    }

    /**
     * When the nested schema's discriminator {@code "mapping"} field is not an {@link ObjectNode},
     * a {@link ClassCastException} should be thrown.
     */
    @Test
    void shouldThrowClassCastWhenSchemaMappingIsNotObjectNode() {
        // Arrange
        DiscriminatorContext ctx = mock(DiscriminatorContext.class);
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        rootDiscriminator.set("mapping", MAPPER.createObjectNode()); // valid empty mapping

        // schema discriminator with a non‑object mapping (text node)
        ObjectNode discriminatorNode = MAPPER.createObjectNode();
        discriminatorNode.set("mapping", MAPPER.textNode("invalid"));
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("discriminator", discriminatorNode);

        JsonSchema schema = mock(JsonSchema.class);
        setField(schema, "schemaNode", schemaNode);
        setField(schema, "schemaLocation", new SchemaLocation(null, null));

        when(ctx.getDiscriminatorForPath(any())).thenReturn(new ObjectNode(MAPPER.getNodeFactory()));

        JsonNodePath instanceLocation = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);

        // Act & Assert
        assertThrows(ClassCastException.class, () ->
                BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));
    }

    /**
     * If the {@link JsonSchema} instance has a {@code null} {@code schemaNode},
     * accessing {@code schema.schemaNode.get("discriminator")} results in a {@link NullPointerException}.
     */
    @Test
    void shouldThrowNPEWhenSchemaNodeIsNull() {
        // Arrange
        DiscriminatorContext ctx = mock(DiscriminatorContext.class);
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();

        JsonSchema schema = mock(JsonSchema.class);
        // explicitly set schemaNode to null
        setField(schema, "schemaNode", null);
        setField(schema, "schemaLocation", new SchemaLocation(null, null));

        JsonNodePath instanceLocation = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);

        // Act & Assert
        assertThrows(NullPointerException.class, () ->
                BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));
    }
}
```