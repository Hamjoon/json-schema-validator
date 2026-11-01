```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BaseJsonValidator#registerAndMergeDiscriminator(DiscriminatorContext, ObjectNode, JsonSchema, JsonNodePath)}.
 *
 * <p>The original test suite used Mockito for mocking, but the project does not include the
 * Mockito dependency.  To keep the tests self‑contained we replace the Mockito usage with
 * lightweight hand‑rolled test doubles.</p>
 */
class BaseJsonValidator_registerAndMergeDiscriminator_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Simple test double for {@link DiscriminatorContext}. */
    private static class SimpleDiscriminatorContext implements DiscriminatorContext {
        /** Value that {@link #getDiscriminatorForPath(JsonNodePath)} should return. */
        private ObjectNode discriminatorForPath;
        /** Records of calls to {@link #registerDiscriminator(JsonNodePath, ObjectNode)}. */
        private final List<RegisterCall> registerCalls = new ArrayList<>();

        SimpleDiscriminatorContext() {}

        void setDiscriminatorForPath(ObjectNode node) {
            this.discriminatorForPath = node;
        }

        @Override
        public ObjectNode getDiscriminatorForPath(JsonNodePath path) {
            return discriminatorForPath;
        }

        @Override
        public void registerDiscriminator(JsonNodePath path, ObjectNode discriminator) {
            registerCalls.add(new RegisterCall(path, discriminator));
        }

        List<RegisterCall> getRegisterCalls() {
            return registerCalls;
        }

        static class RegisterCall {
            final JsonNodePath path;
            final ObjectNode discriminator;

            RegisterCall(JsonNodePath path, ObjectNode discriminator) {
                this.path = path;
                this.discriminator = discriminator;
            }
        }
    }

    /**
     * Helper to inject a value into a private/protected field via reflection.
     */
    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Scenario 1 – the schema does NOT contain a "discriminator" keyword.
     * The method should simply register the (unchanged) discriminator.
     */
    @Test
    void shouldRegisterWhenNoDiscriminatorOnSchema() {
        // Arrange
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        rootDiscriminator.set("mapping", MAPPER.createObjectNode());

        // schema without discriminator – empty object node
        JsonNode schemaNode = MAPPER.createObjectNode();
        JsonSchema schema = new JsonSchema();               // default constructor works
        setField(schema, "schemaNode", schemaNode);
        setField(schema, "schemaLocation", new SchemaLocation(null, null));

        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);

        // Act
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation);

        // Assert – verify that registerDiscriminator was called exactly once with the same discriminator
        assertEquals(1, ctx.getRegisterCalls().size());
        assertSame(rootDiscriminator, ctx.getRegisterCalls().get(0).discriminator);
    }

    /**
     * Scenario 2 – the schema contains a discriminator with a "propertyName".
     * According to the implementation this is illegal and must throw JsonSchemaException.
     */
    @Test
    void shouldThrowWhenDiscriminatorRedefinesPropertyName() {
        // Arrange
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();

        // Build schema node with discriminator.propertyName
        ObjectNode discriminatorNode = MAPPER.createObjectNode();
        discriminatorNode.put("propertyName", "type");
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("discriminator", discriminatorNode);

        JsonSchema schema = new JsonSchema();
        setField(schema, "schemaNode", schemaNode);
        setField(schema, "schemaLocation", new SchemaLocation(null, null));

        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);

        // Act & Assert
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));
        assertTrue(ex.getMessage().contains("attempts redefining the discriminator property"));
    }

    /**
     * Scenario 3 – context discriminator has no "mapping" while schema provides one.
     * The method should copy the mapping from the schema onto the context discriminator.
     */
    @Test
    void shouldCopyMappingWhenContextHasNone() {
        // Arrange
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        // no mapping initially
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();

        // schema discriminator with mapping
        ObjectNode schemaMapping = MAPPER.createObjectNode();
        schemaMapping.set("A", MAPPER.createObjectNode().put("$ref", "#/components/schemas/A"));
        ObjectNode discriminatorNode = MAPPER.createObjectNode();
        discriminatorNode.set("mapping", schemaMapping);
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("discriminator", discriminatorNode);

        JsonSchema schema = new JsonSchema();
        setField(schema, "schemaNode", schemaNode);
        setField(schema, "schemaLocation", new SchemaLocation(null, null));

        // make the context think a discriminator already exists for this location
        ctx.setDiscriminatorForPath(new ObjectNode(MAPPER.getNodeFactory()));

        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);

        // Act
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation);

        // Assert – the root discriminator should now contain the mapping from the schema
        JsonNode resultingMapping = rootDiscriminator.get("mapping");
        assertNotNull(resultingMapping);
        assertEquals(schemaMapping, resultingMapping);
        assertEquals(1, ctx.getRegisterCalls().size());
        assertSame(rootDiscriminator, ctx.getRegisterCalls().get(0).discriminator);
    }

    /**
     * Scenario 4 – both context and schema have mappings; they are merged without conflict.
     */
    @Test
    void shouldMergeMappingsWhenNoConflict() {
        // Arrange
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();

        ObjectNode contextMapping = MAPPER.createObjectNode();
        contextMapping.set("A", MAPPER.createObjectNode().put("$ref", "#/components/schemas/A"));
        rootDiscriminator.set("mapping", contextMapping);

        // schema mapping adds a new entry "B"
        ObjectNode schemaMapping = MAPPER.createObjectNode();
        schemaMapping.set("B", MAPPER.createObjectNode().put("$ref", "#/components/schemas/B"));
        ObjectNode discriminatorNode = MAPPER.createObjectNode();
        discriminatorNode.set("mapping", schemaMapping);
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("discriminator", discriminatorNode);

        JsonSchema schema = new JsonSchema();
        setField(schema, "schemaNode", schemaNode);
        setField(schema, "schemaLocation", new SchemaLocation(null, null));

        ctx.setDiscriminatorForPath(new ObjectNode(MAPPER.getNodeFactory()));
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);

        // Act
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation);

        // Assert – both entries should be present
        JsonNode merged = rootDiscriminator.get("mapping");
        assertNotNull(merged);
        assertEquals(2, merged.size());
        assertTrue(merged.has("A"));
        assertTrue(merged.has("B"));
        assertEquals(1, ctx.getRegisterCalls().size());
        assertSame(rootDiscriminator, ctx.getRegisterCalls().get(0).discriminator);
    }

    /**
     * Scenario 5 – conflict: same mapping key with different values should raise an exception.
     */
    @Test
    void shouldThrowOnMappingRedefinitionConflict() {
        // Arrange
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
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

        JsonSchema schema = new JsonSchema();
        setField(schema, "schemaNode", schemaNode);
        setField(schema, "schemaLocation", new SchemaLocation(null, null));

        ctx.setDiscriminatorForPath(new ObjectNode(MAPPER.getNodeFactory()));
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);

        // Act & Assert
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));
        assertTrue(ex.getMessage().contains("discriminator mapping redefinition"));
        assertEquals(0, ctx.getRegisterCalls().size());
    }

    /**
     * Mapping with the same key/value already present – the method should silently accept it.
     */
    @Test
    void shouldAcceptSameMappingValueWithoutException() {
        // Arrange
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
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

        JsonSchema schema = new JsonSchema();
        setField(schema, "schemaNode", schemaNode);
        setField(schema, "schemaLocation", new SchemaLocation(null, null));

        ctx.setDiscriminatorForPath(new ObjectNode(MAPPER.getNodeFactory()));
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);

        // Act – should not throw
        assertDoesNotThrow(() -> BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));

        // Mapping must stay unchanged (still only one entry)
        JsonNode mapping = rootDiscriminator.get("mapping");
        assertNotNull(mapping);
        assertEquals(1, mapping.size());
        assertTrue(mapping.has("cat"));
        assertEquals(1, ctx.getRegisterCalls().size());
        assertSame(rootDiscriminator, ctx.getRegisterCalls().get(0).discriminator);
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
        JsonSchema schema = new JsonSchema();
        // any non‑null schemaNode is fine
        setField(schema, "schemaNode", MAPPER.createObjectNode());
        setField(schema, "schemaLocation", new SchemaLocation(null, null));
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);

        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));
    }

    /**
     * Supplying a {@code null} discriminator node triggers a {@link NullPointerException}
     * when the method attempts to read its {@code "mapping"} field.
     */
    @Test
    void shouldThrowNPEWhenDiscriminatorNodeIsNull() {
        // Arrange
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        // deliberately null
        ObjectNode rootDiscriminator = null;
        JsonSchema schema = new JsonSchema();
        setField(schema, "schemaNode", MAPPER.createObjectNode());
        setField(schema, "schemaLocation", new SchemaLocation(null, null));
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);

        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));
    }

    /**
     * When the root discriminator's {@code "mapping"} field exists but is not an {@link ObjectNode},
     * a {@link ClassCastException} is expected during the cast.
     */
    @Test
    void shouldThrowClassCastWhenRootMappingIsNotObjectNode() {
        // Arrange
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        // mapping is a plain text node, not an object
        rootDiscriminator.set("mapping", MAPPER.textNode("foo"));
        // schema must contain a discriminator (even empty) to enter the block
        ObjectNode discriminatorNode = MAPPER.createObjectNode();
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("discriminator", discriminatorNode);
        JsonSchema schema = new JsonSchema();
        setField(schema, "schemaNode", schemaNode);
        setField(schema, "schemaLocation", new SchemaLocation(null, null));
        // make the context return a non‑null discriminator for the location
        ctx.setDiscriminatorForPath(new ObjectNode(MAPPER.getNodeFactory()));
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);

        // Act & Assert
        assertThrows(ClassCastException.class,
                () -> BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));
    }

    /**
     * When the nested schema's discriminator {@code "mapping"} field is not an {@link ObjectNode},
     * a {@link ClassCastException} should be thrown.
     */
    @Test
    void shouldThrowClassCastWhenSchemaMappingIsNotObjectNode() {
        // Arrange
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        // valid empty mapping
        rootDiscriminator.set("mapping", MAPPER.createObjectNode());
        // schema discriminator with a non‑object mapping (text node)
        ObjectNode discriminatorNode = MAPPER.createObjectNode();
        discriminatorNode.set("mapping", MAPPER.textNode("invalid"));
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("discriminator", discriminatorNode);
        JsonSchema schema = new JsonSchema();
        setField(schema, "schemaNode", schemaNode);
        setField(schema, "schemaLocation", new SchemaLocation(null, null));
        ctx.setDiscriminatorForPath(new ObjectNode(MAPPER.getNodeFactory()));
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);

        // Act & Assert
        assertThrows(ClassCastException.class,
                () -> BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));
    }

    /**
     * If the {@link JsonSchema} instance has a {@code null} {@code schemaNode},
     * accessing {@code schema.schemaNode.get("discriminator")} results in a {@link NullPointerException}.
     */
    @Test
    void shouldThrowNPEWhenSchemaNodeIsNull() {
        // Arrange
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        JsonSchema schema = new JsonSchema();
        // explicitly set schemaNode to null
        setField(schema, "schemaNode", null);
        setField(schema, "schemaLocation", new SchemaLocation(null, null));
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);

        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));
    }
}
```