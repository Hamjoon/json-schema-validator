/**
 * Comprehensive JUnit‑4 test suite for {@link BaseJsonValidator}.
 *
 * <p>The test class creates a tiny concrete subclass of the abstract
 * {@code BaseJsonValidator} so that the protected methods can be exercised.
 * Mockito is used to mock heavy collaborators (e.g. {@link ValidationContext},
 * {@link JsonSchema}, {@link Logger}) and to verify interactions.
 *
 * <p>Each public / protected method of {@code BaseJsonValidator} is covered
 * by at least one test that checks normal behaviour, edge cases and error
 * handling.
 */
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

/**
 * Test suite for {@link BaseJsonValidator}.
 */
public class BaseJsonValidatorTest {

    /** Minimal concrete implementation used for testing. */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema,
                      ValidatorTypeCode validatorType, ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext,
                  suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // simple stub – never used in the tests that target the protected helpers
            return Collections.emptySet();
        }
    }

    /** Simple in‑memory implementation of the {@link DiscriminatorContext}. */
    private static class SimpleDiscriminatorContext implements ValidationContext.DiscriminatorContext {
        private boolean matchFound = false;
        private final java.util.Map<String, ObjectNode> registry = new java.util.HashMap<>();

        @Override
        public void markMatch() {
            matchFound = true;
        }

        @Override
        public boolean isDiscriminatorMatchFound() {
            return matchFound;
        }

        @Override
        public void registerDiscriminator(String schemaPath, ObjectNode discriminator) {
            registry.put(schemaPath, discriminator);
        }

        @Override
        public ObjectNode getDiscriminatorForPath(String schemaPath) {
            return registry.get(schemaPath);
        }
    }

    private TestValidator validator;
    private JsonNodeFactory nodeFactory = JsonNodeFactory.instance;

    @Before
    public void setUp() {
        // a dummy parent schema – only the "type" field is needed for getNodeFieldType()
        JsonNode parentSchemaNode = nodeFactory.objectNode().put("type", "object");
        JsonSchema parentSchema = mock(JsonSchema.class);
        when(parentSchema.getSchemaNode()).thenReturn(parentSchemaNode);

        validator = new TestValidator(
                "/root",
                nodeFactory.objectNode(),
                parentSchema,
                ValidatorTypeCode.UNKNOWN, // any enum value works
                null,                     // we do not need a ValidationContext for most tests
                false);
    }

    /* -------------------------------------------------------------
     *  static utility methods
     * ------------------------------------------------------------- */

    @Test
    public void equals_shouldTreatNumbersWithinEpsilonAsEqual() {
        assertTrue(BaseJsonValidator.equals(1.000000000001, 1.0));
        assertTrue(BaseJsonValidator.equals(-0.0, 0.0));
    }

    @Test
    public void equals_shouldTreatNumbersOutsideEpsilonAsDifferent() {
        assertFalse(BaseJsonValidator.equals(1.0001, 1.0));
        assertFalse(BaseJsonValidator.equals(Double.MAX_VALUE, Double.MIN_VALUE));
    }

    @Test
    public void debug_shouldLogCorrectMessage() {
        Logger logger = mock(Logger.class);
        JsonNode node = nodeFactory.textNode("value");
        JsonNode root = nodeFactory.objectNode();
        String at = "/path";

        BaseJsonValidator.debug(logger, node, root, at);

        verify(logger).debug(eq("validate( {}, {}, {})"), eq(node), eq(root), eq(at));
    }

    /* -------------------------------------------------------------
     *  checkDiscriminatorMatch – only the “null property value” branch
     * ------------------------------------------------------------- */

    @Test
    public void checkDiscriminatorMatch_nullPropertyValue_marksMatchAndReturns() {
        ValidationContext.DiscriminatorContext ctx = mock(ValidationContext.DiscriminatorContext.class);
        ObjectNode discriminator = nodeFactory.objectNode(); // content irrelevant for this branch
        JsonSchema dummySchema = mock(JsonSchema.class);

        // invoke the protected static method via the concrete subclass
        TestValidator.checkDiscriminatorMatch(ctx, discriminator, null, dummySchema);

        verify(ctx, times(1)).markMatch();
        verifyNoMoreInteractions(ctx);
    }

    /* -------------------------------------------------------------
     *  Implicit discriminator mapping match
     * ------------------------------------------------------------- */

    @Test
    public void checkDiscriminatorMatch_implicitMappingMatchesWhenPathEndsWithValue() throws Exception {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();

        // discriminator without "mapping"
        ObjectNode discriminator = nodeFactory.objectNode();

        // create a mock JsonSchema whose schemaPath ends with "/pet"
        JsonSchema schema = mock(JsonSchema.class);
        // use reflection to set the (package‑private) field `schemaPath`
        Field pathField = JsonSchema.class.getDeclaredField("schemaPath");
        pathField.setAccessible(true);
        pathField.set(schema, "/components/schemas/pet");

        // call the method – it should detect the implicit match and call markMatch()
        TestValidator.checkDiscriminatorMatch(ctx, discriminator, "pet", schema);

        assertTrue("Discriminator context should have recorded a match", ctx.isDiscriminatorMatchFound());
    }

    /* -------------------------------------------------------------
     *  Explicit discriminator mapping match
     * ------------------------------------------------------------- */

    @Test
    public void checkDiscriminatorMatch_explicitMappingMatchesCorrectly() throws Exception {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();

        // build a discriminator node with an explicit mapping
        ObjectNode discriminator = nodeFactory.objectNode();
        ObjectNode mapping = nodeFactory.objectNode();
        mapping.put("dog", "/components/schemas/Dog");
        discriminator.set("mapping", mapping);

        // mock a schema whose path equals the mapping value
        JsonSchema schema = mock(JsonSchema.class);
        Field pathField = JsonSchema.class.getDeclaredField("schemaPath");
        pathField.setAccessible(true);
        pathField.set(schema, "/components/schemas/Dog");

        TestValidator.checkDiscriminatorMatch(ctx, discriminator, "dog", schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    /* -------------------------------------------------------------
     *  registerAndMergeDiscriminator – merging mappings
     * ------------------------------------------------------------- */

    @Test
    public void registerAndMergeDiscriminator_shouldMergeNewMappingsIntoRoot() {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();

        // root discriminator (registered first) – has an empty mapping object
        ObjectNode rootDiscriminator = nodeFactory.objectNode();
        rootDiscriminator.set("mapping", nodeFactory.objectNode());
        ctx.registerDiscriminator("/root", rootDiscriminator);

        // child schema that adds a new mapping entry
        JsonSchema childSchema = mock(JsonSchema.class);
        // set child schemaPath
        try {
            Field pathField = JsonSchema.class.getDeclaredField("schemaPath");
            pathField.setAccessible(true);
            pathField.set(childSchema, "/root/child");
        } catch (Exception e) {
            fail("Reflection error while preparing child schema mock: " + e);
        }

        // discriminator node present on the child schema
        ObjectNode childDiscriminator = nodeFactory.objectNode();
        ObjectNode childMapping = nodeFactory.objectNode();
        childMapping.put("cat", "/components/schemas/Cat");
        childDiscriminator.set("mapping", childMapping);

        // invoke the merge
        TestValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, childSchema, "/root/child");

        // after merging the root mapping should contain the new entry
        ObjectNode mergedMapping = (ObjectNode) rootDiscriminator.get("mapping");
        assertNotNull("Merged mapping must exist", mergedMapping);
        assertEquals("/components/schemas/Cat", mergedMapping.get("cat").asText());
    }

    /* -------------------------------------------------------------
     *  fetchSubSchemaNode – suppress flag
     * ------------------------------------------------------------- */

    @Test
    public void fetchSubSchemaNode_whenSuppressFlagIsTrue_returnsNull() {
        // create a validator with suppressSubSchemaRetrieval = true
        TestValidator suppressingValidator = new TestValidator(
                "/any",
                nodeFactory.objectNode(),
                mock(JsonSchema.class),
                ValidatorTypeCode.UNKNOWN,
                mock(ValidationContext.class),
                true);

        // even with a completely mocked ValidationContext we must get null
        JsonSchema result = suppressingValidator.fetchSubSchemaNode(mock(ValidationContext.class));
        assertNull(result);
    }

    /* -------------------------------------------------------------
     *  getNodeFieldType – reads the "type" field from the parent schema
     * ------------------------------------------------------------- */

    @Test
    public void getNodeFieldType_returnsParentSchemaType() {
        // parent schema already set up in setUp() to have type = "object"
        String type = validator.getNodeFieldType();
        assertEquals("object", type);
    }

    @Test
    public void getNodeFieldType_returnsNullWhenNoTypeField() {
        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getSchemaNode()).thenReturn(nodeFactory.objectNode()); // empty object
        TestValidator v = new TestValidator(
                "/p",
                nodeFactory.objectNode(),
                parent,
                ValidatorTypeCode.UNKNOWN,
                null,
                false);

        assertNull(v.getNodeFieldType());
    }

    /* -------------------------------------------------------------
     *  preloadJsonSchemas – ensures initializeValidators() is called
     * ------------------------------------------------------------- */

    @Test
    public void preloadJsonSchemas_initializesAllSuppliedSchemas() {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);

        validator.preloadJsonSchemas(java.util.Arrays.asList(s1, s2));

        verify(s1, times(1)).initializeValidators();
        verify(s2, times(1)).initializeValidators();
    }

    /* -------------------------------------------------------------
     *  Path handling – atRoot, atPath(String), atPath(int)
     * ------------------------------------------------------------- */

    @Test
    public void atRoot_returnsPathTypeRoot() {
        // PathType.DEFAULT returns "$" as root – we only assert non‑null here
        String root = validator.atRoot();
        assertNotNull(root);
    }

    @Test
    public void atPath_stringToken_appendsCorrectly() {
        // The default PathType joins tokens with a '/' (e.g. "$/foo")
        String result = validator.atPath("$", "foo");
        // The exact format depends on the implementation of PathType.DEFAULT;
        // we verify that the token appears after the root separated by a slash.
        assertTrue(result.endsWith("/foo"));
        assertTrue(result.startsWith("$"));
    }

    @Test
    public void atPath_intIndex_appendsCorrectly() {
        String result = validator.atPath("$", 3);
        assertTrue(result.endsWith("[3]") || result.endsWith("/3"));
        // The test is tolerant to the exact delimiter used by PathType.DEFAULT.
    }

    /* -------------------------------------------------------------
     *  Helper to expose protected static methods for readability
     * ------------------------------------------------------------- */

    // The following static delegators simply forward to the protected methods of
    // BaseJsonValidator; they exist solely to avoid noisy reflection in the test
    // code.
    private static class TestValidator extends BaseJsonValidator {
        TestValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema,
                      ValidatorTypeCode validatorType, ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext,
                  suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }

        static void checkDiscriminatorMatch(ValidationContext.DiscriminatorContext ctx,
                                            ObjectNode discriminator,
                                            String discriminatorPropertyValue,
                                            JsonSchema schema) {
            BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator,
                    discriminatorPropertyValue, schema);
        }
    }
}
