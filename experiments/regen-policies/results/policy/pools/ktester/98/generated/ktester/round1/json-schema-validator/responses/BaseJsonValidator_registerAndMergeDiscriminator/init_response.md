```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        // Reset mocks before each test – MockitoExtension does this automatically.
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

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

        JsonNodePath instanceLocation = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);

        // Act & Assert
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () ->
                BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));

        assertTrue(ex.getMessage().contains("discriminator mapping redefinition"));
        verify(ctx, never()).registerDiscriminator(any(), any());
    }
}
```