package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Iterator;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_registerAndMergeDiscriminator_Test {

    @Mock
    ValidationContext.DiscriminatorContext discriminatorContext;

    @Mock
    JsonSchema jsonSchema;

    /**
     * a reusable factory for creating {@link ObjectNode}s
     */
    private static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;

    @BeforeEach
    void setupBeforeEach() {
        // reset mocks before each test
        Mockito.reset(discriminatorContext, jsonSchema);
        Mockito.reset(discriminatorContext, jsonSchema);
    }

    /**
     * Helper that creates a dummy non‑null object for {@code getDiscriminatorForPath}
     * to make the method enter the merging logic.
     */
    private void stubDiscriminatorForPath(String schemaPath) {
        when(discriminatorContext.getDiscriminatorForPath(schemaPath)).thenReturn(new Object());
    }

    @Test
    void shouldRegisterWhenSchemaHasNoDiscriminator() {
        // arrange
        // empty root discriminator
        ObjectNode rootDiscriminator = FACTORY.objectNode();
        String schemaPath = "#/components/schemas/Thing";
        // no "discriminator"
        when(jsonSchema.getSchemaNode()).thenReturn(FACTORY.objectNode());
        when(jsonSchema.getSchemaPath()).thenReturn(schemaPath);
        stubDiscriminatorForPath(schemaPath);
        // act / assert
        assertDoesNotThrow(() -> BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext, rootDiscriminator, jsonSchema, "at"));
        // verify that registration happened with the original root discriminator
        verify(discriminatorContext).registerDiscriminator(schemaPath, rootDiscriminator);
    }

    @Test
    void shouldThrowWhenSchemaRedefinesPropertyName() {
        // arrange
        ObjectNode rootDiscriminator = FACTORY.objectNode();
        ObjectNode schemaDiscriminator = FACTORY.objectNode().put("propertyName", // illegal redefinition
        "type");
        ObjectNode schemaNode = FACTORY.objectNode().set("discriminator", schemaDiscriminator);
        String schemaPath = "#/components/schemas/Animal";
        when(jsonSchema.getSchemaNode()).thenReturn(schemaNode);
        when(jsonSchema.getSchemaPath()).thenReturn(schemaPath);
        stubDiscriminatorForPath(schemaPath);
        // act & assert
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext, rootDiscriminator, jsonSchema, "at"));
        assertTrue(ex.getMessage().contains("attempts redefining the discriminator property"));
        // registration should still happen (the method registers after the exception is thrown)
        verify(discriminatorContext).registerDiscriminator(schemaPath, rootDiscriminator);
    }

    @Test
    void shouldCopyMappingWhenRootHasNoMapping() {
        // arrange
        // no "mapping"
        ObjectNode rootDiscriminator = FACTORY.objectNode();
        ObjectNode schemaMapping = FACTORY.objectNode().put("dog", "#/components/schemas/Dog").put("cat", "#/components/schemas/Cat");
        ObjectNode schemaDiscriminator = FACTORY.objectNode().set("mapping", schemaMapping);
        ObjectNode schemaNode = FACTORY.objectNode().set("discriminator", schemaDiscriminator);
        String schemaPath = "#/components/schemas/Pet";
        when(jsonSchema.getSchemaNode()).thenReturn(schemaNode);
        when(jsonSchema.getSchemaPath()).thenReturn(schemaPath);
        stubDiscriminatorForPath(schemaPath);
        // act
        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext, rootDiscriminator, jsonSchema, "at");
        // assert – the root discriminator now contains the mapping from the schema
        JsonNode resultingMapping = rootDiscriminator.get("mapping");
        assertNotNull(resultingMapping);
        assertTrue(resultingMapping.isObject());
        assertEquals("#/components/schemas/Dog", resultingMapping.get("dog").asText());
        assertEquals("#/components/schemas/Cat", resultingMapping.get("cat").asText());
        verify(discriminatorContext).registerDiscriminator(schemaPath, rootDiscriminator);
    }

    @Test
    void shouldMergeMappingsWhenBothHaveMappingsWithoutConflict() {
        // arrange
        ObjectNode rootMapping = FACTORY.objectNode().put("bird", "#/components/schemas/Bird");
        ObjectNode rootDiscriminator = FACTORY.objectNode().set("mapping", rootMapping);
        ObjectNode schemaMapping = FACTORY.objectNode().put("fish", "#/components/schemas/Fish");
        ObjectNode schemaDiscriminator = FACTORY.objectNode().set("mapping", schemaMapping);
        ObjectNode schemaNode = FACTORY.objectNode().set("discriminator", schemaDiscriminator);
        String schemaPath = "#/components/schemas/Animal";
        when(jsonSchema.getSchemaNode()).thenReturn(schemaNode);
        when(jsonSchema.getSchemaPath()).thenReturn(schemaPath);
        stubDiscriminatorForPath(schemaPath);
        // act
        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext, rootDiscriminator, jsonSchema, "at");
        // assert – both entries are present
        JsonNode merged = rootDiscriminator.get("mapping");
        assertNotNull(merged);
        assertEquals("#/components/schemas/Bird", merged.get("bird").asText());
        assertEquals("#/components/schemas/Fish", merged.get("fish").asText());
        verify(discriminatorContext).registerDiscriminator(schemaPath, rootDiscriminator);
    }

    @Test
    void shouldThrowWhenMappingConflicts() {
        // arrange
        ObjectNode rootMapping = FACTORY.objectNode().put("reptile", "#/components/schemas/ReptileV1");
        ObjectNode rootDiscriminator = FACTORY.objectNode().set("mapping", rootMapping);
        ObjectNode schemaMapping = FACTORY.objectNode().put("reptile", // conflicting value
        "#/components/schemas/ReptileV2");
        ObjectNode schemaDiscriminator = FACTORY.objectNode().set("mapping", schemaMapping);
        ObjectNode schemaNode = FACTORY.objectNode().set("discriminator", schemaDiscriminator);
        String schemaPath = "#/components/schemas/Creature";
        when(jsonSchema.getSchemaNode()).thenReturn(schemaNode);
        when(jsonSchema.getSchemaPath()).thenReturn(schemaPath);
        stubDiscriminatorForPath(schemaPath);
        // act & assert
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext, rootDiscriminator, jsonSchema, "at"));
        assertTrue(ex.getMessage().contains("discriminator mapping redefinition"));
        // registration still occurs (the method registers after the exception)
        verify(discriminatorContext).registerDiscriminator(schemaPath, rootDiscriminator);
    }

    @Test
    void shouldRegisterWhenContextHasNoEntryEvenIfSchemaHasDiscriminator() {
        // arrange – context returns null, so merging is skipped
        ObjectNode rootDiscriminator = FACTORY.objectNode().set("mapping", FACTORY.objectNode().put("X", "#/components/schemas/X"));
        ObjectNode schemaMapping = FACTORY.objectNode().put("Y", "#/components/schemas/Y");
        ObjectNode schemaDiscriminator = FACTORY.objectNode().set("mapping", schemaMapping);
        ObjectNode schemaNode = FACTORY.objectNode().set("discriminator", schemaDiscriminator);
        String schemaPath = "#/components/schemas/Root";
        when(jsonSchema.getSchemaNode()).thenReturn(schemaNode);
        when(jsonSchema.getSchemaPath()).thenReturn(schemaPath);
        // do NOT stub getDiscriminatorForPath – it returns null by default
        // act
        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext, rootDiscriminator, jsonSchema, "at");
        // assert – discriminator unchanged
        JsonNode mapping = rootDiscriminator.get("mapping");
        assertNotNull(mapping);
        assertEquals("#/components/schemas/X", mapping.get("X").asText());
        // Y was not merged
        assertNull(mapping.get("Y"));
        verify(discriminatorContext).registerDiscriminator(schemaPath, rootDiscriminator);
    }
}
