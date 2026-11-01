```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link BaseJsonValidator#registerAndMergeDiscriminator(DiscriminatorContext, ObjectNode, JsonSchema, JsonNodePath)}.
 * <p>
 * The method is {@code protected static} but the test class resides in the same package,
 * therefore it can be invoked directly.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_registerAndMergeDiscriminator_Test {

    private AutoCloseable mocks;

    @Mock
    private DiscriminatorContext discriminatorContext;

    @Mock
    private JsonNodePath instanceLocation;   // used only for message building – its value is irrelevant for logic

    private final JsonNodeFactory factory = JsonNodeFactory.instance;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required
    }

    @BeforeEach
    void setupBeforeEach() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        mocks.close();
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /**
     * Helper concrete {@link JsonSchema} that allows us to set the protected fields
     * {@code schemaLocation} and {@code schemaNode} used by the method under test.
     */
    private static final class TestJsonSchema extends JsonSchema {
        TestJsonSchema(SchemaLocation location, JsonNode schemaNode) {
            super(Collections.emptyList(),
                  false,
                  false,
                  null,
                  null,
                  false,
                  schemaNode,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  location,
                  null,
                  null);
            // fields {@code schemaLocation} and {@code schemaNode} are protected in BaseJsonValidator,
            // so we can set them directly here.
            this.schemaLocation = location;
            this.schemaNode = schemaNode;
        }
    }

    @Test
    void shouldRegisterWhenNoDiscriminatorOnSchema() {
        // given
        ObjectNode rootDiscriminator = factory.objectNode();
        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonSchema schema = new TestJsonSchema(schemaLocation, factory.objectNode()); // empty schema node

        // when
        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext,
                rootDiscriminator,
                schema,
                instanceLocation);

        // then
        verify(discriminatorContext).registerDiscriminator(schemaLocation, rootDiscriminator);
        // no mapping must have been added
        assertFalse(rootDiscriminator.has("mapping"));
    }

    @Test
    void shouldSetRootMappingWhenRootHasNoMappingButSchemaHasOne() {
        // given
        ObjectNode rootDiscriminator = factory.objectNode(); // no mapping
        ObjectNode schemaMapping = factory.objectNode();
        schemaMapping.set("A", factory.textNode("#/definitions/A"));
        ObjectNode schemaDiscriminator = factory.objectNode();
        schemaDiscriminator.set("mapping", schemaMapping);
        ObjectNode schemaNode = factory.objectNode();
        schemaNode.set("discriminator", schemaDiscriminator);

        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonSchema schema = new TestJsonSchema(schemaLocation, schemaNode);

        when(discriminatorContext.getDiscriminatorForPath(anyString()))
                .thenReturn(rootDiscriminator);

        // when
        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext,
                rootDiscriminator,
                schema,
                instanceLocation);

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

        when(discriminatorContext.getDiscriminatorForPath(anyString()))
                .thenReturn(rootDiscriminator);

        // when
        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext,
                rootDiscriminator,
                schema,
                instanceLocation);

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
        schemaMapping.set("A", factory.textNode("#/definitions/NewA")); // conflicting value
        ObjectNode schemaDiscriminator = factory.objectNode();
        schemaDiscriminator.set("mapping", schemaMapping);
        ObjectNode schemaNode = factory.objectNode();
        schemaNode.set("discriminator", schemaDiscriminator);

        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonSchema schema = new TestJsonSchema(schemaLocation, schemaNode);

        when(discriminatorContext.getDiscriminatorForPath(anyString()))
                .thenReturn(rootDiscriminator);

        // when / then
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () ->
                BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext,
                        rootDiscriminator,
                        schema,
                        instanceLocation));

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

        when(discriminatorContext.getDiscriminatorForPath(anyString()))
                .thenReturn(rootDiscriminator);

        // when / then
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () ->
                BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext,
                        rootDiscriminator,
                        schema,
                        instanceLocation));

        assertTrue(ex.getMessage().contains("attempts redefining the discriminator property"));
        verify(discriminatorContext, never()).registerDiscriminator(any(), any());
    }
}
```