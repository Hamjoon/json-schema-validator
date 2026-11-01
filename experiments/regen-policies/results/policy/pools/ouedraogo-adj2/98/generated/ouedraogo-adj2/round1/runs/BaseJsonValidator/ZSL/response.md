###Test START##
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link BaseJsonValidator}.
 */
public class BaseJsonValidatorTest {

    private static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;

    /**
     * Minimal concrete implementation of {@link BaseJsonValidator} used for testing.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(JsonNode schemaNode, ValidationContext validationContext, boolean suppressSubSchemaRetrieval) {
            super(SchemaLocation.DOCUMENT,
                    new JsonNodePath(PathType.LEGACY),
                    schemaNode,
                    null,
                    ValidatorTypeCode.TYPE,
                    validationContext);
            // Override the flag for fetchSubSchemaNode tests
            this.suppressSubSchemaRetrieval = suppressSubSchemaRetrieval;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node,
                                               JsonNode rootNode, JsonNodePath instanceLocation) {
            return Collections.emptySet();
        }
    }

    /**
     * Simple {@link Logger} implementation that records whether {@code debug} was invoked.
     */
    private static class SimpleLogger implements Logger {
        private final List<String> messages = new ArrayList<>();
        private final boolean debugEnabled;

        SimpleLogger(boolean debugEnabled) {
            this.debugEnabled = debugEnabled;
        }

        List<String> getMessages() {
            return messages;
        }

        @Override public String getName() { return "SimpleLogger"; }
        @Override public boolean isTraceEnabled() { return false; }
        @Override public void trace(String msg) {}
        @Override public void trace(String format, Object arg) {}
        @Override public void trace(String format, Object arg1, Object arg2) {}
        @Override public void trace(String format, Object... arguments) {}
        @Override public void trace(String msg, Throwable t) {}
        @Override public boolean isTraceEnabled(org.slf4j.Marker marker) { return false; }
        @Override public void trace(org.slf4j.Marker marker, String msg) {}
        @Override public void trace(org.slf4j.Marker marker, String format, Object arg) {}
        @Override public void trace(org.slf4j.Marker marker, String format, Object arg1, Object arg2) {}
        @Override public void trace(org.slf4j.Marker marker, String format, Object... argArray) {}
        @Override public void trace(org.slf4j.Marker marker, String msg, Throwable t) {}
        @Override public boolean isDebugEnabled() { return debugEnabled; }
        @Override public void debug(String msg) { messages.add(msg); }
        @Override public void debug(String format, Object arg) { messages.add(String.format(format, arg)); }
        @Override public void debug(String format, Object arg1, Object arg2) { messages.add(String.format(format, arg1, arg2)); }
        @Override public void debug(String format, Object... arguments) { messages.add(String.format(format, arguments)); }
        @Override public void debug(String msg, Throwable t) { messages.add(msg); }
        @Override public boolean isDebugEnabled(org.slf4j.Marker marker) { return debugEnabled; }
        @Override public void debug(org.slf4j.Marker marker, String msg) {}
        @Override public void debug(org.slf4j.Marker marker, String format, Object arg) {}
        @Override public void debug(org.slf4j.Marker marker, String format, Object arg1, Object arg2) {}
        @Override public void debug(org.slf4j.Marker marker, String format, Object... arguments) {}
        @Override public void debug(org.slf4j.Marker marker, String msg, Throwable t) {}
        @Override public boolean isInfoEnabled() { return false; }
        @Override public void info(String msg) {}
        @Override public void info(String format, Object arg) {}
        @Override public void info(String format, Object arg1, Object arg2) {}
        @Override public void info(String format, Object... arguments) {}
        @Override public void info(String msg, Throwable t) {}
        @Override public boolean isInfoEnabled(org.slf4j.Marker marker) { return false; }
        @Override public void info(org.slf4j.Marker marker, String msg) {}
        @Override public void info(org.slf4j.Marker marker, String format, Object arg) {}
        @Override public void info(org.slf4j.Marker marker, String format, Object arg1, Object arg2) {}
        @Override public void info(org.slf4j.Marker marker, String format, Object... arguments) {}
        @Override public void info(org.slf4j.Marker marker, String msg, Throwable t) {}
        @Override public boolean isWarnEnabled() { return false; }
        @Override public void warn(String msg) {}
        @Override public void warn(String format, Object arg) {}
        @Override public void warn(String format, Object... arguments) {}
        @Override public void warn(String format, Object arg1, Object arg2) {}
        @Override public void warn(String msg, Throwable t) {}
        @Override public boolean isWarnEnabled(org.slf4j.Marker marker) { return false; }
        @Override public void warn(org.slf4j.Marker marker, String msg) {}
        @Override public void warn(org.slf4j.Marker marker, String format, Object arg) {}
        @Override public void warn(org.slf4j.Marker marker, String format, Object arg1, Object arg2) {}
        @Override public void warn(org.slf4j.Marker marker, String format, Object... arguments) {}
        @Override public void warn(org.slf4j.Marker marker, String msg, Throwable t) {}
        @Override public boolean isErrorEnabled() { return false; }
        @Override public void error(String msg) {}
        @Override public void error(String format, Object arg) {}
        @Override public void error(String format, Object arg1, Object arg2) {}
        @Override public void error(String format, Object... arguments) {}
        @Override public void error(String msg, Throwable t) {}
        @Override public boolean isErrorEnabled(org.slf4j.Marker marker) { return false; }
        @Override public void error(org.slf4j.Marker marker, String msg) {}
        @Override public void error(org.slf4j.Marker marker, String format, Object arg) {}
        @Override public void error(org.slf4j.Marker marker, String format, Object arg1, Object arg2) {}
        @Override public void error(org.slf4j.Marker marker, String format, Object... arguments) {}
        @Override public void error(org.slf4j.Marker marker, String msg, Throwable t) {}
    }

    /**
     * Simple {@link JsonSchema} subclass that allows us to inject a custom
     * {@link JsonNode} as its schema definition.
     */
    private static class SimpleJsonSchema extends JsonSchema {
        SimpleJsonSchema(JsonNode schemaNode) {
            super(
                    Collections.emptyList(),          // validators
                    false,                           // validatorsLoaded
                    false,                           // recursiveAnchor
                    null,                            // typeValidator
                    null,                            // id
                    false,                           // suppressSubSchemaRetrieval
                    schemaNode,                      // schemaNode
                    null,                            // validationContext
                    null,                            // errorMessageType
                    null,                            // errorMessageKeyword
                    null,                            // messageSource
                    null,                            // keyword
                    null,                            // parentSchema
                    SchemaLocation.DOCUMENT,         // schemaLocation
                    new JsonNodePath(PathType.LEGACY), // evaluationPath
                    null,                            // evaluationParentSchema
                    null);                           // errorMessage
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode jsonNode, JsonNode rootNode, JsonNodePath instanceLocation) {
            return Collections.emptySet();
        }
    }

    private JsonNode dummyNode;
    private JsonNodePath dummyPath;

    @Before
    public void setUp() {
        dummyNode = FACTORY.objectNode();
        dummyPath = new JsonNodePath(PathType.LEGACY);
    }

    @Test
    public void testEqualsNearZero() {
        assertTrue(BaseJsonValidator.equals(1.000000000001, 1.000000000002));
        assertFalse(BaseJsonValidator.equals(1.0, 1.1));
    }

    @Test
    public void testDebugDoesNotThrowWhenDebugDisabled() {
        SimpleLogger logger = new SimpleLogger(false);
        ExecutionContext ctx = new ExecutionContext(); // default config – debug disabled
        BaseJsonValidator.debug(logger, ctx, dummyNode, dummyNode, dummyPath);
        // No messages should be recorded
        assertTrue(logger.getMessages().isEmpty());
    }

    @Test
    public void testCheckDiscriminatorMatchNullPropertyValueMarksIgnore() {
        DiscriminatorContext ctx = new DiscriminatorContext();
        ObjectNode discriminator = FACTORY.objectNode();
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, null, new SimpleJsonSchema(FACTORY.objectNode()));
        assertTrue(ctx.isDiscriminatorIgnore());
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingMatch() {
        DiscriminatorContext ctx = new DiscriminatorContext();
        ObjectNode mapping = FACTORY.objectNode();
        mapping.set("cat", FACTORY.textNode("#/components/schemas/Cat"));
        ObjectNode discriminator = FACTORY.objectNode();
        discriminator.set("mapping", mapping);

        // Schema whose fragment is "/components/schemas/Cat"
        JsonSchema schema = new SimpleJsonSchema(FACTORY.objectNode());
        // Manually set its location fragment (using reflection for test brevity)
        // In practice the fragment is part of the SchemaLocation; we construct a suitable location:
        SchemaLocation loc = SchemaLocation.of("#/components/schemas/Cat");
        // Use a subclass that exposes the location
        JsonSchema testSchema = new SimpleJsonSchema(FACTORY.objectNode()) {
            @Override public SchemaLocation getSchemaLocation() { return loc; }
        };

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "cat", testSchema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchImplicitWhenNoExplicitAndNoOverride() {
        DiscriminatorContext ctx = new DiscriminatorContext();
        ObjectNode mapping = FACTORY.objectNode();
        mapping.set("dog", FACTORY.textNode("#/schemas/Dog"));
        ObjectNode discriminator = FACTORY.objectNode();
        discriminator.set("mapping", mapping);

        // Parent schema fragment is "/schemas/Parent"
        JsonSchema parentSchema = new SimpleJsonSchema(FACTORY.objectNode()) {
            @Override public SchemaLocation getSchemaLocation() { return SchemaLocation.of("#/schemas/Parent"); }
        };

        // Schema fragment "/schemas/cat"
        JsonSchema schema = new SimpleJsonSchema(FACTORY.objectNode()) {
            @Override public SchemaLocation getSchemaLocation() { return SchemaLocation.of("#/schemas/cat"); }
        };

        // No explicit override for "cat" in mapping, so implicit match should be recorded
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "cat", schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchNoImplicitWhenExplicitOverrideExists() {
        DiscriminatorContext ctx = new DiscriminatorContext();
        ObjectNode mapping = FACTORY.objectNode();
        // explicit mapping key "cat" points to parent schema location -> override
        mapping.set("cat", FACTORY.textNode("#/schemas/Parent"));
        ObjectNode discriminator = FACTORY.objectNode();
        discriminator.set("mapping", mapping);

        JsonSchema parentSchema = new SimpleJsonSchema(FACTORY.objectNode()) {
            @Override public SchemaLocation getSchemaLocation() { return SchemaLocation.of("#/schemas/Parent"); }
        };
        JsonSchema schema = new SimpleJsonSchema(FACTORY.objectNode()) {
            @Override public SchemaLocation getSchemaLocation() { return SchemaLocation.of("#/schemas/cat"); }
        };

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "cat", schema);
        // Because of the explicit override, implicit match should NOT be performed
        assertFalse(ctx.isDiscriminatorMatchFound());
    }

    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminatorThrowsWhenPropertyNameRedefined() {
        DiscriminatorContext ctx = new DiscriminatorContext();
        ObjectNode rootDiscriminator = FACTORY.objectNode(); // empty root

        // Schema with its own discriminator containing a propertyName (illegal)
        ObjectNode schemaDiscriminator = FACTORY.objectNode();
        schemaDiscriminator.set("propertyName", FACTORY.textNode("type"));
        JsonNode schemaNode = FACTORY.objectNode().set("discriminator", schemaDiscriminator);
        JsonSchema schema = new SimpleJsonSchema(schemaNode) {
            @Override public SchemaLocation getSchemaLocation() {
                return SchemaLocation.of("#/schemas/Child");
            }
        };

        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema,
                new JsonNodePath(PathType.LEGACY));
    }

    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminatorThrowsOnMappingRedefinitionConflict() {
        DiscriminatorContext ctx = new DiscriminatorContext();

        // Root discriminator with mapping a -> #/A
        ObjectNode rootMapping = FACTORY.objectNode();
        rootMapping.set("a", FACTORY.textNode("#/A"));
        ObjectNode rootDiscriminator = FACTORY.objectNode();
        rootDiscriminator.set("mapping", rootMapping);

        // Child schema with conflicting mapping a -> #/B
        ObjectNode childMapping = FACTORY.objectNode();
        childMapping.set("a", FACTORY.textNode("#/B"));
        ObjectNode childDiscriminator = FACTORY.objectNode();
        childDiscriminator.set("mapping", childMapping);
        JsonNode childNode = FACTORY.objectNode().set("discriminator", childDiscriminator);
        JsonSchema childSchema = new SimpleJsonSchema(childNode) {
            @Override public SchemaLocation getSchemaLocation() {
                return SchemaLocation.of("#/schemas/Child");
            }
        };

        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, childSchema,
                new JsonNodePath(PathType.LEGACY));
    }

    @Test
    public void testRegisterAndMergeDiscriminatorMergesMappingsSuccessfully() {
        DiscriminatorContext ctx = new DiscriminatorContext();

        // Root discriminator without mapping
        ObjectNode rootDiscriminator = FACTORY.objectNode();

        // Child schema with a mapping entry
        ObjectNode childMapping = FACTORY.objectNode();
        childMapping.set("b", FACTORY.textNode("#/B"));
        ObjectNode childDiscriminator = FACTORY.objectNode();
        childDiscriminator.set("mapping", childMapping);
        JsonNode childNode = FACTORY.objectNode().set("discriminator", childDiscriminator);
        JsonSchema childSchema = new SimpleJsonSchema(childNode) {
            @Override public SchemaLocation getSchemaLocation() {
                return SchemaLocation.of("#/schemas/Child");
            }
        };

        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, childSchema,
                new JsonNodePath(PathType.LEGACY));

        // After merging, rootDiscriminator should now contain the child's mapping
        ObjectNode mergedMapping = (ObjectNode) rootDiscriminator.get("mapping");
        assertNotNull(mergedMapping);
        assertEquals("#/B", mergedMapping.get("b").asText());
    }

    @Test
    public void testFetchSubSchemaNodeReturnsNullWhenSuppressedOrNoId() {
        // suppress flag true – should return null irrespective of schema content
        TestValidator validatorSuppressed = new TestValidator(FACTORY.objectNode(), null, true);
        assertNull(validatorSuppressed.fetchSubSchemaNode(null));

        // suppress false but schema has no "id" field – still null
        TestValidator validatorNoId = new TestValidator(FACTORY.objectNode(), null, false);
        assertNull(validatorNoId.fetchSubSchemaNode(null));
    }

    @Test
    public void testGetNodeFieldTypeReturnsParentType() {
        // Parent schema with a "type" field
        ObjectNode parentSchemaNode = FACTORY.objectNode();
        parentSchemaNode.put("type", "object");
        JsonSchema parentSchema = new SimpleJsonSchema(parentSchemaNode);

        // Validator whose getParentSchema returns the above parent
        TestValidator validator = new TestValidator(FACTORY.objectNode(), null, true) {
            @Override public JsonSchema getParentSchema() {
                return parentSchema;
            }
        };

        assertEquals("object", validator.getNodeFieldType());
    }

    @Test
    public void testPreloadJsonSchemasInvokesInitializeValidators() {
        // Create two schemas that record whether initializeValidators was called
        class InitTrackingSchema extends SimpleJsonSchema {
            boolean initialized = false;
            InitTrackingSchema(JsonNode node) { super(node); }
            @Override public void initializeValidators() { initialized = true; }
        }

        InitTrackingSchema s1 = new InitTrackingSchema(FACTORY.objectNode());
        InitTrackingSchema s2 = new InitTrackingSchema(FACTORY.objectNode());

        TestValidator validator = new TestValidator(FACTORY.objectNode(), null, true);
        validator.preloadJsonSchemas(Arrays.asList(s1, s2));

        assertTrue(s1.initialized);
        assertTrue(s2.initialized);
    }

    @Test
    public void testToStringReturnsLastPathSegment() {
        JsonNodePath path = new JsonNodePath(PathType.LEGACY).append("level1").append("level2");
        TestValidator validator = new TestValidator(FACTORY.objectNode(), null, true) {
            @Override public JsonNodePath getEvaluationPath() {
                return path;
            }
        };
        // getName(-1) returns the last segment ("level2")
        assertEquals("level2", validator.toString());
    }

    @Test
    public void testHasAdjacentKeywordInEvaluationPathDetectsKeyword() {
        // Create a dummy validator with keyword "test-key"
        class DummyValidator extends BaseJsonValidator {
            DummyValidator(JsonNode schemaNode) {
                super(SchemaLocation.DOCUMENT, new JsonNodePath(PathType.LEGACY), schemaNode,
                        null, ValidatorTypeCode.TYPE, null);
            }
            @Override public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node,
                                                             JsonNode rootNode, JsonNodePath instanceLocation) {
                return Collections.emptySet();
            }
        }

        // Parent schema containing the dummy validator
        JsonSchema parent = new SimpleJsonSchema(FACTORY.objectNode()) {
            @Override public List<JsonValidator> getValidators() {
                return Collections.singletonList(new DummyValidator(FACTORY.objectNode()));
            }
        };

        // Validator under test whose evaluation parent schema is the above parent
        TestValidator validator = new TestValidator(FACTORY.objectNode(), null, true) {
            @Override public JsonSchema getEvaluationParentSchema() {
                return parent;
            }
        };

        // The dummy validator's keyword is "type" (from ValidatorTypeCode.TYPE). We check for that.
        assertTrue(validator.hasAdjacentKeywordInEvaluationPath("type"));
        // A non‑existent keyword should return false
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("nonexistent"));
    }
}
```
###Test END##