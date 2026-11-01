```java
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

/**
 * Unit tests for {@link BaseJsonValidator#registerAndMergeDiscriminator(DiscriminatorContext, ObjectNode, JsonSchema, JsonNodePath)}.
 *
 * <p>The tests cover the following scenarios:
 * <ul>
 *   <li>No discriminator defined on the schema – only registration occurs.</li>
 *   <li>Attempt to redefine the {@code propertyName} field – throws {@link JsonSchemaException}.</li>
 *   <li>Conflicting mapping entries between root and nested discriminators – throws {@link JsonSchemaException}.</li>
 *   <li>Successful merge of non‑conflicting mappings.</li>
 *   <li>Root discriminator has no mapping, nested schema provides one – mapping is adopted.</li>
 *   <li>Root discriminator's {@code mapping} field is not an {@link ObjectNode} – {@link ClassCastException}.</li>
 *   <li>Nested schema's {@code mapping} field is not an {@link ObjectNode} – {@link ClassCastException}.</li>
 * </ul>
 *
 * The test class follows the provided framework and uses reflection to inject the private
 * {@code schemaNode} and {@code schemaLocation} fields of the {@link JsonSchema} mock.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_registerAndMergeDiscriminator_Test {

    // -------------------------------------------------------------------------
    // Mocks / test fixtures
    // -------------------------------------------------------------------------
    private DiscriminatorContext discriminatorContext;
    private ObjectNode rootDiscriminator;          // the discriminator stored in the context
    private JsonSchema schema;                     // schema being processed
    private ObjectNode schemaNode;                 // underlying JsonNode of the schema
    private JsonNodePath instanceLocation;         // location of the instance being validated

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ---------- DiscriminatorContext ----------
        discriminatorContext = mock(DiscriminatorContext.class);

        // ---------- Root discriminator (the one stored in the context) ----------
        rootDiscriminator = JsonNodeFactory.instance.objectNode(); // empty by default

        // ---------- Schema ----------
        schema = mock(JsonSchema.class);
        // inject private fields `schemaNode` and `schemaLocation` via reflection
        schemaNode = JsonNodeFactory.instance.objectNode(); // empty by default
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

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up
    }

    /**
     * Helper that registers the mock discriminator in the context and invokes the method
     * under test.
     *
     * <p>The context is stubbed to return {@code rootDiscriminator} for any schema location.
     */
    private void invokeRegisterAndMerge() {
        when(discriminatorContext.getDiscriminatorForPath(any(SchemaLocation.class)))
                .thenReturn(rootDiscriminator);
        // The real implementation does not depend on the return value of registerDiscriminator,
        // so we simply stub it to do nothing.
        doNothing().when(discriminatorContext).registerDiscriminator(any(SchemaLocation.class), any(ObjectNode.class));

        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext, rootDiscriminator, schema, instanceLocation);
    }

    // -------------------------------------------------------------------------
    // Test cases derived from the JSON specification
    // -------------------------------------------------------------------------

    @Test
    void whenSchemaHasNoDiscriminator_thenOnlyRegisters() {
        // Schema does NOT contain a "discriminator" field.
        when(discriminatorContext.getDiscriminatorForPath(any(SchemaLocation.class))).thenReturn(null);

        assertDoesNotThrow(() ->
                BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext, rootDiscriminator, schema, instanceLocation));

        // Verify that the discriminator is still registered (unchanged).
        verify(discriminatorContext, times(1))
                .registerDiscriminator(any(SchemaLocation.class), eq(rootDiscriminator));
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

        when(discriminatorContext.getDiscriminatorForPath(any(SchemaLocation.class))).thenReturn(rootDiscriminator);

        JsonSchemaException ex = assertThrows(JsonSchemaException.class, this::invokeRegisterAndMerge);
        assertTrue(ex.getMessage().contains("discriminator mapping redefinition"));
        assertTrue(ex.getMessage().contains("A"));
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

        when(discriminatorContext.getDiscriminatorForPath(any(SchemaLocation.class))).thenReturn(rootDiscriminator);

        assertDoesNotThrow(this::invokeRegisterAndMerge);

        // After merging, the root discriminator must contain both entries.
        ObjectNode resultingMapping = (ObjectNode) rootDiscriminator.get("mapping");
        assertNotNull(resultingMapping);
        assertTrue(resultingMapping.has("A"));
        assertTrue(resultingMapping.has("B"));

        verify(discriminatorContext, times(1))
                .registerDiscriminator(any(SchemaLocation.class), eq(rootDiscriminator));
    }

    @Test
    void whenContextHasNoMappingButSchemaHasMapping_thenAdoptsSchemaMapping() {
        // Context has no "mapping" field.
        // Schema provides a mapping.
        ObjectNode schemaMapping = JsonNodeFactory.instance.objectNode();
        schemaMapping.set("C", JsonNodeFactory.instance.objectNode().put("$ref", "#/components/schemas/C"));
        ObjectNode discOnSchema = JsonNodeFactory.instance.objectNode();
        discOnSchema.set("mapping", schemaMapping);
        schemaNode.set("discriminator", discOnSchema);

        when(discriminatorContext.getDiscriminatorForPath(any(SchemaLocation.class))).thenReturn(rootDiscriminator);

        assertDoesNotThrow(this::invokeRegisterAndMerge);

        // The root discriminator should now contain the mapping from the schema.
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
```