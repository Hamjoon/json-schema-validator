package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_registerAndMergeDiscriminator_Test {

    // Mocks / test fixtures
    private DiscriminatorContext discriminatorContext;

    // the discriminator that lives in the context
    private ObjectNode rootDiscriminator;

    // the schema being processed
    private JsonSchema schema;

    // the underlying JsonNode of the schema
    private ObjectNode schemaNode;

    // location of the instance being validated
    private JsonNodePath instanceLocation;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ---------- DiscriminatorContext ----------
        discriminatorContext = mock(DiscriminatorContext.class);
        // ---------- Root discriminator (the one stored in the context) ----------
        rootDiscriminator = JsonNodeFactory.instance.objectNode();
        // initially empty – tests will populate as needed
        // ---------- Schema ----------
        schema = mock(JsonSchema.class);
        // we need to inject the private fields `schemaNode` and `schemaLocation`
        // into the mock using reflection because the method accesses them directly.
        // empty by default
        schemaNode = JsonNodeFactory.instance.objectNode();
        Field schemaNodeField = JsonSchema.class.getDeclaredField("schemaNode");
        schemaNodeField.setAccessible(true);
        schemaNodeField.set(schema, schemaNode);
        // a dummy SchemaLocation – its concrete type does not matter for the logic
        SchemaLocation dummyLocation = mock(SchemaLocation.class);
        Field schemaLocationField = JsonSchema.class.getDeclaredField("schemaLocation");
        schemaLocationField.setAccessible(true);
        schemaLocationField.set(schema, dummyLocation);
        // ---------- Instance location ----------
        instanceLocation = mock(JsonNodePath.class);
    }

    /**
     * Helper that registers the mock discriminator in the context and invokes the method
     * under test.
     */
    private void invokeRegisterAndMerge() {
        // the context should return the root discriminator for the schema location
        when(discriminatorContext.getDiscriminatorForPath(any(SchemaLocation.class))).thenReturn(rootDiscriminator);
        // we only verify that the method is called – the real implementation does nothing else
        doNothing().when(discriminatorContext).registerDiscriminator(any(SchemaLocation.class), any(ObjectNode.class));
        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext, rootDiscriminator, schema, instanceLocation);
    }

    @Test
    void whenSchemaHasNoDiscriminator_thenOnlyRegisters() {
        // schemaNode has no "discriminator" field
        // getDiscriminatorForPath returns null → the merging block is skipped
        when(discriminatorContext.getDiscriminatorForPath(any(SchemaLocation.class))).thenReturn(null);
        // invoke – should not throw
        assertDoesNotThrow(() -> BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext, rootDiscriminator, schema, instanceLocation));
        // verify that registerDiscriminator is still called
        verify(discriminatorContext, times(1)).registerDiscriminator(any(SchemaLocation.class), eq(rootDiscriminator));
    }

    @Test
    void whenDiscriminatorRedefinesPropertyName_thenThrowsException() {
        // Build a discriminator on the schema that defines "propertyName".
        ObjectNode discOnSchema = JsonNodeFactory.instance.objectNode();
        discOnSchema.set("propertyName", JsonNodeFactory.instance.textNode("type"));
        schemaNode.set("discriminator", discOnSchema);
        // Context already has a discriminator for this path (non‑null).
        when(discriminatorContext.getDiscriminatorForPath(any(SchemaLocation.class))).thenReturn(rootDiscriminator);
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, this::invokeRegisterAndMerge);
        assertTrue(ex.getMessage().contains("attempts redefining the discriminator property"));
    }

    @Test
    void whenMappingConflicts_thenThrowsException() {
        // Context discriminator with a mapping entry "A" -> {"$ref":"#/components/schemas/A"}
        ObjectNode contextMapping = JsonNodeFactory.instance.objectNode();
        contextMapping.set("A", JsonNodeFactory.instance.objectNode().put("$ref", "#/components/schemas/A"));
        rootDiscriminator.set("mapping", contextMapping);
        // Schema discriminator with a *different* mapping for the same key "A"
        ObjectNode schemaMapping = JsonNodeFactory.instance.objectNode();
        schemaMapping.set("A", JsonNodeFactory.instance.objectNode().put("$ref", "#/components/schemas/Other"));
        ObjectNode discOnSchema = JsonNodeFactory.instance.objectNode();
        discOnSchema.set("mapping", schemaMapping);
        schemaNode.set("discriminator", discOnSchema);
        // Context already holds a discriminator for this schema location
        when(discriminatorContext.getDiscriminatorForPath(any(SchemaLocation.class))).thenReturn(rootDiscriminator);
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, this::invokeRegisterAndMerge);
        assertTrue(ex.getMessage().contains("discriminator mapping redefinition"));
    }

    @Test
    void whenMappingsDoNotConflict_thenMergesSuccessfully() {
        // Context mapping contains key "A"
        ObjectNode contextMapping = JsonNodeFactory.instance.objectNode();
        contextMapping.set("A", JsonNodeFactory.instance.objectNode().put("$ref", "#/components/schemas/A"));
        rootDiscriminator.set("mapping", contextMapping);
        // Schema mapping contains a *new* key "B"
        ObjectNode schemaMapping = JsonNodeFactory.instance.objectNode();
        schemaMapping.set("B", JsonNodeFactory.instance.objectNode().put("$ref", "#/components/schemas/B"));
        ObjectNode discOnSchema = JsonNodeFactory.instance.objectNode();
        discOnSchema.set("mapping", schemaMapping);
        schemaNode.set("discriminator", discOnSchema);
        // Context already holds a discriminator for this schema location
        when(discriminatorContext.getDiscriminatorForPath(any(SchemaLocation.class))).thenReturn(rootDiscriminator);
        // invoke – should not throw
        assertDoesNotThrow(this::invokeRegisterAndMerge);
        // After merging, the root discriminator must contain both entries
        ObjectNode resultingMapping = (ObjectNode) rootDiscriminator.get("mapping");
        assertNotNull(resultingMapping);
        assertTrue(resultingMapping.has("A"));
        assertTrue(resultingMapping.has("B"));
        // Verify that registerDiscriminator was called with the *updated* discriminator
        verify(discriminatorContext, times(1)).registerDiscriminator(any(SchemaLocation.class), eq(rootDiscriminator));
    }

    @Test
    void whenContextHasNoMappingButSchemaHasMapping_thenAdoptsSchemaMapping() {
        // Context has no "mapping"
        // Schema has a mapping
        ObjectNode schemaMapping = JsonNodeFactory.instance.objectNode();
        schemaMapping.set("C", JsonNodeFactory.instance.objectNode().put("$ref", "#/components/schemas/C"));
        ObjectNode discOnSchema = JsonNodeFactory.instance.objectNode();
        discOnSchema.set("mapping", schemaMapping);
        schemaNode.set("discriminator", discOnSchema);
        // Context already holds a discriminator for this schema location
        when(discriminatorContext.getDiscriminatorForPath(any(SchemaLocation.class))).thenReturn(rootDiscriminator);
        // invoke – should not throw
        assertDoesNotThrow(this::invokeRegisterAndMerge);
        // The root discriminator should now have the mapping from the schema
        ObjectNode resultingMapping = (ObjectNode) rootDiscriminator.get("mapping");
        assertNotNull(resultingMapping);
        assertTrue(resultingMapping.has("C"));
    }

    @Test
    void whenContextMappingNotObjectNode_thenClassCastException() {
        // Root discriminator has a "mapping" field that is a TextNode (invalid type).
        rootDiscriminator.set("mapping", JsonNodeFactory.instance.textNode("notAnObject"));
        // Schema provides a valid mapping (won't be reached because the cast fails first).
        ObjectNode schemaMapping = JsonNodeFactory.instance.objectNode();
        schemaMapping.set("D", JsonNodeFactory.instance.objectNode().put("$ref", "#/components/schemas/D"));
        ObjectNode discOnSchema = JsonNodeFactory.instance.objectNode();
        discOnSchema.set("mapping", schemaMapping);
        schemaNode.set("discriminator", discOnSchema);
        when(discriminatorContext.getDiscriminatorForPath(any(SchemaLocation.class))).thenReturn(rootDiscriminator);
        // Expect a ClassCastException when the method attempts to cast the root's mapping to ObjectNode.
        assertThrows(ClassCastException.class, this::invokeRegisterAndMerge);
    }

    @Test
    void whenSchemaMappingNotObjectNode_thenClassCastException() {
        // Root discriminator has a proper ObjectNode mapping.
        ObjectNode contextMapping = JsonNodeFactory.instance.objectNode();
        contextMapping.set("X", JsonNodeFactory.instance.objectNode().put("$ref", "#/components/schemas/X"));
        rootDiscriminator.set("mapping", contextMapping);
        // Schema's discriminator contains a "mapping" field that is a TextNode (invalid type).
        ObjectNode discOnSchema = JsonNodeFactory.instance.objectNode();
        discOnSchema.set("mapping", JsonNodeFactory.instance.textNode("notAnObject"));
        schemaNode.set("discriminator", discOnSchema);
        when(discriminatorContext.getDiscriminatorForPath(any(SchemaLocation.class))).thenReturn(rootDiscriminator);
        // Expect a ClassCastException when the method attempts to cast the schema's mapping to ObjectNode.
        assertThrows(ClassCastException.class, this::invokeRegisterAndMerge);
    }
}
