**Generated test class**

```java
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

/**
 * Unit tests for {@link BaseJsonValidator#registerAndMergeDiscriminator(
 * ValidationContext.DiscriminatorContext, ObjectNode, JsonSchema, String)}.
 *
 * The tests cover the different branches of the method:
 * <ul>
 *   <li>No discriminator on the schema – only registration.</li>
 *   <li>Discriminator defines a {@code propertyName} – illegal re‑definition.</li>
 *   <li>Only the schema provides a {@code mapping} – it becomes the root mapping.</li>
 *   <li>Both root and schema provide a mapping – they are merged.</li>
 *   <li>Mapping conflict – an exception is thrown.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_registerAndMergeDiscriminator_Test {

    @Mock
    ValidationContext.DiscriminatorContext discriminatorContext;

    @Mock
    JsonSchema jsonSchema;

    /** a reusable factory for creating {@link ObjectNode}s */
    private static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;

    @BeforeAll
    static void setupBeforeAll() {
        // nothing to initialise globally
    }

    @BeforeEach
    void setupBeforeEach() {
        // reset mocks before each test
        Mockito.reset(discriminatorContext, jsonSchema);
    }

    @AfterEach
    void teardownAfterEach() {
        // no resources to release
    }

    @AfterAll
    static void teardownAfterAll() {
        // nothing to clean up
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
        ObjectNode rootDiscriminator = FACTORY.objectNode(); // empty root discriminator
        String schemaPath = "#/components/schemas/Thing";
        when(jsonSchema.getSchemaNode()).thenReturn(FACTORY.objectNode()); // no "discriminator"
        when(jsonSchema.getSchemaPath()).thenReturn(schemaPath);
        stubDiscriminatorForPath(schemaPath);

        // act / assert
        assertDoesNotThrow(() ->
                BaseJsonValidator.registerAndMergeDiscriminator(
                        discriminatorContext, rootDiscriminator, jsonSchema, "at"));
        // verify that registration happened with the original root discriminator
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
        // registration should still happen (the method registers after the exception is thrown)
        verify(discriminatorContext).registerDiscriminator(schemaPath, rootDiscriminator);
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
        // registration still occurs (the method registers after the exception)
        verify(discriminatorContext).registerDiscriminator(schemaPath, rootDiscriminator);
    }
}
```