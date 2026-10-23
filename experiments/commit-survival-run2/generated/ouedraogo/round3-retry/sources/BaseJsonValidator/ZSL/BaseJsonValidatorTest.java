package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.ValidationContext.DiscriminatorContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BaseJsonValidator}.  The class under test is abstract,
 * therefore a minimal concrete implementation {@link TestValidator} is provided
 * for exercising the protected and public behaviour.
 */
@RunWith(MockitoJUnitRunner.class)
public class BaseJsonValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestValidator validator;

    @Mock
    private ValidationContext validationContextMock;

    @Mock
    private Logger loggerMock;

    @Before
    public void setUp() {
        // a trivial schema node – the content is irrelevant for most tests
        JsonNode schemaNode = MAPPER.createObjectNode();
        validator = new TestValidator("#", schemaNode, null,
                ValidatorTypeCode.TYPE, validationContextMock, false);
    }

    /**
     * Minimal concrete subclass of {@link BaseJsonValidator}.  The {@code validate}
     * method simply returns an empty {@link Set}.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // No validation logic – used only to test BaseJsonValidator plumbing.
            return Collections.emptySet();
        }
    }

    /* -------------------------------------------------
     *  Tests for static helper methods
     * ------------------------------------------------- */

    @Test
    public void testEqualsWithinTolerance() {
        assertTrue(BaseJsonValidator.equals(0.1 + 0.2, 0.3));
        assertTrue(BaseJsonValidator.equals(1.0, 1.0 + 1e-13));
        assertFalse(BaseJsonValidator.equals(1.0, 1.0 + 1e-10));
    }

    @Test
    public void testDebugLogsCorrectMessage() {
        JsonNode node = MAPPER.createObjectNode().put("a", 1);
        JsonNode root = MAPPER.createObjectNode();
        BaseJsonValidator.debug(loggerMock, node, root, "$.path");

        verify(loggerMock).debug(eq("validate( {}, {}, {})"), eq(node), eq(root), eq("$.path"));
    }

    /* -------------------------------------------------
     *  Tests for instance methods dealing with schema path & type
     * ------------------------------------------------- */

    @Test
    public void testGetSchemaPath() {
        assertEquals("#", validator.getSchemaPath());
    }

    @Test
    public void testGetSchemaNode() {
        assertNotNull(validator.getSchemaNode());
    }

    @Test
    public void testGetParentSchema() {
        // parent schema was supplied as null in setUp()
        assertNull(validator.getParentSchema());
    }

    @Test
    public void testFetchSubSchemaNodeWhenSuppressed() {
        TestValidator suppressed = new TestValidator("#", MAPPER.createObjectNode(),
                null, ValidatorTypeCode.TYPE, validationContextMock, true);
        assertNull(suppressed.fetchSubSchemaNode(validationContextMock));
    }

    @Test
    public void testFetchSubSchemaNodeWhenNoId() {
        // schemaNode without an "id" property → obtainSubSchemaNode returns null
        assertNull(validator.fetchSubSchemaNode(validationContextMock));
    }

    @Test
    public void testGetNodeFieldType() {
        // Build a parent schema that contains a "type" field
        JsonNode parentSchemaNode = MAPPER.createObjectNode().put("type", "object");
        JsonSchema parentSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
                .getSchema(parentSchemaNode);
        TestValidator withParent = new TestValidator("#", MAPPER.createObjectNode(),
                parentSchema, ValidatorTypeCode.TYPE, validationContextMock, false);

        assertEquals("object", withParent.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldTypeWhenMissing() {
        // parent schema without a "type" field
        JsonSchema parentSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
                .getSchema(MAPPER.createObjectNode());
        TestValidator withParent = new TestValidator("#", MAPPER.createObjectNode(),
                parentSchema, ValidatorTypeCode.TYPE, validationContextMock, false);

        assertNull(withParent.getNodeFieldType());
    }

    @Test
    public void testPreloadJsonSchemas() {
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);
        validator.preloadJsonSchemas(Set.of(schema1, schema2));

        verify(schema1).initializeValidators();
        verify(schema2).initializeValidators();
    }

    @Test
    public void testPathTypeAndAtMethods() {
        // default PathType is PathType.DEFAULT ("/" as root, "." as separator)
        assertEquals("/", validator.getPathType().getRoot());

        String root = validator.atRoot();
        assertEquals("/", root);

        String child = validator.atPath(root, "foo");
        assertEquals("/foo", child);

        String indexed = validator.atPath(root, 2);
        assertEquals("/2", indexed);
    }

    /* -------------------------------------------------
     *  Tests for discriminator handling
     * ------------------------------------------------- */

    /**
     * Simple implementation of {@link ValidationContext.DiscriminatorContext}
     * that records matches and stored discriminators.
     */
    private static class SimpleDiscriminatorContext implements DiscriminatorContext {

        private boolean matchFound = false;
        private final java.util.Map<String, ObjectNode> registry = new java.util.HashMap<>();

        @Override
        public void markMatch() {
            this.matchFound = true;
        }

        @Override
        public boolean isDiscriminatorMatchFound() {
            return matchFound;
        }

        @Override
        public ObjectNode getDiscriminatorForPath(String path) {
            return registry.get(path);
        }

        @Override
        public void registerDiscriminator(String path, ObjectNode discriminator) {
            registry.put(path, discriminator);
        }
    }

    @Test
    public void testCheckDiscriminatorMatch_nullDiscriminatorPropertyValue_marksMatch() {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode();
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, null, mock(JsonSchema.class));
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatch_implicitMapping_matchByPath() {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode(); // no "mapping"
        // schema whose path ends with the discriminator property value
        JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
                .getSchema(MAPPER.createObjectNode());
        // Force the schemaPath to a known value using reflection
        try {
            java.lang.reflect.Field pathField = JsonSchema.class.getDeclaredField("schemaPath");
            pathField.setAccessible(true);
            pathField.set(schema, "#/myType");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "myType", schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatch_explicitMapping_matchFound() {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode();
        ObjectNode mapping = MAPPER.createObjectNode();
        mapping.put("dog", "#/DogSchema");
        discriminator.set("mapping", mapping);

        JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
                .getSchema(MAPPER.createObjectNode());
        try {
            java.lang.reflect.Field pathField = JsonSchema.class.getDeclaredField("schemaPath");
            pathField.setAccessible(true);
            pathField.set(schema, "#/DogSchema");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "dog", schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatch_explicitMapping_noMatch_thenImplicitMatch() {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode();
        ObjectNode mapping = MAPPER.createObjectNode();
        // mapping does NOT contain the searched key
        mapping.put("cat", "#/CatSchema");
        discriminator.set("mapping", mapping);

        JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
                .getSchema(MAPPER.createObjectNode());
        try {
            java.lang.reflect.Field pathField = JsonSchema.class.getDeclaredField("schemaPath");
            pathField.setAccessible(true);
            pathField.set(schema, "#/myAnimal");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // discriminatorPropertyValue = "myAnimal" – not present in explicit mapping,
        // but no explicit mapping overrides the parent path, so implicit match should fire.
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "myAnimal", schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatch_explicitMapping_noMatchAndOverridePreventsImplicit() {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode();
        ObjectNode mapping = MAPPER.createObjectNode();
        // mapping contains a key that points to the SAME path as the parent schema,
        // therefore implicit matching must be suppressed.
        mapping.put("myAnimal", "#/ParentPath");
        discriminator.set("mapping", mapping);

        // Parent schema (the one passed as argument) will have the same path.
        JsonSchema parentSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
                .getSchema(MAPPER.createObjectNode());
        try {
            java.lang.reflect.Field pathField = JsonSchema.class.getDeclaredField("schemaPath");
            pathField.setAccessible(true);
            pathField.set(parentSchema, "#/ParentPath");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "myAnimal", parentSchema);
        // No match should be recorded
        assertFalse(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testRegisterAndMergeDiscriminator_newMappingAdded() {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();

        // Root discriminator (already registered)
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        rootDiscriminator.set("mapping", MAPPER.createObjectNode());
        ctx.registerDiscriminator("#", rootDiscriminator);

        // Schema that adds a new mapping entry
        ObjectNode schemaDiscriminator = MAPPER.createObjectNode();
        ObjectNode schemaMapping = MAPPER.createObjectNode();
        schemaMapping.put("newKey", "#/NewSchema");
        schemaDiscriminator.set("mapping", schemaMapping);

        JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
                .getSchema(MAPPER.createObjectNode());
        try {
            java.lang.reflect.Field pathField = JsonSchema.class.getDeclaredField("schemaPath");
            pathField.setAccessible(true);
            pathField.set(schema, "#/NewSchema");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // No "propertyName" on the nested discriminator – merge should succeed
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, "$");
        ObjectNode merged = ctx.getDiscriminatorForPath("#");
        assertNotNull(merged);
        assertTrue(merged.get("mapping").has("newKey"));
        assertEquals("#/NewSchema", merged.get("mapping").get("newKey").asText());
    }

    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminator_propertyNameRedefinitionThrows() {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();

        // Root discriminator without propertyName
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        rootDiscriminator.set("mapping", MAPPER.createObjectNode());
        ctx.registerDiscriminator("#", rootDiscriminator);

        // Schema discriminator that defines a propertyName – this should trigger an exception
        ObjectNode schemaDiscriminator = MAPPER.createObjectNode();
        schemaDiscriminator.put("propertyName", "type");
        JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
                .getSchema(MAPPER.createObjectNode());
        try {
            java.lang.reflect.Field pathField = JsonSchema.class.getDeclaredField("schemaPath");
            pathField.setAccessible(true);
            pathField.set(schema, "#/SomeSchema");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // The call is expected to throw JsonSchemaException
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, "$");
    }

    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminator_mappingRedefinitionThrows() {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();

        // Root discriminator with an existing mapping entry
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        ObjectNode rootMapping = MAPPER.createObjectNode();
        rootMapping.put("conflictKey", "#/Original");
        rootDiscriminator.set("mapping", rootMapping);
        ctx.registerDiscriminator("#", rootDiscriminator);

        // Schema discriminator that attempts to redefine the same key with a different value
        ObjectNode schemaDiscriminator = MAPPER.createObjectNode();
        ObjectNode schemaMapping = MAPPER.createObjectNode();
        schemaMapping.put("conflictKey", "#/Different");
        schemaDiscriminator.set("mapping", schemaMapping);

        JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
                .getSchema(MAPPER.createObjectNode());
        try {
            java.lang.reflect.Field pathField = JsonSchema.class.getDeclaredField("schemaPath");
            pathField.setAccessible(true);
            pathField.set(schema, "#/Different");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // This should throw because the mapping key is being redefined
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, "$");
    }
}
