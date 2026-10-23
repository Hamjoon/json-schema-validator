```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Unit tests for {@link BaseJsonValidator#registerAndMergeDiscriminator(DiscriminatorContext, ObjectNode, JsonSchema, String)}.
 *
 * The tests are written in the same package as the classes under test so that they can
 * access package‑private members.
 */
class BaseJsonValidator_registerAndMergeDiscriminator_Test {

    // The discriminator context used by the tests.
    ValidationContext.DiscriminatorContext discriminatorContext;

    // The schema under test.
    JsonSchema jsonSchema;

    /**
     * A reusable factory for creating {@link ObjectNode}s.
     */
    private static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;

    /**
     * Simple implementation of {@link ValidationContext.DiscriminatorContext} used for the tests.
     */
    private static class SimpleDiscriminatorContext extends ValidationContext.DiscriminatorContext {

        private final Map<String, ObjectNode> registry = new HashMap<>();

        @Override
        public ObjectNode getDiscriminatorForPath(String schemaPath) {
            return registry.get(schemaPath);
        }

        @Override
        public void registerDiscriminator(String schemaPath, ObjectNode discriminator) {
            registry.put(schemaPath, discriminator);
        }

        /**
         * Helper used by the tests to assert that a registration happened.
         */
        boolean isRegistered(String schemaPath, ObjectNode expected) {
            ObjectNode stored = registry.get(schemaPath);
            return stored != null && stored.equals(expected);
        }
    }

    /**
     * Minimal concrete {@link JsonSchema} implementation that only supplies the
     * {@code schemaNode} and {@code schemaPath} required by the method under test.
     */
    private static class SimpleJsonSchema extends JsonSchema {

        private final JsonNode schemaNode;
        private final String schemaPath;

        SimpleJsonSchema(JsonNode schemaNode, String schemaPath) {
            // JsonSchema has a protected constructor (ValidationContext, java.net.URI, JsonNode)
            // We can pass null for the first two arguments because they are not used in the tests.
            super(null, null, schemaNode);
            this.schemaNode = schemaNode;
            this.schemaPath = schemaPath;
        }

        @Override
        public JsonNode getSchemaNode() {
            return schemaNode;
        }

        @Override
        public String getSchemaPath() {
            return schemaPath;
        }

        /* The abstract validation methods are not needed for these tests.
         * They are implemented with harmless defaults. */
        @Override
        public Set<ValidationMessage> validate(JsonNode node) {
            return Collections.emptySet();
        }

        @Override
        public Set<ValidationMessage> walk(ExecutionContext executionContext,
                                            JsonNode node,
                                            JsonNode rootNode,
                                            String at,
                                            boolean deepCheck) {
            return Collections.emptySet();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        discriminatorContext = new SimpleDiscriminatorContext();
        jsonSchema = null;
    }

    /**
     * Helper that creates a dummy non‑null object for {@code getDiscriminatorForPath}
     * to make the method enter the merging logic.
     */
    private void stubDiscriminatorForPath(String schemaPath) {
        // any non‑null value
        discriminatorContext.registerDiscriminator(schemaPath, FACTORY.objectNode());
    }

    @Test
    void shouldRegisterWhenSchemaHasNoDiscriminator() {
        // arrange
        ObjectNode rootDiscriminator = FACTORY.objectNode();
        String schemaPath = "#/components/schemas/Thing";
        // schema without a "discriminator" field
        jsonSchema = new SimpleJsonSchema(FACTORY.objectNode(), schemaPath);
        stubDiscriminatorForPath(schemaPath);
        // act / assert
        assertDoesNotThrow(() -> BaseJsonValidator.registerAndMergeDiscriminator(
                discriminatorContext, rootDiscriminator, jsonSchema, "at"));
        // verify registration
        assertTrue(((SimpleDiscriminatorContext) discriminatorContext)
                .isRegistered(schemaPath, rootDiscriminator),
                "Discriminator should be registered for the schema path");
    }

    @Test
    void shouldThrowWhenSchemaRedefinesPropertyName() {
        // arrange
        ObjectNode rootDiscriminator = FACTORY.objectNode();
        ObjectNode schemaDiscriminator = FACTORY.objectNode()
                .put("propertyName", "type"); // illegal redefinition
        ObjectNode schemaNode = FACTORY.objectNode().set("discriminator", schemaDiscriminator);
        String schemaPath = "#/components/schemas/Animal";
        jsonSchema = new SimpleJsonSchema(schemaNode, schemaPath);
        stubDiscriminatorForPath(schemaPath);
        // act & assert
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> BaseJsonValidator.registerAndMergeDiscriminator(
                        discriminatorContext, rootDiscriminator, jsonSchema, "at"));
        assertTrue(ex.getMessage().contains("attempts redefining the discriminator property"));
        // registration should still happen
        assertTrue(((SimpleDiscriminatorContext) discriminatorContext)
                .isRegistered(schemaPath, rootDiscriminator),
                "Discriminator should be registered even after exception");
    }

    @Test
    void shouldCopyMappingWhenRootHasNoMapping() {
        // arrange
        // no "mapping"
        ObjectNode rootDiscriminator = FACTORY.objectNode();
        ObjectNode schemaMapping = FACTORY.objectNode()
                .put("dog", "#/components/schemas/Dog")
                .put("cat", "#/components/schemas/Cat");
        ObjectNode schemaDiscriminator = FACTORY.objectNode().set("mapping", schemaMapping);
        ObjectNode schemaNode = FACTORY.objectNode().set("discriminator", schemaDiscriminator);
        String schemaPath = "#/components/schemas/Pet";
        jsonSchema = new SimpleJsonSchema(schemaNode, schemaPath);
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
        // verify registration
        assertTrue(((SimpleDiscriminatorContext) discriminatorContext)
                .isRegistered(schemaPath, rootDiscriminator));
    }

    @Test
    void shouldMergeMappingsWhenBothHaveMappingsWithoutConflict() {
        // arrange
        ObjectNode rootMapping = FACTORY.objectNode()
                .put("bird", "#/components/schemas/Bird");
        ObjectNode rootDiscriminator = FACTORY.objectNode().set("mapping", rootMapping);
        ObjectNode schemaMapping = FACTORY.objectNode()
                .put("fish", "#/components/schemas/Fish");
        ObjectNode schemaDiscriminator = FACTORY.objectNode().set("mapping", schemaMapping);
        ObjectNode schemaNode = FACTORY.objectNode().set("discriminator", schemaDiscriminator);
        String schemaPath = "#/components/schemas/Animal";
        jsonSchema = new SimpleJsonSchema(schemaNode, schemaPath);
        stubDiscriminatorForPath(schemaPath);
        // act
        BaseJsonValidator.registerAndMergeDiscriminator(
                discriminatorContext, rootDiscriminator, jsonSchema, "at");
        // assert – both entries are present
        JsonNode merged = rootDiscriminator.get("mapping");
        assertNotNull(merged);
        assertEquals("#/components/schemas/Bird", merged.get("bird").asText());
        assertEquals("#/components/schemas/Fish", merged.get("fish").asText());
        // verify registration
        assertTrue(((SimpleDiscriminatorContext) discriminatorContext)
                .isRegistered(schemaPath, rootDiscriminator));
    }

    @Test
    void shouldThrowWhenMappingConflicts() {
        // arrange
        ObjectNode rootMapping = FACTORY.objectNode()
                .put("reptile", "#/components/schemas/ReptileV1");
        ObjectNode rootDiscriminator = FACTORY.objectNode().set("mapping", rootMapping);
        ObjectNode schemaMapping = FACTORY.objectNode()
                .put("reptile", "#/components/schemas/ReptileV2"); // conflicting value
        ObjectNode schemaDiscriminator = FACTORY.objectNode().set("mapping", schemaMapping);
        ObjectNode schemaNode = FACTORY.objectNode().set("discriminator", schemaDiscriminator);
        String schemaPath = "#/components/schemas/Creature";
        jsonSchema = new SimpleJsonSchema(schemaNode, schemaPath);
        stubDiscriminatorForPath(schemaPath);
        // act & assert
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> BaseJsonValidator.registerAndMergeDiscriminator(
                        discriminatorContext, rootDiscriminator, jsonSchema, "at"));
        assertTrue(ex.getMessage().contains("discriminator mapping redefinition"));
        // registration still occurs
        assertTrue(((SimpleDiscriminatorContext) discriminatorContext)
                .isRegistered(schemaPath, rootDiscriminator));
    }

    @Test
    void shouldRegisterWhenContextHasNoEntryEvenIfSchemaHasDiscriminator() {
        // arrange – context returns null, so merging is skipped
        ObjectNode rootDiscriminator = FACTORY.objectNode()
                .set("mapping", FACTORY.objectNode().put("X", "#/components/schemas/X"));
        ObjectNode schemaMapping = FACTORY.objectNode()
                .put("Y", "#/components/schemas/Y");
        ObjectNode schemaDiscriminator = FACTORY.objectNode().set("mapping", schemaMapping);
        ObjectNode schemaNode = FACTORY.objectNode().set("discriminator", schemaDiscriminator);
        String schemaPath = "#/components/schemas/Root";
        jsonSchema = new SimpleJsonSchema(schemaNode, schemaPath);
        // do NOT stub getDiscriminatorForPath – it remains absent (null)
        // act
        BaseJsonValidator.registerAndMergeDiscriminator(
                discriminatorContext, rootDiscriminator, jsonSchema, "at");
        // assert – discriminator unchanged
        JsonNode mapping = rootDiscriminator.get("mapping");
        assertNotNull(mapping);
        assertEquals("#/components/schemas/X", mapping.get("X").asText());
        // Y was not merged
        assertNull(mapping.get("Y"));
        // verify registration
        assertTrue(((SimpleDiscriminatorContext) discriminatorContext)
                .isRegistered(schemaPath, rootDiscriminator));
    }
}
```