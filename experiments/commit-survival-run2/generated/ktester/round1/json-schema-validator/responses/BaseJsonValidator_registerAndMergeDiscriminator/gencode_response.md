```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BaseJsonValidator#registerAndMergeDiscriminator(
 * ValidationContext.DiscriminatorContext, ObjectNode, JsonSchema, String)}.
 *
 * <p>The tests cover every branch described in the JSON test‑case specification
 * – from the simple “no discriminator” path to the various mapping‑merge
 * scenarios and the error conditions that raise {@link JsonSchemaException},
 * {@link ClassCastException} or {@link NullPointerException}.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_registerAndMergeDiscriminator_Test {

    @Mock
    ValidationContext.DiscriminatorContext discriminatorContext;

    @Mock
    JsonSchema jsonSchema;

    /** Re‑usable factory for {@link ObjectNode} creation. */
    private static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(discriminatorContext, jsonSchema);
    }

    /**
     * Helper that makes {@code discriminatorContext.getDiscriminatorForPath(..)} return a
     * non‑null value, forcing the method to enter the merging logic.
     */
    private void stubDiscriminatorForPath(String schemaPath) {
        when(discriminatorContext.getDiscriminatorForPath(schemaPath)).thenReturn(new Object());
    }

    /* --------------------------------------------------------------------- */
    /*                     Tests already covered in the spec                 */
    /* --------------------------------------------------------------------- */

    @Test
    void shouldRegisterWhenSchemaHasNoDiscriminator() {
        // arrange
        ObjectNode rootDiscriminator = FACTORY.objectNode(); // empty root
        String schemaPath = "#/components/schemas/Thing";
        when(jsonSchema.getSchemaNode()).thenReturn(FACTORY.objectNode()); // no "discriminator"
        when(jsonSchema.getSchemaPath()).thenReturn(schemaPath);
        stubDiscriminatorForPath(schemaPath);

        // act / assert
        assertDoesNotThrow(() ->
                BaseJsonValidator.registerAndMergeDiscriminator(
                        discriminatorContext, rootDiscriminator, jsonSchema, "at"));
        // registration must happen with the untouched root discriminator
        verify(discriminatorContext).registerDiscriminator(schemaPath, rootDiscriminator);
    }

    @Test
    void shouldThrowWhenSchemaRedefinesPropertyName() {
        // arrange
        ObjectNode rootDiscriminator = FACTORY.objectNode();
        ObjectNode schemaDiscriminator = FACTORY.objectNode()
                .put("propertyName", "type"); // illegal redefinition
        ObjectNode schemaNode = FACTORY.objectNode()
                .set("discriminator", schemaDiscriminator);

        String schemaPath = "#/components/schemas/Animal";
        when(jsonSchema.getSchemaNode()).thenReturn(schemaNode);
        when(jsonSchema.getSchemaPath()).thenReturn(schemaPath);
        stubDiscriminatorForPath(schemaPath);

        // act & assert
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () ->
                BaseJsonValidator.registerAndMergeDiscriminator(
                        discriminatorContext, rootDiscriminator, jsonSchema, "at"));
        assertTrue(ex.getMessage().contains("attempts redefining the discriminator property"));
        // because the exception aborts the method, registration never occurs
        verify(discriminatorContext, never()).registerDiscriminator(anyString(), any());
    }

    @Test
    void shouldCopyMappingWhenRootHasNoMapping() {
        // arrange
        ObjectNode rootDiscriminator = FACTORY.objectNode(); // no "mapping"
        ObjectNode schemaMapping = FACTORY.objectNode()
                .put("dog", "#/components/schemas/Dog")
                .put("cat", "#/components/schemas/Cat");
        ObjectNode schemaDiscriminator = FACTORY.objectNode()
                .set("mapping", schemaMapping);
        ObjectNode schemaNode = FACTORY.objectNode()
                .set("discriminator", schemaDiscriminator);

        String schemaPath = "#/components/schemas/Pet";
        when(jsonSchema.getSchemaNode()).thenReturn(schemaNode);
        when(jsonSchema.getSchemaPath()).thenReturn(schemaPath);
        stubDiscriminatorForPath(schemaPath);

        // act
        BaseJsonValidator.registerAndMergeDiscriminator(
                discriminatorContext, rootDiscriminator, jsonSchema, "at");

        // assert – mapping was copied to the root discriminator
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
        ObjectNode rootMapping = FACTORY.objectNode()
                .put("bird", "#/components/schemas/Bird");
        ObjectNode rootDiscriminator = FACTORY.objectNode()
                .set("mapping", rootMapping);

        ObjectNode schemaMapping = FACTORY.objectNode()
                .put("fish", "#/components/schemas/Fish");
        ObjectNode schemaDiscriminator = FACTORY.objectNode()
                .set("mapping", schemaMapping);
        ObjectNode schemaNode = FACTORY.objectNode()
                .set("discriminator", schemaDiscriminator);

        String schemaPath = "#/components/schemas/Animal";
        when(jsonSchema.getSchemaNode()).thenReturn(schemaNode);
        when(jsonSchema.getSchemaPath()).thenReturn(schemaPath);
        stubDiscriminatorForPath(schemaPath);

        // act
        BaseJsonValidator.registerAndMergeDiscriminator(
                discriminatorContext, rootDiscriminator, jsonSchema, "at");

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
        ObjectNode rootMapping = FACTORY.objectNode()
                .put("reptile", "#/components/schemas/ReptileV1");
        ObjectNode rootDiscriminator = FACTORY.objectNode()
                .set("mapping", rootMapping);

        ObjectNode schemaMapping = FACTORY.objectNode()
                .put("reptile", "#/components/schemas/ReptileV2"); // conflicting value
        ObjectNode schemaDiscriminator = FACTORY.objectNode()
                .set("mapping", schemaMapping);
        ObjectNode schemaNode = FACTORY.objectNode()
                .set("discriminator", schemaDiscriminator);

        String schemaPath = "#/components/schemas/Creature";
        when(jsonSchema.getSchemaNode()).thenReturn(schemaNode);
        when(jsonSchema.getSchemaPath()).thenReturn(schemaPath);
        stubDiscriminatorForPath(schemaPath);

        // act & assert
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () ->
                BaseJsonValidator.registerAndMergeDiscriminator(
                        discriminatorContext, rootDiscriminator, jsonSchema, "at"));
        assertTrue(ex.getMessage().contains("discriminator mapping redefinition"));
        verify(discriminatorContext, never()).registerDiscriminator(anyString(), any());
    }

    /* --------------------------------------------------------------------- */
    /*                 Additional groups from the provided JSON               */
    /* --------------------------------------------------------------------- */

    @Test
    void shouldRegisterWhenContextHasNoEntryEvenIfSchemaHasDiscriminator() {
        // arrange – context returns null, so merging is skipped
        ObjectNode rootDiscriminator = FACTORY.objectNode()
                .set("mapping", FACTORY.objectNode().put("X", "#/components/schemas/X"));

        ObjectNode schemaMapping = FACTORY.objectNode()
                .put("Y", "#/components/schemas/Y");
        ObjectNode schemaDiscriminator = FACTORY.objectNode()
                .set("mapping", schemaMapping);
        ObjectNode schemaNode = FACTORY.objectNode()
                .set("discriminator", schemaDiscriminator);

        String schemaPath = "#/components/schemas/Root";
        when(jsonSchema.getSchemaNode()).thenReturn(schemaNode);
        when(jsonSchema.getSchemaPath()).thenReturn(schemaPath);
        // do NOT stub getDiscriminatorForPath – it returns null by default

        // act
        BaseJsonValidator.registerAndMergeDiscriminator(
                discriminatorContext, rootDiscriminator, jsonSchema, "at");

        // assert – discriminator unchanged
        JsonNode mapping = rootDiscriminator.get("mapping");
        assertNotNull(mapping);
        assertEquals("#/components/schemas/X", mapping.get("X").asText());
        assertNull(mapping.get("Y")); // Y was not merged
        verify(discriminatorContext).registerDiscriminator(schemaPath, rootDiscriminator);
    }

    @Test
    void shouldDoNothingWhenBothRootAndSchemaMappingsAreAbsent() {
        // arrange – both discriminators exist but have no "mapping" field
        ObjectNode rootDiscriminator = FACTORY.object