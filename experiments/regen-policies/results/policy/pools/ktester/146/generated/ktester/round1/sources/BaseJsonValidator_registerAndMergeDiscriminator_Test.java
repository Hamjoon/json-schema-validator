package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_registerAndMergeDiscriminator_Test {

    private AutoCloseable mocks;

    @Mock
    private DiscriminatorContext discriminatorContext;

    @Mock
    private JsonNodePath // used only for message building – its value is irrelevant for logic
    instanceLocation;

    private final JsonNodeFactory factory = JsonNodeFactory.instance;

    /**
     * Helper concrete {@link JsonSchema} that allows us to set the protected fields
     * {@code schemaLocation} and {@code schemaNode} used by the method under test.
     */
    private static final class TestJsonSchema extends JsonSchema {

        TestJsonSchema(SchemaLocation location, JsonNode schemaNode) {
            super(Collections.emptyList(), false, false, null, null, false, schemaNode, null, null, null, null, null, null, location, null, null);
            // fields {@code schemaLocation} and {@code schemaNode} are protected in BaseJsonValidator,
            // so we can set them directly here.
            this.schemaLocation = location;
            this.schemaNode = schemaNode;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        mocks.close();
    }

    @Test
    void shouldRegisterWhenNoDiscriminatorOnSchema() {
        // given
        ObjectNode rootDiscriminator = factory.objectNode();
        SchemaLocation schemaLocation = new SchemaLocation(null);
        // empty schema node
        JsonSchema schema = new TestJsonSchema(schemaLocation, factory.objectNode());
        // when
        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext, rootDiscriminator, schema, instanceLocation);
        // then
        verify(discriminatorContext).registerDiscriminator(schemaLocation, rootDiscriminator);
        // no mapping must have been added
        assertFalse(rootDiscriminator.has("mapping"));
    }

    @Test
    void shouldSetRootMappingWhenRootHasNoMappingButSchemaHasOne() {
        // given
        // no mapping
        ObjectNode rootDiscriminator = factory.objectNode();
        ObjectNode schemaMapping = factory.objectNode();
        schemaMapping.set("A", factory.textNode("#/definitions/A"));
        ObjectNode schemaDiscriminator = factory.objectNode();
        schemaDiscriminator.set("mapping", schemaMapping);
        ObjectNode schemaNode = factory.objectNode();
        schemaNode.set("discriminator", schemaDiscriminator);
        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonSchema schema = new TestJsonSchema(schemaLocation, schemaNode);
        when(discriminatorContext.getDiscriminatorForPath(anyString())).thenReturn(rootDiscriminator);
        // when
        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext, rootDiscriminator, schema, instanceLocation);
        // then
        verify(discriminatorContext).registerDiscriminator(schemaLocation, rootDiscriminator);
        assertTrue(rootDiscriminator.has("mapping"));
        assertEquals(schemaMapping, rootDiscriminator.get("mapping"));
    }

    @Test
    void shouldMergeMappingsWhenBothRootAndSchemaHaveMappings() {
        // given
        ObjectNode rootDiscriminator = factory.objectNode();
        ObjectNode rootMapping = factory.objectNode();
        rootMapping.set("A", factory.textNode("#/definitions/A"));
        rootDiscriminator.set("mapping", rootMapping);
        ObjectNode schemaMapping = factory.objectNode();
        schemaMapping.set("B", factory.textNode("#/definitions/B"));
        ObjectNode schemaDiscriminator = factory.objectNode();
        schemaDiscriminator.set("mapping", schemaMapping);
        ObjectNode schemaNode = factory.objectNode();
        schemaNode.set("discriminator", schemaDiscriminator);
        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonSchema schema = new TestJsonSchema(schemaLocation, schemaNode);
        when(discriminatorContext.getDiscriminatorForPath(anyString())).thenReturn(rootDiscriminator);
        // when
        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext, rootDiscriminator, schema, instanceLocation);
        // then
        verify(discriminatorContext).registerDiscriminator(schemaLocation, rootDiscriminator);
        ObjectNode merged = (ObjectNode) rootDiscriminator.get("mapping");
        assertTrue(merged.has("A"));
        assertTrue(merged.has("B"));
        assertEquals("#/definitions/A", merged.get("A").asText());
        assertEquals("#/definitions/B", merged.get("B").asText());
    }

    @Test
    void shouldThrowWhenMappingRedefinitionDiffers() {
        // given
        ObjectNode rootDiscriminator = factory.objectNode();
        ObjectNode rootMapping = factory.objectNode();
        rootMapping.set("A", factory.textNode("#/definitions/OldA"));
        rootDiscriminator.set("mapping", rootMapping);
        ObjectNode schemaMapping = factory.objectNode();
        // conflicting value
        schemaMapping.set("A", factory.textNode("#/definitions/NewA"));
        ObjectNode schemaDiscriminator = factory.objectNode();
        schemaDiscriminator.set("mapping", schemaMapping);
        ObjectNode schemaNode = factory.objectNode();
        schemaNode.set("discriminator", schemaDiscriminator);
        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonSchema schema = new TestJsonSchema(schemaLocation, schemaNode);
        when(discriminatorContext.getDiscriminatorForPath(anyString())).thenReturn(rootDiscriminator);
        // when / then
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext, rootDiscriminator, schema, instanceLocation));
        assertTrue(ex.getMessage().contains("discriminator mapping redefinition"));
        verify(discriminatorContext, never()).registerDiscriminator(any(), any());
    }

    @Test
    void shouldThrowWhenDiscriminatorRedefinesPropertyName() {
        // given
        ObjectNode rootDiscriminator = factory.objectNode();
        ObjectNode schemaDiscriminator = factory.objectNode();
        schemaDiscriminator.set("propertyName", factory.textNode("type"));
        ObjectNode schemaNode = factory.objectNode();
        schemaNode.set("discriminator", schemaDiscriminator);
        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonSchema schema = new TestJsonSchema(schemaLocation, schemaNode);
        when(discriminatorContext.getDiscriminatorForPath(anyString())).thenReturn(rootDiscriminator);
        // when / then
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext, rootDiscriminator, schema, instanceLocation));
        assertTrue(ex.getMessage().contains("attempting redefining the discriminator property"));
        verify(discriminatorContext, never()).registerDiscriminator(any(), any());
    }

    @Test
    void shouldRegisterWhenContextHasNoEntryForSchema() {
        // given: context returns null for the schema location
        // empty discriminator
        ObjectNode rootDiscriminator = factory.objectNode();
        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonSchema schema = new TestJsonSchema(schemaLocation, factory.objectNode().set("discriminator", factory.objectNode().set("propertyName", factory.textNode("type"))));
        when(discriminatorContext.getDiscriminatorForPath(anyString())).thenReturn(// no existing discriminator in the context
        null);
        // when
        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext, rootDiscriminator, schema, instanceLocation);
        // then: registration still happens, no merge logic is executed
        verify(discriminatorContext).registerDiscriminator(schemaLocation, rootDiscriminator);
        assertFalse(rootDiscriminator.has("mapping"));
    }

    @Test
    void shouldDoNothingWhenBothRootAndSchemaMappingsAreAbsent() {
        // given: root discriminator without mapping, schema discriminator without mapping
        // no mapping
        ObjectNode rootDiscriminator = factory.objectNode();
        // no mapping field at all
        ObjectNode schemaDiscriminator = factory.objectNode();
        ObjectNode schemaNode = factory.objectNode().set("discriminator", schemaDiscriminator);
        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonSchema schema = new TestJsonSchema(schemaLocation, schemaNode);
        when(discriminatorContext.getDiscriminatorForPath(anyString())).thenReturn(rootDiscriminator);
        // when
        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext, rootDiscriminator, schema, instanceLocation);
        // then: registration occurs, mapping still absent
        verify(discriminatorContext).registerDiscriminator(schemaLocation, rootDiscriminator);
        assertFalse(rootDiscriminator.has("mapping"));
    }

    @Test
    void shouldLeaveRootMappingUnchangedWhenChildHasNoMappingField() {
        // given: root has a mapping, child discriminator lacks a "mapping" field
        ObjectNode rootDiscriminator = factory.objectNode();
        ObjectNode rootMapping = factory.objectNode();
        rootMapping.set("cat", factory.textNode("#/definitions/Cat"));
        rootDiscriminator.set("mapping", rootMapping);
        // no mapping field
        ObjectNode schemaDiscriminator = factory.objectNode();
        ObjectNode schemaNode = factory.objectNode().set("discriminator", schemaDiscriminator);
        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonSchema schema = new TestJsonSchema(schemaLocation, schemaNode);
        when(discriminatorContext.getDiscriminatorForPath(anyString())).thenReturn(rootDiscriminator);
        // when
        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext, rootDiscriminator, schema, instanceLocation);
        // then: mapping stays exactly as before
        verify(discriminatorContext).registerDiscriminator(schemaLocation, rootDiscriminator);
        ObjectNode after = (ObjectNode) rootDiscriminator.get("mapping");
        assertEquals(1, after.size());
        assertEquals("#/definitions/Cat", after.get("cat").asText());
    }

    @Test
    void shouldThrowWhenSameKeyHasIdenticalContentButDifferentNodeInstances() {
        // given: root mapping contains "cat" node, child mapping contains a *different* JsonNode instance
        // with the same content. The implementation checks reference inequality, so this must fail.
        ObjectNode rootDiscriminator = factory.objectNode();
        ObjectNode rootMapping = factory.objectNode();
        // create first instance
        ObjectNode catNodeRoot = factory.objectNode().set("$ref", factory.textNode("#/definitions/Cat"));
        rootMapping.set("cat", catNodeRoot);
        rootDiscriminator.set("mapping", rootMapping);
        // create second instance with identical structure
        ObjectNode catNodeChild = factory.objectNode().set("$ref", factory.textNode("#/definitions/Cat"));
        ObjectNode schemaMapping = factory.objectNode();
        schemaMapping.set("cat", catNodeChild);
        ObjectNode schemaDiscriminator = factory.objectNode().set("mapping", schemaMapping);
        ObjectNode schemaNode = factory.objectNode().set("discriminator", schemaDiscriminator);
        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonSchema schema = new TestJsonSchema(schemaLocation, schemaNode);
        when(discriminatorContext.getDiscriminatorForPath(anyString())).thenReturn(rootDiscriminator);
        // when / then
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext, rootDiscriminator, schema, instanceLocation));
        assertTrue(ex.getMessage().contains("discriminator mapping redefinition"));
        verify(discriminatorContext, never()).registerDiscriminator(any(), any());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenDiscriminatorArgumentIsNull() {
        // given
        ObjectNode nullDiscriminator = null;
        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonSchema schema = new TestJsonSchema(schemaLocation, factory.objectNode().set("discriminator", factory.objectNode().set("propertyName", factory.textNode("type"))));
        // when / then
        assertThrows(NullPointerException.class, () -> BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext, nullDiscriminator, schema, instanceLocation));
        // No registration should happen
        verify(discriminatorContext, never()).registerDiscriminator(any(), any());
    }

    @Test
    void shouldThrowClassCastExceptionWhenDiscriminatorMappingIsNotObjectNode() {
        // given: discriminator has a "mapping" field that is a plain text node (invalid type)
        ObjectNode rootDiscriminator = factory.objectNode();
        rootDiscriminator.set("mapping", factory.textNode("notAnObject"));
        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonSchema schema = new TestJsonSchema(schemaLocation, factory.objectNode().set("discriminator", factory.objectNode().set("mapping", factory.objectNode())));
        when(discriminatorContext.getDiscriminatorForPath(anyString())).thenReturn(rootDiscriminator);
        // when / then
        assertThrows(ClassCastException.class, () -> BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext, rootDiscriminator, schema, instanceLocation));
        verify(discriminatorContext, never()).registerDiscriminator(any(), any());
    }

    @Test
    void shouldThrowClassCastExceptionWhenSchemaDiscriminatorMappingIsNotObjectNode() {
        // given: schema's discriminator.mapping is a text node instead of an ObjectNode
        // no mapping, fine
        ObjectNode rootDiscriminator = factory.objectNode();
        ObjectNode schemaDiscriminator = factory.objectNode();
        schemaDiscriminator.set("mapping", factory.textNode("notAnObject"));
        ObjectNode schemaNode = factory.objectNode().set("discriminator", schemaDiscriminator);
        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonSchema schema = new TestJsonSchema(schemaLocation, schemaNode);
        when(discriminatorContext.getDiscriminatorForPath(anyString())).thenReturn(rootDiscriminator);
        // when / then
        assertThrows(ClassCastException.class, () -> BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext, rootDiscriminator, schema, instanceLocation));
        verify(discriminatorContext, never()).registerDiscriminator(any(), any());
    }

    @Test
    void shouldOverwriteExistingContextEntryWhenSchemaHasNoDiscriminator() {
        // given: context already contains a discriminator for the same location
        ObjectNode existingRootDiscriminator = factory.objectNode();
        ObjectNode existingMapping = factory.objectNode();
        existingMapping.set("old", factory.textNode("#/definitions/Old"));
        existingRootDiscriminator.set("mapping", existingMapping);
        // The new discriminator we pass in (empty object) should replace the previous one
        ObjectNode newDiscriminator = factory.objectNode();
        SchemaLocation schemaLocation = new SchemaLocation(null);
        // schema without discriminator
        JsonSchema schema = new TestJsonSchema(schemaLocation, factory.objectNode());
        // Simulate that the context already has an entry – we don't need to stub getDiscriminatorForPath,
        // because the method only looks at the schema's discriminator to decide merging.
        // We only verify that registerDiscriminator receives the *new* discriminator object.
        ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);
        // when
        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext, newDiscriminator, schema, instanceLocation);
        // then
        verify(discriminatorContext).registerDiscriminator(eq(schemaLocation), captor.capture());
        assertSame(newDiscriminator, captor.getValue(), "The discriminator stored in the context must be the one supplied to the method (overwrites previous).");
    }
}
